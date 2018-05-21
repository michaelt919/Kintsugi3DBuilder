package tetzlaff.ibrelight.export.prioritize;

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
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.IBRRequestUI;
import tetzlaff.ibrelight.core.IBRelightModels;

public class PrioritizedViewSetRequestUI implements IBRRequestUI
{
    @FXML private TextField exportFileField;
    @FXML private TextField targetVSetFileField;
    @FXML private TextField maskFileField;
    @FXML private Button runButton;

    private final FileChooser fileChooser = new FileChooser();

    private IBRelightModels modelAccess;
    private Stage stage;

    private File lastDirectory;

    public static PrioritizedViewSetRequestUI create(Window window, IBRelightModels modelAccess) throws IOException
    {
        String fxmlFileName = "fxml/export/PrioritizedViewSetRequestUI.fxml";
        URL url = PrioritizedViewSetRequestUI.class.getClassLoader().getResource(fxmlFileName);
        assert url != null : "Can't find " + fxmlFileName;

        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent parent = fxmlLoader.load();
        PrioritizedViewSetRequestUI prioritizedViewSetRequestUI = fxmlLoader.getController();
        prioritizedViewSetRequestUI.modelAccess = modelAccess;

        prioritizedViewSetRequestUI.stage = new Stage();
        prioritizedViewSetRequestUI.stage.getIcons().add(new Image(new File("ibr-icon.png").toURI().toURL().toString()));
        prioritizedViewSetRequestUI.stage.setTitle("Prioritize view set...");
        prioritizedViewSetRequestUI.stage.setScene(new Scene(parent));
        prioritizedViewSetRequestUI.stage.initOwner(window);

        return prioritizedViewSetRequestUI;
    }

    @FXML
    private void exportFileButtonAction()
    {
        this.fileChooser.setTitle("Specify the name of the new view set file");
        this.fileChooser.getExtensionFilters().clear();
        this.fileChooser.getExtensionFilters().add(new ExtensionFilter("View set files", "*.vset"));
        if (exportFileField.getText().isEmpty())
        {
            if (lastDirectory != null)
            {
                this.fileChooser.setInitialDirectory(lastDirectory);
            }
        }
        else
        {
            File currentValue = new File(exportFileField.getText());
            this.fileChooser.setInitialDirectory(currentValue.getParentFile());
            this.fileChooser.setInitialFileName(currentValue.getName());
        }
        File file = this.fileChooser.showSaveDialog(stage.getOwner());
        if (file != null)
        {
            exportFileField.setText(file.toString());
            lastDirectory = file.getParentFile();
        }
    }

    @FXML
    private void maskFileButtonAction()
    {
        this.fileChooser.setTitle("Choose a mask file");
        this.fileChooser.getExtensionFilters().clear();
        this.fileChooser.getExtensionFilters().add(new ExtensionFilter(
            "Image files", "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.wbmp", "*.gif"));
        if (maskFileField.getText().isEmpty())
        {
            if (lastDirectory != null)
            {
                this.fileChooser.setInitialDirectory(lastDirectory);
            }
        }
        else
        {
            File currentValue = new File(maskFileField.getText());
            this.fileChooser.setInitialDirectory(currentValue.getParentFile());
            this.fileChooser.setInitialFileName(currentValue.getName());
        }
        File file = this.fileChooser.showOpenDialog(stage.getOwner());
        if (file != null)
        {
            maskFileField.setText(file.toString());
            lastDirectory = file.getParentFile();
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
                new PrioritizedViewSetRequest(
                    new File(exportFileField.getText()),
                    maskFileField.getText().isEmpty() ? null : new File(maskFileField.getText()),
                    modelAccess.getSettingsModel()));
        });
    }
}
