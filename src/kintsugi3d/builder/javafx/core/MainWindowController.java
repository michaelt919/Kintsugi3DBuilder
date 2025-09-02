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
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.SimpleStringProperty;
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
import kintsugi3d.builder.app.WindowSynchronization;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.fit.decomposition.MaterialBasis;
import kintsugi3d.builder.io.specular.SpecularFitSerializer;
import kintsugi3d.builder.javafx.controllers.sidebar.CameraViewListController;
import kintsugi3d.builder.javafx.controllers.sidebar.SideBarController;
import kintsugi3d.builder.javafx.experience.ExportRender;
import kintsugi3d.builder.javafx.internal.ObservableProjectModel;
import kintsugi3d.builder.util.Kintsugi3DViewerLauncher;
import kintsugi3d.gl.javafx.FramebufferView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MainWindowController
{
    private static final Logger LOG = LoggerFactory.getLogger(MainWindowController.class);

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

    @FXML private Menu exportMenu;
    @FXML private Menu recentProjectsMenu;
    @FXML private Menu cleanRecentProjectsMenu;
    @FXML private Menu shadingMenu;
    @FXML private Menu heatmapMenu;
    @FXML private Menu superimposeMenu;
    @FXML private Menu paletteMaterialMenu;
    @FXML private Menu paletteMaterialWeightedMenu;

    @FXML private CustomMenuItem removeAllRefsCustMenuItem;
    @FXML private CustomMenuItem removeSomeRefsCustMenuItem;

    //pull this in so we can explicitly set shader to image-based upon load
    //without this, user could select a shader, load a project,
        //and the loaded project's appearance might not match the selected shader
    @FXML private RadioMenuItem imageBased;

    //shaders which should only be enabled after processing textures
    @FXML private RadioMenuItem materialMetallicity;
    @FXML private RadioMenuItem materialReflectivity;
    @FXML private RadioMenuItem materialBasis;
    @FXML private RadioMenuItem imgBasedWithTextures;
    @FXML private RadioMenuItem weightmapCombination;

    private final List<Menu> shaderMenuFlyouts = new ArrayList<>(4);

    private final List<MenuItem> toggleableShaders = new ArrayList<>();

    @FXML private VBox cameraViewList;

    @FXML private CameraViewListController cameraViewListController;
    @FXML private FramebufferView framebufferView;
    @FXML private SideBarController leftBarController;

    @FXML private Label shaderName;

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

        updateShaderList();

        shaderName.textProperty().bind(Bindings.createStringBinding(() ->
            ((RadioMenuItem) renderGroup.getSelectedToggle()).getText(), renderGroup.selectedToggleProperty()));

        ObservableProjectModel projectModel = javaFXState.getProjectModel();

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
                    return " [Loading]";
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
        toggleableShaders.forEach(shader -> shader.disableProperty().bind(
            projectModel.getProjectLoadedProperty().not().or(projectModel.getProjectProcessedProperty().not())));

        // Refresh shaders after processing
        projectModel.getProjectProcessedProperty()
            .addListener(obs ->
            {
                MainWindowController.getInstance().updateShaderList();

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
        // even though the "processed" state technically hasn't changed.
        projectModel.setOnProcessingComplete(event ->
        {
            // Automatically select material basis shader after processing textures
            materialBasis.setSelected(true);
        });

        projectModel.getProjectLoadedProperty().addListener(
            obs -> cameraViewListController.rebindSearchableListView());

        KeyCombination ctrlUp = new KeyCodeCombination(KeyCode.UP, KeyCombination.CONTROL_DOWN);
        instance.window.getScene().getAccelerators().put(ctrlUp, () ->
        {
            List<RadioMenuItem> availableShaders = getRadioMenuItems(shadingMenu).stream()
                .filter(item -> !item.isDisable()).collect(Collectors.toList());
            int numAvailableShaders = availableShaders.size();

            RadioMenuItem curr = (RadioMenuItem) renderGroup.getSelectedToggle();
            int idx = availableShaders.indexOf(curr);

            //there's probably a better way to do this but whatever
            idx = (idx - 1);
            if (idx < 0)
            {
                idx = numAvailableShaders - 1;
            }
            availableShaders.get(idx).setSelected(true);
        });

        KeyCombination ctrlDown = new KeyCodeCombination(KeyCode.DOWN, KeyCombination.CONTROL_DOWN);
        instance.window.getScene().getAccelerators().put(ctrlDown, () ->
        {
            List<RadioMenuItem> availableShaders = getRadioMenuItems(shadingMenu).stream()
                .filter(item -> !item.isDisable()).collect(Collectors.toList());
            int numAvailableShaders = availableShaders.size();

            RadioMenuItem curr = (RadioMenuItem) renderGroup.getSelectedToggle();
            int idx = availableShaders.indexOf(curr);
            idx = (idx + 1) % numAvailableShaders;
            availableShaders.get(idx).setSelected(true);
        });

        // hide cards in light calibration mode (for now until we have a better UX solution)
        leftBarController.getRootNode().managedProperty().bind(cameraViewList.visibleProperty().not());
        leftBarController.getRootNode().visibleProperty().bind(cameraViewList.visibleProperty().not());

        //add tooltips to recent projects list modifiers
        Tooltip tip = new Tooltip("Remove references to items not found in file explorer. " +
            "Will not modify your file system.");
        Tooltip.install(removeSomeRefsCustMenuItem.getContent(), tip);

        tip = new Tooltip("Remove references to all recent projects. Will not modify your file system.");
        Tooltip.install(removeAllRefsCustMenuItem.getContent(), tip);
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
        List<RadioMenuItem> list = new ArrayList<>();
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
    private void updateShaderList()
    {
        for (Menu flyout : shaderMenuFlyouts)
        {
            flyout.getItems().clear();
        }

        int basisCount = 0;

        if (Global.state().getIOModel().hasValidHandler())
        {
            try
            {
                ViewSet viewSet = Global.state().getIOModel().getLoadedViewSet();
                MaterialBasis basis = SpecularFitSerializer.deserializeBasisFunctions(viewSet.getSupportingFilesFilePath());
                if (basis != null)
                {
                    basisCount = basis.getMaterialCount();
                }
            }
            catch (IOException | NullPointerException e)
            {
                LOG.error("Error attempting to load previous solution basis count:", e);
            }
        }

        Map<String, Optional<Object>> comboDefines = new HashMap<>(2);
        comboDefines.put("WEIGHTMAP_INDEX", Optional.of(0));
        comboDefines.put("WEIGHTMAP_COUNT", Optional.of(basisCount));
        weightmapCombination.setUserData(new RenderingShaderUserData("rendermodes/weightmaps/weightmapCombination.frag", comboDefines));

        for (int i = 0; i < basisCount; ++i)
        {
            Map<String, Optional<Object>> defines = new HashMap<>(1);
            defines.put("WEIGHTMAP_INDEX", Optional.of(i));

            for (Menu flyout : shaderMenuFlyouts)
            {
                RadioMenuItem item = new RadioMenuItem(String.format("Palette material %d", i));
                item.setToggleGroup(renderGroup);
                item.setUserData(new RenderingShaderUserData((String) flyout.getUserData(), defines));
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
            RenderingShaderUserData shaderData = null;
            if (newValue != null && newValue.getUserData() instanceof String)
            {
                shaderData = new RenderingShaderUserData((String) newValue.getUserData());
            }

            if (newValue != null && newValue.getUserData() instanceof RenderingShaderUserData)
            {
                shaderData = (RenderingShaderUserData) newValue.getUserData();
            }

            if (shaderData == null)
            {
                ExceptionHandling.error("Failed to parse shader data for rendering option.", new RuntimeException("shaderData is null!"));
                return;
            }

            Global.state().getIOModel()
                .requestFragmentShader(new File("shaders", shaderData.getShaderName()), shaderData.getShaderDefines());
        });
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

    public void createProject()
    {
        ProjectIO.getInstance().createProject(window);
    }

    public void openProject()
    {
        ProjectIO.getInstance().openProjectWithPrompt(window);
    }

    public void saveProject()
    {
        ProjectIO.getInstance().saveProject(window);
    }

    public void saveProjectAs()
    {
        ProjectIO.getInstance().saveProjectAs(window);
    }

    public void closeProject()
    {
        ProjectIO.getInstance().closeProjectAfterConfirmation();
    }

    public void exit()
    {
        WindowSynchronization.getInstance().quit();
    }

    public void userManual()
    {
        userDocumentationHandler.run();
    }

    public void openExperience(ActionEvent event)
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

    public void lightCalibration()
    {
        ExperienceManager.getInstance().getExperience("LightCalibration").tryOpen();
    }

    public void showProgressBars()
    {
        ProgressBarsController.getInstance().showStage();
        miniProgressPane.setVisible(false);
    }

    public void launchViewerApp()
    {
        try
        {
            Kintsugi3DViewerLauncher.launchViewer();
        }
        catch (IllegalStateException e)
        {
            ExceptionHandling.error("Kintsugi 3D Viewer was not found on this computer. Check that it is installed.", e);
        }
        catch (Exception e)
        {
            ExceptionHandling.error("Failed to launch Kintsugi 3D Viewer", e);
        }
    }

    public void removeInvalidReferences()
    {
        RecentProjects.removeInvalidReferences();
    }

    public void removeAllReferences()
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

    public void resetMiniProgressBar(DoubleExpression progressProperty, StringExpression labelProperty)
    {
        miniProgressPane.setVisible(false);
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

        if (!event.getSource().equals(swapControlsStackPane))
        {
            relX = event.getX() - swapControlsStackPane.getLayoutX();
            relY = event.getY() - swapControlsStackPane.getLayoutY();
        }
        else
        {
            relX = event.getX();
            relY = event.getY();
        }

        setLightestMiniBar();

        //don't highlight individual elements if still processing
        if (ProgressBarsController.getInstance().isProcessing())
        {
            return;
        }

        if (!swapControlsStackPane.contains(relX, relY))
        {
            //highlight label if it's hovered over
            miniProgBarBoundingHBox.setStyle("-fx-background-color: #CECECE");
            swapControlsStackPane.setStyle("-fx-background-color: #ADADAD;");

        }
        else
        {
            //highlight dismiss button area if it's hovered over
            miniProgBarBoundingHBox.setStyle("-fx-background-color: #ADADAD;");
            swapControlsStackPane.setStyle("-fx-background-color: #CECECE;");
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
        this.miniProgressPane.setVisible(true);
    }

    public void setReadyToDismissMiniProgBar()
    {
        setLighterMiniBar();
        miniProgressBar.setVisible(false);
        dismissButton.setVisible(true);
    }

    public void dismissMiniProgressBarAsync()
    {
        Platform.runLater(() -> miniProgressPane.setVisible(false));
    }

    public void hotSwap()
    {
        ProjectIO.getInstance().hotSwap(window);
    }
}
