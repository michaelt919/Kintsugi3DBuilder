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

import kintsugi3d.gl.vecmath.Vector3;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Interface accessible from both the graphics thread and JavaFX for managing project state
 */
public interface ProjectModel
{
    String NULL_PROJECT_NAME = "No Project";

    /**
     * Parses an XML document, typically read from a Kintsugi 3D Builder project file (.k3d) and sets up the lights, camera, etc.
     */
    void parseXMLDocument(Document document) throws IOException, ParserConfigurationException, SAXException;

    /**
     * Converts this project to an XML document that can be saved, typically as a Kintsugi 3D Builder project file (.k3d)
     */
    Document toXMLDocument() throws ParserConfigurationException;

    File getColorCheckerFile();
    void setColorCheckerFile(File colorCheckerFile);

    String getProjectName();

    boolean isProjectOpen();
    boolean isProjectLoaded();
    boolean isProjectProcessed();
    int getProcessedTextureResolution();
    Vector3 getModelSize();

    void confirm(String title, String header, String message, Runnable onConfirm);

    void setProjectOpen(boolean projectOpen);
    void setProjectName(String projectName);
    default void clearProjectName()
    {
        this.setProjectName(NULL_PROJECT_NAME);
    }
    void setProjectLoaded(boolean projectLoaded);
    void setProjectProcessed(boolean projectProcessed);
    void setProcessedTextureResolution(int processedTextureResolution);
    void setModelSize(Vector3 modelSize);

    void notifyProcessingComplete();
}
