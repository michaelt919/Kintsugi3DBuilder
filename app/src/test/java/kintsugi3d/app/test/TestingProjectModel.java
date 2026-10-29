/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Kaelie Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday, Joseph O'Connor
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.app.test;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.texture.ImageReplacer;
import kintsugi3d.builder.state.project.*;
import kintsugi3d.fx.controllers.scene.environment.ObservableEnvironmentSettings;
import kintsugi3d.gl.interactive.DefaultProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
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

    private static final Logger LOG = LoggerFactory.getLogger(TestingProjectModel.class);

    private File colorCheckerFile;
    private final List<ErrorMessage> errors = Collections.synchronizedList(new ArrayList<>(1));
    private final List<ErrorMessage> warnings = Collections.synchronizedList(new ArrayList<>(1));
    private final List<SerializableCameraSettings> cameraList = new ArrayList<>(0);
    private final List<SerializableLightGroupSettings<SerializableLightSettings>> lightList = new ArrayList<>(0);
    private final List<SerializableObjectPoseSettings> objectList = new ArrayList<>(0);
    private final List<SerializableEnvironmentSettings> environmentList = new ArrayList<>(0);

    public TestingProjectModel()
    {
        // Register progress monitor.
        Global.io().addProgressMonitor(new DefaultProgressMonitor()
        {
            @Override
            public void fail(Throwable e)
            {
                TestingProjectModel.this.error("An error occurred during the operation", e);
            }

            @Override
            public void warn(Throwable e)
            {
                TestingProjectModel.this.warn("A potential problem occurred during the operation", e);
            }
        });
    }

    @Override
    public List<SerializableCameraSettings> getCameraList()
    {
        return cameraList;
    }

    @Override
    public List<SerializableEnvironmentSettings> getEnvironmentList()
    {
        return environmentList;
    }

    @Override
    public List<SerializableLightGroupSettings<SerializableLightSettings>> getLightGroupList()
    {
        return lightList;
    }

    @Override
    public List<SerializableObjectPoseSettings> getObjectPoseList()
    {
        return objectList;
    }

    @Override
    protected SerializableCameraSettings constructCameraSetting()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SerializableEnvironmentSettings constructEnvironmentSetting()
    {
        return new ObservableEnvironmentSettings();
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
        LOG.error(message, e);
    }

    @Override
    public void warn(String message, Throwable e)
    {
        warnings.add(new ErrorMessage(message, e));
        LOG.warn(message, e);
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
    public void confirmClose(String text, Runnable onClose)
    {
        new Thread(onClose).start();
    }

    @Override
    public void requestUserImageReplacement(ImageReplacer imageReplacer)
    {
    }

    public List<ErrorMessage> getErrors()
    {
        return Collections.unmodifiableList(errors);
    }

    public List<ErrorMessage> getWarnings()
    {
        return Collections.unmodifiableList(warnings);
    }

    public void clearErrorsAndWarnings()
    {
        errors.clear();
        warnings.clear();
    }
}
