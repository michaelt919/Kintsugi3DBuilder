package tetzlaff.ibr.alexkautz_workspace.render.new_tool_setup_rename_this_later;

import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.window.*;
import tetzlaff.gl.window.listeners.MouseButtonPressListener;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.rendering.ImageBasedRendererList;
import tetzlaff.mvc.controllers.CameraController;
import tetzlaff.mvc.controllers.LightController;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.ReadonlyLightModel;
import tetzlaff.mvc.models.SceneViewportModel;
import tetzlaff.gl.Context;

import javax.swing.*;


public class DragToolController <ContextType extends Context<ContextType>> implements LightController, CameraController, MouseButtonPressListener {

    private DragToolController(LightModelX lightModelX, CameraModelX cameraModelX, Window window, GlobalController globalController) {
        this.lightModelX = lightModelX;
        this.cameraModelX = cameraModelX;
        this.globalController = globalController;
        addAsWindowListener(window);
    }

    private final LightModelX lightModelX;
    private final CameraModelX cameraModelX;
    private final GlobalController globalController;

    private int mousedLightIndex = -1;

    private Boolean enabled(){
        return globalController.getTool() == Tool.DRAG;
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
        private LightModelX lightModelX;
        private CameraModelX cameraModelX;
        private GlobalController globalController;
        private Window window;

        private Builder() {
        }

        public static Builder aDragToolController() {
            return new Builder();
        }

        public Builder setLightModelX(LightModelX lightModelX) {
            this.lightModelX = lightModelX;
            return this;
        }

        public Builder setCameraModelX(CameraModelX cameraModelX) {
            this.cameraModelX = cameraModelX;
            return this;
        }

        public Builder setGlobalController(GlobalController globalController) {
            this.globalController = globalController;
            return this;
        }

        public Builder setWindow(Window window) {
            this.window = window;
            return this;
        }

        public DragToolController build() {
            DragToolController dragToolController = new DragToolController(lightModelX, cameraModelX,window, globalController);
            return dragToolController;
        }
    }
}