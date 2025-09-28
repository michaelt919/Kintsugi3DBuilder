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

package kintsugi3d.builder.javafx.core;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import kintsugi3d.builder.app.OperatingSystem;
import kintsugi3d.builder.app.WindowSynchronization;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.fit.decomposition.VisualizationShaders;
import kintsugi3d.builder.javafx.controllers.sidebar.CameraViewListController;
import kintsugi3d.builder.javafx.controllers.sidebar.SideBarController;
import kintsugi3d.builder.javafx.experience.ExportRender;
import kintsugi3d.builder.javafx.internal.ObservableCardsModel;
import kintsugi3d.builder.javafx.internal.ObservableProjectModel;
import kintsugi3d.builder.javafx.internal.ObservableUserShaderModel;
import kintsugi3d.builder.state.scene.UserShader;
import kintsugi3d.builder.util.Kintsugi3DViewerLauncher;
import kintsugi3d.gl.javafx.FramebufferView;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MainWindowController
{
    private static MainWindowController instance;
    private JavaFXState javaFXState;

    //minimized progress bar
    @FXML private AnchorPane miniProgressPane; //entire bottom bar
    @FXML private HBox miniProgBarBoundingHBox; //only label and progress bar
    @FXML private Label miniProgressLabel;

    @FXML private StackPane swapControlsStackPane; //contains either the progress bar or the dismiss button
    @FXML private ProgressBar miniProgressBar;
    @FXML private Button dismissButton;

    //toggle groups
    @FXML private ToggleGroup renderGroup;

    @FXML private MenuBar mainMenubar;

    //menu items
    @FXML private CheckMenuItem is3DGridCheckMenuItem;
    @FXML private CheckMenuItem isCameraVisualCheckMenuItem;
    @FXML private CheckMenuItem compassCheckMenuItem;
    @FXML private CheckMenuItem multiSamplingCheckMenuItem;
    @FXML private CheckMenuItem relightingCheckMenuItem;
    @FXML private CheckMenuItem sceneWindowCheckMenuItem;
    @FXML private CheckMenuItem environmentMappingCheckMenuItem; //TODO imp. this
    @FXML private CheckMenuItem visibleLightsCheckMenuItem;
    @FXML private CheckMenuItem visibleLightWidgetsCheckMenuItem;
    @FXML private CheckMenuItem visibleCameraPoseCheckMenuItem;
    @FXML private CheckMenuItem visibleSavedCameraPoseCheckMenuItem;

    @FXML private Menu workflowMenu;

    @FXML private Menu exportMenu;
    @FXML private Menu recentProjectsMenu;
    @FXML private Menu cleanRecentProjectsMenu;
    @FXML private Menu shadingMenu;
    @FXML private Menu heatmapMenu;
    @FXML private Menu superimposeMenu;
    @FXML private Menu paletteMaterialMenu;
    @FXML private Menu paletteMaterialWeightedMenu;

    @FXML private MenuItem removeAllRefsCustMenuItem;
    @FXML private MenuItem removeSomeRefsCustMenuItem;

    //pull this in so we can explicitly set shader to image-based upon load
    //without this, user could select a shader, load a project,
        //and the loaded project's appearance might not match the selected shader
    @FXML private RadioMenuItem imageBased;

    // Menu items which should only be enabled when a project is loaded.
    @FXML private MenuItem saveMenuItem;
    @FXML private MenuItem saveAsMenuItem;
    @FXML private MenuItem closeProjectMenuItem;

    // Menu items which should only be enabled after processing textures
    @FXML private MenuItem reoptimizeTexturesMenuItem;
    @FXML private MenuItem workflowExportGLTFModel;
    @FXML private MenuItem fileExportGLTFModel;

    //shaders which should only be enabled after processing textures
    @FXML private RadioMenuItem materialMetallicity;
    @FXML private RadioMenuItem materialReflectivity;
    @FXML private RadioMenuItem materialBasis;
    @FXML private RadioMenuItem imgBasedWithTextures;
    @FXML private RadioMenuItem weightmapCombination;
    @FXML private RadioMenuItem roughnessTexture;
    @FXML private RadioMenuItem metallicityTexture;
    @FXML private RadioMenuItem diffuseTexture;
    @FXML private RadioMenuItem specularTexture;
    @FXML private RadioMenuItem errorTexture;

    private final Collection<Menu> shaderMenuFlyouts = new ArrayList<>(4);

    private final Collection<MenuItem> toggleableShaders = new ArrayList<>(8);

    @FXML private VBox cameraViewList;

    @FXML private CameraViewListController cameraViewListController;
    @FXML private FramebufferView framebufferView;
    @FXML private SideBarController leftBarController;

    @FXML private Label shaderName;

    private ObservableProjectModel projectModel;

    private Window window;
    private Runnable userDocumentationHandler;

    public MainWindowController()
    {
        instance = this;
    }

    static MainWindowController getInstance()
    {
        return instance;
    }

    public void init(Stage injectedStage, JavaFXState javaFXState, Runnable injectedUserDocumentationHandler)
    {
        this.window = injectedStage;
        this.framebufferView.registerKeyAndWindowEventsFromStage(injectedStage);

        ExperienceManager.getInstance().initialize(this.getWindow(), javaFXState);

        // remove camera view list from layout when invisible
        this.cameraViewList.managedProperty().bind(this.cameraViewList.visibleProperty());

        // only show camera view list when light calibration mode is active
        // TODO make this a separate property to allow it to be shown in other contexts
        this.cameraViewList.visibleProperty().bind(javaFXState.getSettingsModel().getBooleanProperty("lightCalibrationMode"));
        this.cameraViewListController.init(javaFXState.getCameraViewListModel());

        this.javaFXState = javaFXState;
        this.userDocumentationHandler = injectedUserDocumentationHandler;

        this.leftBarController.init(javaFXState.getTabModels());


        initExportRenderMenu();
        initToggleGroups();
        bindCheckMenuItems();

        RecentProjects.updateAllControlStructures();

        // Shader menu flyouts
        shaderMenuFlyouts.add(heatmapMenu);
        shaderMenuFlyouts.add(superimposeMenu);
        shaderMenuFlyouts.add(paletteMaterialMenu);
        shaderMenuFlyouts.add(paletteMaterialWeightedMenu);

        // Shader menu
        toggleableShaders.add(materialMetallicity);
        toggleableShaders.add(materialReflectivity);
        toggleableShaders.add(materialBasis);
        toggleableShaders.add(imgBasedWithTextures);
        toggleableShaders.add(weightmapCombination);
        toggleableShaders.addAll(shaderMenuFlyouts);
        toggleableShaders.add(roughnessTexture);
        toggleableShaders.add(metallicityTexture);
        toggleableShaders.add(diffuseTexture);
        toggleableShaders.add(specularTexture);
        toggleableShaders.add(errorTexture);

        updateShaderList(0);

        ObservableUserShaderModel userShaderModel = javaFXState.getUserShaderModel();
        shaderName.textProperty().bind(Bindings.createStringBinding(() ->
            userShaderModel.getUserShader().getFriendlyName(), userShaderModel.getUserShaderProperty()));

        userShaderModel.getUserShaderProperty().addListener((obs, oldValue, newValue) ->
            renderGroup.selectToggle(renderGroup.getToggles().stream()
                .filter(toggle -> Objects.equals(newValue, getUserShaderFromToggle(toggle)))
                .findFirst()
                .orElse(null)));

        projectModel = javaFXState.getProjectModel();

        projectModel.getProjectOpenProperty().addListener(obs ->
        {
            if (!projectModel.isProjectOpen())
            {
                dismissMiniProgressBarAsync();
            }
        });

        shaderName.visibleProperty().bind(projectModel.getProjectLoadedProperty()
            .and(BooleanExpression.booleanExpression(javaFXState.getSettingsModel().getBooleanProperty("lightCalibrationMode")).not()));

        var resolutionFormatted = Bindings.createStringBinding(
            () ->
            {
                if (projectModel.isProjectProcessed())
                {
                    int resolution = projectModel.getProcessedTextureResolution();
                    return String.format(" [Processed, %dx%d]", resolution, resolution);
                }
                else if (projectModel.isProjectLoaded())
                {
                    return " [Not Processed]";
                }
                else if (projectModel.isProjectOpen())
                {
                    return " [Loading...]";
                }
                else
                {
                    return "";
                }
            },
            projectModel.getProjectOpenProperty(),
            projectModel.getProjectLoadedProperty(),
            projectModel.getProjectProcessedProperty(),
            projectModel.getProcessedTextureResolutionProperty());

        injectedStage.titleProperty().bind(
            new SimpleStringProperty("Kintsugi 3D Builder : ")
                .concat(projectModel.getProjectNameProperty())
                .concat(StringExpression.stringExpression(resolutionFormatted)));

        // Enable shaders which only work after processing textures
        toggleableShaders.forEach(this::bindEnabledToProjectProcessed);

        // Disable workflow menu options when a project isn't loaded.
        workflowMenu.getItems().forEach(this::bindEnabledToProjectLoaded);
        exportMenu.getItems().forEach(this::bindEnabledToProjectLoaded);

        // Disable save / close if a project isn't loaded
        bindEnabledToProjectLoaded(saveMenuItem);
        bindEnabledToProjectLoaded(saveAsMenuItem);
        bindEnabledToProjectLoaded(closeProjectMenuItem);

        // Reoptimization and export are only options once the project has been processed once before
        bindEnabledToProjectProcessed(fileExportGLTFModel);
        bindEnabledToProjectProcessed(workflowExportGLTFModel);
        bindEnabledToProjectProcessed(reoptimizeTexturesMenuItem);

        // Refresh shaders after processing
        projectModel.getProjectProcessedProperty()
            .addListener(obs ->
            {
                if (projectModel.isProjectProcessed())
                {
                    // Automatically select material basis shader after processing textures
                    materialBasis.setSelected(true);
                }
                else
                {
                    // Automatically select IBR shader if not processed.
                    imageBased.setSelected(true);
                }
            });

        // For re-processing a second time, we still want to switch to the material basis shader (?)
        // and refresh the number of basis materials, even though the "processed" state technically hasn't changed.
        projectModel.setOnProcessingComplete(event ->
        {
            // Automatically select material basis shader after processing textures
            materialBasis.setSelected(true);
        });

        projectModel.getProjectLoadedProperty().addListener(
            obs -> cameraViewListController.rebindSearchableListView());

        // Update flyout menus if the available materials change (i.e. delete).
        javaFXState.getTabModels().getObservableTabsMap().addListener(
            (MapChangeListener<? super String, ? super ObservableCardsModel>) change ->
            {
                if (change.wasAdded() && "Materials".equals(change.getKey()))
                {
                    ObservableCardsModel materialCardsModel = change.getValueAdded();
                    materialCardsModel.getCardList().addListener((InvalidationListener)
                        obs -> updateShaderList(materialCardsModel.getCardList().size()));
                }
            });

        KeyCombination ctrlUp = new KeyCodeCombination(KeyCode.UP, KeyCombination.CONTROL_DOWN);
        instance.window.getScene().getAccelerators().put(ctrlUp, () ->
        {
            List<RadioMenuItem> availableShaders = getRadioMenuItems(shadingMenu).stream()
                .filter(item -> !item.isDisable()).collect(Collectors.toList());

            RadioMenuItem curr = (RadioMenuItem) renderGroup.getSelectedToggle();
            int index = availableShaders.indexOf(curr);

            if (index >= 0)
            {
                index -= 1;
                if (index < 0)
                {
                    int numAvailableShaders = availableShaders.size();
                    index = numAvailableShaders - 1;
                }
                availableShaders.get(index).setSelected(true);
            }
        });

        KeyCombination ctrlDown = new KeyCodeCombination(KeyCode.DOWN, KeyCombination.CONTROL_DOWN);
        instance.window.getScene().getAccelerators().put(ctrlDown, () ->
        {
            List<RadioMenuItem> availableShaders = getRadioMenuItems(shadingMenu).stream()
                .filter(item -> !item.isDisable()).collect(Collectors.toList());

            RadioMenuItem curr = (RadioMenuItem) renderGroup.getSelectedToggle();
            int index = availableShaders.indexOf(curr);

            if (index >= 0)
            {
                int numAvailableShaders = availableShaders.size();
                index = (index + 1) % numAvailableShaders;
                availableShaders.get(index).setSelected(true);
            }
        });

        // hide cards in light calibration mode (for now until we have a better UX solution)
        leftBarController.getRootNode().managedProperty().bind(cameraViewList.visibleProperty().not());
        leftBarController.getRootNode().visibleProperty().bind(cameraViewList.visibleProperty().not());

        if (OperatingSystem.getCurrentOS() != OperatingSystem.MACOS)
        {
            // Remove fallback menu items for MacOS and replace with custom menu items with tooltips.
            cleanRecentProjectsMenu.getItems().clear();

            removeSomeRefsCustMenuItem = new CustomMenuItem(new Label("Clear Missing Projects"));
            removeSomeRefsCustMenuItem.setOnAction(event -> RecentProjects.removeInvalidReferences());

            Tooltip.install(((CustomMenuItem) removeSomeRefsCustMenuItem).getContent(),
                new Tooltip("Remove references to items not found in file explorer. Will not modify your file system."));

            cleanRecentProjectsMenu.getItems().add(removeSomeRefsCustMenuItem);

            removeAllRefsCustMenuItem = new CustomMenuItem(new Label("Clear All Projects"));
            removeAllRefsCustMenuItem.setOnAction(event -> RecentProjects.removeInvalidReferences());

            Tooltip.install(((CustomMenuItem)removeAllRefsCustMenuItem).getContent(),
                new Tooltip("Remove references to all recent projects. Will not modify your file system."));

            cleanRecentProjectsMenu.getItems().add(removeAllRefsCustMenuItem);
        }
    }

    private void bindEnabledToProjectProcessed(MenuItem shader)
    {
        shader.disableProperty().bind(
            projectModel.getProjectLoadedProperty().not().or(projectModel.getProjectProcessedProperty().not()));
    }

    private void bindEnabledToProjectLoaded(MenuItem menuItem)
    {
        menuItem.disableProperty().bind(projectModel.getProjectLoadedProperty().not());
    }

    private void initExportRenderMenu()
    {
        List<ExportRender> exportRenderList = ExperienceManager.getInstance().getExportRenderManager().getList();

        if (exportRenderList.isEmpty())
        {
            exportMenu.setVisible(false);
        }
        else
        {
            for (ExportRender exportRender : exportRenderList)
            {
                MenuItem newItem = new MenuItem(exportRender.getShortName());
                newItem.setOnAction(event -> exportRender.tryOpen());
                exportMenu.getItems().add(newItem);
            }
        }
    }

    /**
     * Send menubar accelerators to another scene
     * @param scene
     */
    public void initAccelerators(Scene scene)
    {
        for (Menu menu : mainMenubar.getMenus())
        {
            for (MenuItem item : menu.getItems())
            {
                KeyCombination keyCodeCombo = item.getAccelerator();
                EventHandler<ActionEvent> action = item.getOnAction();

                if (keyCodeCombo != null && action != null)
                {
                    scene.getAccelerators().put(keyCodeCombo,
                        () -> Platform.runLater(() -> action.handle(new ActionEvent())));
                }
            }
        }
    }

    private List<RadioMenuItem> getRadioMenuItems(Menu menu)
    {
        List<RadioMenuItem> list = new ArrayList<>(menu.getItems().size());
        getRadioMenuItemsHelper(list, menu.getItems());
        return list;
    }

    private void getRadioMenuItemsHelper(List<RadioMenuItem> radioMenuItems, List<MenuItem> menuItems)
    {
        for (MenuItem item : menuItems)
        {
            if (item instanceof RadioMenuItem)
            {
                radioMenuItems.add((RadioMenuItem) item);
            }
            if (item instanceof Menu)
            {
                Menu menu = (Menu) item;
                getRadioMenuItemsHelper(radioMenuItems, menu.getItems());
            }
        }
    }

    // Populate menu based on a given input number
    private void updateShaderList(int basisCount)
    {
        for (Menu flyout : shaderMenuFlyouts)
        {
            flyout.getItems().clear();
        }

        Map<String, Optional<Object>> comboDefines = new HashMap<>(2);
        comboDefines.put("WEIGHTMAP_INDEX", Optional.of(0));
        comboDefines.put("WEIGHTMAP_COUNT", Optional.of(basisCount));
        weightmapCombination.setUserData(new UserShader(weightmapCombination.getText(),
            "rendermodes/weightmaps/weightmapCombination.frag", comboDefines));

        for (int i = 0; i < basisCount; ++i)
        {
            for (Menu flyout : shaderMenuFlyouts)
            {
                UserShader shader = VisualizationShaders.getForBasisMaterial((String) flyout.getUserData(), i);
                RadioMenuItem item = new RadioMenuItem(String.format(shader.getFriendlyName(), i));
                item.setToggleGroup(renderGroup);
                item.setUserData(shader);
                flyout.getItems().add(i, item);
            }
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
            if (newValue != null)
            {
                UserShader shader = getUserShaderFromToggle(newValue);

                if (shader != null)
                {
                    Global.state().getUserShaderModel().setUserShader(shader);
                }

//                if (shader == null)
//                {
//                    ExceptionHandling.error("Failed to parse shader data for rendering option.", new RuntimeException("shader is null!"));
//                    return;
//                }
            }
        });

        // Set default shader
        Global.state().getUserShaderModel().setUserShader(getUserShaderFromToggle(renderGroup.getSelectedToggle()));
    }

    private UserShader getUserShaderFromToggle(Toggle newValue)
    {
        if (newValue.getUserData() instanceof String)
        {
            return new UserShader(((MenuItem) renderGroup.getSelectedToggle()).getText(), (String) newValue.getUserData());
        }
        else if (newValue.getUserData() instanceof UserShader)
        {
            return (UserShader) newValue.getUserData();
        }
        else
        {
            return null;
        }
    }

    private void bindCheckMenuItems()
    {
        visibleLightWidgetsCheckMenuItem.disableProperty().bind(relightingCheckMenuItem.selectedProperty().not());

        //value binding
        is3DGridCheckMenuItem.selectedProperty().bindBidirectional(
            javaFXState.getSettingsModel().getBooleanProperty("is3DGridEnabled"));
        isCameraVisualCheckMenuItem.selectedProperty().bindBidirectional(
            javaFXState.getSettingsModel().getBooleanProperty("isCameraVisualEnabled"));
        compassCheckMenuItem.selectedProperty().bindBidirectional(
            javaFXState.getSettingsModel().getBooleanProperty("compassEnabled"));
        relightingCheckMenuItem.selectedProperty().bindBidirectional(
            javaFXState.getSettingsModel().getBooleanProperty("relightingEnabled"));
        visibleLightsCheckMenuItem.selectedProperty().bindBidirectional(
            javaFXState.getSettingsModel().getBooleanProperty("visibleLightsEnabled"));
        visibleLightWidgetsCheckMenuItem.selectedProperty().bindBidirectional(
            javaFXState.getSettingsModel().getBooleanProperty("lightWidgetsEnabled"));
        sceneWindowCheckMenuItem.selectedProperty().bindBidirectional(
            javaFXState.getSettingsModel().getBooleanProperty("sceneWindowOpen"));
        visibleCameraPoseCheckMenuItem.selectedProperty().bindBidirectional(
            javaFXState.getSettingsModel().getBooleanProperty("visibleCameraPosesEnabled"));
        visibleSavedCameraPoseCheckMenuItem.selectedProperty().bindBidirectional(
            javaFXState.getSettingsModel().getBooleanProperty("visibleSavedCameraPosesEnabled"));
        multiSamplingCheckMenuItem.selectedProperty().bindBidirectional(
            javaFXState.getSettingsModel().getBooleanProperty("multisamplingEnabled"));
    }

    @FXML public void createProject()
    {
        ProjectIO.getInstance().createProject(window);
    }

    @FXML public void openProject()
    {
        ProjectIO.getInstance().openProjectWithPrompt(window);
    }

    @FXML public void saveProject()
    {
        ProjectIO.getInstance().saveProject(window);
    }

    @FXML public void saveProjectAs()
    {
        ProjectIO.getInstance().saveProjectAs(window);
    }

    @FXML public void closeProject()
    {
        ProjectIO.getInstance().closeProjectAfterConfirmation();
    }

    @FXML public void exit()
    {
        WindowSynchronization.getInstance().quit();
    }

    @FXML public void userManual()
    {
        userDocumentationHandler.run();
    }

    @FXML public void openExperience(ActionEvent event)
    {
        if (event.getSource() instanceof MenuItem)
        {
            MenuItem source = (MenuItem) event.getSource();
            if (source.getUserData() instanceof String)
            {
                String experienceName = (String)source.getUserData();
                ExperienceManager.getInstance().getExperience(experienceName).tryOpen();
            }
        }
    }

    @FXML public void lightCalibration()
    {
        ExperienceManager.getInstance().getExperience("LightCalibration").tryOpen();
    }

    private void setMiniProgressPaneVisible(boolean value)
    {
        miniProgressPane.setVisible(value);
        miniProgressPane.setManaged(value);
    }

    @FXML public void showProgressBars()
    {
        ProgressBarsController.getInstance().showStage();
        setMiniProgressPaneVisible(false);
    }

    @FXML public void launchViewerApp()
    {
        try
        {
            Kintsugi3DViewerLauncher.launchViewer();
        }
        catch (IllegalStateException e)
        {
            ExceptionHandling.error("Kintsugi 3D Viewer was not found on this computer. Check that it is installed.", e);
        }
        catch (IOException|RuntimeException e)
        {
            ExceptionHandling.error("Failed to launch Kintsugi 3D Viewer", e);
        }
    }

    @FXML public void removeInvalidReferences()
    {
        RecentProjects.removeInvalidReferences();
    }

    @FXML public void removeAllReferences()
    {
        RecentProjects.removeAllReferences();
    }

    public Menu getRecentProjectsMenu()
    {
        return recentProjectsMenu;
    }

    public Menu getCleanRecentProjectsMenu()
    {
        return cleanRecentProjectsMenu;
    }

    public Window getWindow() // Useful for creating alerts in back-end classes
    {
        return window;
    }

    public void handleMiniProgressBar(MouseEvent event)
    {
        double relX = event.getX() - swapControlsStackPane.getLayoutX();
        double relY = event.getY() - swapControlsStackPane.getLayoutY();

        if (!ProgressBarsController.getInstance().isProcessing() &&
            swapControlsStackPane.contains(relX, relY))
        {
            dismissMiniProgressBarAsync();
        }
        else
        {
            showProgressBars();
        }
    }

    public void resetMiniProgressBar(ObservableValue<Number> progressProperty, ObservableValue<String> labelProperty)
    {
        setMiniProgressPaneVisible(false);
        miniProgressBar.setVisible(true);
        dismissButton.setVisible(false);
        setDarkestMiniBar();

        miniProgressBar.progressProperty().bind(progressProperty);
        miniProgressLabel.textProperty().bind(labelProperty);
    }

    public void mouseEnterMiniBar(MouseEvent event)
    {
        double relX;
        double relY;

        if (event.getSource().equals(swapControlsStackPane))
        {
            relX = event.getX();
            relY = event.getY();
        }
        else
        {
            relX = event.getX() - swapControlsStackPane.getLayoutX();
            relY = event.getY() - swapControlsStackPane.getLayoutY();
        }

        setLightestMiniBar();

        //don't highlight individual elements if still processing
        if (ProgressBarsController.getInstance().isProcessing())
        {
            return;
        }

        if (swapControlsStackPane.contains(relX, relY))
        {
            //highlight dismiss button area if it's hovered over
            miniProgBarBoundingHBox.setStyle("-fx-background-color: #ADADAD;");
            swapControlsStackPane.setStyle("-fx-background-color: #CECECE;");
        }
        else
        {
            //highlight label if it's hovered over
            miniProgBarBoundingHBox.setStyle("-fx-background-color: #CECECE");
            swapControlsStackPane.setStyle("-fx-background-color: #ADADAD;");

        }
    }

    public void mouseExitMiniBar()
    {
        if (ProgressBarsController.getInstance().isProcessing())
        {
            setDarkestMiniBar();
        }
        else
        {
            setLighterMiniBar();
        }
    }

    private void setLighterMiniBar()
    {
        miniProgBarBoundingHBox.setStyle("-fx-background-color: #ADADAD;");
        miniProgressLabel.setStyle("-fx-text-fill: #202020;");
        swapControlsStackPane.setStyle("fx-fill: #ADADAD");

        miniProgressBar.lookup(".track").setStyle("-fx-background-color: #383838");
    }

    private void setLightestMiniBar()
    {
        miniProgBarBoundingHBox.setStyle("-fx-background-color: #CECECE");
        miniProgressLabel.setStyle("-fx-text-fill: #202020;");
        swapControlsStackPane.setStyle("-fx-background-color: #CECECE;");

        miniProgressBar.lookup(".track").setStyle("-fx-background-color: #383838");
    }

    public void setDarkestMiniBar()
    {
        miniProgBarBoundingHBox.setStyle("-fx-background-color: none;");
        miniProgressLabel.setStyle("-fx-text-fill: #CECECE;");
        swapControlsStackPane.setStyle("fx-fill: none");

        miniProgressBar.lookup(".track").setStyle("-fx-background-color: #CECECE");
    }

    public void showMiniProgressBar()
    {
        setMiniProgressPaneVisible(true);
    }

    public void setReadyToDismissMiniProgBar()
    {
        setLighterMiniBar();
        miniProgressBar.setVisible(false);
        dismissButton.setVisible(true);
    }

    public void dismissMiniProgressBarAsync()
    {
        Platform.runLater(() -> setMiniProgressPaneVisible(false));
    }

    public void hotSwap()
    {
        ProjectIO.getInstance().hotSwap(window);
    }
}
