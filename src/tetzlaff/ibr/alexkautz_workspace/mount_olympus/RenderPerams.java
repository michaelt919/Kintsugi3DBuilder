package tetzlaff.ibr.alexkautz_workspace.mount_olympus;

import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.ibr.IBRRenderableListModel;
import tetzlaff.ibr.util.IBRRequestQueue;

public class RenderPerams {

    private IBRRequestQueue<OpenGLContext> requestQueue;

    private final IBRRenderableListModel model;

    public RenderPerams(IBRRenderableListModel model) {
        this.model = model;
    }

    public IBRRenderableListModel getModel() {
        return model;
    }
}
