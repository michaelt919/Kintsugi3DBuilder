package tetzlaff.ibr.alexkautz_workspace.mount_olympus;

import tetzlaff.ibr.IBRRenderableListModel;
import tetzlaff.ibr.alexkautz_workspace.render.new_tool_setup_rename_this_later.GlobalController;

public class RenderPerams {

    private final IBRRenderableListModel model;

    private final GlobalController globalController;

    public RenderPerams(IBRRenderableListModel model, GlobalController globalController) {
        this.model = model;
        this.globalController = globalController;
    }

    public IBRRenderableListModel getModel() {
        return model;
    }

    public GlobalController getGlobalController() {
        return globalController;
    }
}
