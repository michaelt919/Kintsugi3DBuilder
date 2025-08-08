package kintsugi3d.builder.javafx.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionHandling
{
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandling.class);

    public static void error(String message, Throwable e)
    {
        LOG.error("{}:", message, e);
        Platform.runLater(() ->
        {
            ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType showLog = new ButtonType("Show Log", ButtonBar.ButtonData.YES);
            Alert alert = new Alert(Alert.AlertType.NONE, message + "\nSee the log for more info.", ok, showLog);
            ((ButtonBase) alert.getDialogPane().lookupButton(showLog)).setOnAction(event -> {
                // Use the menubar's console open function to prevent 2 console windows from appearing
                MenubarController.getInstance().help_console();
            });
            alert.show();
        });
    }
}
