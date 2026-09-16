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

package kintsugi3d.builder.javafx.multithread;

import javafx.application.Platform;
import kintsugi3d.builder.core.texture.ImageReplacer;
import kintsugi3d.builder.state.project.ProjectModel;
import kintsugi3d.gl.vecmath.Vector3;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Wraps project model for thread safety when accessed from the graphics thread
 * (Platform.runLater needed since setters typically are bound to JavaFX properties)
 */
public class SynchronizedProjectModel implements ProjectModel
{
    public static final String NULL_PROJECT_NAME = "No Project";
    private final ProjectModel baseModel;
    private final SynchronizedValue<File> colorCheckerFile;

    public SynchronizedProjectModel(ProjectModel baseModel)
    {
        this.baseModel = baseModel;
        this.colorCheckerFile = SynchronizedValue.createFromFunctions(baseModel::getColorCheckerFile, baseModel::setColorCheckerFile);
    }

    @Override
    public void parseXMLDocument(Document document) throws IOException, ParserConfigurationException, SAXException
    {
        baseModel.parseXMLDocument(document);
    }

    @Override
    public Document toXMLDocument() throws ParserConfigurationException
    {
        return baseModel.toXMLDocument();
    }

    @Override
    public File getColorCheckerFile()
    {
        return colorCheckerFile.getValue();
    }

    @Override
    public void setColorCheckerFile(File colorCheckerFile)
    {
        this.colorCheckerFile.setValue(colorCheckerFile);
    }

    @Override
    public String getProjectName()
    {
        return baseModel.getProjectName();
    }

    @Override
    public boolean isProjectOpen()
    {
        return baseModel.isProjectOpen();
    }

    @Override
    public boolean isProjectLoaded()
    {
        return baseModel.isProjectLoaded();
    }

    @Override
    public boolean isProjectProcessed()
    {
        return baseModel.isProjectProcessed();
    }

    @Override
    public int getProcessedTextureWidth()
    {
        return baseModel.getProcessedTextureHeight();
    }

    @Override
    public int getProcessedTextureHeight()
    {
        return baseModel.getProcessedTextureHeight();
    }


    @Override
    public Vector3 getModelSize()
    {
        return baseModel.getModelSize();
    }

    @Override
    public void error(String message, Throwable e)
    {
        // error() already runs its JavaFX code within a Platform.runLater, so it should be thread-safe.
        baseModel.error(message, e);
    }

    @Override
    public void warn(String message, Throwable e)
    {
        // warn() already runs its JavaFX code within a Platform.runLater, so it should be thread-safe.
        baseModel.warn(message, e);
    }

    @Override
    public void cancelled(String message)
    {
        Platform.runLater(() -> baseModel.cancelled(message));
    }

    @Override
    public void confirm(String title, String header, String message, Runnable onConfirm)
    {
        Platform.runLater(() -> baseModel.confirm(title, header, message, onConfirm));
    }

    @Override
    public void requestUserImageReplacement(ImageReplacer imageReplacer)
    {
        Platform.runLater(() -> baseModel.requestUserImageReplacement(imageReplacer));
    }
}
