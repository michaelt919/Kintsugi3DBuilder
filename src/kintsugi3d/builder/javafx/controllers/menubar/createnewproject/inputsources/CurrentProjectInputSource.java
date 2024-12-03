package kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources;

import javafx.stage.FileChooser;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.io.ViewSetReader;
import kintsugi3d.builder.io.primaryview.GenericPrimaryViewSelectionModel;
import kintsugi3d.builder.javafx.MultithreadModels;

import java.util.List;

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
    public void loadProject(String orientationViewName, double rotate)
    {
        ViewSet currentViewSet = MultithreadModels.getInstance().getIOModel().getLoadedViewSet();

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
