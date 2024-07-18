/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.app.WindowSynchronization;
import kintsugi3d.builder.core.*;
import kintsugi3d.builder.export.projectExporter.ExportRequestUI;
import kintsugi3d.builder.export.specular.SpecularFitRequestUI;
import kintsugi3d.builder.javafx.InternalModels;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.ProjectIO;
import kintsugi3d.builder.javafx.controllers.menubar.systemsettings.AdvPhotoViewController;
import kintsugi3d.builder.javafx.controllers.scene.ProgressBarsController;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import kintsugi3d.builder.javafx.controllers.scene.object.ObjectPoseSetting;
import kintsugi3d.builder.javafx.controllers.scene.object.SettingsObjectSceneController;
import kintsugi3d.builder.util.Kintsugi3DViewerLauncher;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.javafx.FramebufferView;
import kintsugi3d.util.Flag;
import kintsugi3d.util.RecentProjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MenubarController
{
    private static final Logger log = LoggerFactory.getLogger(MenubarController.class);

    private static MenubarController instance;

    private InternalModels internalModels;

    //Window open flags
    private final Flag advPhotoViewWindowOpen = new Flag(false);
    private final Flag systemMemoryWindowOpen = new Flag(false);
    private final Flag loadOptionsWindowOpen = new Flag(false);
    private final Flag objectOrientationWindowOpen = new Flag(false);
    private final Flag lightCalibrationWindowOpen = new Flag(false);
    private final Flag colorCheckerWindowOpen = new Flag(false);
    private final Flag unzipperOpen = new Flag(false);
    private final Flag loggerWindowOpen = new Flag(false);

    //progress bar modal
    private ProgressBar localProgressBar;
    private ProgressBar overallProgressBar;
    private Button cancelButton;
    private Label localTextLabel;
    private Label overallTextLabel;

    //minimized progress bar
    @FXML private HBox miniProgressHBox; //entire bottom bar
    @FXML private HBox miniProgBarBoundingHBox; //only label and progress bar
    @FXML private Label miniProgressLabel;
    @FXML private ProgressBar miniProgressBar;
    private Font origMiniProgLabelFont;

    //toggle groups
    @FXML private ToggleGroup renderGroup;

    @FXML private Menu aboutMenu;

    @FXML private MenuBar mainMenubar;

    //menu items
    @FXML private CheckMenuItem is3DGridCheckMenuItem;
    @FXML private CheckMenuItem compassCheckMenuItem;
    @FXML private CheckMenuItem halfResolutionCheckMenuItem;
    @FXML private CheckMenuItem multiSamplingCheckMenuItem;
    @FXML private CheckMenuItem sceneWindowMenuItem;
    @FXML private CheckMenuItem relightingCheckMenuItem;
    @FXML private CheckMenuItem sceneWindowCheckMenuItem;
    @FXML private CheckMenuItem environmentMappingCheckMenuItem; //TODO imp. this
    @FXML private CheckMenuItem shadowsCheckMenuItem;
    @FXML private CheckMenuItem visibleLightsCheckMenuItem;
    @FXML private CheckMenuItem visibleLightWidgetsCheckMenuItem;
    @FXML private CheckMenuItem visibleCameraPoseCheckMenuItem;
    @FXML private CheckMenuItem visibleSavedCameraPoseCheckMenuItem;

    @FXML private CheckMenuItem phyMaskingCheckMenuItem;
    @FXML private CheckMenuItem fresnelEffectCheckMenuItem;

    @FXML private CheckMenuItem autoCacheClearingCheckMenuItem;
    @FXML private CheckMenuItem autoSaveCheckMenuItem;
    @FXML private CheckMenuItem mipmapCheckMenuItem;
    @FXML private CheckMenuItem reduceViewportResolutionCheckMenuItem;
    @FXML private CheckMenuItem darkModeCheckMenuItem;
    @FXML private CheckMenuItem standAlone3dViewerCheckMenuItem;

    @FXML private CheckMenuItem imageCompressionCheckMenuItem;

    @FXML private Menu perLightIntensityMenu;
    @FXML private Menu ambientLightSettingsMenu;
    @FXML public Slider perLight1IntensitySlider;
    @FXML public Slider ambientLightIntensitySlider;

    @FXML public TextField perLight1IntensityTxtField;
    @FXML
    private MenuItem perLightColorPickerMenuItem;

    @FXML public TextField ambientLightIntensityTxtField;

    @FXML private Menu exportMenu;
    @FXML private Menu recentProjectsMenu;
    @FXML private Menu cleanRecentProjectsMenu;

    @FXML private CustomMenuItem removeAllRefsCustMenuItem;
    @FXML private CustomMenuItem removeSomeRefsCustMenuItem;

    //shaders which should only be enabled after processing textures
    @FXML private RadioMenuItem materialMetallicity;
    @FXML private RadioMenuItem materialReflectivity;
    @FXML private RadioMenuItem materialBasis;
    @FXML private RadioMenuItem imgBasedWithTextures;

    private List<RadioMenuItem> toggleableShaders = new ArrayList<>();

    @FXML private VBox cameraViewList;
    @FXML private CameraViewListController cameraViewListController;
    @FXML private FramebufferView framebufferView;

    private Window window;

    private Runnable userDocumentationHandler;

    public MenubarController()
    {
        instance = this;
    }

    public static MenubarController getInstance()
    {
        return instance;
    }

    public <ContextType extends Context<ContextType>> void init(
        Stage injectedStage, InternalModels injectedInternalModels, Runnable injectedUserDocumentationHandler)
    {

        this.window = injectedStage;
        this.framebufferView.registerKeyAndWindowEventsFromStage(injectedStage);

        // remove camera view list from layout when invisible
        this.cameraViewList.managedProperty().bind(this.cameraViewList.visibleProperty());

        // only show camera view list when light calibration mode is active
        // TODO make this a separate property to allow it to be shown in other contexts
        this.cameraViewList.visibleProperty().bind(injectedInternalModels.getSettingsModel().getBooleanProperty("lightCalibrationMode"));

        this.cancelButton = ProgressBarsController.getInstance().getCancelButton();
        this.localTextLabel = ProgressBarsController.getInstance().getLocalTextLabel();
        this.overallTextLabel = ProgressBarsController.getInstance().getOverallTextLabel();

        this.localProgressBar = ProgressBarsController.getInstance().getLocalProgressBar();
        this.overallProgressBar = ProgressBarsController.getInstance().getOverallProgressBar();

        this.localProgressBar.getScene().getWindow().setOnCloseRequest(
                event->{
                    if(ProgressBarsController.getInstance().isProcessing()){
                        this.miniProgressHBox.setVisible(true);
                    }
                });
        this.origMiniProgLabelFont = miniProgressLabel.getFont();

        this.cameraViewListController.init(injectedInternalModels.getCameraViewListModel());

        this.internalModels = injectedInternalModels;
        this.userDocumentationHandler = injectedUserDocumentationHandler;

        // Keep track of whether cancellation was requested.
        AtomicBoolean cancelRequested = new AtomicBoolean(false);

        cancelButton.setOnAction(event -> cancelRequested.set(true));
//send accelerators to welcome window
        List<Menu> menus = mainMenubar.getMenus();

        for (Menu menu : menus){
            List<MenuItem> menuItems = menu.getItems();
            for (MenuItem item : menuItems){
                KeyCombination keyCodeCombo =  item.getAccelerator();
                EventHandler<ActionEvent> action = item.getOnAction();


                if (keyCodeCombo == null || action == null){continue;}

                WelcomeWindowController.getInstance().addAccelerator(keyCodeCombo, () -> {
                    Platform.runLater(() -> action.handle(new ActionEvent()));
                });
            }
        }
        MultithreadModels.getInstance().getIOModel().addProgressMonitor(new ProgressMonitor()
        {
            private double maximum = 0.0;
            private double localProgress = 0.0;

            private double overallProgress = 0.0;
            private IntegerProperty stageCountProperty = new SimpleIntegerProperty(0);
            private IntegerProperty currentStageProperty = new SimpleIntegerProperty(0);

            @Override
            public void allowUserCancellation() throws UserCancellationException
            {
                if (cancelRequested.get())
                {
                    cancelRequested.set(false); // reset cancel flag
                    WelcomeWindowController.getInstance().showIfNoModelLoaded();
                    throw new UserCancellationException("Cancellation requested by user.");
                }
            }

            @Override
            public void cancelComplete(UserCancellationException e)
            {
                complete();
            }

            @Override
            public void start()
            {
                cancelRequested.set(false);

                localProgress = 0.0;
                overallProgress = 0.0;
                Platform.runLater(() ->
                {
                    localProgressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : 0.0);
                    overallProgressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : 0.0);
                });

                ProgressBarsController.getInstance().resetText();
                ProgressBarsController.getInstance().showStage();
                ProgressBarsController.getInstance().startStopwatches();

                miniProgressBar.progressProperty().bind(overallProgressBar.progressProperty());
//                miniProgressLabel.textProperty().bind(overallTextLabel.textProperty());

                miniProgressLabel.textProperty().bind(Bindings.createStringBinding(()-> {
                    StringProperty stringProperty = overallTextLabel.textProperty();

                    if (currentStageProperty.getValue() > stageCountProperty.getValue() ||
                        stageCountProperty.getValue() == 0){
                        return stringProperty.getValue();
                    }

                    return String.format("%s (Stage %s/%s)",
                            stringProperty.getValue(), currentStageProperty.getValue(), stageCountProperty.getValue());

                }, overallTextLabel.textProperty(), currentStageProperty, stageCountProperty));

                miniProgressHBox.setVisible(false);
                resetMiniProgressBar();
            }

            @Override
            public void setStageCount(int count)
            {
                stageCountProperty.setValue(count);
            }

            @Override
            public void setStage(int stage, String message)
            {
                this.maximum = 0.0;
                this.localProgress = 0.0;
                this.currentStageProperty.setValue(stage + 1); //index starting from 1

                Platform.runLater(() -> localProgressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS));

                //index current stage from 0 in this instance
                overallProgress = (double) (currentStageProperty.getValue() - 1) / stageCountProperty.getValue();
                Platform.runLater(()-> overallProgressBar.setProgress(overallProgress));

                log.info("[Stage {}/{}] {}", currentStageProperty.getValue(), stageCountProperty.getValue(), message);

                Platform.runLater(()-> overallTextLabel.setText(message));

                if(currentStageProperty.getValue() > stageCountProperty.getValue()){
                    Platform.runLater(()->localTextLabel.setText("Finishing up..."));
                }

                ProgressBarsController.getInstance().beginNewStage();
            }

            @Override
            public void setMaxProgress(double maxProgress)
            {
                this.maximum = maxProgress;
                Platform.runLater(() -> localProgressBar.setProgress(maxProgress == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : localProgress / maxProgress));
            }

            @Override
            public void setProgress(double progress, String message)
            {
                this.localProgress = progress / maximum;
                Platform.runLater(() -> localProgressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : localProgress));

                //index current stage from 0 in this instance
                double offset = (double) (currentStageProperty.getValue() - 1) / stageCountProperty.getValue();
                this.overallProgress = offset + (localProgress / stageCountProperty.getValue());
                Platform.runLater(() -> overallProgressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : overallProgress));

                log.info("[{}%] {}", new DecimalFormat("#.##").format(localProgress * 100), message);
                Platform.runLater(()-> localTextLabel.setText(
                        String.format("Stage %s/%s—%s", currentStageProperty.getValue(), stageCountProperty.getValue(), message)));

                ProgressBarsController.getInstance().clickStopwatches(progress, maximum, overallProgress);
            }

            @Override
            public void complete()
            {
                this.maximum = 0.0;
                //TODO: disable progress bars menu item if not processing?
                ProgressBarsController.getInstance().stopAndClose();
                setReadyToDismissMiniProgBar();
            }

            @Override
            public void fail(Throwable e)
            {
                complete();
            }
        });

        boolean foundExportClass = false;
        File exportClassDefinitionFile = new File("export-classes.txt");
        if (exportClassDefinitionFile.exists())
        {
            foundExportClass = loadExportClasses(injectedStage, exportClassDefinitionFile);
        }

        if (!foundExportClass)
        {
            exportMenu.setVisible(false);
        }

        initToggleGroups();
        bindCheckMenuItems();
        updateRelightingVisibility();

        RecentProjects.updateAllControlStructures();

        toggleableShaders.add(materialMetallicity);
        toggleableShaders.add(materialReflectivity);
        toggleableShaders.add(materialBasis);
        toggleableShaders.add(imgBasedWithTextures);

        setToggleableShaderDisable(true);

        //add tooltips to recent projects list modifiers
        Tooltip tip = new Tooltip("Remove references to items not found in file explorer. " +
                "Will not modify your file system.");
        Tooltip.install(removeSomeRefsCustMenuItem.getContent(), tip);

        tip = new Tooltip("Remove references to all recent projects. Will not modify your file system.");
        Tooltip.install(removeAllRefsCustMenuItem.getContent(), tip);
    }

    private void setReadyToDismissMiniProgBar() {
        //need to set all styling at the same time because JavaFX CSS is fussy
        miniProgressLabel.setStyle("-fx-text-fill: #202020; " +
                                    "-fx-background-color: #CECECE;");
        miniProgressBar.setVisible(false);
    }

    private boolean loadExportClasses(Stage injectedStage, File exportClassDefinitionFile) {
        boolean foundExportClass = false;
        try (Scanner scanner = new Scanner(exportClassDefinitionFile, StandardCharsets.UTF_8))
        {
            scanner.useLocale(Locale.US);

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
                                catch (IllegalAccessException | InvocationTargetException | RuntimeException e)
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
        catch (IOException e)
        {
            log.error("Failed to find export classes file:", e);
        }
        return foundExportClass;
    }

    public void file_exportGLTF()
    {
        try
        {
            IBRRequestUI requestUI = ExportRequestUI.create(window, MultithreadModels.getInstance());
            requestUI.bind(internalModels.getSettingsModel());
            requestUI.prompt(Rendering.getRequestQueue());
        }
        catch (IOException|RuntimeException e)
        {
            log.error("Error opening glTF export window", e);
        }
    }

    public FramebufferView getFramebufferView()
    {
        return framebufferView;
    }

    private void initToggleGroups()
    {
        renderGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) ->
        {
            if (newValue != null && newValue.getUserData() instanceof String)
            {
                MultithreadModels.getInstance().getIOModel()
                    .requestFragmentShader(new File("shaders", (String)newValue.getUserData()));
            }

//            if (newValue != null && newValue.getUserData() instanceof StandardRenderingMode)
//            {
//                internalModels.getSettingsModel().set("renderingMode", newValue.getUserData());
//            }
        });
    }

    private void bindCheckMenuItems()
    {
        //value binding
//        lightCalibrationCheckMenuItem.selectedProperty().bindBidirectional(
//            internalModels.getSettingsModel().getBooleanProperty("lightCalibrationMode"));
        is3DGridCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("is3DGridEnabled"));
        compassCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("compassEnabled"));
        relightingCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("relightingEnabled"));
        shadowsCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("shadowsEnabled"));
        shadowsCheckMenuItem.setSelected(true);//need to do this here because it doesn't work in the fxml after binding
        visibleLightsCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("visibleLightsEnabled"));
        visibleLightWidgetsCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("lightWidgetsEnabled"));
        sceneWindowCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("sceneWindowOpen"));
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

        mipmapCheckMenuItem.selectedProperty().bindBidirectional(internalModels.getLoadOptionsModel().mipmaps);

        imageCompressionCheckMenuItem.selectedProperty().bindBidirectional(
                internalModels.getLoadOptionsModel().compression);
    }

    //Menubar->File

    @FXML
    private void file_createProject()
    {
        ProjectIO.getInstance().createProject(window);
    }



    @FXML
    private void file_openProject()
    {
        ProjectIO.getInstance().openProjectWithPrompt(window);
    }

    @FXML
    private void file_saveProject()
    {
        ProjectIO.getInstance().saveProject(window);
    }

    @FXML
    private void file_saveProjectAs()
    {
        ProjectIO.getInstance().saveProjectAs(window);
    }

    @FXML
    private void file_closeProject()
    {
        ProjectIO.getInstance().closeProjectAfterConfirmation();
    }

    @FXML
    private void exportSpecularFit() {
        try {
            IBRRequestUI requestUI = SpecularFitRequestUI.create(this.window, MultithreadModels.getInstance());
            requestUI.bind(internalModels.getSettingsModel());
            requestUI.prompt(Rendering.getRequestQueue());

        } catch (Exception e) {
            handleException("An error occurred handling request", e);
        }
    }

    //TODO: REMOVE?
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
    private void exportRequestUI(){
        try{
            IBRRequestUI requestUI = ExportRequestUI.create(this.window, MultithreadModels.getInstance());
            requestUI.bind(internalModels.getSettingsModel());
            requestUI.prompt(Rendering.getRequestQueue());
        } catch (Exception e) {
            handleException("An error occurred with ExportRequest handler", e);
        }
    }

    @FXML
    private void file_exit()
    {
        WindowSynchronization.getInstance().quit();
    }//TODO: how to apply dark mode here?

    @FXML
    private void help_userManual()
    {
        userDocumentationHandler.run();
    }

    public void openAboutModal()
    {
        ProjectIO.getInstance().openAboutModal(window);

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
        stage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.initOwner(this.window);

        stage.setResizable(false);

        flag.set(true);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, param -> flag.set(false));

        stage.show();

        return fxmlLoader.getController();
    }

    @FXML
    private void shading_IBRSettings()
    {
        if (advPhotoViewWindowOpen.get())
        {
            return;
        }

        try
        {
            AdvPhotoViewController advPhotoViewController = makeWindow("Advanced Photo View", advPhotoViewWindowOpen, "fxml/menubar/systemsettings/PhotoProjectionSettings.fxml");
            advPhotoViewController.bind(internalModels.getSettingsModel());
        }
        catch(Exception e)
        {
            handleException("An error occurred opening IBR settings", e);
        }
    }

    //window helpers

    private Stage makeStage(String title, Flag flag, int width, int height, FXMLLoader fxmlLoader) throws IOException
    {
        Parent root = fxmlLoader.load();
        Stage stage = new Stage();
        stage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        stage.setTitle(title);

        if (width >= 0 && height >= 0)
        {
            stage.setScene(new Scene(root, width, height));
        }
        else
        {
            stage.setScene(new Scene(root));
        }

        stage.initOwner(this.window);

        flag.set(true);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, param -> flag.set(false));

        return stage;
    }

    private FXMLLoader getFXMLLoader(String urlString) throws FileNotFoundException
    {
        URL url = MenubarController.class.getClassLoader().getResource(urlString);
        if (url == null)
        {
            throw new FileNotFoundException(urlString);
        }
        return new FXMLLoader(url);
    }

    private Stage makeStage(String title, Flag flag, String urlString) throws IOException
    {
        FXMLLoader fxmlLoader = getFXMLLoader(urlString);
        return makeStage(title, flag, -1, -1, fxmlLoader);
    }

    private <ControllerType> ControllerType makeWindow(String title, Flag flag, int width, int height, String urlString, Consumer<Stage> stageCallback) throws IOException
    {
        FXMLLoader fxmlLoader = getFXMLLoader(urlString);
        Stage stage = makeStage(title, flag, width, height, fxmlLoader);

        stage.setResizable(false);

        if (stageCallback != null)
        {
            stageCallback.accept(stage);
        }

        stage.show();

        return fxmlLoader.getController();
    }

    public UnzipFileSelectionController unzip() {
        UnzipFileSelectionController unzipFileSelectionController = null;
        try {
            unzipFileSelectionController = makeWindow(".psx Unzipper", unzipperOpen, "fxml/menubar/UnzipFileSelection.fxml");
            unzipFileSelectionController.init();
        }
        catch(Exception e)
        {
            handleException("An error occurred opening file unzipper", e);
        }
        return unzipFileSelectionController;
    }

    public void objectOrientation()
    {
        if (!objectOrientationWindowOpen.get())
        {
            try
            {
                var stageCapture = new Object()
                {
                    Stage stage;
                };

                SettingsObjectSceneController objectOrientationController =
                    makeWindow("Object Orientation", objectOrientationWindowOpen, "fxml/scene/object/SettingsObjectScene.fxml",
                        stage -> stageCapture.stage = stage);

                ObjectPoseSetting boundObjectPose = internalModels.getObjectModel().getSelectedObjectPoseProperty().getValue();

                objectOrientationController.bind(boundObjectPose);

                stageCapture.stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,
                    e -> objectOrientationController.unbind(boundObjectPose));
            }
            catch(Exception e)
            {
                handleException("An error occurred opening color checker window", e);
            }
        }
    }

    private <ControllerType> ControllerType makeWindow(String title, Flag flag, String urlString, Consumer<Stage> stageCallback) throws IOException
    {
        return makeWindow(title, flag, -1, -1, urlString, stageCallback);
    }

    public void lightCalibration()
    {
        if (!lightCalibrationWindowOpen.get())
        {
            try
            {
                var stageCapture = new Object()
                {
                    Stage stage;
                };

                LightCalibrationController lightCalibrationController =
                    makeWindow("Light Calibration", lightCalibrationWindowOpen, "fxml/menubar/LightCalibration.fxml",
                        stage ->
                        {
                            stageCapture.stage = stage;
                            stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, e ->
                            {
                                MultithreadModels.getInstance().getIOModel().applyLightCalibration();
                                MultithreadModels.getInstance().getSettingsModel().set("lightCalibrationMode", false);
                            });
                        });

                // Must wait until the controllers is created to add this additional window close event handler.
                stageCapture.stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,
                    e -> lightCalibrationController.unbind(internalModels.getSettingsModel()));

                // Bind controller to settings model to synchronize with "currentLightCalibration".
                lightCalibrationController.bind(internalModels.getSettingsModel());

                if (MultithreadModels.getInstance().getIOModel().isInstanceLoaded())
                {
                    // Set the "currentLightCalibration" to the existing calibration values in the view set.
                    ViewSet loadedViewSet = MultithreadModels.getInstance().getIOModel().getLoadedViewSet();

                    internalModels.getSettingsModel().set("currentLightCalibration",
                        loadedViewSet.getLightPosition(loadedViewSet.getLightIndex(loadedViewSet.getPrimaryViewIndex())).getXY());
                }

                // Enables light calibration mode when the window is opened.
                internalModels.getSettingsModel().set("lightCalibrationMode", true);
            }
            catch (Exception e)
            {
                handleException("An error occurred opening light calibration window", e);
            }
        }
    }

    public void eyedropperColorChecker()
    {
        try
        {
            EyedropperController eyedropperController =
                    makeWindow("Tone Calibration", colorCheckerWindowOpen, "fxml/menubar/EyedropperColorChecker.fxml");

            eyedropperController.setProjectModel(internalModels.getProjectModel());
            eyedropperController.setIOModel(MultithreadModels.getInstance().getIOModel());
        }
        catch (IOException|RuntimeException e)
        {
            handleException("An error occurred opening color checker window", e);
        }
    }

    public void shading_SystemMemory()
    {
        if (systemMemoryWindowOpen.get())
        {
            return;
        }

        try
        {
            makeWindow("System Memory", systemMemoryWindowOpen, "fxml/menubar/systemsettings/SystemMemorySettings.fxml");
        }
        catch(Exception e)
        {
            handleException("An error occurred opening jvm settings window", e);
        }
    }

    public void help_console()
    {
        if (loggerWindowOpen.get())
        {
            return;
        }

        try
        {
            Stage stage = makeStage("Log", loggerWindowOpen, "fxml/menubar/Logger.fxml");
            stage.setResizable(true);
            stage.initStyle(StageStyle.DECORATED);
            stage.show();
        }
        catch (Exception e)
        {
            handleException("An error occurred opening console window", e);
        }
    }

    public void showProgressBars(){
        ProjectIO.getInstance().openProgressBars();
        miniProgressHBox.setVisible(false);
    }


    public void updateRelightingVisibility() {
        ArrayList<Object> controlItems = new ArrayList<>();
        controlItems.add(visibleLightWidgetsCheckMenuItem);
        controlItems.add(perLightIntensityMenu);
        controlItems.add(ambientLightSettingsMenu);
        updateCheckMenuItemVisibilities(relightingCheckMenuItem, controlItems);
    }

    //if the check menu item is disabled, also disable the control items (labels, text fields, etc.)
    // or Menu Items it is associated with
    private void updateCheckMenuItemVisibilities(CheckMenuItem checkMenuItem, Collection<Object> items){
        boolean isChecked = checkMenuItem.isSelected();

        for(Object item : items){
            if (item instanceof javafx.scene.control.Control){
                Control convertedControlItem = (Control) item;
                convertedControlItem.setDisable(!isChecked);
            }
            else if (item instanceof MenuItem){
                MenuItem convertedMenuItem = (MenuItem) item;
                convertedMenuItem.setDisable(!isChecked);
            }
        }
    }


    //used so the user can click on the About menu and immediately see the about modal
    //instead of clicking on a single menu item
    //NOT IN USE as of July 9, 2024
    public void hideAndShowAboutModal() {
        aboutMenu.hide();
        openAboutModal();
    }

    public void openSystemSettingsModal() {
        ProjectIO.getInstance().openSystemSettingsModal(internalModels, window);
    }

    public void launchViewerApp()
    {
        try
        {
            Kintsugi3DViewerLauncher.launchViewer();
        }
        catch (IllegalStateException e)
        {
            handleException("Kintsugi 3D Viewer was not found on this computer. Check that it is installed.", e);
        }
        catch (Exception e)
        {
            handleException("Failed to launch Kintsugi 3D Viewer", e);
        }
    }

    private void handleException(String message, Exception e)
    {
        log.error("{}:", message, e);
        Platform.runLater(() ->
        {
            ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            ButtonType showLog = new ButtonType("Show Log", ButtonBar.ButtonData.YES);
            Alert alert = new Alert(AlertType.ERROR, message + "\nSee the log for more info.", ok, showLog);
            ((Button) alert.getDialogPane().lookupButton(showLog)).setOnAction(event -> {
                help_console();
            });
            alert.show();
        });
    }

    public void file_removeInvalidReferences() {
        RecentProjects.removeInvalidReferences();
    }

    public void file_removeAllReferences() {
        RecentProjects.removeAllReferences();
    }

    public Menu getRecentProjectsMenu() {
        return recentProjectsMenu;
    }

    public Menu getCleanRecentProjectsMenu() {
        return cleanRecentProjectsMenu;
    }

    //come up with a clearer name for this
    //set the disable of shaders which only work after processing textures
    public void setToggleableShaderDisable(boolean b) {
        for (RadioMenuItem item : toggleableShaders){
            item.setDisable(b);
        }
    }

    public Window getWindow(){return window;} //useful for creating alerts in back-end classes

    public void showWelcomeWindow() {
        WelcomeWindowController.getInstance().show();
    }

    public void handleMiniProgressBar(MouseEvent mouseEvent) {
        if(ProgressBarsController.getInstance().isProcessing()){
            showProgressBars();
        }
        else{
            //dismiss and reset mini progress bar
            miniProgressHBox.setVisible(false);
            resetMiniProgressBar();
        }
    }

    private void resetMiniProgressBar() {
        miniProgressBar.setVisible(true);
        miniProgressLabel.setFont(origMiniProgLabelFont);
        miniProgressLabel.setStyle("-fx-background-color: none;");
    }
}
