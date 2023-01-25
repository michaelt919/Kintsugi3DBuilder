/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx.controllers.menubar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.*;
import javafx.stage.FileChooser.ExtensionFilter;
import org.xml.sax.SAXException;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.ibrelight.app.WindowSynchronization;
import tetzlaff.ibrelight.core.*;
import tetzlaff.ibrelight.javafx.InternalModels;
import tetzlaff.ibrelight.javafx.MultithreadModels;
import tetzlaff.util.Flag;

public class MenubarController
{
    private InternalModels internalModels;

    //Window open flags
    private final Flag ibrOptionsWindowOpen = new Flag(false);
    private final Flag loadOptionsWindowOpen = new Flag(false);
    private final Flag loaderWindowOpen = new Flag(false);
    private final Flag colorCheckerWindowOpen = new Flag(false);

    @FXML private ProgressBar progressBar;

    //toggle groups
    @FXML private ToggleGroup renderGroup;

    //menu items
    @FXML private CheckMenuItem lightCalibrationCheckMenuItem;
    @FXML private CheckMenuItem is3DGridCheckMenuItem;
    @FXML private CheckMenuItem compassCheckMenuItem;
    @FXML private CheckMenuItem halfResolutionCheckMenuItem;
    @FXML private CheckMenuItem multiSamplingCheckMenuItem;
    @FXML private CheckMenuItem relightingCheckMenuItem;
    @FXML private CheckMenuItem environmentMappingCheckMenuItem; //TODO imp. this
    @FXML private CheckMenuItem shadowsCheckMenuItem;
    @FXML private CheckMenuItem visibleLightsCheckMenuItem;
    @FXML private CheckMenuItem visibleLightWidgetsCheckMenuItem;
    @FXML private CheckMenuItem visibleCameraPoseCheckMenuItem;
    @FXML private CheckMenuItem visibleSavedCameraPoseCheckMenuItem;

    @FXML private CheckMenuItem phyMaskingCheckMenuItem;
    @FXML private CheckMenuItem fresnelEffectCheckMenuItem;

    @FXML private FileChooser projectFileChooser;

    @FXML private Menu exportMenu;

    private Window parentWindow;

    private File projectFile;
    private File vsetFile;
    private boolean projectLoaded;

    private Runnable userDocumentationHandler;


    public <ContextType extends Context<ContextType>> void init(
        Window injectedParentWindow, IBRRequestManager<ContextType> requestQueue, InternalModels injectedInternalModels,
        Runnable injectedUserDocumentationHandler)
    {
        this.parentWindow = injectedParentWindow;
        this.internalModels = injectedInternalModels;
        this.userDocumentationHandler = injectedUserDocumentationHandler;

        projectFileChooser = new FileChooser();

        projectFileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        projectFileChooser.getExtensionFilters().add(new ExtensionFilter("Full projects", "*.ibr"));
        projectFileChooser.getExtensionFilters().add(new ExtensionFilter("Standalone view sets", "*.vset"));

        MultithreadModels.getInstance().getLoadingModel().setLoadingMonitor(new LoadingMonitor()
        {
            private double maximum = 0.0;
            private double progress = 0.0;
            @Override
            public void startLoading()
            {
                progress = 0.0;
                Platform.runLater(() ->
                {
                    progressBar.setVisible(true);
                    progressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : 0.0);
                });
            }

            @Override
            public void setMaximum(double maximum)
            {
                this.maximum = maximum;
                Platform.runLater(() -> progressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : progress / maximum));
            }

            @Override
            public void setProgress(double progress)
            {
                this.progress = progress;
                Platform.runLater(() -> progressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : progress / maximum));
            }

            @Override
            public void loadingComplete()
            {
                this.maximum = 0.0;
                Platform.runLater(() ->  progressBar.setVisible(false));
            }

            @Override
            public void loadingFailed(Exception e)
            {
                loadingComplete();
                projectLoaded = false;
                Platform.runLater(() -> new Alert(AlertType.ERROR, e.toString()).show());
            }
        });

        boolean foundExportClass = false;
        File exportClassDefinitionFile = new File("export-classes.txt");
        if (exportClassDefinitionFile.exists())
        {
            try (Scanner scanner = new Scanner(exportClassDefinitionFile))
            {
                while (scanner.hasNext())
                {
                    String className = scanner.next();

                    if (scanner.hasNextLine())
                    {
                        String menuName = scanner.nextLine().trim();

                        try
                        {
                            Class<?> requestUIClass = Class.forName(className);
                            Method createMethod = requestUIClass.getDeclaredMethod("create", Window.class, IBRelightModels.class);
                            if (IBRRequestUI.class.isAssignableFrom(createMethod.getReturnType())
                                && ((createMethod.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) == (Modifier.PUBLIC | Modifier.STATIC)))
                            {
                                MenuItem newItem = new MenuItem(menuName);
                                newItem.setOnAction(event ->
                                {
                                    try
                                    {
                                        IBRRequestUI requestUI = (IBRRequestUI) createMethod.invoke(null, injectedParentWindow, MultithreadModels.getInstance());
                                        requestUI.bind(internalModels.getSettingsModel());
                                        requestUI.prompt(requestQueue);
                                    }
                                    catch (IllegalAccessException | InvocationTargetException e)
                                    {
                                        e.printStackTrace();
                                    }
                                });
                                exportMenu.getItems().add(newItem);
                                foundExportClass = true;
                            }
                            else
                            {
                                System.err.println("create() method for " + requestUIClass.getName() + " is invalid.");
                            }
                        }
                        catch (ClassNotFoundException | NoSuchMethodException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }

        if (!foundExportClass)
        {
            exportMenu.setVisible(false);
        }

        initToggleGroups();
        bindCheckMenuItems();

        lightCalibrationCheckMenuItem.selectedProperty().addListener(observable ->
        {
            if (!lightCalibrationCheckMenuItem.isSelected())
            {
                MultithreadModels.getInstance().getLoadingModel().applyLightCalibration();
                MultithreadModels.getInstance().getSettingsModel().set("currentLightCalibration", Vector2.ZERO);
            }
        });
    }

    private void initToggleGroups()
    {
        renderGroup.selectedToggleProperty().addListener((ob, o, n) ->
        {
            if (n != null && n.getUserData() instanceof StandardRenderingMode)
            {
                internalModels.getSettingsModel().set("renderingMode", n.getUserData());
            }
        });
    }

    private void bindCheckMenuItems()
    {
        //value binding
        lightCalibrationCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("lightCalibrationMode"));
        is3DGridCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("is3DGridEnabled"));
        compassCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("compassEnabled"));
        relightingCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("relightingEnabled"));
        shadowsCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("shadowsEnabled"));
        visibleLightsCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("visibleLightsEnabled"));
        visibleLightWidgetsCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("lightWidgetsEnabled"));
        visibleCameraPoseCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("visibleCameraPosesEnabled"));
        visibleSavedCameraPoseCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("visibleSavedCameraPosesEnabled"));
        phyMaskingCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("pbrGeometricAttenuationEnabled"));
        fresnelEffectCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("fresnelEnabled"));
        halfResolutionCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("halfResolutionEnabled"));
        multiSamplingCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("multisamplingEnabled"));
    }

    //Menubar->File

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
    }

    private boolean confirmClose(String text)
    {
        if (projectLoaded)
        {
            Dialog<ButtonType> confirmation = new Alert(AlertType.CONFIRMATION,
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
    private void file_openProject()
    {
        if (confirmClose("Are you sure you want to open another project?"))
        {
            projectFileChooser.setTitle("Open project");
            File selectedFile = projectFileChooser.showOpenDialog(parentWindow);
            if (selectedFile != null)
            {
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

                    projectLoaded = true;

                    new Thread(() -> MultithreadModels.getInstance().getLoadingModel().loadFromVSETFile(vsetFileRef.getPath(), vsetFileRef)).start();
                }
            }
        }
    }

    @FXML
    private void file_saveProject()
    {
        if (projectFile == null)
        {
            file_saveProjectAs();
        }
        else
        {
            try
            {
                if (projectFile.getName().endsWith(".vset"))
                {
                    MultithreadModels.getInstance().getLoadingModel().saveToVSETFile(projectFile);
                    this.vsetFile = projectFile;
                    this.projectFile = null;
                }
                else
                {
                    this.vsetFile = new File(projectFile + ".vset");
                    MultithreadModels.getInstance().getLoadingModel().saveToVSETFile(vsetFile);
                    internalModels.getProjectModel().saveProjectFile(projectFile, vsetFile);
                }
            }
            catch(IOException | TransformerException | ParserConfigurationException e)
            {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void file_saveProjectAs()
    {
        projectFileChooser.setTitle("Save project");
        projectFileChooser.setSelectedExtensionFilter(projectFileChooser.getExtensionFilters().get(0));
        if (projectFile != null)
        {
            projectFileChooser.setInitialFileName(projectFile.getName());
            projectFileChooser.setInitialDirectory(projectFile.getParentFile());
        }
        else if (vsetFile != null)
        {
            projectFileChooser.setInitialFileName("");
            projectFileChooser.setInitialDirectory(vsetFile.getParentFile());
        }
        File selectedFile = projectFileChooser.showSaveDialog(parentWindow);
        if (selectedFile != null)
        {
            this.projectFile = selectedFile;
            file_saveProject();
        }
    }

    @FXML
    private void file_closeProject()
    {
        if (confirmClose("Are you sure you want to close the current project?"))
        {
            projectFile = null;
            vsetFile = null;

            MultithreadModels.getInstance().getLoadingModel().unload();
            projectLoaded = false;
        }
    }

    @FXML
    private void file_loadOptions()
    {
        if (loadOptionsWindowOpen.get())
        {
            return;
        }

        try
        {
            LoadOptionsController loadOptionsController = makeWindow("Load Options", loadOptionsWindowOpen, "fxml/menubar/LoadOptions.fxml");
            loadOptionsController.bind(internalModels.getLoadOptionsModel());
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    @FXML
    private void file_exit()
    {
        WindowSynchronization.getInstance().quit();
    }

    @FXML
    private void help_userManual()
    {
        userDocumentationHandler.run();
    }

    public void help_about()
    {
        try
        {
            List<String> lines = Files.readAllLines(new File("ibrelight-about.txt").toPath());
            Alert alert = new Alert(AlertType.INFORMATION, String.join(System.lineSeparator(), lines));
            alert.setTitle("About IBRelight");
            alert.setHeaderText("About IBRelight");
            alert.initOwner(this.parentWindow);
            alert.initModality(Modality.NONE);
            alert.show();
            alert.setY(100.0);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @FXML
    private void shading_IBRSettings()
    {
        if (ibrOptionsWindowOpen.get())
        {
            return;
        }

        try
        {
            IBROptionsController ibrOptionsController = makeWindow("IBR Settings", ibrOptionsWindowOpen, "fxml/menubar/IBROptions.fxml");
            ibrOptionsController.bind(internalModels.getSettingsModel());
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    //window helpers
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

    public void file_colorChecker()
    {
        if (colorCheckerWindowOpen.get())
        {
            return;
        }

        try
        {
            ColorCheckerController colorCheckerController =
                makeWindow("Color Checker", colorCheckerWindowOpen, "fxml/menubar/ColorChecker.fxml");
            colorCheckerController.init(MultithreadModels.getInstance().getLoadingModel());

        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
