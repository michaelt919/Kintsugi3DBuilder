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

package kintsugi3d.builder.state;

import kintsugi3d.gl.vecmath.Vector3;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;

public interface ProjectModel
{
    String NULL_PROJECT_NAME = "No Project";

    File openProjectFile(File projectFile) throws IOException, ParserConfigurationException, SAXException;
    void saveProjectFile(File projectFile, File vsetFile) throws IOException, ParserConfigurationException, TransformerException;

    boolean isProjectOpen();
    void setProjectOpen(boolean projectOpen);

    String getProjectName();
    void setProjectName(String projectName);
    default void clearProjectName()
    {
        this.setProjectName(NULL_PROJECT_NAME);
    }

    boolean isProjectLoaded();
    void setProjectLoaded(boolean projectLoaded);

    boolean isProjectProcessed();
    void setProjectProcessed(boolean projectProcessed);

    int getProcessedTextureResolution();
    void setProcessedTextureResolution(int processedTextureResolution);

    Vector3 getModelSize();
    void setModelSize(Vector3 modelSize);
}
