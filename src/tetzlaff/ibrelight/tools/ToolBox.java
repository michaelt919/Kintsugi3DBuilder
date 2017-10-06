package tetzlaff.ibrelight.tools;//Created by alexk on 7/24/2017.

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.Key;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.listeners.*;
import tetzlaff.ibrelight.tools.EnvironmentBrightnessTool.Type;
import tetzlaff.models.*;
import tetzlaff.util.KeyPress;
import tetzlaff.util.MouseMode;
import tetzlaff.util.WindowBasedController;

public final class ToolBox
    implements CursorPositionListener, MouseButtonPressListener, MouseButtonReleaseListener, ScrollListener, KeyPressListener, WindowBasedController
{
    private MouseMode currentMode;
    private final ToolBindingModel toolBindingModel;

    private final Map<DragToolType, DragTool> dragTools;
    private final Map<KeyPressToolType, KeyPressTool> keyPressTools;
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

    private DragTool getSelectedDragTool()
    {
        if (currentMode == null)
        {
            return null;
        }
        else if (dragTools.get(toolBindingModel.getDragTool(currentMode)) != null)
        {
            return dragTools.get(toolBindingModel.getDragTool(currentMode));
        }
        else
        {
            return dragTools.get(DragToolType.ORBIT);
        }
    }

    //pass methods to selected tool
    @Override
    public void scroll(Window<?> window, double xOffset, double yOffset)
    {
//        try
//        {
//            getSelectedDragTool().scroll(window, xOffset, yOffset);
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
                DragTool selectedTool = getSelectedDragTool();
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
    public void keyPressed(Window<?> window, Key key, ModifierKeys mods)
    {
        try
        {
            KeyPressTool tool = keyPressTools.get(toolBindingModel.getKeyPressTool(new KeyPress(key, mods)));
            if (tool != null)
            {
                tool.keyPressed();
            }
        }
        catch(RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods)
    {
        if (getSelectedDragTool() == null) // Only do something if another drag isn't already in progress.
        {
            try
            {
                if (!lightTool.mouseButtonPressed(window, buttonIndex, mods))
                {
                    currentMode = new MouseMode(buttonIndex, mods);
                    DragTool selectedTool = getSelectedDragTool();
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
                DragTool selectedTool = getSelectedDragTool();
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
    private ToolBox(ExtendedCameraModel cameraModel, EnvironmentModel environmentModel, ExtendedLightingModel lightingModel,
        ExtendedObjectModel objectModel, SettingsModel settingsModel, ToolBindingModel toolBindingModel, SceneViewportModel sceneViewportModel)
    {
        this.toolBindingModel = toolBindingModel;

        this.dragTools = new EnumMap<>(DragToolType.class);
        this.keyPressTools = new EnumMap<>(KeyPressToolType.class);

        Map<DragToolType, ToolBuilder<? extends DragTool>> dragToolBuilders = new EnumMap<>(DragToolType.class);
        dragToolBuilders.put(DragToolType.DOLLY, DollyTool.getBuilder());
        dragToolBuilders.put(DragToolType.TWIST, TwistTool.getBuilder());
        dragToolBuilders.put(DragToolType.ORBIT, OrbitTool.getBuilder());
        dragToolBuilders.put(DragToolType.PAN, PanTool.getBuilder());
        dragToolBuilders.put(DragToolType.FOCAL_LENGTH, FocalLengthTool.getBuilder());
        dragToolBuilders.put(DragToolType.ROTATE_ENVIRONMENT, RotateEnvironmentTool.getBuilder());
        dragToolBuilders.put(DragToolType.OBJECT_CENTER, ObjectCenterTool.getBuilder());
        dragToolBuilders.put(DragToolType.OBJECT_ROTATION, ObjectRotationTool.getBuilder());
        dragToolBuilders.put(DragToolType.OBJECT_TWIST, ObjectTwistTool.getBuilder());
        dragToolBuilders.put(DragToolType.LOOK_AT_POINT, LookAtPointTool.getBuilder());

        for (Entry<DragToolType, ToolBuilder<? extends DragTool>> entries : dragToolBuilders.entrySet())
        {
            dragTools.put(entries.getKey(),
                entries.getValue()
                    .setCameraModel(cameraModel)
                    .setEnvironmentMapModel(environmentModel)
                    .setLightingModel(lightingModel)
                    .setObjectModel(objectModel)
                    .setSettingsModel(settingsModel)
                    .setSceneViewportModel(sceneViewportModel)
                    .setToolBindingModel(toolBindingModel)
                    .build());
        }

        Map<KeyPressToolType, ToolBuilder<? extends KeyPressTool>> keyPressToolBuilders = new EnumMap<>(KeyPressToolType.class);
        keyPressToolBuilders.put(KeyPressToolType.ENVIRONMENT_BRIGHTNESS_UP_LARGE, EnvironmentBrightnessTool.getBuilder(Type.UP_LARGE));
        keyPressToolBuilders.put(KeyPressToolType.ENVIRONMENT_BRIGHTNESS_DOWN_LARGE, EnvironmentBrightnessTool.getBuilder(Type.DOWN_LARGE));
        keyPressToolBuilders.put(KeyPressToolType.ENVIRONMENT_BRIGHTNESS_UP_SMALL, EnvironmentBrightnessTool.getBuilder(Type.UP_SMALL));
        keyPressToolBuilders.put(KeyPressToolType.ENVIRONMENT_BRIGHTNESS_DOWN_SMALL, EnvironmentBrightnessTool.getBuilder(Type.DOWN_SMALL));
        keyPressToolBuilders.put(KeyPressToolType.TOGGLE_LIGHTS,
            ToggleSettingTool.getBuilder(model -> model.set("visibleLightsEnabled", !model.get("visibleLightsEnabled", Boolean.class))));
        keyPressToolBuilders.put(KeyPressToolType.TOGGLE_LIGHT_WIDGETS,
            ToggleSettingTool.getBuilder(model -> model.set("lightWidgetsEnabled", !model.get("lightWidgetsEnabled", Boolean.class))));

        for (Entry<KeyPressToolType, ToolBuilder<? extends KeyPressTool>> entries : keyPressToolBuilders.entrySet())
        {
            keyPressTools.put(entries.getKey(),
                entries.getValue()
                    .setCameraModel(cameraModel)
                    .setEnvironmentMapModel(environmentModel)
                    .setLightingModel(lightingModel)
                    .setObjectModel(objectModel)
                    .setSettingsModel(settingsModel)
                    .setSceneViewportModel(sceneViewportModel)
                    .setToolBindingModel(toolBindingModel)
                    .build());
        }

        lightTool = LightTool.getBuilder()
            .setCameraModel(cameraModel)
            .setEnvironmentMapModel(environmentModel)
            .setLightingModel(lightingModel)
            .setObjectModel(objectModel)
            .setSettingsModel(settingsModel)
            .setSceneViewportModel(sceneViewportModel)
            .setToolBindingModel(toolBindingModel)
            .build();
    }

    public static final class Builder
    {
        private ToolBindingModel toolBindingModel;
        private ExtendedCameraModel cameraModel;
        private EnvironmentModel environmentModel;
        private SettingsModel settingsModel;
        private ExtendedLightingModel lightingModel;
        private ExtendedObjectModel objectModel;
        private SceneViewportModel sceneViewportModel;

        public static Builder create()
        {
            return new Builder();
        }

        private Builder()
        {
        }

        public Builder setToolBindingModel(ToolBindingModel toolBindingModel)
        {
            this.toolBindingModel = toolBindingModel;
            return this;
        }

        public Builder setCameraModel(ExtendedCameraModel cameraModel)
        {
            this.cameraModel = cameraModel;
            return this;
        }

        public Builder setEnvironmentModel(EnvironmentModel environmentModel)
        {
            this.environmentModel = environmentModel;
            return this;
        }

        public Builder setObjectModel(ExtendedObjectModel objectModel)
        {
            this.objectModel = objectModel;
            return this;
        }

        public Builder setSettingsModel(SettingsModel settingsModel)
        {
            this.settingsModel = settingsModel;
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
            return new ToolBox(cameraModel, environmentModel, lightingModel, objectModel, settingsModel, toolBindingModel, sceneViewportModel);
        }
    }
}
