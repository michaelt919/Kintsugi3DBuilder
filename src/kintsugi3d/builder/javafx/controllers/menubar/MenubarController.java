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

package kintsugi3d.builder.javafx.controllers.menubar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import javafx.util.StringConverter;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.app.WindowSynchronization;
import kintsugi3d.builder.core.IBRRequestUI;
import kintsugi3d.builder.core.Kintsugi3DBuilderState;
import kintsugi3d.builder.core.LoadingMonitor;
import kintsugi3d.builder.export.specular.SpecularFitRequestUI;
import kintsugi3d.builder.javafx.InternalModels;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.ProjectIO;
import kintsugi3d.builder.javafx.controllers.menubar.systemsettings.AdvPhotoViewController;
import kintsugi3d.builder.javafx.controllers.menubar.systemsettings.SystemSettingsController;
import kintsugi3d.builder.util.Kintsugi3DViewerLauncher;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.javafx.FramebufferView;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.util.Flag;
import kintsugi3d.util.RecentProjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenubarController
{
    String defaultAutosavePath = "C:\\";//TODO: WILL CHANGE WHEN FILE STRUCTURE IS CEMENTED

    String defaultAutosaveSelection = "Default Path: --> " + defaultAutosavePath;
    static final String CHOOSE_LOCATION = "Choose Location...";
    private DirectoryChooser directoryChooser = new DirectoryChooser();

    private static final Logger log = LoggerFactory.getLogger(MenubarController.class);

    private static MenubarController instance;

    private InternalModels internalModels;

    //Window open flags
    private final Flag advPhotoViewWindowOpen = new Flag(false);
    private final Flag systemMemoryWindowOpen = new Flag(false);
    private final Flag loadOptionsWindowOpen = new Flag(false);
    private final Flag colorCheckerWindowOpen = new Flag(false);
    private final Flag unzipperOpen = new Flag(false);
    private final Flag consoleWindowOpen = new Flag(false);
    private Flag systemSettingsModalOpen = new Flag(false);

    private Flag aboutWindowOpen = new Flag(false);


    @FXML private ProgressBar progressBar;

    //toggle groups
    @FXML private ToggleGroup renderGroup;

    public Menu aboutMenu;
    public Button settingsButton;

    //menu items
    //TODO: ORGANIZE CHECK MENU ITEMS
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

    @FXML private CheckMenuItem autoCacheClearingCheckMenuItem;
    @FXML private CheckMenuItem autoSaveCheckMenuItem;
//    @FXML private CheckMenuItem preloadVisibilityEtcCheckMenuItem;
    @FXML private CheckMenuItem mipmapCheckMenuItem;
    @FXML private CheckMenuItem reduceViewportResolutionCheckMenuItem;
    @FXML private CheckMenuItem darkModeCheckMenuItem;
    @FXML private CheckMenuItem standAlone3dViewerCheckMenuItem;
//    @FXML public ChoiceBox<String> autosaveOptionsChoiceBox;

    @FXML private CheckMenuItem imageCompressionCheckMenuItem;

//    @FXML private Label widthLabel;
//    @FXML private TextField widthTxtField;
//    @FXML private Label heightLabel;
//    @FXML private TextField heightTxtField;

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

    @FXML private VBox cameraViewList;
    @FXML private CameraViewListController cameraViewListController;
    @FXML private FramebufferView framebufferView;

    private Window window;

    private Runnable userDocumentationHandler;

    final static Number DEFAULT_VALUE = 1024;
//    private IntegerProperty widthIntProperty = new SimpleIntegerProperty((Integer) DEFAULT_VALUE);
//    private IntegerProperty heightIntProperty = new SimpleIntegerProperty((Integer) DEFAULT_VALUE);

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

        // remove progress bar from layout when invisible
        this.progressBar.managedProperty().bind(this.progressBar.visibleProperty());

        this.cameraViewListController.init(injectedInternalModels.getCameraViewListModel());

        this.internalModels = injectedInternalModels;
        this.userDocumentationHandler = injectedUserDocumentationHandler;

        MultithreadModels.getInstance().getLoadingModel().addLoadingMonitor(new LoadingMonitor()
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
            public void loadingFailed(Throwable e)
            {
                loadingComplete();
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
        bindSlidersToTxtFields();
        updateRelightingVisibility();
//        updatePreloadVisibilityEtc();

//        setupTxtFieldProperties(widthIntProperty, widthTxtField);
//        setupTxtFieldProperties(heightIntProperty, heightTxtField);
//
//        widthTxtField.disableProperty().bind(preloadVisibilityEtcCheckMenuItem.selectedProperty().not());
//        heightTxtField.disableProperty().bind(preloadVisibilityEtcCheckMenuItem.selectedProperty().not());


        lightCalibrationCheckMenuItem.selectedProperty().addListener(observable ->
        {
            if (!lightCalibrationCheckMenuItem.isSelected())
            {
                MultithreadModels.getInstance().getLoadingModel().applyLightCalibration();
                MultithreadModels.getInstance().getSettingsModel().set("currentLightCalibration", Vector2.ZERO);
            }
        });

        updateRecentProjectsMenu();

//        //add "Default Path" and "Choose Location..." items to choiceBox
//        //initialize directory selection dropdown menu
//        autosaveOptionsChoiceBox.getItems().addAll(defaultAutosaveSelection, CHOOSE_LOCATION);
//
//        //initialize option to default path
//        autosaveOptionsChoiceBox.setValue(defaultAutosaveSelection);
//
//        //attach event handler (this cannot be done in scenebuilder)
//        autosaveOptionsChoiceBox.setOnAction(this::handleDirectoryDropdownSelection);
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
                MultithreadModels.getInstance().getLoadingModel()
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
        shadowsCheckMenuItem.setSelected(true);//need to do this here because it doesn't work in the fxml after binding
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

        mipmapCheckMenuItem.selectedProperty().bindBidirectional(internalModels.getLoadOptionsModel().mipmaps);
//        preloadVisibilityEtcCheckMenuItem.selectedProperty().bindBidirectional(
//                internalModels.getLoadOptionsModel().depthImages);

//        widthIntProperty.bindBidirectional(internalModels.getLoadOptionsModel().depthWidth);
//        heightIntProperty.bindBidirectional(internalModels.getLoadOptionsModel().depthHeight);

        imageCompressionCheckMenuItem.selectedProperty().bindBidirectional(
                internalModels.getLoadOptionsModel().compression);
    }

    private void bindSlidersToTxtFields()
    {
        //TODO: BIND INTENSITY SLIDERS TO TEXT FIELDS HERE
        //ignore now that this setting is in the system settings modal?
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
    private void exportSpecularFit(){
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
    private void file_exit()
    {
        WindowSynchronization.getInstance().quit();
    }//TODO: how to apply dark mode here?

    @FXML
    private void help_userManual()
    {
        userDocumentationHandler.run();
    }

    public void help_about()
    {
        try
        {

            AboutController aboutController = makeWindow(
                    "About Kintsugi 3D Builder", aboutWindowOpen, "fxml/menubar/About.fxml");
            aboutController.init();

        }
        catch (Exception e)
        {
            handleException("An error occurred showing help and about", e);
        }
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
            AdvPhotoViewController advPhotoViewController = makeWindow("Advanced Photo View", advPhotoViewWindowOpen, "fxml/menubar/systemsettings/AdvancedPhotoView.fxml");
            advPhotoViewController.bind(internalModels.getSettingsModel());
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
        stage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        stage.setTitle(title);
        stage.setScene(new Scene(root, width, height));
        stage.initOwner(this.window);
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
        stage.getIcons().add(new Image(new File("Kintsugi3D-icon.png").toURI().toURL().toString()));
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.initOwner(this.window);

        flag.set(true);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, param -> flag.set(false));

        return stage;
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
            EyedropperController eyedropperController =
                    makeWindow("Color Checker", colorCheckerWindowOpen, "fxml/menubar/EyedropperColorChecker.fxml");
            eyedropperController.setLoadingModel(MultithreadModels.getInstance().getLoadingModel());

        }
        catch(Exception e)
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
            makeWindow("System Memory", systemMemoryWindowOpen, "fxml/menubar/systemsettings/SystemMemory.fxml");
        }
        catch(Exception e)
        {
            handleException("An error occurred opening jvm settings window", e);
        }
    }

    public void help_console()
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

    public void updateRecentProjectsMenu() {
        RecentProjects.updateRecentProjectsControl(recentProjectsMenu);
    }

    public static void handleMenuItemSelection(MenuItem item) {
        String projectName = item.getText();
        ProjectIO.getInstance().openProjectFromFile(new File(projectName));
    }

//    private void handleDirectoryDropdownSelection(ActionEvent actionEvent) {
//        //if user clicks "choose directory" option, open the directory chooser
//        //then add an item to the dropdown which contains the path they selected
//
//        if (autosaveOptionsChoiceBox.getValue().equals(CHOOSE_LOCATION)){
//            this.directoryChooser.setTitle("Choose an output directory");
//
//            Stage stage = (Stage) window;
//            File file = this.directoryChooser.showDialog(stage.getOwner());
//
//            if (file != null && file.exists()){
//                directoryChooser.setInitialDirectory(file);
//                autosaveOptionsChoiceBox.getItems().add(file.getAbsolutePath());
//                autosaveOptionsChoiceBox.setValue(file.getAbsolutePath());
//            }
//            else{
//                Toolkit.getDefaultToolkit().beep();
//            }
//        }
//    }

//    public void updatePreloadVisibilityEtc() {
//        //if check menu item is unchecked, disable "Preload Visibility & Shadow Testing" options
//        ArrayList<Object> controlItems = new ArrayList<>();
//        controlItems.add(heightLabel);
//        controlItems.add(widthLabel);
//        controlItems.add(heightTxtField);
//        controlItems.add(widthTxtField);
//
//        updateCheckMenuItemVisibilities(preloadVisibilityEtcCheckMenuItem, controlItems);
//    }

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

    //this function is used to hook up the text field's string property to the backend
    //StringProperty --> IntegerProperty
    private void setupTxtFieldProperties(IntegerProperty integerProperty, TextField txtField) {StringConverter<Number> numberStringConverter = new StringConverter<Number>()
        {
            @Override
            public String toString(Number object)
            {
                if (object != null)
                {
                    return Integer.toString(object.intValue());
                }
                else
                {
                    return String.valueOf(DEFAULT_VALUE);
                }
            }

            @Override
            public Number fromString(String string)
            {
                try
                {
                    return Integer.valueOf(string);
                }
                catch (NumberFormatException nfe)
                {
                    return DEFAULT_VALUE;
                }
            }
        };
        txtField.textProperty().bindBidirectional(integerProperty, numberStringConverter);
        txtField.focusedProperty().addListener((ob, o, n) ->
        {
            if (o && !n)
            {
                txtField.setText(integerProperty.getValue().toString());
            }
        });
        integerProperty.addListener((ob, o, n) ->
        {
            if (n.intValue() < 1)
            {
                integerProperty.setValue(1);
            }
        });
    }

    //used so the user can click on the About menu and immediately see the about modal
    //instead of clicking on a single menu item
    public void hideAndShowAboutModal() {
        aboutMenu.hide();
        help_about();
    }

    public void openSystemSettingsModal() {
        if (systemSettingsModalOpen.get())
        {
            return;
        }

        try
        {
            SystemSettingsController systemSettingsController = makeWindow("System Settings", systemSettingsModalOpen, "fxml/menubar/systemsettings/SystemSettings.fxml");
            systemSettingsController.init(internalModels, window);
        }
        catch (IOException e)
        {
            log.error("An error occurred opening the settings modal:", e);
        }
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
}
