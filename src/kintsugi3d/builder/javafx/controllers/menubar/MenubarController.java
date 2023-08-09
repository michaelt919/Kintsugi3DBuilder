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

package kintsugi3d.builder.javafx.controllers.menubar;

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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.*;
import javafx.stage.FileChooser.ExtensionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.javafx.FramebufferView;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.app.WindowSynchronization;
import kintsugi3d.builder.core.IBRRequestUI;
import kintsugi3d.builder.core.Kintsugi3DBuilderState;
import kintsugi3d.builder.core.LoadingMonitor;
import kintsugi3d.builder.core.StandardRenderingMode;
import kintsugi3d.builder.export.specularfit.SpecularFitRequestUI;
import kintsugi3d.builder.javafx.InternalModels;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.util.Flag;

public class MenubarController
{
    private static final Logger log = LoggerFactory.getLogger(MenubarController.class);

    private InternalModels internalModels;

    //Window open flags
    private final Flag ibrOptionsWindowOpen = new Flag(false);
    private final Flag jvmOptionsWindowOpen = new Flag(false);
    private final Flag loadOptionsWindowOpen = new Flag(false);
    private final Flag loaderWindowOpen = new Flag(false);
    private final Flag colorCheckerWindowOpen = new Flag(false);
    private final Flag unzipperOpen = new Flag(false);
    private final Flag consoleWindowOpen = new Flag(false);

    @FXML private ProgressBar progressBar;

    //toggle groups
    @FXML private ToggleGroup renderGroup;

    //menu items
    @FXML private CheckMenuItem lightCalibrationCheckMenuItem;
    @FXML private CheckMenuItem is3DGridCheckMenuItem;
    @FXML private CheckMenuItem compassCheckMenuItem;
    @FXML private CheckMenuItem halfResolutionCheckMenuItem;
    @FXML private CheckMenuItem multiSamplingCheckMenuItem;
    @FXML private CheckMenuItem sceneWindowMenuItem;
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

    @FXML private FramebufferView framebufferView;

    private Window stage;

    private File projectFile;
    private File vsetFile;
    private boolean projectLoaded;

    private Runnable userDocumentationHandler;


    public <ContextType extends Context<ContextType>> void init(
        Stage injectedStage, InternalModels injectedInternalModels, Runnable injectedUserDocumentationHandler)
    {
        this.stage = injectedStage;
        this.framebufferView.registerKeyAndWindowEventsFromStage(injectedStage);

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
                handleException("An error occurred while loading project", e);
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
                            Method createMethod = requestUIClass.getDeclaredMethod("create", Window.class, Kintsugi3DBuilderState.class);
                            if (IBRRequestUI.class.isAssignableFrom(createMethod.getReturnType())
                                && ((createMethod.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) == (Modifier.PUBLIC | Modifier.STATIC)))
                            {
                                MenuItem newItem = new MenuItem(menuName);
                                newItem.setOnAction(event ->
                                {
                                    try
                                    {
                                        IBRRequestUI requestUI = (IBRRequestUI) createMethod.invoke(null, injectedStage, MultithreadModels.getInstance());
                                        requestUI.bind(internalModels.getSettingsModel());
                                        requestUI.prompt(Rendering.getRequestQueue());
                                    }
                                    catch (IllegalAccessException | InvocationTargetException e)
                                    {
                                        log.error("An error has occurred:", e);
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
                            log.error("An error has occurred:", e);
                        }
                    }
                }
            }
            catch (FileNotFoundException e)
            {
                log.error("Failed to find export classes file:", e);
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

    public FramebufferView getFramebufferView()
    {
        return framebufferView;
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
        sceneWindowMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("sceneWindowOpen"));
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
            catch (Exception e)
            {
                handleException("An error occurred creating project", e);
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
            File selectedFile = projectFileChooser.showOpenDialog(stage);
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
                    catch (Exception e)
                    {
                        handleException("An error occurred opening project", e);
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
            catch(Exception e)
            {
                handleException("An error occurred saving project", e);
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
        File selectedFile = projectFileChooser.showSaveDialog(stage);
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
    private void exportSpecularFit(){
        try {
            IBRRequestUI requestUI = SpecularFitRequestUI.create(this.stage, MultithreadModels.getInstance());
            requestUI.bind(internalModels.getSettingsModel());
            requestUI.prompt(Rendering.getRequestQueue());

        } catch (Exception e) {
            handleException("An error occurred handling request", e);
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
        catch(Exception e)
        {
            handleException("An error occurred opening load options", e);
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
            List<String> lines = Files.readAllLines(new File("kintsugi3d-builder-about.txt").toPath());
            Alert alert = new Alert(AlertType.INFORMATION, String.join(System.lineSeparator(), lines));
            alert.setTitle("About Kintsugi 3D Builder");
            alert.setHeaderText("About Kintsugi 3D Builder");
            alert.initOwner(this.stage);
            alert.initModality(Modality.NONE);
            alert.show();
            alert.setY(100.0);
        }
        catch (Exception e)
        {
            handleException("An error occurred showing help and about", e);
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
        catch(Exception e)
        {
            handleException("An error occurred opening IBR settings", e);
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
        stage.initOwner(this.stage);

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
        stage.initOwner(this.stage);
        stage.setResizable(false);
        flag.set(true);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, param -> flag.set(false));
        stage.show();

        return fxmlLoader.getController();
    }

    private Stage makeStage(String title, Flag flag, String urlString) throws IOException
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
        stage.initOwner(this.stage);

        flag.set(true);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, param -> flag.set(false));

        return stage;
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
        catch(Exception e)
        {
            handleException("An error occurred opening color checker", e);
        }
    }

    public void unzip() {
        try {
            UnzipFileSelectionController unzipFileSelectionController =
                makeWindow(".psx Unzipper", unzipperOpen, "fxml/menubar/UnzipFileSelection.fxml");
            unzipFileSelectionController.init();
        }
        catch(Exception e)
        {
            handleException("An error occurred opening file unzipper", e);
        }
    }
          
    public void eyedropperColorChecker()
    {
        if (colorCheckerWindowOpen.get())
        {
            return;
        }

        try
        {
            ColorCheckerImgSelectionController colorCheckerController =
                    makeWindow("Color Checker", colorCheckerWindowOpen, "fxml/menubar/ColorCheckerImgSelection.fxml");
            colorCheckerController.init(MultithreadModels.getInstance().getLoadingModel());

        }
        catch(Exception e)
        {
            handleException("An error occurred opening color checker window", e);
        }
    }

    public void shading_JVMSettings(ActionEvent actionEvent)
    {
        if (jvmOptionsWindowOpen.get())
        {
            return;
        }

        try
        {
            JvmSettingsController jvmSettingsController = makeWindow("JVM Settings", jvmOptionsWindowOpen, "fxml/menubar/JvmSettings.fxml");
        }
        catch(Exception e)
        {
            handleException("An error occurred opening jvm settings window", e);
        }
    }

    public void help_console(ActionEvent actionEvent)
    {
        if (consoleWindowOpen.get())
        {
            return;
        }

        try
        {
            Stage stage = makeStage("Log", consoleWindowOpen, "fxml/menubar/Console.fxml");
            stage.setResizable(true);
            stage.initStyle(StageStyle.DECORATED);
            stage.show();
        }
        catch (Exception e)
        {
            handleException("An error occurred opening console window", e);
        }
    }

    private void handleException(String message, Exception e)
    {
        log.error("{}:", message, e);
        Platform.runLater(() ->
        {
            new Alert(AlertType.ERROR, message + "\nSee the log for more info.").show();
        });
    }
}
