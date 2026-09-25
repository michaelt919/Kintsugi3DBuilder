/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.test;

import kintsugi3d.builder.core.texture.ImageReplacer;
import kintsugi3d.builder.state.project.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TestingProjectModel extends ProjectModelBase<SerializableCameraSettings, SerializableEnvironmentSettings,
    SerializableLightGroupSettings<SerializableLightSettings>, SerializableLightSettings, SerializableObjectPoseSettings>
{
    public static final class ErrorMessage
    {
        public final String message;
        public final Throwable throwable;

        private ErrorMessage(String message, Throwable throwable)
        {
            this.message = message;
            this.throwable = throwable;
        }
    }

    private File colorCheckerFile;

    private final Collection<ErrorMessage> errors = new ArrayList<>(1);
    private final Collection<ErrorMessage> warnings = new ArrayList<>(1);

    @Override
    public List<SerializableCameraSettings> getCameraList()
    {
        return List.of();
    }

    @Override
    public List<SerializableEnvironmentSettings> getEnvironmentList()
    {
        return List.of();
    }

    @Override
    public List<SerializableLightGroupSettings<SerializableLightSettings>> getLightGroupList()
    {
        return List.of();
    }

    @Override
    public List<SerializableObjectPoseSettings> getObjectPoseList()
    {
        return List.of();
    }

    @Override
    protected SerializableCameraSettings constructCameraSetting()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SerializableEnvironmentSettings constructEnvironmentSetting()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SerializableLightGroupSettings<SerializableLightSettings> constructLightGroupSetting()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SerializableObjectPoseSettings constructObjectPoseSetting()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getColorCheckerFile()
    {
        return colorCheckerFile;
    }

    @Override
    public void setColorCheckerFile(File colorCheckerFile)
    {
        this.colorCheckerFile = colorCheckerFile;
    }

    @Override
    public void error(String message, Throwable e)
    {
        errors.add(new ErrorMessage(message, e));
    }

    @Override
    public void warn(String message, Throwable e)
    {
        warnings.add(new ErrorMessage(message, e));
    }

    @Override
    public void cancelled(String message)
    {
    }

    @Override
    public void confirm(String title, String header, String message, Runnable onConfirm)
    {
        new Thread(onConfirm).start();
    }

    @Override
    public void requestUserImageReplacement(ImageReplacer imageReplacer)
    {
    }

    public Collection<ErrorMessage> getErrors()
    {
        return Collections.unmodifiableCollection(errors);
    }

    public Collection<ErrorMessage> getWarnings()
    {
        return Collections.unmodifiableCollection(warnings);
    }
}
