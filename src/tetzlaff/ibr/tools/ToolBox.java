package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.listeners.CursorPositionListener;
import tetzlaff.gl.window.listeners.KeyPressListener;
import tetzlaff.gl.window.listeners.MouseButtonPressListener;
import tetzlaff.gl.window.listeners.ScrollListener;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ExtendedLightingModel;
import tetzlaff.models.ReadonlyEnvironmentMapModel;
import tetzlaff.models.SceneViewportModel;
import tetzlaff.util.WindowBasedController;

public final class ToolBox
    implements CursorPositionListener, MouseButtonPressListener, ScrollListener, KeyPressListener, WindowBasedController
{
    private final ToolSelectionModel toolModel;

    //toolSelect
    private final Map<ToolType, Tool> tools;

    //window listener
    @Override
    public void addAsWindowListener(Window<?> window)
    {
        window.addCursorPositionListener(this);
        window.addMouseButtonPressListener(this);
        window.addScrollListener(this);
        window.addKeyPressListener(this);
    }

    private Tool selectedTool()
    {
        if (tools.containsKey(toolModel.getTool()))
        {
            return tools.get(toolModel.getTool());
        }
        else
        {
            return tools.get(ToolType.ORBIT);
        }
    }

    //pass methods to selected tool
    @Override
    public void scroll(Window<?> window, double xOffset, double yOffset)
    {
        try
        {
            selectedTool().scroll(window, xOffset, yOffset);
        }
        catch(RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xPos, double yPos)
    {
        try
        {
            selectedTool().cursorMoved(window, xPos, yPos);
        }
        catch(RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void keyPressed(Window<?> window, int keyCode, ModifierKeys mods)
    {
        try
        {
            selectedTool().keyPressed(window, keyCode, mods);
        }
        catch(RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        try
        {
            selectedTool().mouseButtonPressed(window, buttonIndex, mods);
        }
        catch(RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    //builder
    private ToolBox(ExtendedCameraModel cameraModel, ReadonlyEnvironmentMapModel environmentMapModel, ExtendedLightingModel lightingModel,
        ToolSelectionModel toolSelectionModel, SceneViewportModel sceneViewportModel)
    {
        this.toolModel = toolSelectionModel;

        this.tools = new EnumMap<>(ToolType.class);

        Map<ToolType, ToolBuilder<?>> builders = new EnumMap<>(ToolType.class);
        builders.put(ToolType.DOLLY, DollyTool.getBuilder());
        builders.put(ToolType.ORBIT, OrbitTool.getBuilder());
        builders.put(ToolType.PAN, PanTool.getBuilder());
        builders.put(ToolType.CENTER_POINT, CenterPointTool.getBuilder());
        builders.put(ToolType.LIGHT, LightTool.getBuilder());

        for (Entry<ToolType, ToolBuilder<?>> entries : builders.entrySet())
        {
            tools.put(entries.getKey(),
                entries.getValue()
                    .setCameraModel(cameraModel)
                    .setEnvironmentMapModel(environmentMapModel)
                    .setLightingModel(lightingModel)
                    .setSceneViewportModel(sceneViewportModel)
                    .setToolSelectionModel(toolSelectionModel)
                    .build());
        }
    }

    public static final class Builder
    {
        private ToolSelectionModel toolModel;
        private ExtendedCameraModel cameraModel;
        private ReadonlyEnvironmentMapModel environmentMapModel;
        private ExtendedLightingModel lightingModel;
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

        public Builder setLightingModel(ExtendedLightingModel lightingModel)
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
