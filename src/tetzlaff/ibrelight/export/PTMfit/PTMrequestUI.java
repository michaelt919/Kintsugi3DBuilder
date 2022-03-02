package tetzlaff.ibrelight.export.PTMfit;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.IBRRequestUI;
import tetzlaff.ibrelight.core.IBRelightModels;
import tetzlaff.ibrelight.core.TextureFitSettings;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

public class PTMrequestUI implements IBRRequestUI {

    @FXML private TextField widthTextField;
    @FXML private TextField heightTextField;
    @FXML private TextField exportDirectoryField;
    @FXML private Button runButton;
    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    private IBRelightModels modelAccess;
    private Stage stage;

    private File lastDirectory;




    public static PTMrequestUI create(Window window, IBRelightModels modelAccess) throws IOException
    {
        String fxmlFileName = "fxml/export/PTMrequestUI.fxml";
        URL url = PTMrequestUI.class.getClassLoader().getResource(fxmlFileName);
        assert url != null : "Can't find " + fxmlFileName;
        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent parent = fxmlLoader.load();
        PTMrequestUI svdRequestUI = fxmlLoader.getController();
        svdRequestUI.modelAccess = modelAccess;

        svdRequestUI.stage = new Stage();
        svdRequestUI.stage.getIcons().add(new Image(new File("ibr-icon.png").toURI().toURL().toString()));
        svdRequestUI.stage.setTitle("PTM fit request");
        svdRequestUI.stage.setScene(new Scene(parent));
        svdRequestUI.stage.initOwner(window);

        return svdRequestUI;

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
    public <ContextType extends Context<ContextType>> void prompt(Consumer<IBRRequest<ContextType>> requestHandler) {
        stage.show();

        runButton.setOnAction(event ->
        {
            //stage.close();

            IBRRequest<ContextType> request = new PTMrequest<>(new TextureFitSettings(
                    Integer.parseInt(widthTextField.getText()),
                    Integer.parseInt(heightTextField.getText()),
                    new File(exportDirectoryField.getText()),
                    modelAccess.getSettingsModel()
                    ));

            requestHandler.accept(request);
        });
    }
}
