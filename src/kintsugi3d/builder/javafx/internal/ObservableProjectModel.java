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

package kintsugi3d.builder.javafx.internal;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.IntegerExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.texture.ImageReplacer;
import kintsugi3d.builder.io.IOModel;
import kintsugi3d.builder.javafx.controllers.scene.camera.ObservableCameraSettings;
import kintsugi3d.builder.javafx.controllers.scene.environment.ObservableEnvironmentSettings;
import kintsugi3d.builder.javafx.controllers.scene.lights.ObservableLightGroupSettings;
import kintsugi3d.builder.javafx.controllers.scene.lights.ObservableLightSettings;
import kintsugi3d.builder.javafx.controllers.scene.object.ObservableObjectPoseSettings;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.builder.javafx.core.ExperienceManager;
import kintsugi3d.builder.javafx.experience.ReplaceImage;
import kintsugi3d.builder.state.project.ProjectModelBase;
import kintsugi3d.gl.vecmath.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Project model with all the JavaFX properties and bindings.
 */
public class ObservableProjectModel extends ProjectModelBase<
    ObservableCameraSettings, ObservableEnvironmentSettings, ObservableLightGroupSettings,
    ObservableLightSettings, ObservableObjectPoseSettings>
{
    private static final Logger LOG = LoggerFactory.getLogger(ObservableProjectModel.class);

    private final ObservableList<ObservableCameraSettings> cameraList =
        new ObservableListWrapper<>(Collections.synchronizedList(new ArrayList<>(16)));
    private final ObservableList<ObservableEnvironmentSettings> environmentList =
        new ObservableListWrapper<>(Collections.synchronizedList(new ArrayList<>(16)));
    private final ObservableList<ObservableLightGroupSettings> lightGroupList =
        new ObservableListWrapper<>(Collections.synchronizedList(new ArrayList<>(16)));
    private final ObservableList<ObservableObjectPoseSettings> objectPoseList =
        new ObservableListWrapper<>(Collections.synchronizedList(new ArrayList<>(16)));

    private final ObjectProperty<File> colorCheckerFile = new SimpleObjectProperty<>();

    private final BooleanProperty projectOpen = new SimpleBooleanProperty(false);
    private final StringProperty projectName = new SimpleStringProperty(NULL_PROJECT_NAME);
    private final BooleanProperty projectLoaded = new SimpleBooleanProperty();
    private final BooleanProperty projectProcessed = new SimpleBooleanProperty();
    private final IntegerProperty processedTextureWidth = new SimpleIntegerProperty();
    private final IntegerProperty processedTextureHeight = new SimpleIntegerProperty();
    private final ObjectProperty<Vector3> modelSize = new SimpleObjectProperty<>(new Vector3(1));

    private final ObjectProperty<EventHandler<ProcessingCompleteEvent>> onProcessingComplete = new SimpleObjectProperty<>();

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<ObservableCameraSettings> getCameraList()
    {
        return this.cameraList;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<ObservableEnvironmentSettings> getEnvironmentList()
    {
        return this.environmentList;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<ObservableLightGroupSettings> getLightGroupList()
    {
        return this.lightGroupList;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<ObservableObjectPoseSettings> getObjectPoseList()
    {
        return this.objectPoseList;
    }

    @Override
    protected ObservableCameraSettings constructCameraSetting()
    {
        return new ObservableCameraSettings();
    }

    @Override
    protected ObservableEnvironmentSettings constructEnvironmentSetting()
    {
        return new ObservableEnvironmentSettings();
    }

    @Override
    protected ObservableLightGroupSettings constructLightGroupSetting()
    {
        return new ObservableLightGroupSettings();
    }

    @Override
    protected ObservableObjectPoseSettings constructObjectPoseSetting()
    {
        return new ObservableObjectPoseSettings();
    }

    public EventHandler<ProcessingCompleteEvent> getOnProcessingComplete()
    {
        return onProcessingComplete.get();
    }

    public void setOnProcessingComplete(EventHandler<ProcessingCompleteEvent> onProcessingComplete)
    {
        this.onProcessingComplete.set(onProcessingComplete);
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

    public boolean isProjectOpen()
    {
        return projectOpen.get();
    }

    private void setProjectOpen(boolean projectOpen)
    {
        this.projectOpen.set(projectOpen);
    }

    public BooleanExpression getProjectOpenProperty()
    {
        return projectOpen;
    }

    public String getProjectName()
    {
        return projectName.get();
    }

    private void setProjectName(String projectName)
    {
        this.projectName.set(projectName);
    }

    public StringExpression getProjectNameProperty()
    {
        return projectName;
    }

    public boolean isProjectLoaded()
    {
        return projectLoaded.get();
    }

    private void setProjectLoaded(boolean projectLoaded)
    {
        this.projectLoaded.set(projectLoaded);
    }

    public BooleanExpression getProjectLoadedProperty()
    {
        return projectLoaded;
    }

    public boolean isProjectProcessed()
    {
        return projectProcessed.get();
    }

    private void setProjectProcessed(boolean projectProcessed)
    {
        this.projectProcessed.set(projectProcessed);
    }

    public BooleanExpression getProjectProcessedProperty()
    {
        return projectProcessed;
    }

    public int getProcessedTextureWidth()
    {
        return processedTextureWidth.get();
    }

    public int getProcessedTextureHeight()
    {
        return processedTextureHeight.get();
    }

    private void setProcessedTextureWidth(int processedTextureWidth)
    {
        this.processedTextureWidth.set(processedTextureWidth);
    }

    private void setProcessedTextureHeight(int processedTextureHeight)
    {
        this.processedTextureHeight.set(processedTextureHeight);
    }

    public IntegerExpression getProcessedTextureWidthProperty()
    {
        return processedTextureWidth;
    }

    public IntegerExpression getProcessedTextureHeightProperty()
    {
        return processedTextureHeight;
    }

    public Vector3 getModelSize()
    {
        return modelSize.get();
    }

    private void setModelSize(Vector3 modelSize)
    {
        this.modelSize.set(modelSize);
    }

    public ObjectProperty<Vector3> getModelSizeProperty()
    {
        return modelSize;
    }

    private void notifyProcessingComplete()
    {
        onProcessingComplete.get().handle(new ProcessingCompleteEvent());
    }

    @Override
    public void error(String message, Throwable e)
    {
        ExceptionHandling.error(message, e);
    }

    @Override
    public void warn(String message, Throwable e)
    {
        ExceptionHandling.warn(message, e);
    }

    @Override
    public void cancelled(String message)
    {
        Alert alert = new Alert(AlertType.INFORMATION, message);
        alert.setTitle("Cancelled");
        alert.setHeaderText("Cancelled");
        alert.show();
    }

    @Override
    public void confirm(String title, String header, String message, Runnable onConfirm)
    {
        // Temp solution -- will eventually create a custom modal.
        Alert alert = new Alert(AlertType.CONFIRMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(header);
        var result = alert.showAndWait();

        if (result.isPresent() && result.get().equals(ButtonType.OK))
        {
            onConfirm.run();
        }
    }

    @Override
    public void requestUserImageReplacement(ImageReplacer imageReplacer)
    {
        ReplaceImage replaceImage = ExperienceManager.getInstance().getExperience(
            ExperienceManager.REPLACE_IMAGE, ReplaceImage.class);
        if (replaceImage != null)
        {
            replaceImage.setData(imageReplacer);
            replaceImage.tryOpen();
        }
        else
        {
            LOG.error("Failed to open image replacement modal.");
        }
    }

    public void registerIOListeners()
    {
        IOModel ioModel = Global.io();

        ioModel.projectOpenedListeners().addListener(event ->
            Platform.runLater(() ->
            {
                setProjectOpen(true);
                setProjectName(event.projectName);
            }));

        ioModel.projectSavedListeners().addListener(event ->
            Platform.runLater(() -> setProjectName(event.projectName)));

        ioModel.projectClosedListeners().addListener(event ->
            Platform.runLater(() ->
            {
                setProjectOpen(false);
                this.setProjectName(NULL_PROJECT_NAME);
                setProjectLoaded(false);
                setProjectProcessed(false);
                setProcessedTextureWidth(0);
                setProcessedTextureWidth(0);
                setModelSize(new Vector3(1.0f));
            }));

        ioModel.projectLoadedListeners().addListener(event ->
            Platform.runLater(() ->
            {
                setProjectLoaded(true);
                setModelSize(event.modelSize);
            }));

        ioModel.projectProcessedListeners().addListener(event ->
            Platform.runLater(() ->
            {
                setProjectProcessed(true);
                setProcessedTextureWidth(event.textureWidth);
                setProcessedTextureHeight(event.textureHeight);
                notifyProcessingComplete();
            }));
    }
}
