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

package kintsugi3d.builder.state.project;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.io.IOModel;
import kintsugi3d.gl.vecmath.Vector3;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Base class that primarily handles serialization in a UI-agnostic manner (no JavaFX coupling)
 * The camera / environment / lights / object pose exist here primarily for serialization purposes
 * (saving and loading from the project file).
 * They will also typically be bound to the JavaFX frontend to ensure that they are constantly synchronized.
 * For typical 3D manipulation use cases (interaction between 3D backend and JavaFX frontend),
 * the CameraModel / LightingModel / EnvironmentModel / ObjectModel instances  accessible through Global.state() should be used instead.
 * @param <CameraType>
 * @param <EnvironmentType>
 * @param <LightGroupType>
 * @param <LightType>
 * @param <ObjectPoseType>
 */
public abstract class ProjectModelBase<
    CameraType extends SerializableCameraSettings, EnvironmentType extends SerializableEnvironmentSettings,
    LightGroupType extends SerializableLightGroupSettings<LightType>, LightType extends SerializableLightSettings,
    ObjectPoseType extends SerializableObjectPoseSettings>
    implements ProjectModel
{
    protected static final String NULL_PROJECT_NAME = "No Project";

    private final EnvironmentType noEnvironment = SerializableEnvironmentSettings.createNoEnvironment(this::constructEnvironmentSetting);

    public void registerIOListeners()
    {
        IOModel ioModel = Global.io();

        ioModel.projectOpenedListeners().addListener(event ->
        {
            setProjectOpen(true);
            setProjectName(event.projectName);
        });

        ioModel.projectSavedListeners().addListener(event -> setProjectName(event.projectName));

        ioModel.projectClosedListeners().addListener(event ->
        {
            setProjectOpen(false);
            this.setProjectName(NULL_PROJECT_NAME);
            setProjectLoaded(false);
            setProjectProcessed(false);
            setProcessedTextureWidth(0);
            setProcessedTextureWidth(0);
            setModelSize(new Vector3(1.0f));
        });

        ioModel.projectLoadedListeners().addListener(event ->
        {
            setProjectLoaded(true);
            setModelSize(event.modelSize);
        });

        ioModel.projectProcessedListeners().addListener(event ->
        {
            setProjectProcessed(true);
            setProcessedTextureWidth(event.textureWidth);
            setProcessedTextureHeight(event.textureHeight);
            notifyProcessingComplete();
        });
    }

    public abstract List<CameraType> getCameraList();

    public abstract List<EnvironmentType> getEnvironmentList();

    public abstract List<LightGroupType> getLightGroupList();

    public abstract List<ObjectPoseType> getObjectPoseList();

    @Override
    public final void parseXMLDocument(Document document)
    {
        Node cameraListNode = document.getElementsByTagName("CameraList").item(0);
        if (cameraListNode != null)
        {
            NodeList cameraNodes = cameraListNode.getChildNodes();

            synchronized (this.getCameraList())
            {
                this.getCameraList().clear();
                for (int i = 0; i < cameraNodes.getLength(); i++)
                {
                    Node cameraNode = cameraNodes.item(i);
                    if (cameraNode instanceof Element)
                    {
                        this.getCameraList().add(SerializableCameraSettings.fromDOMElement(
                            (Element) cameraNode, this::constructCameraSetting));
                    }
                }
            }
        }

        Node environmentListNode = document.getElementsByTagName("EnvironmentList").item(0);
        if (environmentListNode != null)
        {
            NodeList environmentNodes = environmentListNode.getChildNodes();

            synchronized (this.getEnvironmentList())
            {
                this.getEnvironmentList().clear();
                this.getEnvironmentList().add(noEnvironment);

                for (int i = 0; i < environmentNodes.getLength(); i++)
                {
                    Node environmentNode = environmentNodes.item(i);
                    if (environmentNode instanceof Element)
                    {
                        this.getEnvironmentList().add(SerializableEnvironmentSettings.fromDOMElement(
                            (Element) environmentNode, this::constructEnvironmentSetting));
                    }
                }
            }
        }

        Node lightGroupListNode = document.getElementsByTagName("LightGroupList").item(0);
        if (lightGroupListNode != null)
        {
            NodeList lightGroupNodes = lightGroupListNode.getChildNodes();

            synchronized (this.getLightGroupList())
            {
                this.getLightGroupList().clear();
                for (int i = 0; i < lightGroupNodes.getLength(); i++)
                {
                    Node lightGroupNode = lightGroupNodes.item(i);
                    if (lightGroupNode instanceof Element)
                    {
                        this.getLightGroupList().add(SerializableLightGroupSettings.fromDOMElement(
                            (Element) lightGroupNode, this::constructLightGroupSetting));
                    }
                }
            }
        }

        Node objectPoseListNode = document.getElementsByTagName("ObjectPoseList").item(0);
        if (objectPoseListNode != null)
        {
            NodeList objectPoseNodes = objectPoseListNode.getChildNodes();

            synchronized (this.getObjectPoseList())
            {
                this.getObjectPoseList().clear();
                for (int i = 0; i < objectPoseNodes.getLength(); i++)
                {
                    Node objectPoseNode = objectPoseNodes.item(i);
                    if (objectPoseNode instanceof Element)
                    {
                        this.getObjectPoseList().add(SerializableObjectPoseSettings.fromDOMElement(
                            (Element) objectPoseNode, this::constructObjectPoseSetting));
                    }
                }
            }
        }

        Node colorPickerImageNode = document.getElementsByTagName("ColorCheckerFile").item(0);
        if (colorPickerImageNode != null)
        {
            this.setColorCheckerFile(new File(colorPickerImageNode.getTextContent()));
        }
    }

    @Override
    public final Document toXMLDocument() throws ParserConfigurationException
    {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element rootElement = document.createElement("Project");
        document.appendChild(rootElement);

        synchronized (this.getCameraList())
        {
            Element cameraListElement = document.createElement("CameraList");
            rootElement.appendChild(cameraListElement);

            for (SerializableCameraSettings camera : this.getCameraList())
            {
                cameraListElement.appendChild(camera.toDOMElement(document));
            }
        }

        synchronized (this.getEnvironmentList())
        {
            Element environmentListElement = document.createElement("EnvironmentList");
            rootElement.appendChild(environmentListElement);

            for (SerializableEnvironmentSettings environment : this.getEnvironmentList())
            {
                if (!Objects.equals(environment, noEnvironment))
                {
                    environmentListElement.appendChild(environment.toDOMElement(document));
                }
            }
        }

        synchronized (this.getLightGroupList())
        {
            Element lightGroupListElement = document.createElement("LightGroupList");
            rootElement.appendChild(lightGroupListElement);

            for (SerializableLightGroupSettings<LightType> lightGroup : this.getLightGroupList())
            {
                lightGroupListElement.appendChild(lightGroup.toDOMElement(document));
            }
        }

        synchronized (this.getObjectPoseList())
        {
            Element objectPoseListElement = document.createElement("ObjectPoseList");
            rootElement.appendChild(objectPoseListElement);

            for (SerializableObjectPoseSettings objectPose : this.getObjectPoseList())
            {
                objectPoseListElement.appendChild(objectPose.toDOMElement(document));
            }
        }

        if (this.getColorCheckerFile() != null)
        {
            Element colorPickerImageElement = document.createElement("ColorCheckerFile");
            colorPickerImageElement.setTextContent(this.getColorCheckerFile().getPath());
            rootElement.appendChild(colorPickerImageElement);
        }

        return document;
    }

    protected abstract CameraType constructCameraSetting();
    protected abstract EnvironmentType constructEnvironmentSetting();
    protected abstract LightGroupType constructLightGroupSetting();
    protected abstract ObjectPoseType constructObjectPoseSetting();

    public EnvironmentType getNoEnvironment()
    {
        return this.noEnvironment;
    }

    protected abstract void setProjectOpen(boolean projectOpen);
    protected abstract void setProjectName(String projectName);
    protected abstract void setProjectLoaded(boolean projectLoaded);
    protected abstract void setModelSize(Vector3 modelSize);
    protected abstract void setProjectProcessed(boolean projectProcessed);
    protected abstract void setProcessedTextureWidth(int processedTextureWidth);
    protected abstract void setProcessedTextureHeight(int processedTextureHeight);
    protected abstract void notifyProcessingComplete();
}
