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

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import kintsugi3d.builder.core.IBRRequestManager;
import kintsugi3d.builder.javafx.InternalModels;
import kintsugi3d.builder.javafx.ProjectIO;
import kintsugi3d.gl.core.Context;
import kintsugi3d.util.Flag;
import kintsugi3d.util.RecentProjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class WelcomeWindowController
{
    private static final Logger log = LoggerFactory.getLogger(WelcomeWindowController.class);

    //Window open flags
    private final Flag ibrOptionsWindowOpen = new Flag(false);
    private final Flag jvmOptionsWindowOpen = new Flag(false);
    private final Flag loadOptionsWindowOpen = new Flag(false);
    private final Flag colorCheckerWindowOpen = new Flag(false);
    private final Flag unzipperOpen = new Flag(false);


    @FXML private ProgressBar progressBar;

    //toggle groups
    @FXML private ToggleGroup renderGroup;

    @FXML public SplitMenuButton recentProjectsSplitMenuButton;

    private Window parentWindow;

    private Runnable userDocumentationHandler;

    public <ContextType extends Context<ContextType>> void init(
            Stage injectedStage, IBRRequestManager<ContextType> requestQueue, InternalModels injectedInternalModels,
            Runnable injectedUserDocumentationHandler) {
        this.parentWindow = injectedStage.getOwner();
        this.userDocumentationHandler = injectedUserDocumentationHandler;

        RecentProjects.initializeWelcomeWindowController(this);
        updateRecentProjectsButton();

//        MultithreadModels.getInstance().getLoadingModel().addLoadingMonitor(new LoadingMonitor()
//        {
//            private double maximum = 0.0;
//            private double progress = 0.0;
//
//            @Override
//            public void startLoading() {
//                progress = 0.0;
//                Platform.runLater(() ->
//                {
//                    progressBar.setVisible(true);
//                    progressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : 0.0);
//                });
//            }
//
//            @Override
//            public void setMaximum(double maximum) {
//                this.maximum = maximum;
//                Platform.runLater(() -> progressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : progress / maximum));
//            }
//
//            @Override
//            public void setProgress(double progress) {
//                this.progress = progress;
//                Platform.runLater(() -> progressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : progress / maximum));
//            }
//
//            @Override
//            public void loadingComplete() {
//                this.maximum = 0.0;
//                Platform.runLater(() -> progressBar.setVisible(false));
//            }
//
//            @Override
//            public void loadingFailed(Exception e) {
//                loadingComplete();
//            }
//        });
    }

    public void updateRecentProjectsButton() {
        RecentProjects.updateRecentProjectsControl(recentProjectsSplitMenuButton);
    }

    public void handleMenuItemSelection(MenuItem item) {
        String projectName = item.getText();
        ProjectIO.getInstance().openProjectFromFile(new File(projectName));
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
        if (!ProjectIO.getInstance().isCreateProjectWindowOpen())
        {
            ProjectIO.getInstance().createProject(parentWindow);
            updateRecentProjectsButton();
        }
    }

    public void createProject()
    {
        if (!ProjectIO.getInstance().isCreateProjectWindowOpen())
        {
            ProjectIO.getInstance().createProjectNew(parentWindow);
            updateRecentProjectsButton();
        }
    }

    @FXML
    private void file_openProject()//TODO: CHANGE NAMING CONVENTION? (file_...)
    {
        ProjectIO.getInstance().openProjectWithPrompt(parentWindow);
    }

    @FXML
    private void file_closeProject()
    {
        //TODO: DISABLE THIS BUTTON IF NO PROJECT IS OPEN?
        ProjectIO.getInstance().closeProjectAfterConfirmation();
    }

    //TODO: HIDE WELCOME WINDOW WHEN A PROJECT IS MADE/OPENED
//    public void hideWelcomeWindow(){
//        stage.hide();
//    }
//    public void showWelcomeWindow(){
//        stage.show();
//    }
//

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
