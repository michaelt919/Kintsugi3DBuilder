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

package kintsugi3d.builder.javafx.core;

import com.sun.glass.ui.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.io.IOModel;
import kintsugi3d.builder.io.RecentProjects;
import kintsugi3d.builder.javafx.experience.CreateProject;
import kintsugi3d.builder.resources.project.MeshImportException;
import kintsugi3d.gl.interactive.DefaultProgressMonitor;
import kintsugi3d.gl.interactive.UserCancellationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;

public final class FrontendIO
{
    private static final FrontendIO INSTANCE = new FrontendIO();

    static FrontendIO getInstance()
    {
        return INSTANCE;
    }

    private static final Logger LOG = LoggerFactory.getLogger(FrontendIO.class);

    private FileChooser projectFileChooser;

    private FileChooser getProjectFileChooserSafe()
    {
        if (projectFileChooser == null)
        {
            projectFileChooser = new FileChooser();
        }

        projectFileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());

        return projectFileChooser;
    }

    private FrontendIO()
    {
        // Try to initialize file chooser in advance of when it will be needed.
        Platform.runLater(this::getProjectFileChooserSafe);

        Global.state().getIOModel().addProgressMonitor(new DefaultProgressMonitor()
        {
            @Override
            public void cancelComplete(UserCancellationException e)
            {
                Global.state().getProjectModel().setProjectOpen(false);

                // We know that the welcome window is going to be shown when the progress modal opens
                // so wait until that happens so that the welcome window doesn't cover up the alert
                // (and by extension, the main window as well as a parent of the welcome window)
                // TODO figure out a less hacky workaround.
                WelcomeWindowController.getInstance().runOnceWhenShown(() ->
                {
                    Alert alert = new Alert(AlertType.INFORMATION, "The operation was cancelled.");
                    alert.setTitle("Cancelled");
                    alert.setHeaderText("Cancelled");
                    alert.show();
                });
            }

            @Override
            public void fail(Throwable e)
            {
                Global.state().getProjectModel().setProjectOpen(false);

                if (e instanceof MeshImportException)
                {
                    String message = e.getMessage();
                    ExceptionHandling.error(message, e);
                }
                else
                {
                    ExceptionHandling.error("An error occurred", e);
                }
            }

            @Override
            public void warn(Throwable e)
            {
                ExceptionHandling.error("A potential problem occurred", e);
            }
        });
    }

    private static boolean confirmClose(String text)
    {
        if (JavaFXState.getInstance().getProjectModel().isProjectOpen())
        {
            Dialog<ButtonType> confirmation = new Alert(AlertType.CONFIRMATION,
                "If you click OK, any unsaved changes to the current project will be lost.");
            confirmation.setTitle("Close Project Confirmation");
            confirmation.setHeaderText(text);

            //TODO: apply dark mode to popups
            return confirmation.showAndWait()
                .filter(Predicate.isEqual(ButtonType.OK))
                .isPresent();
        }
        else
        {
            return true;
        }
    }

    private static CreateProject getCreateProjectExperience()
    {
        return ExperienceManager.getInstance().getExperience("CreateProject", CreateProject.class);
    }

    public static boolean isCreateProjectWindowOpen()
    {
        return getCreateProjectExperience().isOpen();
    }

    public void createProject(Window parentWindow)
    {
        if (!confirmClose("Are you sure you want to create a new project?"))
        {
            return;
        }

        CreateProject createProject = getCreateProjectExperience();
        createProject.setConfirmCallback(() ->
            // Force user to save the project before proceeding, so that they have a place to save the results
            // User can still cancel saving (TODO where does it save the results in that case?)
            Global.state().getIOModel().addViewSetLoadCallback(viewSet -> saveProjectAs(parentWindow)));
        createProject.tryOpen();
    }

    public void hotSwap(Window parentWindow)
    {
        // remember old project filename
        File oldProjectFile = Global.state().getIOModel().getLoadedProjectFile();

        CreateProject createProject = getCreateProjectExperience();

        // "force" the user to save their project (user can still cancel saving)
        Global.state().getIOModel().addViewSetLoadCallback(viewSet -> saveProject(oldProjectFile, parentWindow));

        createProject.tryOpenHotSwap();
    }

    public void openProjectWithPrompt(Window parentWindow)
    {
        if (confirmClose("Are you sure you want to open another project?"))
        {
            FileChooser fileChooser = getProjectFileChooserSafe();
            fileChooser.setTitle("Open project");
            projectFileChooser.getExtensionFilters().clear();
            projectFileChooser.getExtensionFilters().add(new ExtensionFilter("Full projects", "*.k3d", "*.ibr"));
            projectFileChooser.getExtensionFilters().add(new ExtensionFilter("Standalone view sets", "*.vset"));
            File selectedFile = fileChooser.showOpenDialog(parentWindow);
            if (selectedFile != null)
            {
                //opens project and also updates the recently opened files list
                Global.state().getIOModel().loadExistingProject(selectedFile);
            }
        }
    }

    public static void openProjectFromFile(File file)
    {
        if (confirmClose("Are you sure you want to open another project?"))
        {
            Global.state().getIOModel().loadExistingProject(file);
        }
    }

    /**
     * Saves the project.
     * Does not need to run on the JavaFX thread.
     *
     * @param parentWindow
     */
    public void saveProject(Window parentWindow)
    {
        saveProject(Global.state().getIOModel().getLoadedProjectFile(), parentWindow);
    }

    /**
     * Saves the project.
     * Does not need to run on the JavaFX thread.
     *
     * @param parentWindow
     */
    public void saveProject(File projectFile, Window parentWindow)
    {
        if (projectFile == null)
        {
            saveProjectAs(parentWindow);
        }
        else
        {
            try
            {
                Global.state().getIOModel().saveProject(projectFile, () ->
                {
                    // Display message when all textures have been saved on graphics thread.
                    // TODO: MAKE PRETTIER, LOOK INTO NULL SAFETY
                    Platform.runLater(() ->
                    {
                        Dialog<ButtonType> saveInfo = new Alert(AlertType.INFORMATION,
                            "Save Complete!");
                        saveInfo.setTitle("Save successful");
                        saveInfo.setHeaderText(projectFile.getName());
                        saveInfo.show();
                    });
                });
            }
            catch (RuntimeException | IOException | ParserConfigurationException | TransformerException e)
            {
                ExceptionHandling.error("An error occurred saving project", e);
            }
        }
    }

    /**
     * Prompts the user for a project name and saves the project.
     * Blocks the thread while waiting for user input; does not need to be run on the JavaFX thread.
     * <p>
     * NOTE: After "Save As", view set will share the same UUID as the original project,
     * including the preview resolution images and specular fit cache in the user's AppData folder.
     * Not sure if this is a feature or a bug -- so long as the view set doesn't change, this will reduce
     * the footprint on the user's hard drive.  But problems could happen if the ability to modify the
     * actual views (add / remove view) later on down the road.
     *
     * @param parentWindow
     */
    public void saveProjectAs(Window parentWindow)
    {
        FileChooser fileChooser = getProjectFileChooserSafe();
        fileChooser.setTitle("Save project");
        projectFileChooser.getExtensionFilters().clear();
        projectFileChooser.getExtensionFilters().add(new ExtensionFilter("Full projects", "*.k3d"));
        projectFileChooser.getExtensionFilters().add(new ExtensionFilter("Standalone view sets", "*.vset"));
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(0));

        IOModel ioModel = Global.state().getIOModel();
        File projectFile = ioModel.getLoadedProjectFile();
        File vsetFile = ioModel.getLoadedViewSetFile();

        if (projectFile != null && !Objects.equals(projectFile, vsetFile))
        {
            fileChooser.setInitialFileName(projectFile.getName());
            fileChooser.setInitialDirectory(projectFile.getParentFile());
        }
        else if (vsetFile != null)
        {
            fileChooser.setInitialFileName("");
            fileChooser.setInitialDirectory(vsetFile.getParentFile());
        }

        var fileContainer = new Object()
        {
            volatile boolean complete = false;
            volatile File selectedFile;
        };

        if (Application.isEventThread())
        {
            // If already on the JavaFX application thread, just open the dialog here to avoid deadlock
            fileContainer.selectedFile = fileChooser.showSaveDialog(parentWindow);
            fileContainer.complete = true;
        }
        else
        {
            // On MacOS, the save dialog needs to run on JavaFX thread, so use Platform.runLater if not already on that thread.
            Platform.runLater(() ->
            {
                fileContainer.selectedFile = fileChooser.showSaveDialog(parentWindow);
                fileContainer.complete = true;
            });

            while (!fileContainer.complete)
            {
                Thread.onSpinWait();
            }
        }

        if (fileContainer.selectedFile != null)
        {
            saveProject(fileContainer.selectedFile, parentWindow);
        }
    }

    public static void closeProject()
    {
        if (confirmClose("Are you sure you want to close the current project?"))
        {
            Global.state().getIOModel().closeProject();
        }
    }
}
