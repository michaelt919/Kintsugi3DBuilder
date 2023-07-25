package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import org.apache.logging.log4j.core.LogEvent;
import tetzlaff.ibrelight.app.logging.LogMessageListener;
import tetzlaff.ibrelight.app.logging.RecentLogMessageAppender;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ConsoleController implements Initializable
{
    @FXML private ListView<String> messageListView;

    private RecentLogMessageAppender logMessages;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        logMessages = RecentLogMessageAppender.getInstance();
        for (String msg : logMessages.getMessages())
        {
            messageListView.getItems().add(msg);
        }

        logMessages.addListener(new LogMessageListener()
        {
            @Override
            public void newLogMessage(LogEvent logEvent)
            {
                messageListView.getItems().add(logEvent.getMessage().getFormattedMessage());
            }
        });
    }
}
