package tetzlaff.ibrelight.javafx.controllers.menubar;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.apache.logging.log4j.core.LogEvent;
import org.slf4j.event.Level;
import tetzlaff.ibrelight.app.IBRelight;
import tetzlaff.ibrelight.app.logging.LogMessage;
import tetzlaff.ibrelight.app.logging.LogMessageListener;
import tetzlaff.ibrelight.app.logging.RecentLogMessageAppender;
import tetzlaff.ibrelight.javafx.MainApplication;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ConsoleController implements Initializable
{
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

        populateListFromCache();

        logMessages.addListener(new LogMessageListener()
        {
            @Override
            public void newLogMessage(LogMessage logMessage)
            {
                if (logMessagePassesFilter(logMessage))
                {
                    Platform.runLater(() -> {
                        messageListView.getItems().add(logMessage);
                    });
                }
            }
        });
    }

    private void populateListFromCache()
    {
        messageListView.getItems().clear();
        messageListView.getSelectionModel().clearSelection();

        for (LogMessage msg : logMessages.getMessages())
        {
            if (logMessagePassesFilter(msg))
            {
                messageListView.getItems().add(msg);
            }
        }
    }

    public void buttonOpenLogDir(ActionEvent actionEvent)
    {
        //TODO
    }

    public void buttonChangeLogLevel(ActionEvent actionEvent)
    {
        populateListFromCache();
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
                    super.updateItem(message, empty);
                    if (empty || message == null)
                    {
                        setText(null);
                    } else
                    {
                        setText(null);

                        Label levelLabel = new Label(message.getLogLevel().toString());
                        levelLabel.setPrefWidth(40);
                        Label messageLabel = new Label(message.getMessage());

                        HBox box = new HBox(levelLabel, messageLabel);

                        box.setSpacing(10);

                        if (message.getLogLevel() == Level.ERROR)
                        {
                            setStyle("-fx-background-color: #fa6d6d");
                        }
                        else if (message.getLogLevel() == Level.WARN)
                        {
                            setStyle("-fx-background-color: #fab66d");
                        }

                        setGraphic(box);
                    }
                }
            };
        }
    }
}
