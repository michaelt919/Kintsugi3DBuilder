package tetzlaff.ibr.rendering2.tools;

import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.window.*;
import tetzlaff.gl.window.listeners.MouseButtonPressListener;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.rendering.ImageBasedRendererList;
import tetzlaff.ibr.rendering2.CameraModel2;
import tetzlaff.ibr.rendering2.LightModel2;
import tetzlaff.mvc.controllers.CameraController;
import tetzlaff.mvc.controllers.LightController;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.ReadonlyLightModel;
import tetzlaff.mvc.models.SceneViewportModel;
import tetzlaff.gl.Context;


public class DragToolController <ContextType extends Context<ContextType>> implements LightController, CameraController, MouseButtonPressListener {

    private DragToolController(LightModel2 lightModel2, CameraModel2 cameraModel2, Window window, ToolModel2 toolModel2) {
        this.lightModel2 = lightModel2;
        this.cameraModel2 = cameraModel2;
        this.toolModel2 = toolModel2;
        addAsWindowListener(window);
    }

    private final LightModel2 lightModel2;
    private final CameraModel2 cameraModel2;
    private final ToolModel2 toolModel2;

    private int mousedLightIndex = -1;

    private Boolean enabled(){
        return toolModel2.getTool() == Tool.DRAG;
    }

    @Override
    public void addAsWindowListener(Window<?> window) {
        window.addMouseButtonPressListener(this);
    }

    @Override
    public ReadonlyLightModel getLightModel() {
        return null;
    }

    @Override
    public ReadonlyCameraModel getCameraModel() {
        return null;
    }


    private ImageBasedRendererList<OpenGLContext> model;
    public void setModel(ImageBasedRendererList<OpenGLContext> model) {
        this.model = model;
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) {
        if(!enabled()){
            return;
        }
        try
        {
            if (model.getSelectedItem() != null)
            {
                CursorPosition pos = window.getCursorPosition();
                WindowSize size = window.getWindowSize();
                double x = pos.x / size.width;
                double y = pos.y / size.height;

                IBRRenderable<OpenGLContext> item = model.getSelectedItem();

                SceneViewportModel viewportModel = item.getSceneViewportModel();

                Object object = viewportModel.getObjectAtCoordinates(x,y);

                if(object != null) anilizeObject(object);

                System.out.println(object);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void anilizeObject(Object object){
        System.out.println("-----------------");

        System.out.println(object.getClass());

        if(object instanceof String){
            System.out.println("Value: " + object);

            if(((String) object).startsWith("Light")){
                mousedLightIndex = Integer.parseInt(((String) object).substring(5));
                System.out.println("mousedLightIndex: " + mousedLightIndex);
            }
        }

        System.out.println("-----------------");
    }









    public static final class Builder {
        private LightModel2 lightModel2;
        private CameraModel2 cameraModel2;
        private ToolModel2 toolModel2;
        private Window window;

        private Builder() {
        }

        public static Builder aDragToolController() {
            return new Builder();
        }

        public Builder setLightModel2(LightModel2 lightModel2) {
            this.lightModel2 = lightModel2;
            return this;
        }

        public Builder setCameraModel2(CameraModel2 cameraModel2) {
            this.cameraModel2 = cameraModel2;
            return this;
        }

        public Builder setToolModel2(ToolModel2 toolModel2) {
            this.toolModel2 = toolModel2;
            return this;
        }

        public Builder setWindow(Window window) {
            this.window = window;
            return this;
        }

        public DragToolController build() {
            DragToolController dragToolController = new DragToolController(lightModel2, cameraModel2,window, toolModel2);
            return dragToolController;
        }
    }
}