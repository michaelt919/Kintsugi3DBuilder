package kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources;

import javafx.scene.control.TreeItem;
import javafx.stage.FileChooser;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.io.ViewSetReader;
import kintsugi3d.builder.io.primaryview.GenericPrimaryViewSelectionModel;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.PrimaryViewSelectController;

import java.util.List;
import java.util.Objects;

public class CurrentProjectInputSource extends InputSource
{
    @Override
    public List<FileChooser.ExtensionFilter> getExtensionFilters()
    {
        return List.of();
    }

    @Override
    ViewSetReader getCameraFileReader()
    {
        return null;
    }

    @Override
    public void initTreeView()
    {
        ViewSet currentViewSet = MultithreadModels.getInstance().getIOModel().getLoadedViewSet();
        primaryViewSelectionModel = GenericPrimaryViewSelectionModel.createInstance("Current Project", currentViewSet);

        addTreeElems(primaryViewSelectionModel);
        searchableTreeView.bind();
    }

    @Override
    public void setOrientationViewDefaultSelections(PrimaryViewSelectController controller)
    {
        ViewSet currentViewSet = MultithreadModels.getInstance().getIOModel().getLoadedViewSet();

        if (currentViewSet == null)
            return;

        // Set the initial selection to what is currently being used
        TreeItem<String> selectionItem = InputSource.NONE_ITEM;

        if (currentViewSet.getOrientationViewIndex() >= 0)
        {
            String viewName = currentViewSet.getImageFileName(currentViewSet.getOrientationViewIndex());

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
        controller.setImageRotation(currentViewSet.getOrientationViewRotationDegrees());
    }

    @Override
    public void loadProject(String orientationViewName, double rotate)
    {
        ViewSet currentViewSet = MultithreadModels.getInstance().getIOModel().getLoadedViewSet();

        if (currentViewSet == null)
            return;

        if (orientationViewName == null)
        {
            currentViewSet.setOrientationViewIndex(-1);
        }
        else
        {
            currentViewSet.setOrientationView(orientationViewName);
        }

        currentViewSet.setOrientationViewRotationDegrees(rotate);
    }

    @Override
    public boolean equals(Object obj)
    {
        return false;
    }
}
