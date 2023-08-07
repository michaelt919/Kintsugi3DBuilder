/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.javafx.controllers.scene;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import main.resources.fxml.menubar.CreateProjectController;
import org.xml.sax.SAXException;
import kintsugi3d.gl.core.Context;
import kintsugi3d.builder.core.IBRRequestManager;
import kintsugi3d.builder.core.LoadingMonitor;
import kintsugi3d.builder.javafx.InternalModels;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.controllers.menubar.LoaderController;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import kintsugi3d.util.Flag;
import kintsugi3d.util.RecentProjects;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;

public class WelcomeWindowController {
    private InternalModels internalModels;

    //Window open flags
    private final Flag ibrOptionsWindowOpen = new Flag(false);
    private final Flag jvmOptionsWindowOpen = new Flag(false);
    private final Flag loadOptionsWindowOpen = new Flag(false);
    private final Flag loaderWindowOpen = new Flag(false);
    private final Flag colorCheckerWindowOpen = new Flag(false);
    private final Flag unzipperOpen = new Flag(false);


    @FXML private ProgressBar progressBar;

    //toggle groups
    @FXML private ToggleGroup renderGroup;

    @FXML public FileChooser projectFileChooser;

    @FXML public SplitMenuButton recentProjectsSplitMenuButton;

    private Window parentWindow;

    private File projectFile;
    private File vsetFile;
    private boolean projectLoaded;

    private Runnable userDocumentationHandler;

    private IBRRequestManager<?> requestQueue;

    public <ContextType extends Context<ContextType>> void init(
            Window injectedParentWindow, IBRRequestManager<ContextType> requestQueue, InternalModels injectedInternalModels,
            Runnable injectedUserDocumentationHandler) {
        this.parentWindow = injectedParentWindow;
        this.internalModels = injectedInternalModels;
        this.userDocumentationHandler = injectedUserDocumentationHandler;

        projectFileChooser = new FileChooser();

        this.requestQueue = requestQueue;

        projectFileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        projectFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Full projects", "*.ibr"));
        projectFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Standalone view sets", "*.vset"));

        RecentProjects.initializeWelcomeWindowController(this);
        updateRecentProjectsButton();

        MultithreadModels.getInstance().getLoadingModel().setLoadingMonitor(new LoadingMonitor() {
            private double maximum = 0.0;
            private double progress = 0.0;

            @Override
            public void startLoading() {
                progress = 0.0;
                Platform.runLater(() ->
                {
                    progressBar.setVisible(true);
                    progressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : 0.0);
                });
            }

            @Override
            public void setMaximum(double maximum) {
                this.maximum = maximum;
                Platform.runLater(() -> progressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : progress / maximum));
            }

            @Override
            public void setProgress(double progress) {
                this.progress = progress;
                Platform.runLater(() -> progressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : progress / maximum));
            }

            @Override
            public void loadingComplete() {
                this.maximum = 0.0;
                Platform.runLater(() -> progressBar.setVisible(false));
            }

            @Override
            public void loadingFailed(Exception e) {
                loadingComplete();
                projectLoaded = false;
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, e.toString()).show());
            }
        });
    }

    public void updateRecentProjectsButton() {//TODO: FORMAT ----- PROJECT NAME --> PATH
        //TODO: REMOVE REPETITION WITH MENUBARCONTROLLER
        recentProjectsSplitMenuButton.getItems().clear();

        ArrayList<MenuItem> recentItems = (ArrayList<MenuItem>) RecentProjects.getItemsAsMenuItems();

        recentProjectsSplitMenuButton.getItems().addAll(recentItems);

        //disable button if there are no recent projects
        if(recentProjectsSplitMenuButton.getItems().isEmpty()){
            recentProjectsSplitMenuButton.setDisable(true);
        }

        //attach event handlers to all menu items
        for (MenuItem item : recentItems){
            item.setOnAction(event -> handleMenuItemSelection(item));
        }
    }

    public void handleMenuItemSelection(MenuItem item) {
        String projectName = item.getText();
        openProjectFromFile(new File(projectName));
    }

    public void splitMenuButtonActions(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();

        //user clicks on a menu item
        if (source.getClass() == MenuItem.class) {
            handleMenuItemSelection((MenuItem) actionEvent.getSource());
        }

        //user clicks on the button, so unroll the menu
        else{
            unrollMenu();
        }
    }

    @FXML
    private void file_createProject()
    {
        if (loaderWindowOpen.get())
        {
            return;
        }

        if (confirmClose("Are you sure you want to create a new project?"))
        {
            try
            {
                LoaderController loaderController = makeWindow("Load Files", loaderWindowOpen, 750, 330, "fxml/menubar/Loader.fxml");
                loaderController.setCallback(() ->
                {
                    this.file_closeProject();
                    projectLoaded = true;
                });
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        updateRecentProjectsButton();
    }

    public void createProject() {
        if (loaderWindowOpen.get())
        {
            return;
        }

        if (confirmClose("Are you sure you want to create a new project?"))
        {
            try//recent files are updated in CreateProjectController after project is made
            {
                CreateProjectController createProjectController = makeWindow("Load Files", loaderWindowOpen, "fxml/menubar/CreateProject.fxml");
                createProjectController.setCallback(() ->
                {
                    this.file_closeProject();
                    projectLoaded = true;
                });
                createProjectController.init();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void file_openProject()//TODO: CHANGE NAMING CONVENTION? (file_...)
    {
        if (confirmClose("Are you sure you want to open another project?"))
        {
            projectFileChooser.setTitle("Open project");
            File selectedFile = projectFileChooser.showOpenDialog(parentWindow);
            if (selectedFile != null)
            {
                //opens project and also updates the recently opened files list
                openProjectFromFile(selectedFile);
            }
        }
    }

    private void openProjectFromFile(File selectedFile) {
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
                newVsetFile = internalModels.getProjectModel().openProjectFile(projectFile);
            }
            catch (IOException | ParserConfigurationException | SAXException e)
            {
                e.printStackTrace();
            }
        }

        if (newVsetFile != null)
        {
            MultithreadModels.getInstance().getLoadingModel().unload();

            this.vsetFile = newVsetFile;
            File vsetFileRef = newVsetFile;

            RecentProjects.updateRecentFiles(projectFile.getAbsolutePath());

            projectLoaded = true;

            new Thread(() -> MultithreadModels.getInstance().getLoadingModel().loadFromVSETFile(vsetFileRef.getPath(), vsetFileRef)).start();
        }
    }

    private <ControllerType> ControllerType makeWindow(String title, Flag flag, String urlString) throws IOException
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
        stage.setScene(new Scene(root));
        stage.initOwner(parentWindow);

        stage.setResizable(false);

        flag.set(true);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, param -> flag.set(false));

        stage.show();

        return fxmlLoader.getController();
    }

    private <ControllerType> ControllerType makeWindow(String title, Flag flag, int width, int height, String urlString) throws IOException
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
        stage.setScene(new Scene(root, width, height));
        stage.initOwner(parentWindow);
        stage.setResizable(false);
        flag.set(true);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, param -> flag.set(false));
        stage.show();

        return fxmlLoader.getController();
    }

    private boolean confirmClose(String text)
    {
        if (projectLoaded)
        {
            Dialog<ButtonType> confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "If you click OK, any unsaved changes to the current project will be lost.");
            confirmation.setTitle("Close Project Confirmation");
            confirmation.setHeaderText(text);
            return confirmation.showAndWait()
                    .filter(Predicate.isEqual(ButtonType.OK))
                    .isPresent();
        }
        else
        {
            return true;
        }
    }
    @FXML
    private void file_closeProject()
    {
        //TODO: DISABLE THIS BUTTON IF NO PROJECT IS OPEN?
        if (confirmClose("Are you sure you want to close the current project?"))
        {
            projectFile = null;
            vsetFile = null;

            MultithreadModels.getInstance().getLoadingModel().unload();
            projectLoaded = false;
        }
    }



    @FXML
    private void help_userManual()
    {
        userDocumentationHandler.run();
    }

    public void unrollMenu() {
        recentProjectsSplitMenuButton.show();
    }

    public void hideMenu(MouseEvent mouseEvent){
        //recentProjectsSplitMenuButton.hide();
        //TODO: ONLY HIDE THE MENU WHEN THE USER'S MOUSE LEAVES THE CONTEXT MENU
    }

}
