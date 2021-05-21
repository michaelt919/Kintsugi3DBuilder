/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx.controllers.scene;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.collections.ObservableList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import tetzlaff.ibrelight.javafx.controllers.scene.camera.CameraSetting;
import tetzlaff.ibrelight.javafx.controllers.scene.environment.EnvironmentSetting;
import tetzlaff.ibrelight.javafx.controllers.scene.lights.LightGroupSetting;
import tetzlaff.ibrelight.javafx.controllers.scene.object.ObjectPoseSetting;

public class SceneModel
{
    private final ObservableList<CameraSetting> cameraList = new ObservableListWrapper<>(new ArrayList<>(16));
    private final ObservableList<EnvironmentSetting> environmentList = new ObservableListWrapper<>(new ArrayList<>(16));
    private final ObservableList<LightGroupSetting> lightGroupList = new ObservableListWrapper<>(new ArrayList<>(16));
    private final ObservableList<ObjectPoseSetting> objectPoseList = new ObservableListWrapper<>(new ArrayList<>(16));

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<CameraSetting> getCameraList()
    {
        return this.cameraList;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<EnvironmentSetting> getEnvironmentList()
    {
        return this.environmentList;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<LightGroupSetting> getLightGroupList()
    {
        return this.lightGroupList;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<ObjectPoseSetting> getObjectPoseList()
    {
        return this.objectPoseList;
    }

    public final File openProjectFile(File projectFile) throws IOException, ParserConfigurationException, SAXException
    {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(projectFile);

        Node vsetNode = document.getElementsByTagName("ViewSet").item(0);
        if (vsetNode instanceof Element)
        {
            File newVsetFile = new File(projectFile.getParent(), ((Element) vsetNode).getAttribute("src"));

            Node cameraListNode = document.getElementsByTagName("CameraList").item(0);
            if (cameraListNode != null)
            {
                NodeList cameraNodes = cameraListNode.getChildNodes();
                this.cameraList.clear();
                for (int i = 0; i < cameraNodes.getLength(); i++)
                {
                    Node cameraNode = cameraNodes.item(i);
                    if (cameraNode instanceof Element)
                    {
                        this.cameraList.add(CameraSetting.fromDOMElement((Element) cameraNode));
                    }
                }
            }

            Node environmentListNode = document.getElementsByTagName("EnvironmentList").item(0);
            if (environmentListNode != null)
            {
                NodeList environmentNodes = environmentListNode.getChildNodes();
                this.environmentList.clear();
                this.environmentList.add(EnvironmentSetting.NO_ENVIRONMENT);

                for (int i = 0; i < environmentNodes.getLength(); i++)
                {
                    Node environmentNode = environmentNodes.item(i);
                    if (environmentNode instanceof Element)
                    {
                        this.environmentList.add(EnvironmentSetting.fromDOMElement((Element) environmentNode));
                    }
                }
            }

            Node lightGroupListNode = document.getElementsByTagName("LightGroupList").item(0);
            if (lightGroupListNode != null)
            {
                NodeList lightGroupNodes = lightGroupListNode.getChildNodes();
                this.lightGroupList.clear();
                for (int i = 0; i < lightGroupNodes.getLength(); i++)
                {
                    Node lightGroupNode = lightGroupNodes.item(i);
                    if (lightGroupNode instanceof Element)
                    {
                        this.lightGroupList.add(LightGroupSetting.fromDOMElement((Element) lightGroupNode));
                    }
                }
            }

            Node objectPoseListNode = document.getElementsByTagName("ObjectPoseList").item(0);
            if (objectPoseListNode != null)
            {
                NodeList objectPoseNodes = objectPoseListNode.getChildNodes();
                this.objectPoseList.clear();
                for (int i = 0; i < objectPoseNodes.getLength(); i++)
                {
                    Node objectPoseNode = objectPoseNodes.item(i);
                    if (objectPoseNode instanceof Element)
                    {
                        this.objectPoseList.add(ObjectPoseSetting.fromDOMElement((Element) objectPoseNode));
                    }
                }
            }

            return newVsetFile;
        }
        else
        {
            throw new IOException("Error while processing the ViewSet element.");
        }
    }

    public final void saveProjectFile(File projectFile, File vsetFile) throws IOException, ParserConfigurationException, TransformerException
    {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element rootElement = document.createElement("Project");
        document.appendChild(rootElement);

        Element vsetElement = document.createElement("ViewSet");
        vsetElement.setAttribute("src", projectFile.getParentFile().toPath().relativize(vsetFile.toPath()).toString());
        rootElement.appendChild(vsetElement);

        Element cameraListElement = document.createElement("CameraList");
        rootElement.appendChild(cameraListElement);

        for (CameraSetting camera : this.cameraList)
        {
            cameraListElement.appendChild(camera.toDOMElement(document));
        }

        Element environmentListElement = document.createElement("EnvironmentList");
        rootElement.appendChild(environmentListElement);

        for (EnvironmentSetting environment : this.environmentList)
        {
            if (!Objects.equals(environment, EnvironmentSetting.NO_ENVIRONMENT))
            {
                environmentListElement.appendChild(environment.toDOMElement(document));
            }
        }

        Element lightGroupListElement = document.createElement("LightGroupList");
        rootElement.appendChild(lightGroupListElement);

        for (LightGroupSetting lightGroup : this.lightGroupList)
        {
            lightGroupListElement.appendChild(lightGroup.toDOMElement(document));
        }

        Element objectPoseListElement = document.createElement("ObjectPoseList");
        rootElement.appendChild(objectPoseListElement);

        for (ObjectPoseSetting objectPose : this.objectPoseList)
        {
            objectPoseListElement.appendChild(objectPose.toDOMElement(document));
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        try (OutputStream out = new FileOutputStream(projectFile))
        {
            transformer.transform(new DOMSource(document), new StreamResult(out));
        }
    }
}
