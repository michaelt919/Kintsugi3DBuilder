package tetzlaff.ibr.export.btf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import tetzlaff.ibr.core.IBRRequest;
import tetzlaff.ibr.core.IBRRequestUI;
import tetzlaff.ibr.core.IBRelightModelAccess;

public class BTFRequestUI implements IBRRequestUI
{
    @FXML private TextField widthTextField;
    @FXML private TextField heightTextField;
    @FXML private TextField exportDirectoryField;
    @FXML private Button runButton;

    private final DirectoryChooser directoryChooser = new DirectoryChooser();

    private IBRelightModelAccess modelAccess;
    private Stage stage;

    private File lastDirectory;

    public static BTFRequestUI create(Window window, IBRelightModelAccess modelAccess) throws IOException
    {
        String fxmlFileName = "fxml/export/BTFRequestUI.fxml";
        URL url = BTFRequestUI.class.getClassLoader().getResource(fxmlFileName);
        assert url != null : "Can't find " + fxmlFileName;

        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent parent = fxmlLoader.load();
        BTFRequestUI btfRequestUI = fxmlLoader.getController();
        btfRequestUI.modelAccess = modelAccess;

        btfRequestUI.stage = new Stage();
        btfRequestUI.stage.setTitle("Fidelity metric request");
        btfRequestUI.stage.setScene(new Scene(parent));
        btfRequestUI.stage.initOwner(window);

        return btfRequestUI;
    }

    @FXML
    private void exportDirectoryButtonAction()
    {
        this.directoryChooser.setTitle("Choose an export directory");
        if (exportDirectoryField.getText().isEmpty())
        {
            if (lastDirectory != null)
            {
                this.directoryChooser.setInitialDirectory(lastDirectory);
            }
        }
        else
        {
            File currentValue = new File(exportDirectoryField.getText());
            this.directoryChooser.setInitialDirectory(currentValue);
        }
        File file = this.directoryChooser.showDialog(stage.getOwner());
        if (file != null)
        {
            exportDirectoryField.setText(file.toString());
            lastDirectory = file;
        }
    }

    @FXML
    public void cancelButtonAction(ActionEvent actionEvent)
    {
        stage.close();
    }

    @Override
    public void prompt(Consumer<IBRRequest> requestHandler)
    {
        stage.show();

        runButton.setOnAction(event ->
        {
            //stage.close();

            requestHandler.accept(
                new BTFRequest(
                    Integer.parseInt(widthTextField.getText()),
                    Integer.parseInt(heightTextField.getText()),
                    new File(exportDirectoryField.getText()),
                    modelAccess.getSettingsModel(),
                    modelAccess.getLightingModel().getLightColor(0)));
        });
    }
}
