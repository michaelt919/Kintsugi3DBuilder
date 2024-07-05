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

    @FXML private Label localEstimTimeRemainingLabel;
    @FXML private Label totalEstimTimeRemainingLabel;
    @FXML private Label totalTimeElapsedLabel;

    @FXML private Label localTextLabel;
    @FXML private Label overallTextLabel;

    @FXML private ProgressBar overallProgressBar;
    @FXML private ProgressBar localProgressBar;
    @FXML private Button cancelButton;

    private Stopwatch stopwatch;

    private String defaultLocalText;
    private String defaultOverallText;
    private String defaultTotalTimeElapsedText;
    private String defaultEstimTimeRemainingTxt;

    private String defaultEstimLocalTimeRemainingTxt;

    private Stage stage;

    public static ProgressBarsController getInstance()
    {
        return INSTANCE;
    }

    public void init(Stage stage){
        this.stage = stage;
        defaultLocalText = localTextLabel.getText();
        defaultOverallText = overallTextLabel.getText();

        defaultTotalTimeElapsedText = totalTimeElapsedLabel.getText();
        defaultEstimTimeRemainingTxt = totalEstimTimeRemainingLabel.getText();
        defaultEstimLocalTimeRemainingTxt = localEstimTimeRemainingLabel.getText();

        stopwatch = new Stopwatch();

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
            totalTimeElapsedLabel.setText(defaultTotalTimeElapsedText);
            totalEstimTimeRemainingLabel.setText(defaultEstimTimeRemainingTxt);
            localEstimTimeRemainingLabel.setText(defaultEstimLocalTimeRemainingTxt);
        });
    }

    public void showStage() {
        Platform.runLater(()-> stage.show());
    }


    public Button getCancelButton() {return cancelButton;}

    public void hideStage() {
        Platform.runLater(()->stage.hide());
    }

    public void stopwatchStart(){
        stopwatch.start();
    }

    public void stopwatchClick(double localProgress, double overallProgress){
        stopwatch.click();
        updateTotalRemainingTime(overallProgress);
        updateLocalRemainingTime(localProgress);
    }

    private void updateTotalRemainingTime(double progress) {
        long elapsedTime = stopwatch.getTotalElapsedTime();

        double remainingProgress = 1.0 / progress;

        Platform.runLater(()-> totalEstimTimeRemainingLabel.setText(nanoToMinAndSec((long) (elapsedTime * remainingProgress))));
    }

    private void updateLocalRemainingTime(double localProgress) {
        long avgDif = stopwatch.getAvgDifference();

        double remainingProgress = (1.0 - localProgress) * 100;

        long estimatedRemaining = (long) (avgDif * remainingProgress);

        Platform.runLater(()-> localEstimTimeRemainingLabel.setText(nanoToMinAndSec(estimatedRemaining)));
    }


    public void updateTotalElapsedTime() {
        long totalElapsedTime = stopwatch.getTotalElapsedTime();

        Platform.runLater(()->totalTimeElapsedLabel.setText(nanoToMinAndSec(totalElapsedTime)));
    }

    private static String nanoToMinAndSec(long nanoTime){
        long minutes = TimeUnit.NANOSECONDS.toMinutes(nanoTime);
        long seconds = TimeUnit.NANOSECONDS.toSeconds(nanoTime) -
                TimeUnit.MINUTES.toSeconds(minutes);

        //TODO: replace with 00:00 format?
        String minutesString = minutes == 1 ? "minute" : "minutes";
        String secondsString = seconds == 1 ? "second" : "seconds";

        return String.format("%d %s, %d %s", minutes, minutesString, seconds, secondsString);
    }


    public void stopwatchStop() {
        stopwatch.stop();
    }

    public boolean isStopwatchRunning() {
        return stopwatch.isRunning();
    }
}
