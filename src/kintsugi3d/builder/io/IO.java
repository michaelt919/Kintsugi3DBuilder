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

package kintsugi3d.builder.io;

import kintsugi3d.builder.core.viewset.ViewSet;
import kintsugi3d.builder.io.events.*;
import kintsugi3d.builder.io.metashape.MetashapeModel;
import kintsugi3d.builder.rendering.ProjectRenderableInstance;
import kintsugi3d.builder.state.scene.UserShader;
import kintsugi3d.builder.util.EventListeners;
import kintsugi3d.gl.interactive.ProgressMonitor;
import kintsugi3d.util.EncodableColorImage;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

public interface IO
{
    ReadonlyLoadOptionsModel getLoadOptionsModel();

    void addProgressMonitor(ProgressMonitor monitor);

    EventListeners<ProjectOpenedListener> projectOpenedListeners();
    EventListeners<ProjectSavedListener> projectSavedListeners();
    EventListeners<ProjectLoadedListener> projectLoadedListeners();
    EventListeners<ProjectProcessedListener> projectProcessedListeners();
    EventListeners<ProjectClosedListener> projectClosedListeners();

    File getLoadedViewSetFile();
    File getLoadedProjectFile();
    ViewSet getLoadedViewSet();
    ProjectRenderableInstance<?> getMainRenderable();
    void addMainRenderableLoadCallback(Consumer<ProjectRenderableInstance<?>> callback);

    ProjectRenderableInstance<?> getRenderableForShader(UserShader shader);

    void loadFromLooseFiles(File newProjectFile, String id, File xmlFile, ViewSetLoadOptions viewSetLoadOptions);
    void hotSwapLooseFiles(String id, File xmlFile, ViewSetLoadOptions viewSetLoadOptions);
    void loadFromMetashapeModel(File newProjectFile, MetashapeModel model);
    void loadExistingProject(File projectFile);
    File getViewSetFileForProject(File projectFile) throws IOException, ParserConfigurationException, SAXException;

    /**
     * Saves the project, including textures and glTF model.  If the project file is not a .vset, the .vset will be created in a supporting files directory.
     * @param projectFile The file path for the project.
     * @param finishedCallback Called after basis materials and textures have finished saving,
     *                         which maybe asynchronous since this requires GPU access.
     *                         No guarantees are made about which thread the callback will run on.
     * @return The file path for the .vset (which may match the project name or be in a supporting files directory).
     *         On return, the textures and basis materials may not have been saved yet (which happens asynchronously),
     *         but the project itself (including the view set) should be fully written out to disk,
     *
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    void saveProject(File projectFile, Runnable finishedCallback) throws IOException, ParserConfigurationException, TransformerException;

    /**
     * Saves the project, including textures and glTF model.  If the project file is not a .vset, the .vset will be created in a supporting files directory.
     * @param projectFile The file path for the project.
     * @return The file path for the .vset (which may match the project name or be in a supporting files directory).
     *         On return, the textures and basis materials may not have been saved yet (which happens asynchronously),
     *         but the project itself (including the view set) should be fully written out to disk,
     *
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    default void saveProject(File projectFile) throws IOException, ParserConfigurationException, TransformerException
    {
        saveProject(projectFile, null);
    }

    /**
     * Saves the project, including textures and glTF model, using the current loaded project filename.
     * If the project file is not a .vset, the .vset will be created in a supporting files directory.
     * @param finishedCallback Called after basis materials and textures have finished saving,
     *                         which maybe asynchronous since this requires GPU access.
     *                         No guarantees are made about which thread the callback will run on.
     * @return The file path for the .vset (which may match the project name or be in a supporting files directory).
     *         On return, the textures and basis materials may not have been saved yet (which happens asynchronously),
     *         but the project itself (including the view set) should be fully written out to disk,
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    default void saveProject(Runnable finishedCallback) throws IOException, ParserConfigurationException, TransformerException
    {
        saveProject(getLoadedProjectFile(), finishedCallback);
    }

    /**
     * Saves the project, including textures and glTF model, using the current loaded project filename.
     * If the project file is not a .vset, the .vset will be created in a supporting files directory.
     * @return The file path for the .vset (which may match the project name or be in a supporting files directory).
     *         On return, the textures and basis materials may not have been saved yet (which happens asynchronously),
     *         but the project itself (including the view set) should be fully written out to disk,
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    default void saveProject() throws IOException, ParserConfigurationException, TransformerException
    {
        saveProject(getLoadedProjectFile(), null);
    }

    Optional<EncodableColorImage> loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException;
    void loadBackplate(File backplateFile) throws FileNotFoundException;

    DoubleUnaryOperator getLuminanceEncodingFunction();
    void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues);
    void clearTonemapping();

    void requestLightIntensityCalibration();
    void applyLightOffsetCalibration();

    void closeProject();

    boolean hasLoadedRenderable();
    boolean hasValidHandler();

    /**
     * Checks if this has a valid project instance loaded.  Otherwise, throws an IllegalStateException.
     * @return This model if it has a valid project instance.
     */
    IOModel validateRenderable();
}
