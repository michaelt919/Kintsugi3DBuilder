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

package kintsugi3d.builder.javafx;

import com.sun.glass.ui.Application;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import kintsugi3d.builder.core.*;
import kintsugi3d.builder.javafx.controllers.menubar.AboutController;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import kintsugi3d.builder.javafx.controllers.menubar.systemsettings.SystemSettingsController;
import kintsugi3d.builder.javafx.controllers.scene.ProgressBarsController;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import kintsugi3d.builder.javafx.util.PageWindow;
import kintsugi3d.builder.javafx.util.WindowUtilities;
import kintsugi3d.builder.resources.ibr.MeshImportException;
import kintsugi3d.util.Flag;
import kintsugi3d.util.RecentProjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;

public final class ProjectIO
{
    private static final ProjectIO INSTANCE = new ProjectIO();
    public static ProjectIO getInstance()
    {
        return INSTANCE;
    }

    private static final Logger log = LoggerFactory.getLogger(ProjectIO.class);

    private File projectFile;
    private File vsetFile;
    private boolean projectLoaded;

    private Flag systemSettingsModalOpen = new Flag(false);
    private Flag progressBarsModalOpen = new Flag(false);
    private Flag aboutWindowOpen = new Flag(false);

    private final PageWindow loaderWindow = new PageWindow();

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

    private ProjectIO()
    {
        // Try to initialize file chooser in advance of when it will be needed.
        Platform.runLater(this::getProjectFileChooserSafe);

        MultithreadModels.getInstance().getIOModel().addProgressMonitor(new DefaultProgressMonitor()
        {
            @Override
            public void cancelComplete(UserCancellationException e)
            {
                projectLoaded = false;
                Platform.runLater(() ->
                {
                    Alert alert = new Alert(AlertType.INFORMATION, "The operation was cancelled. Loading has stopped.");
                    alert.setTitle("Cancelled");
                    alert.setHeaderText("Cancelled");
                    alert.show();
                });
            }

            @Override
            public void fail(Throwable e)
            {
                projectLoaded = false;
                if (e instanceof MeshImportException)
                {
                    handleException(e.getMessage(), e);
                }
                else
                {
                    handleException("An error occurred while loading project", e);
                }
            }

            @Override
            public void warn(Throwable e)
            {
                handleException("An error occurred while loading project", e);
            }
        });
    }

    public File getProjectFile()
    {
        return projectFile;
    }

    public File getVsetFile()
    {
        return vsetFile;
    }

    public boolean isProjectLoaded()
    {
        return projectLoaded;
    }

    public static void handleException(String message, Throwable e)
    {
        log.error("{}:", message, e);
        Platform.runLater(() ->
        {
            ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType showLog = new ButtonType("Show Log", ButtonBar.ButtonData.YES);
            Alert alert = new Alert(Alert.AlertType.NONE, message + "\nSee the log for more info.", ok, showLog);
            ((ButtonBase) alert.getDialogPane().lookupButton(showLog)).setOnAction(event -> {
                // Use the menubar's console open function to prevent 2 console windows from appearing
                MenubarController.getInstance().help_console();
            });
            alert.show();
        });
    }

    private boolean confirmClose(String text)
    {
        if (projectLoaded)
        {
            Dialog<ButtonType> confirmation = new Alert(Alert.AlertType.CONFIRMATION,
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

    public boolean isCreateProjectWindowOpen()
    {
        return loaderWindow.isOpen();
    }

    private void onLoadStart()
    {
        projectFile = null;
        vsetFile = null;

        MultithreadModels.getInstance().getIOModel().unload();
        projectLoaded = true;
    }

    private void onViewSetCreated(ViewSet viewSet, Window parentWindow)
    {
        // Force user to save the project before proceeding, so that they have a place to save the results
        saveProjectAs(parentWindow, () -> {
            setViewsetDirectories(viewSet);
            ProgressMonitor monitor = MultithreadModels.getInstance().getIOModel().getProgressMonitor();
            if (monitor != null){
                monitor.setStage(0, ProgressMonitor.PREPARING_PROJECT);
            }
            viewSet.loadMasks();
        });
    }

    public static File getDefaultSupportingFilesDirectory(File projectFile)
    {
        return new File(projectFile.getParentFile(), projectFile.getName() + ".files");
    }

    private void setViewsetDirectories(ViewSet viewSet) {
        File filesDirectory = getDefaultSupportingFilesDirectory(projectFile);
        filesDirectory.mkdirs();

        // need to use a lambda callback so that this is called after the file location is chosen
        // but before its actually saved
        if (Objects.equals(vsetFile, projectFile)) // Saved as a VSET
        {
            // Set the root directory first, then the supporting files directory
                MultithreadModels.getInstance().getIOModel()
                    .getLoadedViewSet().setRootDirectory(projectFile.getParentFile());

            viewSet.setSupportingFilesDirectory(filesDirectory);
        }
        else // Saved as a Kintsugi 3D project
        {
            viewSet.setRootDirectory(filesDirectory);
            viewSet.setSupportingFilesDirectory(filesDirectory);
        }
    }

    public void createProject(Window parentWindow)
    {
        if (!confirmClose("Are you sure you want to create a new project?"))
        {
            return;
        }

        loaderWindow.open(parentWindow,"Load Files",
            "/fxml/menubar/createnewproject/SelectImportOptions.fxml",
            WelcomeWindowController.getInstance()::hide,
            () ->
            {
                onLoadStart();

                // "force" the user to save their project (user can still cancel saving)
                MultithreadModels.getInstance().getIOModel().addViewSetLoadCallback(
                    viewSet -> onViewSetCreated(viewSet, parentWindow));
            });
    }

    public void hotSwap(Window parentWindow)
    {
        // remember old project filename
        File oldProjectFile = projectFile;

        loaderWindow.open(parentWindow,"Load Files",
            "/fxml/menubar/createnewproject/HotSwap.fxml", null, this::onLoadStart);

        // "force" the user to save their project (user can still cancel saving)
        MultithreadModels.getInstance().getIOModel().addViewSetLoadCallback(
            viewSet ->
            {
                projectFile = oldProjectFile;
                saveProject(parentWindow);
            });
    }

    private static void startLoad(File projectFile, File vsetFile)
    {
        MultithreadModels.getInstance().getIOModel().unload();

        RecentProjects.addToRecentFiles(projectFile.getAbsolutePath());

        if (Objects.equals(projectFile.getParentFile(), vsetFile.getParentFile()))
        {
            // VSET file is the project file or they're in the same directory.
            // Use a supporting files directory underneath by default
            new Thread(() ->
            {
                try
                {
                    MultithreadModels.getInstance().getIOModel()
                        .loadFromVSETFile(vsetFile.getPath(), vsetFile, getDefaultSupportingFilesDirectory(projectFile));
                }
                catch (RuntimeException e)
                {
                    log.error("Error loading view set file", e);
                }
                catch (Error e)
                {
                    log.error("Error loading view set file", e);
                    //noinspection ProhibitedExceptionThrown
                    throw e;
                }
            })
            .start();
        }
        else
        {
            // VSET file is presumably already in a supporting files directory, so just use that directory by default
            new Thread(() ->
            {
                try
                {
                    MultithreadModels.getInstance().getIOModel()
                        .loadFromVSETFile(vsetFile.getPath(), vsetFile);
                }
                catch (RuntimeException e)
                {
                    log.error("Error loading view set file", e);
                }
                catch (Error e)
                {
                    log.error("Error loading view set file", e);
                    //noinspection ProhibitedExceptionThrown
                    throw e;
                }
            })
            .start();
        }

        //TODO: update color checker here, if the window for it is open
        //right now, the model will load but the color checker's apply button
        //  does not update until the text fields are changed
    }

    public void openProjectFromFile(File selectedFile)
    {
        //need to check for conflicting process early so crucial info isn't unloaded
        if(MultithreadModels.getInstance().getIOModel().getProgressMonitor().isConflictingProcess()){
            return;
        }

        //open the project, update the recent files list & recentDirectory, disable shaders which aren't useful until processing textures
        this.projectFile = selectedFile;
        RecentProjects.setMostRecentDirectory(this.projectFile.getParentFile());

        File newVsetFile = null;

        if (projectFile.getName().endsWith(".vset"))
        {
            newVsetFile = projectFile;
        }
        else
        {
            try
            {
                newVsetFile = InternalModels.getInstance().getProjectModel().openProjectFile(projectFile);
            }
            catch (Exception e)
            {
                handleException("An error occurred opening project", e);
            }
        }

        if (newVsetFile != null)
        {
            this.vsetFile = newVsetFile;
            this.projectLoaded = true;

            startLoad(projectFile, vsetFile);

            // Have to set loaded project file after startLoad since startLoad resets everything in order to unload a previously loaded project.
            MultithreadModels.getInstance().getIOModel().setLoadedProjectFile(projectFile);

            WelcomeWindowController.getInstance().hide();

            // Disable shaders that need processed textures until project load is complete.
            MenubarController.getInstance().setToggleableShaderDisable(true);
        }
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
                openProjectFromFile(selectedFile);
            }
        }
    }

    public void openProjectFromFileWithPrompt(File file){
       if (confirmClose("Are you sure you want to open another project?")) {
          openProjectFromFile(file);
       }
    }

    /**
     * NOTE: After "Save As", view set will share the same UUID as the original project,
     * including the preview resolution images and specular fit cache in the user's AppData folder.
     * Not sure if this is a feature or a bug -- so long as the view set doesn't change, this will reduce
     * the footprint on the user's hard drive.  But problems could happen if the ability to modify the
     * actual views (add / remove view) later on down the road.
     * @param parentWindow
     */
    public void saveProject(Window parentWindow)
    {
        if (projectFile == null)
        {
            saveProjectAs(parentWindow);
        }
        else
        {
            RecentProjects.setMostRecentDirectory(projectFile.getParentFile());
            try
            {
                IOModel ioModel = MultithreadModels.getInstance().getIOModel();

                File filesDirectory = getDefaultSupportingFilesDirectory(projectFile);
                if (projectFile.getName().endsWith(".vset"))
                {
                    ioModel.getLoadedViewSet().setRootDirectory(projectFile.getParentFile());
                    ioModel.getLoadedViewSet().setSupportingFilesDirectory(filesDirectory);
                    ioModel.saveToVSETFile(projectFile);
                    this.vsetFile = projectFile;
                    this.projectFile = null;
                    MultithreadModels.getInstance().getIOModel().setLoadedProjectFile(vsetFile);
                }
                else
                {
                    filesDirectory.mkdirs();
                    ioModel.getLoadedViewSet().setRootDirectory(filesDirectory);
                    ioModel.getLoadedViewSet().setSupportingFilesDirectory(filesDirectory);
                    this.vsetFile = new File(filesDirectory, projectFile.getName() + ".vset");
                    ioModel.saveToVSETFile(vsetFile);
                    InternalModels.getInstance().getProjectModel().saveProjectFile(projectFile, vsetFile);
                    MultithreadModels.getInstance().getIOModel().setLoadedProjectFile(projectFile);
                }

                ioModel.saveGlTF(filesDirectory);

                // Save textures and basis funtions (will be deferred to graphics thread).
                ioModel.saveMaterialFiles(filesDirectory, () ->
                {
                    // Display message when all textures have been saved on graphics thread.
                    //TODO: MAKE PRETTIER, LOOK INTO NULL SAFETY
                    Platform.runLater(() ->
                    {
                        Dialog<ButtonType> saveInfo = new Alert(Alert.AlertType.INFORMATION,
                            "Save Complete!");
                        saveInfo.setTitle("Save successful");
                        saveInfo.setHeaderText(projectFile.getName());
                        saveInfo.show();
                    });
                });
            }
            catch(Exception e)
            {
                handleException("An error occurred saving project", e);
            }
        }
    }

    public void saveProjectAs(Window parentWindow, Runnable callback)
    {
        saveProjectAs(parentWindow, callback, null);
    }

    public void saveProjectAs(Window parentWindow, Runnable callback, File defaultDirectory)
    {
        FileChooser fileChooser = getProjectFileChooserSafe();
        fileChooser.setTitle("Save project");
        projectFileChooser.getExtensionFilters().clear();
        projectFileChooser.getExtensionFilters().add(new ExtensionFilter("Full projects", "*.k3d"));
        projectFileChooser.getExtensionFilters().add(new ExtensionFilter("Standalone view sets", "*.vset"));
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(0));
        if (defaultDirectory != null)
        {
            fileChooser.setInitialFileName("");
            fileChooser.setInitialDirectory(defaultDirectory);
        }
        else if (projectFile != null)
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
            boolean complete = false;
            File selectedFile = null;
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
            this.projectFile = fileContainer.selectedFile;

            if (callback != null)
            {
                callback.run();
            }

            RecentProjects.addToRecentFiles(fileContainer.selectedFile.toString());
            saveProject(parentWindow);
        }
    }


    /**
     * NOTE: After "Save As", view set will share the same UUID as the original project,
     * including the preview resolution images and specular fit cache in the user's AppData folder.
     * Not sure if this is a feature or a bug -- so long as the view set doesn't change, this will reduce
     * the footprint on the user's hard drive.  But problems could happen if the ability to modify the
     * actual views (add / remove view) later on down the road.
     * @param parentWindow
     */
    public void saveProjectAs(Window parentWindow)
    {
        saveProjectAs(parentWindow, null);
    }

    public void closeProject()
    {
        projectFile = null;
        vsetFile = null;

        MultithreadModels.getInstance().getIOModel().unload();
        projectLoaded = false;

        WelcomeWindowController.getInstance().show();

        //TODO: do we want this here?
        MenubarController.getInstance().dismissMiniProgressBar();

        MenubarController.getInstance().setToggleableShaderDisable(true);
        MenubarController.getInstance().setShaderNameVisibility(false);
        MenubarController.getInstance().updateShaderList();
    }

    public void closeProjectAfterConfirmation()
    {
        if (confirmClose("Are you sure you want to close the current project?"))
        {
            closeProject();
        }
    }

    public void openProgressBars(){
        if(progressBarsModalOpen.get()){
            return;
        }

        ProgressBarsController.getInstance().showStage();
    }

    public void openSystemSettingsModal(InternalModels internalModels, Window window) {
        if (systemSettingsModalOpen.get())
        {
            return;
        }

        try
        {
            SystemSettingsController systemSettingsController = WindowUtilities.makeWindow(window, "System Settings", systemSettingsModalOpen, "fxml/menubar/systemsettings/SystemSettings.fxml");
            systemSettingsController.init(internalModels, window);
            WelcomeWindowController.getInstance().hide();
            systemSettingsController.getHostWindow().setOnCloseRequest(e->WelcomeWindowController.getInstance().showIfNoModelLoadedAndNotProcessing());
        }
        catch (IOException e)
        {
            log.error("An error occurred opening the settings modal:", e);
        }
    }

    public void openAboutModal(Window window) {
        try
        {

            AboutController aboutController = WindowUtilities.makeWindow(window,
                    "About Kintsugi 3D Builder", aboutWindowOpen, "fxml/menubar/About.fxml");
            aboutController.init();
            WelcomeWindowController.getInstance().hide();
        }
        catch (Exception e)
        {
            handleException("An error occurred showing help and about", e);
        }
    }
}
