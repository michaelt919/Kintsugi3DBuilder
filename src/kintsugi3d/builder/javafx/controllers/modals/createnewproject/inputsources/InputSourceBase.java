package kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import kintsugi3d.builder.io.primaryview.ViewSelectionModel;
import kintsugi3d.builder.javafx.controllers.modals.viewselect.ViewSelectableBase;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.javafx.experience.Modal;
import kintsugi3d.builder.resources.project.MissingImagesException;

import java.io.File;
import java.util.function.Consumer;

public abstract class InputSourceBase extends ViewSelectableBase implements InputSource
{
    protected abstract void loadForViewSelectionOrThrow(Consumer<ViewSelectionModel> onLoadComplete) throws Exception;

    @Override
    public void loadForViewSelection(Consumer<ViewSelectionModel> onLoadComplete)
    {
        try
        {
            loadForViewSelectionOrThrow(onLoadComplete);
        }
        catch (MissingImagesException e)
        {
            showMissingImagesAlert(e, () -> loadForViewSelection(onLoadComplete),
                searchableTreeView.getTreeView().getScene().getWindow());
        }
        catch (Exception e)
        {
            ExceptionHandling.error("Error initializing view selection.", e);
        }
    }

    private void showMissingImagesAlert(MissingImagesException exception, Runnable reattampt, Window modalWindow)
    {
        int numMissingImgs = exception.getNumMissingImgs();
        File prevTriedDirectory = exception.getImgDirectory();

        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.OTHER);
        ButtonType newDirectory = new ButtonType("Choose Different Image Directory", ButtonBar.ButtonData.YES);
        ButtonType skipMissingCams = new ButtonType("Skip Missing Cameras", ButtonBar.ButtonData.NO);

        Alert alert = new Alert(Alert.AlertType.NONE,
            "Imported object is missing " + numMissingImgs + " images.",
            cancel, newDirectory, skipMissingCams/*, openDirectory*/);

        ((ButtonBase) alert.getDialogPane().lookupButton(cancel)).setOnAction(
            event -> Modal.requestClose(modalWindow));

        ((ButtonBase) alert.getDialogPane().lookupButton(newDirectory)).setOnAction(event ->
        {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(getInitialPhotosDirectory());

            directoryChooser.setTitle("Choose New Image Directory");

            overrideFullResImageDirectory(directoryChooser.showDialog(modalWindow));
            reattampt.run();
        });

        ((ButtonBase) alert.getDialogPane().lookupButton(skipMissingCams)).setOnAction(event ->
        {
            overrideFullResImageDirectory(prevTriedDirectory);
            reattampt.run();
        });

        alert.setTitle("Project is Missing Images");
        alert.show();
    }
}
