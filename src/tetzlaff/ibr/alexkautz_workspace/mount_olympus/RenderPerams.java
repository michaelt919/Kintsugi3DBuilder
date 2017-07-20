package tetzlaff.ibr.alexkautz_workspace.mount_olympus;

import tetzlaff.ibr.IBRRenderableListModel;
import tetzlaff.ibr.rendering2.tools.ToolModel2;

public class RenderPerams {

    private final IBRRenderableListModel model;

    private final ToolModel2 toolModel2;

    public RenderPerams(IBRRenderableListModel model, ToolModel2 toolModel2) {
        this.model = model;
        this.toolModel2 = toolModel2;
    }

    public IBRRenderableListModel getModel() {
        return model;
    }

    public ToolModel2 getToolModel2() {
        return toolModel2;
    }
}
