package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import org.apache.logging.log4j.core.LogEvent;
import tetzlaff.ibrelight.app.IBRelight;
import tetzlaff.ibrelight.app.logging.LogMessage;
import tetzlaff.ibrelight.app.logging.LogMessageListener;
import tetzlaff.ibrelight.app.logging.RecentLogMessageAppender;
import tetzlaff.ibrelight.javafx.MainApplication;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ConsoleController implements Initializable
{
    @FXML private CheckBox checkboxError;
    @FXML private CheckBox checkboxInfo;
    @FXML private CheckBox checkboxDebug;
    @FXML private CheckBox checkboxTrace;
    @FXML private CheckBox checkboxFatal;
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
                Platform.runLater(() -> {
                    messageListView.getItems().add(logMessage);
                });
            }
        });
    }

    private void populateListFromCache()
    {
        messageListView.getItems().clear();

        for (LogMessage msg : logMessages.getMessages())
        {
            messageListView.getItems().add(msg);
        }
    }

    public void buttonOpenLogDir(ActionEvent actionEvent)
    {
        //TODO
    }

    private class LogMessageCellFactory implements Callback<ListView<LogMessage>, ListCell<LogMessage>>
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
                        setText(message.getMessage());
                    }
                }
            };
        }
    }
}
