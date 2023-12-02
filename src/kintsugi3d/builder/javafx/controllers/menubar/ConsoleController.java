/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import kintsugi3d.builder.app.logging.LogMessage;
import kintsugi3d.builder.app.logging.RecentLogMessageAppender;
import kintsugi3d.builder.javafx.MainApplication;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ConsoleController implements Initializable
{
    private static final Logger log = LoggerFactory.getLogger(ConsoleController.class);

    @FXML private ToggleButton toggleButtonPause;
    @FXML private ToggleButton toggleButtonError;
    @FXML private ToggleButton toggleButtonInfo;
    @FXML private ToggleButton toggleButtonDebug;
    @FXML private ToggleButton toggleButtonTrace;
    @FXML private ToggleButton toggleButtonWarn;
    @FXML private ListView<LogMessage> messageListView;

    private RecentLogMessageAppender logMessages;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        logMessages = RecentLogMessageAppender.getInstance();

        messageListView.setCellFactory(new LogMessageCellFactory());
        messageListView.setItems(logMessages.getMessages().filtered(this::logMessagePassesFilter));
        messageListView.scrollTo(messageListView.getItems().size() - 1);

        // If the logger cannot log a level, disable a filter button
        if (! logMessages.isLevelAvailable(Level.ERROR))
        {
            toggleButtonError.setSelected(false);
            toggleButtonError.setDisable(true);
        }
        if (! logMessages.isLevelAvailable(Level.WARN))
        {
            toggleButtonWarn.setSelected(false);
            toggleButtonWarn.setDisable(true);
        }
        if (! logMessages.isLevelAvailable(Level.INFO))
        {
            toggleButtonInfo.setSelected(false);
            toggleButtonInfo.setDisable(true);
        }
        if (! logMessages.isLevelAvailable(Level.DEBUG))
        {
            toggleButtonDebug.setSelected(false);
            toggleButtonDebug.setDisable(true);
        }
        if (! logMessages.isLevelAvailable(Level.TRACE))
        {
            toggleButtonTrace.setSelected(false);
            toggleButtonTrace.setDisable(true);
        }
    }

    public void buttonOpenLogDir(ActionEvent actionEvent)
    {
        try
        {
            File logDir = new File(System.getProperty("Kintsugi3D.logDir"));
            MainApplication.getAppInstance().getHostServices().showDocument(logDir.getAbsolutePath());
        }
        catch (Exception e)
        {
            log.error("An error occurred while opening log directory:", e);
        }
    }

    public void buttonChangeLogLevel(ActionEvent actionEvent)
    {
        FilteredList<LogMessage> filteredList = (FilteredList<LogMessage>) messageListView.getItems();
        if (filteredList == null)
            return;
        filteredList.setPredicate(this::logMessagePassesFilter);
    }

    public void buttonUpdatePaused(ActionEvent event)
    {
        ObservableList<LogMessage> items;

        if (toggleButtonPause.isSelected())
        {
            synchronized (logMessages.getMessages())
            {
                items = FXCollections.observableArrayList(logMessages.getMessages());
            }
        }
        else
        {
            items = logMessages.getMessages();
        }

        messageListView.setItems(items.filtered(this::logMessagePassesFilter));
    }

    private boolean logMessagePassesFilter(LogMessage message)
    {
        switch (message.getLogLevel())
        {
            case ERROR:
                if (! toggleButtonError.isSelected())
                    return false;
                break;
            case WARN:
                if (! toggleButtonWarn.isSelected())
                    return false;
                break;
            case INFO:
                if (! toggleButtonInfo.isSelected())
                    return false;
                break;
            case DEBUG:
                if (! toggleButtonDebug.isSelected())
                    return false;
                break;
            case TRACE:
                if (! toggleButtonTrace.isSelected())
                    return false;
                break;
        }

        return true;
    }

    private static class LogMessageCellFactory implements Callback<ListView<LogMessage>, ListCell<LogMessage>>
    {
        @Override
        public ListCell<LogMessage> call(ListView<LogMessage> logMessageListView)
        {
            return new ListCell<>()
            {
                @Override
                public void updateItem(LogMessage message, boolean empty)
                {
                    Platform.runLater(() ->
                    {
                        super.updateItem(message, empty);
                        if (empty || message == null)
                        {
                            setText(null);
                            setGraphic(null);
                            setStyle(null);
                        } else
                        {
                            setText(null);

                            Label levelLabel = new Label(message.getLogLevel().toString());
                            levelLabel.setPrefWidth(40);

                            StringBuilder labelText = new StringBuilder(message.getMessage());

                            if (message.getThrown() != null)
                            {
                                if (this.isSelected())
                                {
                                    labelText.append("\n");
                                    labelText.append(strStackTrace(message.getThrown()));
                                }
                                else
                                {
                                    labelText.append("... (Click for more)");
                                }
                            }

                            Label messageLabel = new Label(labelText.toString());

                            HBox box = new HBox(levelLabel, messageLabel);

                            box.setSpacing(10);

                            Tooltip.install(box, new Tooltip(formatTooltip(message)));

                            if (message.getLogLevel() == Level.ERROR)
                            {
                                setStyle("-fx-background-color: #fa6d6d");
                                //TODO: MAKE TEXT BLACK
                            }
                            else if (message.getLogLevel() == Level.WARN)
                            {
                                setStyle("-fx-background-color: #fab66d");
                                //TODO: MAKE TEXT BLACK
                            }
                            else
                            {
                                setStyle(null);
                            }

                            setGraphic(box);
                        }
                    });
                }
            };
        }

        private String strStackTrace(Throwable thrown)
        {
            if (thrown == null)
                return "";

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            thrown.printStackTrace(pw);
            return sw.toString().trim();
        }

        private String formatTooltip(LogMessage message)
        {
            StringBuilder sb = new StringBuilder("Logged by ");
            sb.append(message.getSourceClassName());
            sb.append(" at ");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyy hh:mm:ss").withZone(ZoneId.systemDefault());
            sb.append(formatter.format(message.getTimestamp()));
            return sb.toString();
        }
    }
}
