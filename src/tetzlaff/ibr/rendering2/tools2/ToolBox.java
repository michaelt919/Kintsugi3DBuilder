package tetzlaff.ibr.rendering2.tools2;//Created by alexk on 7/24/2017.

import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.ibr.ControllableToolModel;
import tetzlaff.mvc.models.ExtendedCameraModel;
import tetzlaff.mvc.models.impl.LightingModelBase;
import tetzlaff.mvc.models.impl.EnvironmentMapModelBase;

public class ToolBox extends AbstractTool implements Controller {

    private ControllableToolModel toolModel;

    //window listener
    @Override
    public void addAsWindowListener(Window<?> window) {
        window.addCursorPositionListener(this);
        window.addMouseButtonPressListener(this);
        window.addScrollListener(this);
        window.addKeyPressListener(this);
    }

    //toolSelect
    private DollyTool dollyTool;
    private OrbitTool orbitTool;
    private PanTool panTool;
    private CenterPointTool centerPointTool;
    public enum TOOL{
        DOLLY, ORBIT, PAN, LIGHT_DRAG, CENTER_POINT
    }
    private AbstractTool selectedTool(){
        switch (toolModel.getTool()){
            case DOLLY: return dollyTool;
            case ORBIT: return orbitTool;
            case PAN: return panTool;
            case CENTER_POINT: return centerPointTool;
            default: return orbitTool;
        }
    }

    //pass methods to selected tool
    @Override
    public void scroll(Window<?> window, double xoffset, double yoffset) {
        selectedTool().scroll(window, xoffset, yoffset);
    }

    @Override
    public void cursorMoved(Window<?> window, double xpos, double ypos) {
        selectedTool().cursorMoved(window, xpos, ypos);
    }

    @Override
    public void keyPressed(Window<?> window, int keycode, ModifierKeys mods) {
        selectedTool().keyPressed(window, keycode, mods);
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) {
        selectedTool().mouseButtonPressed(window, buttonIndex, mods);
    }

    //builder
    private ToolBox(ExtendedCameraModel cameraModel, EnvironmentMapModelBase environmentMapModel, LightingModelBase lightModel, ControllableToolModel toolModel, Window<?> window) {
        super(cameraModel, environmentMapModel, lightModel);
        this.toolModel = toolModel;

        dollyTool = new DollyTool(cameraModel, environmentMapModel, lightModel);
        orbitTool = new OrbitTool(cameraModel, environmentMapModel, lightModel);
        panTool = new PanTool(cameraModel, environmentMapModel, lightModel);
        centerPointTool = new CenterPointTool(cameraModel, environmentMapModel, lightModel, toolModel);

        addAsWindowListener(window);
    }

    public static final class ToolBoxBuilder {
        private ControllableToolModel toolModel;
        private ExtendedCameraModel cameraModel;
        private EnvironmentMapModelBase environmentMapModel;
        private LightingModelBase lightModel;
        private Window<?> window;

        public static ToolBoxBuilder aToolBox() {
            return new ToolBoxBuilder();
        }

        public ToolBoxBuilder setToolModel(ControllableToolModel toolModel) {
            this.toolModel = toolModel;
            return this;
        }

        public ToolBoxBuilder setCameraModel(ExtendedCameraModel cameraModel) {
            this.cameraModel = cameraModel;
            return this;
        }

        public ToolBoxBuilder setEnvironmentMapModel(EnvironmentMapModelBase environmentMapModel) {
            this.environmentMapModel = environmentMapModel;
            return this;
        }

        public ToolBoxBuilder setLightModel(LightingModelBase lightModel) {
            this.lightModel = lightModel;
            return this;
        }

        public ToolBoxBuilder setWindow(Window<?> window) {
            this.window = window;
            return this;
        }

        public ToolBox build() {
            return new ToolBox(cameraModel, environmentMapModel, lightModel, toolModel, window);
        }
    }
}
