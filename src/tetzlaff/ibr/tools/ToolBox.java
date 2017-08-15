package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.listeners.CursorPositionListener;
import tetzlaff.gl.window.listeners.KeyPressListener;
import tetzlaff.gl.window.listeners.MouseButtonPressListener;
import tetzlaff.gl.window.listeners.ScrollListener;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ReadonlyEnvironmentMapModel;
import tetzlaff.models.ReadonlyLightingModel;
import tetzlaff.models.SceneViewportModel;
import tetzlaff.util.WindowBasedController;

public class ToolBox implements CursorPositionListener, MouseButtonPressListener, ScrollListener, KeyPressListener, WindowBasedController 
{
    private ToolSelectionModel toolModel;

    //toolSelect
    private DollyTool dollyTool;
    private OrbitTool orbitTool;
    private PanTool panTool;
    private CenterPointTool centerPointTool;

    //window listener
    @Override
    public void addAsWindowListener(Window<?> window) 
    {
        window.addCursorPositionListener(this);
        window.addMouseButtonPressListener(this);
        window.addScrollListener(this);
        window.addKeyPressListener(this);
    }
    
    private AbstractTool selectedTool()
    {
        switch (toolModel.getTool())
        {
            case DOLLY: return dollyTool;
            case ORBIT: return orbitTool;
            case PAN: return panTool;
            case CENTER_POINT: return centerPointTool;
            default: return orbitTool;
        }
    }

    //pass methods to selected tool
    @Override
    public void scroll(Window<?> window, double xoffset, double yoffset) 
    {
        selectedTool().scroll(window, xoffset, yoffset);
    }

    @Override
    public void cursorMoved(Window<?> window, double xpos, double ypos) 
    {
        selectedTool().cursorMoved(window, xpos, ypos);
    }

    @Override
    public void keyPressed(Window<?> window, int keycode, ModifierKeys mods) 
    {
        selectedTool().keyPressed(window, keycode, mods);
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) 
    {
        selectedTool().mouseButtonPressed(window, buttonIndex, mods);
    }

    //builder
    private ToolBox(ExtendedCameraModel cameraModel, ReadonlyEnvironmentMapModel environmentMapModel, ReadonlyLightingModel lightingModel, 
    		ToolSelectionModel toolModel, SceneViewportModel sceneViewportModel) 
    {
        this.toolModel = toolModel;

        dollyTool = new DollyTool(cameraModel, environmentMapModel, lightingModel, sceneViewportModel);
        orbitTool = new OrbitTool(cameraModel, environmentMapModel, lightingModel, sceneViewportModel);
        panTool = new PanTool(cameraModel, environmentMapModel, lightingModel, sceneViewportModel);
        centerPointTool = new CenterPointTool(cameraModel, environmentMapModel, lightingModel, toolModel, sceneViewportModel);
    }

    public static final class Builder 
    {
        private ToolSelectionModel toolModel;
        private ExtendedCameraModel cameraModel;
        private ReadonlyEnvironmentMapModel environmentMapModel;
        private ReadonlyLightingModel lightingModel;
        private SceneViewportModel sceneViewportModel;

        public static Builder create() 
        {
            return new Builder();
        }
        
        private Builder()
        {
        }

        public Builder setToolModel(ToolSelectionModel toolModel) 
        {
            this.toolModel = toolModel;
            return this;
        }

        public Builder setCameraModel(ExtendedCameraModel cameraModel) 
        {
            this.cameraModel = cameraModel;
            return this;
        }

        public Builder setEnvironmentMapModel(ReadonlyEnvironmentMapModel environmentMapModel) 
        {
            this.environmentMapModel = environmentMapModel;
            return this;
        }

        public Builder setLightingModel(ReadonlyLightingModel lightingModel) 
        {
            this.lightingModel = lightingModel;
            return this;
        }

        public Builder setSceneViewportModel(SceneViewportModel sceneViewportModel) 
        {
            this.sceneViewportModel = sceneViewportModel;
            return this;
        }

        public WindowBasedController build() 
        {
            return new ToolBox(cameraModel, environmentMapModel, lightingModel, toolModel, sceneViewportModel);
        }
    }
}
