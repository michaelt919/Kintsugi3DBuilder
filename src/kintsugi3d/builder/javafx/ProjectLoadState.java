/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.core.LoadingModel;
import kintsugi3d.builder.core.LoadingMonitor;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.controllers.menubar.LoaderController;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import kintsugi3d.builder.javafx.controllers.scene.CreateProjectController;
import kintsugi3d.util.Flag;
import kintsugi3d.util.RecentProjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ProjectLoadState
{
    private static final ProjectLoadState INSTANCE = new ProjectLoadState();
    public static ProjectLoadState getInstance()
    {
        return INSTANCE;
    }

    private static final Logger log = LoggerFactory.getLogger(ProjectLoadState.class);

    private File projectFile;
    private File vsetFile;
    private boolean projectLoaded;

    private final Flag loaderWindowOpen = new Flag(false);

    private FileChooser projectFileChooser;

    private FileChooser getProjectFileChooserSafe()
    {
        if (projectFileChooser == null)
        {
            projectFileChooser = new FileChooser();

            projectFileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            projectFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Full projects", "*.ibr"));
            projectFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Standalone view sets", "*.vset"));
        }

        return projectFileChooser;
    }

    private ProjectLoadState()
    {
        // Try to initialize file chooser in advance of when it will be needed.
        Platform.runLater(this::getProjectFileChooserSafe);

        MultithreadModels.getInstance().getLoadingModel().addLoadingMonitor(new LoadingMonitor()
        {
            @Override
            public void startLoading()
            {
            }

            @Override
            public void setMaximum(double maximum)
            {
            }

            @Override
            public void setProgress(double progress)
            {
            }

            @Override
            public void loadingComplete()
            {
            }

            @Override
            public void loadingFailed(Exception e)
            {
                projectLoaded = false;
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

    private static void handleException(String message, Exception e)
    {
        log.error("{}:", message, e);
        Platform.runLater(() ->
        {
            ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            ButtonType showLog = new ButtonType("Show Log", ButtonBar.ButtonData.YES);
            Alert alert = new Alert(Alert.AlertType.ERROR, message + "\nSee the log for more info.", ok, showLog);
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

    private static <ControllerType> ControllerType makeWindow(Window parentWindow, String title, Flag flag,
        Function<Parent, Scene> sceneFactory, String urlString) throws IOException
    {
        URL url = MenubarController.class.getClassLoader().getResource(urlString);
        if (url == null)
        {
            throw new FileNotFoundException(urlString);
        }
        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent root = fxmlLoader.load();
        Stage stage = new Stage();
        stage.getIcons().add(new Image(new File("ibr-icon.png").toURI().toURL().toString()));
        stage.setTitle(title);
        stage.setScene(sceneFactory.apply(root));
        stage.initOwner(parentWindow);

        stage.setResizable(false);

        flag.set(true);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, param -> flag.set(false));

        stage.show();

        return fxmlLoader.getController();
    }

    private static <ControllerType> ControllerType makeWindow(Window parentWindow, String title, Flag flag, String urlString) throws IOException
    {
        return makeWindow(parentWindow, title, flag, Scene::new, urlString);
    }

    private static <ControllerType> ControllerType makeWindow(
        Window parentWindow, String title, Flag flag, int width, int height, String urlString) throws IOException
    {
        return makeWindow(parentWindow, title, flag, root -> new Scene(root, width, height), urlString);
    }

    public boolean isCreateProjectWindowOpen()
    {
        return loaderWindowOpen.get();
    }

    private void onLoadStart()
    {
        projectFile = null;
        vsetFile = null;

        MultithreadModels.getInstance().getLoadingModel().unload();
        projectLoaded = true;
    }

    private void onViewSetCreated(ViewSet viewSet, Window parentWindow)
    {
        // Force user to save the project before proceeding, so that they have a place to save the results
        saveProjectAs(parentWindow, () ->
        {
            File filesDirectory = ViewSet.getDefaultSupportingFilesDirectory(projectFile);
            filesDirectory.mkdirs();

            // need to use a lambda callback so that this is called after the file location is chosen
            // but before its actually saved
            if (Objects.equals(vsetFile, projectFile)) // Saved as a VSET
            {
                // Set the root directory first, then the supporting files directory
                MultithreadModels.getInstance().getLoadingModel()
                    .getLoadedViewSet().setRootDirectory(projectFile.getParentFile());

                viewSet.setSupportingFilesDirectory(filesDirectory);
            }
            else // Saved as a Kintsugi 3D project
            {
                viewSet.setRootDirectory(filesDirectory);
                viewSet.setSupportingFilesDirectory(filesDirectory);
            }
        });
    }

    public void createProject(Window parentWindow)
    {
        if (!loaderWindowOpen.get())
        {
            if (confirmClose("Are you sure you want to create a new project?"))
            {
                try//recent files are updated in CreateProjectController after project is made
                {
                    LoaderController createProjectController =
                        makeWindow(parentWindow, "Load Files", loaderWindowOpen, 750, 330, "fxml/menubar/Loader.fxml");
                    createProjectController.setLoadStartCallback(this::onLoadStart);
                    createProjectController.setViewSetCallback(viewSet -> onViewSetCreated(viewSet, parentWindow));
                    createProjectController.init();
                }
                catch (Exception e)
                {
                    handleException("An error occurred creating a new project", e);
                }
            }
        }
    }

    public void createProjectNew(Window parentWindow)
    {
        if (!loaderWindowOpen.get())
        {
            if (confirmClose("Are you sure you want to create a new project?"))
            {
                try//recent files are updated in CreateProjectController after project is made
                {
                    CreateProjectController createProjectController =
                            makeWindow(parentWindow, "Load Files", loaderWindowOpen, "fxml/menubar/CreateProject.fxml");
                    createProjectController.setLoadStartCallback(this::onLoadStart);
                    createProjectController.setViewSetCallback(viewSet -> onViewSetCreated(viewSet, parentWindow));
                    createProjectController.init();
                }
                catch (Exception e)
                {
                    handleException("An error occurred creating a new project", e);
                }
            }
        }
    }

    private static void startLoad(File projectFile, File vsetFile)
    {
        MultithreadModels.getInstance().getLoadingModel().unload();

        RecentProjects.updateRecentFiles(projectFile.getAbsolutePath());

        if (Objects.equals(projectFile.getParentFile(), vsetFile.getParentFile()))
        {
            // VSET file is the project file or they're in the same directory.
            // Use a supporting files directory underneath by default
            new Thread(() -> MultithreadModels.getInstance().getLoadingModel()
                .loadFromVSETFile(vsetFile.getPath(), vsetFile, ViewSet.getDefaultSupportingFilesDirectory(projectFile)))
                .start();
        }
        else
        {
            // VSET file is presumably already in a supporting files directory, so just use that directory by default
            new Thread(() -> MultithreadModels.getInstance().getLoadingModel()
                .loadFromVSETFile(vsetFile.getPath(), vsetFile))
                .start();
        }
    }

    public void openProjectFromFile(File selectedFile)
    {
        //open the project and update the recent files list
        this.projectFile = selectedFile;
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
        }
    }

    public void openProjectWithPrompt(Window parentWindow)
    {
        if (confirmClose("Are you sure you want to open another project?"))
        {
            FileChooser fileChooser = getProjectFileChooserSafe();
            fileChooser.setTitle("Open project");
            File selectedFile = fileChooser.showOpenDialog(parentWindow);
            if (selectedFile != null)
            {
                //opens project and also updates the recently opened files list
                openProjectFromFile(selectedFile);
            }
        }
    }

    public void saveProject(Window parentWindow)
    {
        if (projectFile == null)
        {
            saveProjectAs(parentWindow);
        }
        else
        {
            try
            {
                LoadingModel loadingModel = MultithreadModels.getInstance().getLoadingModel();

                if (projectFile.getName().endsWith(".vset"))
                {
                    loadingModel.getLoadedViewSet().setRootDirectory(projectFile.getParentFile());
                    loadingModel.saveToVSETFile(projectFile);
                    this.vsetFile = projectFile;
                    this.projectFile = null;
                }
                else
                {
                    File filesDirectory = ViewSet.getDefaultSupportingFilesDirectory(projectFile);
                    filesDirectory.mkdirs();
                    loadingModel.getLoadedViewSet().setRootDirectory(filesDirectory);
                    this.vsetFile = new File(filesDirectory, projectFile.getName() + ".vset");
                    loadingModel.saveToVSETFile(vsetFile);
                    InternalModels.getInstance().getProjectModel().saveProjectFile(projectFile, vsetFile);
                }

                //TODO: MAKE PRETTIER, LOOK INTO NULL SAFETY
                Platform.runLater(() ->
                {
                    Dialog<ButtonType> saveInfo = new Alert(Alert.AlertType.INFORMATION,
                            "Save Complete!");
                    saveInfo.setTitle("Save successful");
                    saveInfo.setHeaderText(projectFile.getName());
                    saveInfo.show();
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
        FileChooser fileChooser = getProjectFileChooserSafe();
        fileChooser.setTitle("Save project");
        fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().get(0));
        if (projectFile != null)
        {
            fileChooser.setInitialFileName(projectFile.getName());
            fileChooser.setInitialDirectory(projectFile.getParentFile());
        }
        else if (vsetFile != null)
        {
            fileChooser.setInitialFileName("");
            fileChooser.setInitialDirectory(vsetFile.getParentFile());
        }
        File selectedFile = fileChooser.showSaveDialog(parentWindow);
        if (selectedFile != null)
        {
            this.projectFile = selectedFile;

            if (callback != null)
            {
                callback.run();
            }

            saveProject(parentWindow);
        }
    }

    public void saveProjectAs(Window parentWindow)
    {
        saveProjectAs(parentWindow, null);
    }

    public void closeProject()
    {
        projectFile = null;
        vsetFile = null;

        MultithreadModels.getInstance().getLoadingModel().unload();
        projectLoaded = false;
    }

    public void closeProjectAfterConfirmation()
    {
        if (confirmClose("Are you sure you want to close the current project?"))
        {
            closeProject();
        }
        }
}
