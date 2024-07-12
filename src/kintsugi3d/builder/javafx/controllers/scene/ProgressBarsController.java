/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.scene;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import kintsugi3d.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ProgressBarsController {
    private static final Logger log = LoggerFactory.getLogger(ProgressBarsController.class);
    private static ProgressBarsController INSTANCE;

    @FXML private Label localElapsedTimeLabel;
    @FXML private Label localEstimTimeRemainingLabel;
    @FXML private Label totalEstimTimeRemainingLabel;
    @FXML private Label totalElapsedTimeLabel;

    @FXML private Label localTextLabel;
    @FXML private Label overallTextLabel;

    @FXML private ProgressBar overallProgressBar;
    @FXML private ProgressBar localProgressBar;
    @FXML private Button cancelButton;

    private Stopwatch overallStopwatch;
    private Stopwatch localStopwatch;

    private String defaultLocalText;
    private String defaultOverallText;
    private String defaultTotalTimeElapsedText;
    private String defaultEstimTimeRemainingTxt;

    private String defaultEstimLocalTimeRemainingTxt;

    private Stage stage;
    private String defaultOverallElapsedTimeTxt;

    public static ProgressBarsController getInstance()
    {
        return INSTANCE;
    }

    public void init(Stage stage){
        this.stage = stage;
        defaultOverallText = overallTextLabel.getText();
        defaultLocalText = localTextLabel.getText();

        defaultTotalTimeElapsedText = totalElapsedTimeLabel.getText();
        defaultEstimTimeRemainingTxt = totalEstimTimeRemainingLabel.getText();
        defaultEstimLocalTimeRemainingTxt = localEstimTimeRemainingLabel.getText();
        defaultOverallElapsedTimeTxt = localElapsedTimeLabel.getText();

        overallStopwatch = new Stopwatch();
        localStopwatch = new Stopwatch();

        INSTANCE = this;
    }

    public ProgressBar getOverallProgressBar(){return overallProgressBar;}
    public ProgressBar getLocalProgressBar(){return localProgressBar;}
    public Stage getStage(){return stage;}
    public Label getLocalTextLabel(){return localTextLabel;}
    public Label getOverallTextLabel() {return overallTextLabel;}

    public void resetText(){
        Platform.runLater(()->{
            localTextLabel.setText(defaultLocalText);
            overallTextLabel.setText(defaultOverallText);

            totalElapsedTimeLabel.setText(defaultTotalTimeElapsedText);
            localElapsedTimeLabel.setText(defaultOverallElapsedTimeTxt);

            totalEstimTimeRemainingLabel.setText(defaultEstimTimeRemainingTxt);
            localEstimTimeRemainingLabel.setText(defaultEstimLocalTimeRemainingTxt);
        });
    }

    public void showStage() {
        Platform.runLater(()-> {
            stage.show();

            if(!isProcessing()){
                reset();
            }
        });
    }

    private void reset() {
        resetText();
        Platform.runLater(()->{
            overallProgressBar.setProgress(0.0);
            localProgressBar.setProgress(0.0);
        });
    }


    public Button getCancelButton() {return cancelButton;}

    public void hideStage() {
        Platform.runLater(()->stage.hide());
    }

    public void startStopwatches(){
        overallStopwatch.start();
        localStopwatch.start();

        new Thread(() -> {
            while (isProcessing()) {
                try {
                    Thread.sleep(200);
                    updateElapsedTime();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public void clickStopwatches(double progress, double maximum, double overallProgress){
        overallStopwatch.click();
        localStopwatch.click();
        updateTotalRemainingTime(overallProgress);
        updateLocalRemainingTime(progress, maximum);
    }

    private void updateTotalRemainingTime(double progress) {
        long elapsedTime = overallStopwatch.getElapsedTime();

        double remainingProgress = progress == 0 ? 1.0 : 1.0 / progress - 1;

        long estimatedRemaining = (long) Math.max(0L, elapsedTime * remainingProgress);

        Platform.runLater(()-> totalEstimTimeRemainingLabel.setText(nanosecToFormatTime(estimatedRemaining)));
    }

    private void updateLocalRemainingTime(double progress, double maximum) {
        long avgDif = localStopwatch.getAvgDifference();

        double remainingProcesses = maximum - progress;

        long estimatedRemaining = Math.max(0, (long) (avgDif * remainingProcesses));

        Platform.runLater(()-> localEstimTimeRemainingLabel.setText(nanosecToFormatTime(estimatedRemaining)));
    }


    public void updateElapsedTime() {
        long totalElapsedTime = overallStopwatch.getElapsedTime();
        long localElapsedTime = localStopwatch.getElapsedTime();

        Platform.runLater(()-> totalElapsedTimeLabel.setText(nanosecToFormatTime(totalElapsedTime)));
        Platform.runLater(()->localElapsedTimeLabel.setText(nanosecToFormatTime(localElapsedTime)));
    }

    private static String nanosecToFormatTime(long nanoTime){
        long hours = TimeUnit.NANOSECONDS.toHours(nanoTime);

        long minutes = TimeUnit.NANOSECONDS.toMinutes(nanoTime) -
                TimeUnit.HOURS.toMinutes(hours);

        long seconds = TimeUnit.NANOSECONDS.toSeconds(nanoTime) -
                TimeUnit.HOURS.toSeconds(hours) -
                TimeUnit.MINUTES.toSeconds(minutes);

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }


    public void endStopwatches() {
        overallStopwatch.stop();
        localStopwatch.stop();
    }

    public void stopAndClose(){
        endStopwatches();
        reset();
        hideStage();
    }

    public boolean isProcessing() {
        return overallStopwatch.isRunning();
    }

    public void beginNewStage() {
        localStopwatch = new Stopwatch();
        localStopwatch.start();
    }
}
