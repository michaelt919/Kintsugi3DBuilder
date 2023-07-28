/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.tools;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

import kintsugi3d.builder.state.*;

public final class MultiplierTool implements KeyPressTool
{
    private static final float SMALL_FACTOR = 1.125f;
    private static final float LARGE_FACTOR = 2.0f;

    public enum Type
    {
        UP_LARGE(LARGE_FACTOR),
        DOWN_LARGE(1 / LARGE_FACTOR),
        UP_SMALL(SMALL_FACTOR),
        DOWN_SMALL(1 / SMALL_FACTOR);

        private final float factor;

        Type(float factor)
        {
            this.factor = factor;
        }
    }

    private static class Builder implements ToolBuilder<MultiplierTool>
    {
        private final DoubleSupplier getter;
        private final DoubleConsumer setter;

        private final float factor;

        Builder(DoubleSupplier getter, DoubleConsumer setter, Type type)
        {
            this.getter = getter;
            this.setter = setter;
            this.factor = type.factor;
        }

        @Override
        public ToolBuilder<MultiplierTool> setToolBindingModel(ToolBindingModel toolBindingModel)
        {
            return this;
        }

        @Override
        public ToolBuilder<MultiplierTool> setCameraModel(ExtendedCameraModel cameraModel)
        {
            return this;
        }

        @Override
        public ToolBuilder<MultiplierTool> setEnvironmentMapModel(EnvironmentModel environmentModel)
        {
            return this;
        }

        @Override
        public ToolBuilder<MultiplierTool> setLightingModel(ExtendedLightingModel lightingModel)
        {
            return this;
        }

        @Override
        public ToolBuilder<MultiplierTool> setObjectModel(ExtendedObjectModel lightingModel)
        {
            return this;
        }

        @Override
        public ToolBuilder<MultiplierTool> setSceneViewportModel(SceneViewportModel sceneViewportModel)
        {
            return this;
        }

        @Override
        public ToolBuilder<MultiplierTool> setSettingsModel(SettingsModel settingsModel)
        {
            return this;
        }

        @Override
        public MultiplierTool create()
        {
            return new MultiplierTool(getter, setter, factor);
        }
    }

    static ToolBuilder<MultiplierTool> getBuilder(DoubleSupplier getter, DoubleConsumer setter, Type type)
    {
        return new Builder(getter, setter, type);
    }

    private final DoubleSupplier getter;
    private final DoubleConsumer setter;
    private final float factor;

    private MultiplierTool(DoubleSupplier getter, DoubleConsumer setter, float factor)
    {
        this.getter = getter;
        this.setter = setter;
        this.factor = factor;
    }

    @Override
    public void keyPressed()
    {
        setter.accept(getter.getAsDouble() * this.factor);
    }
}
