/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.tools;//Created by alexk on 7/24/2017.

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import kintsugi3d.gl.window.Canvas3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.window.CursorPosition;
import kintsugi3d.gl.window.Key;
import kintsugi3d.gl.window.ModifierKeys;
import kintsugi3d.gl.window.listeners.*;
import kintsugi3d.builder.tools.MultiplierTool.Type;
import kintsugi3d.builder.state.*;
import kintsugi3d.util.CanvasInputController;
import kintsugi3d.util.KeyPress;
import kintsugi3d.util.MouseMode;

public final class ToolBox
    implements CursorPositionListener, MouseButtonPressListener, MouseButtonReleaseListener, ScrollListener, KeyPressListener, CanvasInputController
{
    private static final Logger log = LoggerFactory.getLogger(ToolBox.class);
    private MouseMode currentMode;
    private final ToolBindingModel toolBindingModel;

    private final Map<DragToolType, DragTool> dragTools;
    private final Map<KeyPressToolType, KeyPressTool> keyPressTools;
    private final LightTool lightTool;

    private final ScrollListener scrollTool;

    //window listener
    @Override
    public void addAsCanvasListener(Canvas3D<? extends kintsugi3d.gl.core.Context<?>> canvas)
    {
        canvas.addCursorPositionListener(this);
        canvas.addMouseButtonPressListener(this);
        canvas.addMouseButtonReleaseListener(this);
        canvas.addScrollListener(this);
        canvas.addKeyPressListener(this);
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
    public void scroll(Canvas3D<? extends kintsugi3d.gl.core.Context<?>> canvas, double xOffset, double yOffset)
    {
        try
        {
            scrollTool.scroll(canvas, xOffset, yOffset);
        }
        catch(RuntimeException e)
        {
            log.error("An error occurred handling scroll event:", e);
        }
    }

    @Override
    public void cursorMoved(Canvas3D<? extends kintsugi3d.gl.core.Context<?>> canvas, double xPos, double yPos)
    {
        try
        {
            if (!lightTool.cursorMoved(canvas, xPos, yPos))
            {
                DragTool selectedTool = getSelectedDragTool();
                if (selectedTool != null)
                {
                    selectedTool.cursorDragged(new CursorPosition(xPos, yPos), canvas.getSize());
                }
            }
        }
        catch(RuntimeException e)
        {
            log.error("Error handling event:", e);
        }
    }

    @Override
    public void keyPressed(Canvas3D<? extends kintsugi3d.gl.core.Context<?>> canvas, Key key, ModifierKeys mods)
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
            log.error("Error handling event:", e);
        }
    }

    @Override
    public void mouseButtonPressed(Canvas3D<? extends kintsugi3d.gl.core.Context<?>> canvas, int buttonIndex, ModifierKeys mods)
    {
        if (getSelectedDragTool() == null) // Only do something if another drag isn't already in progress.
        {
            try
            {
                if (!lightTool.mouseButtonPressed(canvas, buttonIndex, mods))
                {
                    currentMode = new MouseMode(buttonIndex, mods);
                    DragTool selectedTool = getSelectedDragTool();
                    if (selectedTool != null)
                    {
                        selectedTool.mouseButtonPressed(canvas.getCursorPosition(), canvas.getSize());
                    }
                }
            }
            catch (RuntimeException e)
            {
                log.error("Error handling event:", e);
            }
        }
    }

    @Override
    public void mouseButtonReleased(Canvas3D<? extends kintsugi3d.gl.core.Context<?>> canvas, int buttonIndex, ModifierKeys mods)
    {
        try
        {
            if (Objects.equals(currentMode, new MouseMode(buttonIndex, mods)))
            {
                lightTool.mouseButtonReleased(canvas, buttonIndex, mods);
                DragTool selectedTool = getSelectedDragTool();
                if (selectedTool != null)
                {
                    selectedTool.mouseButtonReleased(canvas.getCursorPosition(), canvas.getSize());
                }
                currentMode = null;
            }
            else
            {
                lightTool.mouseButtonReleased(canvas, buttonIndex, mods);
            }
        }
        catch (RuntimeException e)
        {
            log.error("Error handling event:", e);
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

        // A little bit weird but using a closure here simultaneously avoids copy-pasting code and boilerplate classes
        var dependencies = new Object()
        {
            <ToolType> ToolBuilder<? extends ToolType> inject(ToolBuilder<ToolType> builder)
            {
                return builder
                    .setCameraModel(cameraModel)
                    .setEnvironmentMapModel(environmentModel)
                    .setLightingModel(lightingModel)
                    .setObjectModel(objectModel)
                    .setSettingsModel(settingsModel)
                    .setSceneViewportModel(sceneViewportModel)
                    .setToolBindingModel(toolBindingModel);
            }
        };

        for (Entry<DragToolType, ToolBuilder<? extends DragTool>> entries : dragToolBuilders.entrySet())
        {
            dragTools.put(entries.getKey(), dependencies.inject(entries.getValue()).create());
        }

        Map<KeyPressToolType, ToolBuilder<? extends KeyPressTool>> keyPressToolBuilders = new EnumMap<>(KeyPressToolType.class);

        keyPressToolBuilders.put(KeyPressToolType.BACKGROUND_BRIGHTNESS_UP_LARGE, MultiplierTool.getBuilder(
            environmentModel::getBackgroundIntensity,
            intensity -> environmentModel.setBackgroundIntensity((float)intensity),
            Type.UP_LARGE));
        keyPressToolBuilders.put(KeyPressToolType.BACKGROUND_BRIGHTNESS_DOWN_LARGE, MultiplierTool.getBuilder(
            environmentModel::getBackgroundIntensity,
            intensity -> environmentModel.setBackgroundIntensity((float)intensity),
            Type.DOWN_LARGE));
        keyPressToolBuilders.put(KeyPressToolType.BACKGROUND_BRIGHTNESS_UP_SMALL, MultiplierTool.getBuilder(
            environmentModel::getBackgroundIntensity,
            intensity -> environmentModel.setBackgroundIntensity((float)intensity),
            Type.UP_SMALL));
        keyPressToolBuilders.put(KeyPressToolType.BACKGROUND_BRIGHTNESS_DOWN_SMALL, MultiplierTool.getBuilder(
            environmentModel::getBackgroundIntensity,
            intensity -> environmentModel.setBackgroundIntensity((float)intensity),
            Type.DOWN_SMALL));

        keyPressToolBuilders.put(KeyPressToolType.ENVIRONMENT_BRIGHTNESS_UP_LARGE, MultiplierTool.getBuilder(
            environmentModel::getEnvironmentIntensity,
            intensity -> environmentModel.setEnvironmentIntensity((float)intensity),
            Type.UP_LARGE));
        keyPressToolBuilders.put(KeyPressToolType.ENVIRONMENT_BRIGHTNESS_DOWN_LARGE, MultiplierTool.getBuilder(
            environmentModel::getEnvironmentIntensity,
            intensity -> environmentModel.setEnvironmentIntensity((float)intensity),
            Type.DOWN_LARGE));
        keyPressToolBuilders.put(KeyPressToolType.ENVIRONMENT_BRIGHTNESS_UP_SMALL, MultiplierTool.getBuilder(
            environmentModel::getEnvironmentIntensity,
            intensity -> environmentModel.setEnvironmentIntensity((float)intensity),
            Type.UP_SMALL));
        keyPressToolBuilders.put(KeyPressToolType.ENVIRONMENT_BRIGHTNESS_DOWN_SMALL, MultiplierTool.getBuilder(
            environmentModel::getEnvironmentIntensity,
            intensity -> environmentModel.setEnvironmentIntensity((float)intensity),
            Type.DOWN_SMALL));

        keyPressToolBuilders.put(KeyPressToolType.TOGGLE_LIGHTS, ToggleSettingTool.getBuilder("visibleLightsEnabled"));
        keyPressToolBuilders.put(KeyPressToolType.TOGGLE_LIGHT_WIDGETS, ToggleSettingTool.getBuilder("lightWidgetsEnabled"));

        for (Entry<KeyPressToolType, ToolBuilder<? extends KeyPressTool>> entries : keyPressToolBuilders.entrySet())
        {
            keyPressTools.put(entries.getKey(), dependencies.inject(entries.getValue()).create());
        }

        scrollTool = dependencies.inject(ScrollDollyTool.getBuilder()).create();

        lightTool = dependencies.inject(LightTool.getBuilder()).create();
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

        public CanvasInputController build()
        {
            return new ToolBox(cameraModel, environmentModel, lightingModel, objectModel, settingsModel, toolBindingModel, sceneViewportModel);
        }
    }
}
