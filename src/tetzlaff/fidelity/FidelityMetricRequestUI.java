package tetzlaff.fidelity;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;
import tetzlaff.ibr.core.IBRRequest;
import tetzlaff.ibr.core.IBRRequestUI;
import tetzlaff.ibr.core.IBRelightModelAccess;

public class FidelityMetricRequestUI implements IBRRequestUI
{
    @FXML private TextField exportFileField;
    @FXML private TextField targetVSetFileField;
    @FXML private TextField maskFileField;
    @FXML private Button runButton;

    private final FileChooser fileChooser = new FileChooser();

    private IBRelightModelAccess modelAccess;
    private Stage stage;

    private File lastDirectory;

    public static FidelityMetricRequestUI create(Window window, IBRelightModelAccess modelAccess) throws IOException
    {
        String fxmlFileName = "fxml/fidelity/FidelityMetricRequestUI.fxml";
        URL url = FidelityMetricRequestUI.class.getClassLoader().getResource(fxmlFileName);
        assert url != null : "Can't find " + fxmlFileName;

        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent parent = fxmlLoader.load();
        FidelityMetricRequestUI fidelityMetricRequestUI = fxmlLoader.getController();
        fidelityMetricRequestUI.modelAccess = modelAccess;

        fidelityMetricRequestUI.stage = new Stage();
        fidelityMetricRequestUI.stage.setTitle("Fidelity metric request");
        fidelityMetricRequestUI.stage.setScene(new Scene(parent));
        fidelityMetricRequestUI.stage.initOwner(window);

        return fidelityMetricRequestUI;
    }

    @FXML
    private void exportFileButtonAction()
    {
        this.fileChooser.setTitle("Choose an export filename");
        this.fileChooser.getExtensionFilters().clear();
        this.fileChooser.getExtensionFilters().add(new ExtensionFilter("Text files", "*.txt"));
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
    private void targetVSetFileButtonAction()
    {
        this.fileChooser.setTitle("Choose a target view set file");
        this.fileChooser.getExtensionFilters().clear();
        this.fileChooser.getExtensionFilters().add(new ExtensionFilter("View set files", "*.vset"));
        if (targetVSetFileField.getText().isEmpty())
        {
            if (lastDirectory != null)
            {
                this.fileChooser.setInitialDirectory(lastDirectory);
            }
        }
        else
        {
            File currentValue = new File(targetVSetFileField.getText());
            this.fileChooser.setInitialDirectory(currentValue.getParentFile());
            this.fileChooser.setInitialFileName(currentValue.getName());
        }
        File file = this.fileChooser.showOpenDialog(stage.getOwner());
        if (file != null)
        {
            targetVSetFileField.setText(file.toString());
            lastDirectory = file.getParentFile();
        }
    }

    @FXML
    private void maskFileButtonAction()
    {
        this.fileChooser.setTitle("Choose an mask file");
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

    @Override
    public void prompt(Consumer<IBRRequest> requestHandler)
    {
        stage.show();

        runButton.setOnAction(event ->
        {
            stage.close();

            requestHandler.accept(
                new FidelityMetricRequest(
                    new File(exportFileField.getText()),
                    new File(targetVSetFileField.getText()),
                    maskFileField.getText().isEmpty() ? null : new File(maskFileField.getText()),
                    modelAccess.getSettingsModel()));
        });
    }
}
