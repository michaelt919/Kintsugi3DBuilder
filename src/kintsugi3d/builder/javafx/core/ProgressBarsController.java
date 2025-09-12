/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.core;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.UserCancellationException;
import kintsugi3d.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProgressBarsController
{
    private static final Logger LOG = LoggerFactory.getLogger(ProgressBarsController.class);
    private static ProgressBarsController INSTANCE;

    @FXML private Label localElapsedTimeLabel;
    @FXML private Label localEstimTimeRemainingLabel;

    @FXML private Label totalElapsedTimeLabel;

    @FXML private Label localTextLabel;
    @FXML private Label overallTextLabel;

    @FXML private ProgressBar overallProgressBar;
    @FXML private ProgressBar localProgressBar;

    @FXML private Button cancelButton;
    @FXML private Label pageCountLabel; //TODO: imp this, currently invisible
    @FXML private Button doneButton;

    private Stopwatch overallStopwatch;
    private Stopwatch localStopwatch;

    private String defaultLocalText;
    private String defaultOverallText;
    private String defaultTotalTimeElapsedText;
    private String defaultEstimLocalTimeRemainingTxt;
    private String defaultTitle;

    private Stage stage;
    private String defaultOverallElapsedTimeTxt;
    private final BooleanProperty processingProperty = new SimpleBooleanProperty(false);

    static ProgressBarsController getInstance()
    {
        return INSTANCE;
    }

    public void init(Stage stage)
    {
        this.stage = stage;
        defaultOverallText = overallTextLabel.getText();
        defaultLocalText = localTextLabel.getText();

        defaultTotalTimeElapsedText = totalElapsedTimeLabel.getText();
        defaultEstimLocalTimeRemainingTxt = localEstimTimeRemainingLabel.getText();
        defaultOverallElapsedTimeTxt = localElapsedTimeLabel.getText();

        defaultTitle = stage.getTitle();

        //remove estimated times from view if not processing (would just be 00:00:00/otherwise useless anyway)
        localEstimTimeRemainingLabel.managedProperty().bind(processingProperty);
        localEstimTimeRemainingLabel.visibleProperty().bind(processingProperty);

        overallStopwatch = new Stopwatch();
        localStopwatch = new Stopwatch();

        processingProperty.bind(overallStopwatch.isRunningProperty());

        localProgressBar.getScene().getWindow().setOnCloseRequest(
            event -> MainWindowController.getInstance().showMiniProgressBar());


        // Keep track of whether cancellation was requested.
        AtomicBoolean cancelRequested = new AtomicBoolean(false);
        cancelButton.setOnAction(event ->
        {
            cancelRequested.set(true);
            Platform.runLater(() -> cancelButton.setText("Cancelling..."));
        });

        doneButton.setOnAction(event -> hideAllProgress());

        cancelButton.disableProperty().bind(getProcessingProperty().not());
        doneButton.disableProperty().bind(getProcessingProperty());

        Global.state().getIOModel().addProgressMonitor(
            new Monitor(cancelRequested, localProgressBar, overallProgressBar, overallTextLabel, localTextLabel, cancelButton));

        INSTANCE = this;
    }

    public Stage getStage()
    {
        return stage;
    }

    public void resetText()
    {
        Platform.runLater(() ->
        {
            localTextLabel.setText(defaultLocalText);
            overallTextLabel.setText(defaultOverallText);

            totalElapsedTimeLabel.setText(defaultTotalTimeElapsedText);
            localElapsedTimeLabel.setText(defaultOverallElapsedTimeTxt);

            localEstimTimeRemainingLabel.setText(defaultEstimLocalTimeRemainingTxt);

            stage.setTitle(defaultTitle);
        });
    }

    public void showStage()
    {
        Platform.runLater(stage::show);
    }

    private void reset()
    {
        resetText();
        Platform.runLater(() ->
        {
            overallProgressBar.setProgress(0.0);
            localProgressBar.setProgress(0.0);
        });
    }

    public void hideStage()
    {
        Platform.runLater(() -> stage.hide());
    }

    public void startStopwatches()
    {
        Platform.runLater(() ->
        {
            overallStopwatch.start();
            localStopwatch.start();

            // Don't start the thread until the stopwatches have started or it will stop right away.
            new Thread(() ->
            {
                while (isProcessing())
                {
                    try
                    {
                        Thread.sleep(200);

                        if (isProcessing())
                        {
                            updateElapsedTime();
                        }

                        Thread.sleep(800);

                        tickDownRemainingTime();
                    }
                    catch (InterruptedException e)
                    {
                        break;
                    }
                }
            }).start();

            saturateProgressBars();
        });
    }

    private void tickDownRemainingTime()
    {
        String remainingTimeStr = localEstimTimeRemainingLabel.getText();
        if (remainingTimeStr.matches("[A-Za-z].*"))
        {
            //text is "Almost done..." or some other manually entered message
            return;
        }

        int hours = Integer.parseInt(remainingTimeStr.substring(0, 2));
        int minutes = Integer.parseInt(remainingTimeStr.substring(3, 5));
        int seconds = Integer.parseInt(remainingTimeStr.substring(6, 8));

        long estimatedRemaining = TimeUnit.HOURS.toNanos(hours) +
            TimeUnit.MINUTES.toNanos(minutes) +
            TimeUnit.SECONDS.toNanos(seconds);

        estimatedRemaining -= TimeUnit.SECONDS.toNanos(1);

        updateTimeEstimationLabel(estimatedRemaining);
    }

    private void updateTimeEstimationLabel(long estimatedRemaining)
    {
        long secondsRemaining = TimeUnit.NANOSECONDS.toSeconds(estimatedRemaining);

        if (secondsRemaining > 0)
        {
            String timeTxt = nanosecToFormatTime(estimatedRemaining, true);
            Platform.runLater(() -> localEstimTimeRemainingLabel.setText(timeTxt + " Remaining"));
        }
        else
        {
            Platform.runLater(() -> localEstimTimeRemainingLabel.setText("Almost done..."));
        }
    }

    public void clickStopwatches(double progress, double maximum)
    {
        Platform.runLater(() ->
        {
            overallStopwatch.click();
            localStopwatch.click();
            updateLocalRemainingTime(progress, maximum);
        });
    }

    private void updateLocalRemainingTime(double progress, double maximum)
    {
        long avgDif = localStopwatch.getAvgDifference();

        double remainingProcesses = maximum - progress;

        long estimatedRemaining = Math.max(0, (long) (avgDif * remainingProcesses));

        updateTimeEstimationLabel(estimatedRemaining);
    }


    public void updateElapsedTime()
    {
        long totalElapsedTime = overallStopwatch.getElapsedTime();
        long localElapsedTime = localStopwatch.getElapsedTime();

        String totalTimeTxt = nanosecToFormatTime(totalElapsedTime, true);
        String localTimeTxt = nanosecToFormatTime(localElapsedTime, true);

        if (isProcessing())
        {
            Platform.runLater(() -> totalElapsedTimeLabel.setText(totalTimeTxt + " Lapsed"));
            Platform.runLater(() -> localElapsedTimeLabel.setText("(" + localTimeTxt + " Lapsed)"));
        }
    }

    private static String nanosecToFormatTime(long nanoTime, boolean useSeconds)
    {
        long hours = TimeUnit.NANOSECONDS.toHours(nanoTime);

        long minutes = TimeUnit.NANOSECONDS.toMinutes(nanoTime) -
            TimeUnit.HOURS.toMinutes(hours);

        long seconds = TimeUnit.NANOSECONDS.toSeconds(nanoTime) -
            TimeUnit.HOURS.toSeconds(hours) -
            TimeUnit.MINUTES.toSeconds(minutes);

        return useSeconds ? String.format("%02d:%02d:%02d", hours, minutes, seconds) :
            String.format("%02d:%02d", hours, minutes);
    }


    public void endStopwatches()
    {
        Platform.runLater(() ->
        {
            overallStopwatch.stop();
            localStopwatch.stop();

            //remove parenthesis from elapsed times
            totalElapsedTimeLabel.setText(totalElapsedTimeLabel.getText().replace("(", "")
                .replace(")", ""));

            localElapsedTimeLabel.setText(localElapsedTimeLabel.getText().replace("(", "")
                .replace(")", ""));
        });

        desaturateProgressBars();
    }

    private void desaturateProgressBars()
    {
        overallProgressBar.getStyleClass().add("desaturated-progress-bar");
        localProgressBar.getStyleClass().add("desaturated-progress-bar");
    }

    private void saturateProgressBars()
    {
        overallProgressBar.getStyleClass().clear();
        localProgressBar.getStyleClass().clear();

        overallProgressBar.getStyleClass().add("progress-bar");
        localProgressBar.getStyleClass().add("progress-bar");
    }

    public void stopAndClose()
    {
        endStopwatches();
        hideStage();
    }

    public boolean isProcessing()
    {
        return processingProperty.getValue();
    }

    public void beginNewStage()
    {
        Platform.runLater(() ->
        {
            localStopwatch = new Stopwatch();
            localStopwatch.start();
        });
    }

    public ReadOnlyBooleanProperty getProcessingProperty()
    {
        return processingProperty;
    }

    private void hideAllProgress()
    {
        hideStage();
        MainWindowController.getInstance().dismissMiniProgressBarAsync();
    }

    private class Monitor implements ProgressMonitor
    {
        private final AtomicBoolean cancelRequested;
        private final ProgressBar localProgressBar;
        private final ProgressBar overallProgressBar;
        private final Label overallTextLabel;
        private final Label localTextLabel;
        private final Button cancelButton;
        private double maximum;
        private double localProgress;

        private double overallProgress;
        private final IntegerProperty stageCountProperty;
        private final IntegerProperty currentStageProperty;

        private String revertText; //when process is finishing up, store last msg into here while displaying "Finishing up..."

        public Monitor(AtomicBoolean cancelRequested, ProgressBar localProgressBar, ProgressBar overallProgressBar, Label overallTextLabel, Label localTextLabel, Button cancelButton)
        {
            this.cancelRequested = cancelRequested;
            this.localProgressBar = localProgressBar;
            this.overallProgressBar = overallProgressBar;
            this.overallTextLabel = overallTextLabel;
            this.localTextLabel = localTextLabel;
            this.cancelButton = cancelButton;
            maximum = 0.0;
            localProgress = 0.0;
            overallProgress = 0.0;
            stageCountProperty = new SimpleIntegerProperty(0);
            currentStageProperty = new SimpleIntegerProperty(0);
        }

        @Override
        public void allowUserCancellation() throws UserCancellationException
        {
            if (cancelRequested.get())
            {
                cancelRequested.set(false); // reset cancel flag

                MainWindowController.getInstance().dismissMiniProgressBarAsync();

                //need to end stopwatches here because they might need to be reused for another process
                //   before cancelComplete() is called
                endStopwatches();

                throw new UserCancellationException("Cancellation requested by user.");
            }
        }

        @Override
        public void cancelComplete(UserCancellationException e)
        {
            complete();
            hideAllProgress();
        }

        @Override
        public void start()
        {
            cancelRequested.set(false);

            stageCountProperty.setValue(0);
            currentStageProperty.setValue(0);

            localProgress = 0.0;
            overallProgress = 0.0;
            Platform.runLater(() ->
            {
                localProgressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : 0.0);
                overallProgressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : 0.0);
            });

            resetText();
            showStage();
            startStopwatches();

            MainWindowController mainWindow = MainWindowController.getInstance();
            mainWindow.resetMiniProgressBar(overallProgressBar.progressProperty(),
            Bindings.createStringBinding(() ->
                {
                    String currProcessTxt = overallTextLabel.textProperty().getValue();

                    //Display "Finishing up..." or something similar
                    if (currentStageProperty.getValue() > stageCountProperty.getValue() &&
                        isProcessing())
                    {
                        return localTextLabel.getText();
                    }

                    //Display "Loading..." or some end message (ex. "Finished loading images")
                    // or just remove redundant "Stage 1/1"
                    if (!isProcessing() ||
                        stageCountProperty.getValue() <= 1)
                    {
                        return currProcessTxt;
                    }

                    return String.format("%s (Stage %s/%s)",
                        currProcessTxt, currentStageProperty.getValue(), stageCountProperty.getValue());
                },
                overallTextLabel.textProperty(), currentStageProperty, stageCountProperty,
                localTextLabel.textProperty()));//pass localTextLabel text property so this binding updates more often
        }

        @Override
        public void setProcessName(String processName)
        {
            Stage progressStage = (Stage) overallProgressBar.getScene().getWindow();
            Platform.runLater(() -> progressStage.setTitle(processName));
        }

        @Override
        public void setStageCount(int count)
        {
            Platform.runLater(() -> stageCountProperty.setValue(count));
        }

        @Override
        public void setStage(int stage, String message)
        {
            this.localProgress = 0.0;
            int currentStage = stage + 1; //index from 1, copy so we can update currentStageProperty w/ Platform.runLater to avoid threading issue
            Platform.runLater(() -> this.currentStageProperty.setValue(currentStage));

            Platform.runLater(() -> localProgressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS));

            //index current stage from 0 in this instance
            overallProgress = (double) (currentStage - 1) / stageCountProperty.getValue();
            Platform.runLater(() -> overallProgressBar.setProgress(overallProgress));

            LOG.info("[Stage {}/{}] {}", currentStage, stageCountProperty.getValue(), message);

            Platform.runLater(() -> overallTextLabel.setText(message));

            if(currentStage > stageCountProperty.getValue())
            {
                if (message.equals(ProgressMonitor.PREPARING_PROJECT))
                {
                    Platform.runLater(()-> localTextLabel.setText(ProgressMonitor.ALMOST_READY));
                }
                else
                {
                    Platform.runLater(()-> localTextLabel.setText(FINISHING_UP));
                }
            }
            else
            {
                beginNewStage();
            }
        }

        @Override
        public void setMaxProgress(double maxProgress)
        {
            this.maximum = maxProgress;
            Platform.runLater(() -> localProgressBar.setProgress(maxProgress == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : localProgress / maxProgress));
        }

        @Override
        public void setProgress(double progress, String message)
        {
            this.localProgress = progress / maximum;
            Platform.runLater(() -> localProgressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : localProgress));

            //index current stage from 0 in this instance
            double offset = (double) (currentStageProperty.getValue() - 1) / stageCountProperty.getValue();
            this.overallProgress = offset + (localProgress / stageCountProperty.getValue());
            Platform.runLater(() -> overallProgressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : overallProgress));

            LOG.info("[{}%] {}", new DecimalFormat("#.##").format(localProgress * 100), message);

            //remove stage/stageCount from txt if it wouldn't make sense for it to be there (ex. Stage 0/0)
            //useful for simple exports like orbit animation
            boolean removeStageNums = stageCountProperty.getValue() <= 1 || currentStageProperty.getValue() == 0;
            revertText = removeStageNums ? message :
                String.format("Stage %s/%s — %s", currentStageProperty.getValue(), stageCountProperty.getValue(), message);

            Platform.runLater(() -> localTextLabel.setText(revertText));

            clickStopwatches(progress, maximum);
        }

        @Override
        public void complete()
        {
            this.maximum = 0.0;
            endStopwatches();

            MainWindowController mainWindow = MainWindowController.getInstance();
            mainWindow.setReadyToDismissMiniProgBar();

            if (!stage.isShowing())
            {
                mainWindow.showMiniProgressBar();
            }

            if (overallProgressBar.getProgress() == ProgressIndicator.INDETERMINATE_PROGRESS)
            {
                Platform.runLater(() -> overallProgressBar.setProgress(1.0));
            }

            if (localProgressBar.getProgress() == ProgressIndicator.INDETERMINATE_PROGRESS)
            {
                Platform.runLater(() -> localProgressBar.setProgress(1.0));
            }

            //only revert text for processes which are not lightweight
            if (localTextLabel.getText().equals(FINISHING_UP))
            {
                Platform.runLater(() -> localTextLabel.setText(revertText));
            }

            Platform.runLater(() -> cancelButton.setText("Cancel"));
        }

        @Override
        public void fail(Throwable e)
        {
            complete();
        }

        @Override
        public boolean isConflictingProcess()
        {
            if (!isProcessing())
            {
                return false;
            }

            Platform.runLater(() ->
            {
                ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                //ButtonType stopProcess = new ButtonType("Start New Process", ButtonBar.ButtonData.YES);
                Alert alert = new Alert(Alert.AlertType.NONE, "Cannot run multiple tasks at the same time.\n" +
                    "Either wait for the current task to complete or cancel it." /*+
                        "Press OK to finish the current process."*/, ok/*, stopProcess*/);
                alert.setHeaderText("Conflicting Tasks");

//                    //continue current process, don't start a new one
//                    ((Button) alert.getDialogPane().lookupButton(ok)).setOnAction(event -> {
//                    });
//
//                    //cancel current process and start new one
//                    ((Button) alert.getDialogPane().lookupButton(stopProcess)).setOnAction(event -> {
//                        cancelRequested.set(true);
//                    });

                alert.showAndWait();
            });

            return true;
        }
    }
}
