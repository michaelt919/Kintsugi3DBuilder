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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import kintsugi3d.builder.javafx.controllers.scene.camera.CameraSetting;
import kintsugi3d.builder.javafx.controllers.scene.environment.EnvironmentSetting;
import kintsugi3d.builder.javafx.controllers.scene.lights.LightGroupSetting;
import kintsugi3d.builder.javafx.controllers.scene.object.ObjectPoseSetting;
import kintsugi3d.builder.state.ProjectModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// TODO can this class be refactored to eliminate dependency on JavaFX?
public abstract class ProjectModelBase implements ProjectModel
{
    public abstract List<CameraSetting> getCameraList();

    public abstract List<EnvironmentSetting> getEnvironmentList();

    public abstract List<LightGroupSetting> getLightGroupList();

    public abstract List<ObjectPoseSetting> getObjectPoseList();

    private File colorCheckerFile;


    /**
     * Opens a Kintsugi 3D Builder project file (.k3d) and sets up the lights, camera, etc.
     * Returns the file containing the viewset with the actual image data.
     *
     * @param projectFile
     * @return The .vset file containing the actual viewset.
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */

    @Override
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

                synchronized (this.getCameraList())
                {
                    this.getCameraList().clear();
                    for (int i = 0; i < cameraNodes.getLength(); i++)
                    {
                        Node cameraNode = cameraNodes.item(i);
                        if (cameraNode instanceof Element)
                        {
                            this.getCameraList().add(CameraSetting.fromDOMElement((Element) cameraNode));
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
                    this.getEnvironmentList().add(EnvironmentSetting.NO_ENVIRONMENT);

                    for (int i = 0; i < environmentNodes.getLength(); i++)
                    {
                        Node environmentNode = environmentNodes.item(i);
                        if (environmentNode instanceof Element)
                        {
                            this.getEnvironmentList().add(EnvironmentSetting.fromDOMElement((Element) environmentNode));
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
                            this.getLightGroupList().add(LightGroupSetting.fromDOMElement((Element) lightGroupNode));
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
                            this.getObjectPoseList().add(ObjectPoseSetting.fromDOMElement((Element) objectPoseNode));
                        }
                    }
                }
            }

            Node colorPickerImageNode = document.getElementsByTagName("ColorCheckerFile").item(0);
            if (colorPickerImageNode != null)
            {
                this.colorCheckerFile = new File(colorPickerImageNode.getTextContent());
            }

            return newVsetFile;
        }
        else
        {
            throw new IOException("Error while processing the ViewSet element.");
        }
    }

    @Override
    public final void saveProjectFile(File projectFile, File vsetFile) throws IOException, ParserConfigurationException, TransformerException
    {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element rootElement = document.createElement("Project");
        document.appendChild(rootElement);

        Element vsetElement = document.createElement("ViewSet");
        vsetElement.setAttribute("src", projectFile.getParentFile().toPath().relativize(vsetFile.toPath()).toString());
        rootElement.appendChild(vsetElement);

        synchronized (this.getCameraList())
        {
            Element cameraListElement = document.createElement("CameraList");
            rootElement.appendChild(cameraListElement);

            for (CameraSetting camera : this.getCameraList())
            {
                cameraListElement.appendChild(camera.toDOMElement(document));
            }
        }

        synchronized (this.getEnvironmentList())
        {
            Element environmentListElement = document.createElement("EnvironmentList");
            rootElement.appendChild(environmentListElement);

            for (EnvironmentSetting environment : this.getEnvironmentList())
            {
                if (!Objects.equals(environment, EnvironmentSetting.NO_ENVIRONMENT))
                {
                    environmentListElement.appendChild(environment.toDOMElement(document));
                }
            }
        }

        synchronized (this.getLightGroupList())
        {
            Element lightGroupListElement = document.createElement("LightGroupList");
            rootElement.appendChild(lightGroupListElement);

            for (LightGroupSetting lightGroup : this.getLightGroupList())
            {
                lightGroupListElement.appendChild(lightGroup.toDOMElement(document));
            }
        }

        synchronized (this.getObjectPoseList())
        {
            Element objectPoseListElement = document.createElement("ObjectPoseList");
            rootElement.appendChild(objectPoseListElement);

            for (ObjectPoseSetting objectPose : this.getObjectPoseList())
            {
                objectPoseListElement.appendChild(objectPose.toDOMElement(document));
            }
        }

        if (this.getColorCheckerFile() != null){
            Element colorPickerImageElement = document.createElement("ColorCheckerFile");
            colorPickerImageElement.setTextContent(this.getColorCheckerFile().getPath());
            rootElement.appendChild(colorPickerImageElement);
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        try (OutputStream out = new FileOutputStream(projectFile))
        {
            transformer.transform(new DOMSource(document), new StreamResult(out));
        }
    }

    public File getColorCheckerFile() {return this.colorCheckerFile;}

    public void setColorCheckerFile(File colorCheckerFile)
    {
        this.colorCheckerFile = colorCheckerFile;
    }
}
