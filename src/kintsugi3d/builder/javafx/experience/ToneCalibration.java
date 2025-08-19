package kintsugi3d.builder.javafx.experience;

import javafx.scene.control.TreeItem;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.controllers.modals.EyedropperController;
import kintsugi3d.builder.javafx.controllers.modals.SelectToneCalibrationImageController;
import kintsugi3d.builder.javafx.controllers.modals.ViewSelectController;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.LightCalibrationViewSelectController;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.CurrentProjectInputSource;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.paged.SimpleDataReceiverPage;

import java.io.IOException;
import java.util.Objects;

public class ToneCalibration extends ExperienceBase
{
    @Override
    public String getName()
    {
        return "Tone Calibration";
    }

    @Override
    protected void open() throws IOException
    {
        buildPagedModal("/fxml/modals/createnewproject/PrimaryViewSelect.fxml",
                SimpleDataReceiverPage<InputSource, LightCalibrationViewSelectController>::new,
                LightCalibrationViewSelectController::new)
            .withDefault(getCurrentProjectInputSource())
            .<SelectToneCalibrationImageController>thenNonData("/fxml/modals/SelectToneCalibrationImage.fxml")
            .<EyedropperController>then("/fxml/modals/EyedropperColorChecker.fxml")
            .finish();
    }

    private static CurrentProjectInputSource getCurrentProjectInputSource()
    {
        CurrentProjectInputSource inputSource = new CurrentProjectInputSource()
        {
            // Override this method to set the initial selection to the primary view instead of orientation view
            @Override
            public void setOrientationViewDefaultSelections(ViewSelectController controller)
            {
                ViewSet currentViewSet = Global.state().getIOModel().getLoadedViewSet();

                if (currentViewSet == null)
                    return;

                // Set the initial selection to what is currently being used
                TreeItem<String> selectionItem = InputSource.NONE_ITEM;

                if (currentViewSet.getPrimaryViewIndex() >= 0)
                {
                    String viewName = currentViewSet.getImageFileName(currentViewSet.getPrimaryViewIndex());

                    for (int i = 0; i < searchableTreeView.getTreeView().getExpandedItemCount(); i++)
                    {
                        TreeItem<String> item = searchableTreeView.getTreeView().getTreeItem(i);
                        if (Objects.equals(item.getValue(), viewName))
                        {
                            selectionItem = item;
                            break;
                        }
                    }
                }

                searchableTreeView.getTreeView().getSelectionModel().select(selectionItem);
            }
        };

        inputSource.setIncludeNoneItem(false);
        return inputSource;
    }
}
