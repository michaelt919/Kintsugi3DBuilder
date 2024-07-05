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

    @FXML private Label totalTimeElapsedLabel;
    @FXML private Label timeElapsedLabel;

    @FXML private Label localTextLabel;
    @FXML private Label overallTextLabel;

    @FXML private ProgressBar overallProgressBar;
    @FXML private ProgressBar localProgressBar;
    @FXML private Button cancelButton;

    private Stopwatch stopwatch;

    private String defaultLocalText;
    private String defaultOverallText;

    private String defaultTimeElapsedText;
    private String defaultTotalTimeElapsedText;

    private Stage stage;

    public static ProgressBarsController getInstance()
    {
        return INSTANCE;
    }

    public void init(Stage stage){
        this.stage = stage;
        defaultLocalText = localTextLabel.getText();
        defaultOverallText = overallTextLabel.getText();
        defaultTimeElapsedText = timeElapsedLabel.getText();
        defaultTotalTimeElapsedText = totalTimeElapsedLabel.getText();

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
            timeElapsedLabel.setText(defaultTimeElapsedText);
            totalTimeElapsedLabel.setText(defaultTotalTimeElapsedText);
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

    public void stopwatchClick(){
        long difference = stopwatch.click();

        long minutes = TimeUnit.NANOSECONDS.toMinutes(difference);
        long seconds = TimeUnit.NANOSECONDS.toSeconds(difference) -
                TimeUnit.MINUTES.toSeconds(minutes);


        String minutesString = minutes == 1 ? "minute" : "minutes";
        String secondsString = seconds == 1 ? "second" : "seconds";

        String formattedElapsedTime = String.format("%d %s, %d %s", minutes, minutesString, seconds, secondsString);
        Platform.runLater(()->timeElapsedLabel.setText(formattedElapsedTime));
    }

    public void updateTotalElapsedTime() {
        long totalElapsedTime = stopwatch.getTotalElapsedTime();

        long totalMins = TimeUnit.NANOSECONDS.toMinutes(totalElapsedTime);
        long totalSecs = TimeUnit.NANOSECONDS.toSeconds(totalElapsedTime) -
                TimeUnit.MINUTES.toSeconds(totalMins);

        String minutesString = totalMins == 1 ? "minute" : "minutes";
        String secondsString = totalSecs == 1 ? "second" : "seconds";

        String formattedTotalElapsedTime = String.format("%d %s, %d %s", totalMins, minutesString, totalSecs, secondsString);

        Platform.runLater(()->totalTimeElapsedLabel.setText(formattedTotalElapsedTime));
    }
}
