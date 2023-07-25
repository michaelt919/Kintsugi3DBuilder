package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import org.apache.logging.log4j.core.LogEvent;
import tetzlaff.ibrelight.app.logging.LogMessage;
import tetzlaff.ibrelight.app.logging.LogMessageListener;
import tetzlaff.ibrelight.app.logging.RecentLogMessageAppender;

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
    @FXML private ListView<String> messageListView;

    private RecentLogMessageAppender logMessages;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        logMessages = RecentLogMessageAppender.getInstance();

        populateListFromCache();

        logMessages.addListener(new LogMessageListener()
        {
            @Override
            public void newLogMessage(LogMessage logMessage)
            {
                Platform.runLater(() -> {
                    messageListView.getItems().add(logMessage.getMessage());
                });
            }
        });
    }

    private void populateListFromCache()
    {
        messageListView.getItems().clear();

        for (LogMessage msg : logMessages.getMessages())
        {
            messageListView.getItems().add(msg.getMessage());
        }
    }
}
