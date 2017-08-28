package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.listeners.*;
import tetzlaff.models.ExtendedCameraModel;
import tetzlaff.models.ExtendedLightingModel;
import tetzlaff.models.ReadonlyEnvironmentMapModel;
import tetzlaff.models.SceneViewportModel;
import tetzlaff.util.MouseMode;
import tetzlaff.util.WindowBasedController;

public final class ToolBox
    implements CursorPositionListener, MouseButtonPressListener, MouseButtonReleaseListener, ScrollListener, KeyPressListener, WindowBasedController
{
    private MouseMode currentMode;
    private final ToolBindingModel toolBindingModel;

    private final Map<ToolType, DragTool> tools;
    private final LightTool lightTool;

    //window listener
    @Override
    public void addAsWindowListener(Window<?> window)
    {
        window.addCursorPositionListener(this);
        window.addMouseButtonPressListener(this);
        window.addMouseButtonReleaseListener(this);
        window.addScrollListener(this);
        window.addKeyPressListener(this);
    }

    private DragTool getSelectedTool()
    {
        if (currentMode == null)
        {
            return null;
        }
        else if (tools.get(toolBindingModel.getTool(currentMode)) != null)
        {
            return tools.get(toolBindingModel.getTool(currentMode));
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
//        try
//        {
//            getSelectedTool().scroll(window, xOffset, yOffset);
//        }
//        catch(RuntimeException e)
//        {
//            e.printStackTrace();
//        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xPos, double yPos)
    {
        try
        {
            if (!lightTool.cursorMoved(window, xPos, yPos))
            {
                DragTool selectedTool = getSelectedTool();
                if (selectedTool != null)
                {
                    selectedTool.cursorDragged(new CursorPosition(xPos, yPos), window.getWindowSize());
                }
            }
        }
        catch(RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void keyPressed(Window<?> window, int keyCode, ModifierKeys mods)
    {
//        try
//        {
//            getSelectedTool().keyPressed(window, keyCode, mods);
//        }
//        catch(RuntimeException e)
//        {
//            e.printStackTrace();
//        }
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        if (getSelectedTool() == null) // Only do something if another drag isn't already in progress.
        {
            try
            {
                if (!lightTool.mouseButtonPressed(window, buttonIndex, mods))
                {
                    currentMode = new MouseMode(buttonIndex, mods);
                    DragTool selectedTool = getSelectedTool();
                    if (selectedTool != null)
                    {
                        selectedTool.mouseButtonPressed(window.getCursorPosition(), window.getWindowSize());
                    }
                }
            }
            catch (RuntimeException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void mouseButtonReleased(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        try
        {
            if (Objects.equals(currentMode, new MouseMode(buttonIndex, mods)))
            {
                lightTool.mouseButtonReleased(window, buttonIndex, mods);
                DragTool selectedTool = getSelectedTool();
                if (selectedTool != null)
                {
                    selectedTool.mouseButtonReleased(window.getCursorPosition(), window.getWindowSize());
                }
                currentMode = null;
            }
            else
            {
                lightTool.mouseButtonReleased(window, buttonIndex, mods);
            }
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    //builder
    private ToolBox(ExtendedCameraModel cameraModel, ReadonlyEnvironmentMapModel environmentMapModel, ExtendedLightingModel lightingModel,
        ToolBindingModel toolBindingModel, SceneViewportModel sceneViewportModel)
    {
        this.toolBindingModel = toolBindingModel;

        this.tools = new EnumMap<>(ToolType.class);

        Map<ToolType, ToolBuilder<? extends DragTool>> builders = new EnumMap<>(ToolType.class);
        builders.put(ToolType.DOLLY, DollyTool.getBuilder());
        builders.put(ToolType.ORBIT, OrbitTool.getBuilder());
        builders.put(ToolType.PAN, PanTool.getBuilder());
        builders.put(ToolType.CENTER_POINT, CenterPointTool.getBuilder());

        for (Entry<ToolType, ToolBuilder<? extends DragTool>> entries : builders.entrySet())
        {
            tools.put(entries.getKey(),
                entries.getValue()
                    .setCameraModel(cameraModel)
                    .setEnvironmentMapModel(environmentMapModel)
                    .setLightingModel(lightingModel)
                    .setSceneViewportModel(sceneViewportModel)
                    .setToolSelectionModel(toolBindingModel)
                    .build());
        }

        lightTool = LightTool.getBuilder()
            .setCameraModel(cameraModel)
            .setEnvironmentMapModel(environmentMapModel)
            .setLightingModel(lightingModel)
            .setSceneViewportModel(sceneViewportModel)
            .setToolSelectionModel(toolBindingModel)
            .build();
    }

    public static final class Builder
    {
        private ToolBindingModel toolModel;
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

        public Builder setToolModel(ToolBindingModel toolModel)
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
