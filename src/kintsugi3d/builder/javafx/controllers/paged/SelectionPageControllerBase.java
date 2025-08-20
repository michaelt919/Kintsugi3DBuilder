package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXML;
import javafx.scene.layout.Region;

public abstract class SelectionPageControllerBase<T> extends PageControllerBase<T, Page<?,?>>
{
    @FXML private Region rootNode;

    @Override
    public Region getRootNode()
    {
        return rootNode;
    }

    public void initPage()
    {

    }
}
