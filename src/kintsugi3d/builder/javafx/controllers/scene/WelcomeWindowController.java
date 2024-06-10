/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.scene;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import kintsugi3d.builder.core.IBRRequestManager;
import kintsugi3d.builder.javafx.InternalModels;
import kintsugi3d.builder.javafx.ProjectIO;
import kintsugi3d.gl.core.Context;
import kintsugi3d.util.RecentProjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WelcomeWindowController
{
    private static final Logger log = LoggerFactory.getLogger(WelcomeWindowController.class);


    private static WelcomeWindowController INSTANCE;
    @FXML private Button recent1;
    @FXML private Button recent2;
    @FXML private Button recent3;
    @FXML private Button recent4;
    @FXML private Button recent5;
    public List<Button> recentButtons = new ArrayList<>();
    public static WelcomeWindowController getInstance()
    {
        return INSTANCE;
    }

    @FXML public SplitMenuButton recentProjectsSplitMenuButton;

    private Window parentWindow;

    private Runnable userDocumentationHandler;
    private Stage window;

    public <ContextType extends Context<ContextType>> void init(
            Stage injectedStage, IBRRequestManager<ContextType> requestQueue, InternalModels injectedInternalModels,
            Runnable injectedUserDocumentationHandler) {
        INSTANCE = this;

        this.parentWindow = injectedStage.getOwner();
        this.window = injectedStage;
        this.userDocumentationHandler = injectedUserDocumentationHandler;

        recentButtons.add(recent1);
        recentButtons.add(recent2);
        recentButtons.add(recent3);
        recentButtons.add(recent4);
        recentButtons.add(recent5);

        RecentProjects.updateAllControlStructures();
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

    public void createProject()
    {
        if (!ProjectIO.getInstance().isCreateProjectWindowOpen())
        {
            ProjectIO.getInstance().createProject(parentWindow);
        }
    }

    @FXML
    private void openProject()
    {
        ProjectIO.getInstance().openProjectWithPrompt(parentWindow);
    }

    //TODO: FIND WAY TO NOT CLOSE FILE, BUT HIDE SO IT CAN BE RESHOWN
    public void hideWelcomeWindow(){
        window.close();
    }

    //TODO: just create a new welcome window?
//    public void showWelcomeWindow(){
//        window.show();
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

    public void recentButton(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
        //user clicks on a menu item
        if (source.getClass() == Button.class) {
            handleButtonSelection((Button) actionEvent.getSource());
        }
    }

    public void handleButtonSelection(Button item) {
        ArrayList<String> recentFileNames = (ArrayList<String>) RecentProjects.getItemsFromRecentsFile();
        int i = 0;
        for (Button button : recentButtons){
            if (button == item){
                ProjectIO.getInstance().openProjectFromFile(new File(recentFileNames.get(i)));
            }
            i++;
        }
    }
}
