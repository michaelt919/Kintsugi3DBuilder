/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.internal;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.IntegerExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import kintsugi3d.builder.javafx.controllers.scene.camera.ObservableCameraSetting;
import kintsugi3d.builder.javafx.controllers.scene.environment.ObservableEnvironmentSetting;
import kintsugi3d.builder.javafx.controllers.scene.lights.ObservableLightGroupSetting;
import kintsugi3d.builder.javafx.controllers.scene.lights.ObservableLightInstanceSetting;
import kintsugi3d.builder.javafx.controllers.scene.object.ObservableObjectPoseSetting;
import kintsugi3d.builder.state.ProjectModelBase;
import kintsugi3d.gl.vecmath.Vector3;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Project model with all the JavaFX properties and bindings.
 */
public class ObservableProjectModel extends ProjectModelBase<
    ObservableCameraSetting, ObservableEnvironmentSetting, ObservableLightGroupSetting,
    ObservableLightInstanceSetting, ObservableObjectPoseSetting>
{
    private final ObservableList<ObservableCameraSetting> cameraList =
        new ObservableListWrapper<>(Collections.synchronizedList(new ArrayList<>(16)));
    private final ObservableList<ObservableEnvironmentSetting> environmentList =
        new ObservableListWrapper<>(Collections.synchronizedList(new ArrayList<>(16)));
    private final ObservableList<ObservableLightGroupSetting> lightGroupList =
        new ObservableListWrapper<>(Collections.synchronizedList(new ArrayList<>(16)));
    private final ObservableList<ObservableObjectPoseSetting> objectPoseList =
        new ObservableListWrapper<>(Collections.synchronizedList(new ArrayList<>(16)));

    private final ObjectProperty<File> colorCheckerFile = new SimpleObjectProperty<>();

    private final BooleanProperty projectOpen = new SimpleBooleanProperty(false);
    private final StringProperty projectName = new SimpleStringProperty(NULL_PROJECT_NAME);
    private final BooleanProperty projectLoaded = new SimpleBooleanProperty();
    private final BooleanProperty projectProcessed = new SimpleBooleanProperty();
    private final IntegerProperty processedTextureResolution = new SimpleIntegerProperty();
    private final ObjectProperty<Vector3> modelSize = new SimpleObjectProperty<>(new Vector3(1));

    private final ObjectProperty<EventHandler<ProcessingCompleteEvent>> onProcessingComplete = new SimpleObjectProperty<>();

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<ObservableCameraSetting> getCameraList()
    {
        return this.cameraList;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<ObservableEnvironmentSetting> getEnvironmentList()
    {
        return this.environmentList;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<ObservableLightGroupSetting> getLightGroupList()
    {
        return this.lightGroupList;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<ObservableObjectPoseSetting> getObjectPoseList()
    {
        return this.objectPoseList;
    }

    @Override
    protected ObservableCameraSetting constructCameraSetting()
    {
        return new ObservableCameraSetting();
    }

    @Override
    protected ObservableEnvironmentSetting constructEnvironmentSetting()
    {
        return new ObservableEnvironmentSetting();
    }

    @Override
    protected ObservableLightGroupSetting constructLightGroupSetting()
    {
        return new ObservableLightGroupSetting();
    }

    @Override
    protected ObservableObjectPoseSetting constructObjectPoseSetting()
    {
        return new ObservableObjectPoseSetting();
    }

    public EventHandler<ProcessingCompleteEvent> getOnProcessingComplete()
    {
        return onProcessingComplete.get();
    }

    public ObjectProperty<EventHandler<ProcessingCompleteEvent>> onProcessingCompleteProperty()
    {
        return onProcessingComplete;
    }

    public void setOnProcessingComplete(EventHandler<ProcessingCompleteEvent> onProcessingComplete)
    {
        this.onProcessingComplete.set(onProcessingComplete);
    }

    @Override
    public void notifyProcessingComplete()
    {
        onProcessingComplete.get().handle(new ProcessingCompleteEvent());
    }

    @Override
    public File getColorCheckerFile()
    {
        return this.colorCheckerFile.get();
    }

    @Override
    public void setColorCheckerFile(File colorCheckerFile)
    {
        this.colorCheckerFile.set(colorCheckerFile);
    }

    @Override
    public boolean isProjectOpen()
    {
        return projectOpen.get();
    }

    @Override
    public void setProjectOpen(boolean projectOpen)
    {
        this.projectOpen.set(projectOpen);
    }

    public BooleanExpression getProjectOpenProperty()
    {
        return projectOpen;
    }

    @Override
    public String getProjectName()
    {
        return projectName.get();
    }

    @Override
    public void setProjectName(String projectName)
    {
        this.projectName.set(projectName);
    }

    public StringExpression getProjectNameProperty()
    {
        return projectName;
    }

    @Override
    public boolean isProjectLoaded()
    {
        return projectLoaded.get();
    }

    @Override
    public void setProjectLoaded(boolean projectLoaded)
    {
        this.projectLoaded.set(projectLoaded);
    }

    public BooleanExpression getProjectLoadedProperty()
    {
        return projectLoaded;
    }

    @Override
    public boolean isProjectProcessed()
    {
        return projectProcessed.get();
    }

    @Override
    public void setProjectProcessed(boolean projectProcessed)
    {
        this.projectProcessed.set(projectProcessed);
    }

    public BooleanExpression getProjectProcessedProperty()
    {
        return projectProcessed;
    }

    @Override
    public int getProcessedTextureResolution()
    {
        return processedTextureResolution.get();
    }

    @Override
    public void setProcessedTextureResolution(int processedTextureResolution)
    {
        this.processedTextureResolution.set(processedTextureResolution);
    }

    public IntegerExpression getProcessedTextureResolutionProperty()
    {
        return processedTextureResolution;
    }

    @Override
    public Vector3 getModelSize()
    {
        return modelSize.get();
    }

    @Override
    public void setModelSize(Vector3 modelSize)
    {
        this.modelSize.set(modelSize);
    }

    public ObjectProperty<Vector3> getModelSizeProperty()
    {
        return modelSize;
    }
}
