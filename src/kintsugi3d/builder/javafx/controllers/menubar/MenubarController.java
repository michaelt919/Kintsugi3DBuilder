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

package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.app.WindowSynchronization;
import kintsugi3d.builder.core.*;
import kintsugi3d.builder.export.projectExporter.ExportRequestUI;
import kintsugi3d.builder.export.specular.SpecularFitRequestUI;
import kintsugi3d.builder.export.specular.SpecularFitSerializer;
import kintsugi3d.builder.fit.decomposition.MaterialBasis;
import kintsugi3d.builder.javafx.InternalModels;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.ProjectIO;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.LightCalibrationViewSelectController;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.PrimaryViewSelectController;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.CurrentProjectInputSource;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPage;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageScrollerController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ShareInfo;
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
import java.util.stream.Collectors;

import static kintsugi3d.builder.javafx.ProjectIO.handleException;

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
    private Button doneButton;

    private Label localTextLabel;
    private Label overallTextLabel;

    //minimized progress bar
    @FXML private AnchorPane miniProgressPane; //entire bottom bar
    @FXML private HBox miniProgBarBoundingHBox; //only label and progress bar
    @FXML private Label miniProgressLabel;

    @FXML private StackPane swapControlsStackPane; //contains either the progress bar or the dismiss button
    @FXML private ProgressBar miniProgressBar;
    @FXML private Button dismissButton;

    //toggle groups
    @FXML private ToggleGroup renderGroup;

    @FXML private Menu aboutMenu;

    @FXML private MenuBar mainMenubar;

    //menu items
    @FXML private CheckMenuItem is3DGridCheckMenuItem;
    @FXML public CheckMenuItem isCameraVisualCheckMenuItem;
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

    private final List<MenuItem> toggleableShaders = new ArrayList<>();

    @FXML private VBox cameraViewList;
    @FXML private CameraViewListController cameraViewListController;
    @FXML private FramebufferView framebufferView;

    @FXML private Label shaderName;

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
        this.doneButton = ProgressBarsController.getInstance().getDoneButton();
        this.localTextLabel = ProgressBarsController.getInstance().getLocalTextLabel();
        this.overallTextLabel = ProgressBarsController.getInstance().getOverallTextLabel();

        this.localProgressBar = ProgressBarsController.getInstance().getLocalProgressBar();
        this.overallProgressBar = ProgressBarsController.getInstance().getOverallProgressBar();

        this.localProgressBar.getScene().getWindow().setOnCloseRequest(
                event-> this.miniProgressPane.setVisible(true));
        this.cameraViewListController.init(injectedInternalModels.getCameraViewListModel());

        this.internalModels = injectedInternalModels;
        this.userDocumentationHandler = injectedUserDocumentationHandler;

        // Keep track of whether cancellation was requested.
        AtomicBoolean cancelRequested = new AtomicBoolean(false);

        cancelButton.setOnAction(event -> {
            cancelRequested.set(true);
            Platform.runLater(()->cancelButton.setText("Cancelling..."));
        });

        doneButton.setOnAction(event ->{
            hideAllProgress();
        });

        cancelButton.disableProperty().bind(ProgressBarsController.getInstance().getProcessingProperty().not());
        doneButton.disableProperty().bind(ProgressBarsController.getInstance().getProcessingProperty());

        //send menubar accelerators to welcome window
        for (Menu menu : mainMenubar.getMenus()){
            for (MenuItem item : menu.getItems()){
                KeyCombination keyCodeCombo =  item.getAccelerator();
                EventHandler<ActionEvent> action = item.getOnAction();

                if (keyCodeCombo == null || action == null){continue;}

                WelcomeWindowController.getInstance().addAccelerator(keyCodeCombo, () ->
                        Platform.runLater(() -> action.handle(new ActionEvent())));
            }
        }
        MultithreadModels.getInstance().getIOModel().addProgressMonitor(new ProgressMonitor()
        {
            private double maximum = 0.0;
            private double localProgress = 0.0;

            private double overallProgress = 0.0;
            private IntegerProperty stageCountProperty = new SimpleIntegerProperty(0);
            private IntegerProperty currentStageProperty = new SimpleIntegerProperty(0);

            private String revertText; //when process is finishing up, store last msg into here while displaying "Finishing up..."

            @Override
            public void allowUserCancellation() throws UserCancellationException
            {
                if (cancelRequested.get())
                {
                    cancelRequested.set(false); // reset cancel flag

                    WelcomeWindowController.getInstance().showIfNoModelLoaded();
                    dismissMiniProgressBar();

                    //need to end stopwatches here because they might need to be reused for another process
                    //   before cancelComplete() is called
                    ProgressBarsController.getInstance().endStopwatches();

                    throw new UserCancellationException("Cancellation requested by user.");
                }
            }

            @Override
            public void cancelComplete(UserCancellationException e)
            {
                complete();
                hideAllProgress();
            }

            @Override
            public void start()
            {
                cancelRequested.set(false);

                stageCountProperty.setValue(0);
                currentStageProperty.setValue(0);

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

                miniProgressPane.setVisible(false);
                resetMiniProgressBar();

                miniProgressBar.progressProperty().bind(overallProgressBar.progressProperty());

                miniProgressLabel.textProperty().bind(Bindings.createStringBinding(()-> {
                    String currProcessTxt = overallTextLabel.textProperty().getValue();

                    //Display "Finishing up..." or something similar
                    if (currentStageProperty.getValue() > stageCountProperty.getValue() &&
                    ProgressBarsController.getInstance().isProcessing()){
                        return localTextLabel.getText();
                    }

                    //Display "Loading..." or some end message (ex. "Finished loading images")
                    // or just remove redundant "Stage 1/1"
                    if (!ProgressBarsController.getInstance().isProcessing() ||
                        stageCountProperty.getValue() <= 1){
                        return currProcessTxt;
                    }

                    return String.format("%s (Stage %s/%s)",
                            currProcessTxt, currentStageProperty.getValue(), stageCountProperty.getValue());


                }, overallTextLabel.textProperty(), currentStageProperty, stageCountProperty,
                        localTextLabel.textProperty()));//pass localTextLabel text property so this binding updates more often
            }

            @Override
            public void setProcessName(String processName) {
                Stage progressStage = (Stage) overallProgressBar.getScene().getWindow();
                Platform.runLater(()->progressStage.setTitle(processName));
            }

            @Override
            public void setStageCount(int count)
            {
                Platform.runLater(()->stageCountProperty.setValue(count));
            }

            @Override
            public void setStage(int stage, String message)
            {
                this.localProgress = 0.0;
                int currentStage = stage + 1; //index from 1, copy so we can update currentStageProperty w/ Platform.runLater to avoid threading issue
                Platform.runLater(()-> this.currentStageProperty.setValue(currentStage));

                Platform.runLater(() -> localProgressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS));

                //index current stage from 0 in this instance
                overallProgress = (double) (currentStage - 1) / stageCountProperty.getValue();
                Platform.runLater(() -> overallProgressBar.setProgress(overallProgress));

                log.info("[Stage {}/{}] {}", currentStage, stageCountProperty.getValue(), message);

                Platform.runLater(()-> overallTextLabel.setText(message));

                if(currentStage > stageCountProperty.getValue()){
                    if (message.equals(ProgressMonitor.PREPARING_PROJECT)){
                        Platform.runLater(()->localTextLabel.setText(ProgressMonitor.ALMOST_READY));
                    }
                    else{
                        Platform.runLater(()->localTextLabel.setText(FINISHING_UP));
                    }
                }
                else{
                    ProgressBarsController.getInstance().beginNewStage();
                }
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

                //remove stage/stageCount from txt if it wouldn't make sense for it to be there (ex. Stage 0/0)
                //useful for simple exports like orbit animation
                boolean removeStageNums = stageCountProperty.getValue() <= 1 || currentStageProperty.getValue() == 0;
                revertText = removeStageNums ? message :
                        String.format("Stage %s/%s—%s", currentStageProperty.getValue(), stageCountProperty.getValue(), message);

                Platform.runLater(()-> localTextLabel.setText(revertText));

                ProgressBarsController.getInstance().clickStopwatches(progress, maximum);
            }

            @Override
            public void complete()
            {
                this.maximum = 0.0;
                ProgressBarsController.getInstance().endStopwatches();
                setReadyToDismissMiniProgBar();

                if(overallProgressBar.getProgress() == ProgressIndicator.INDETERMINATE_PROGRESS){
                    Platform.runLater(()->overallProgressBar.setProgress(1.0));
                }

                if(localProgressBar.getProgress() == ProgressIndicator.INDETERMINATE_PROGRESS){
                    Platform.runLater(()->localProgressBar.setProgress(1.0));
                }

                //only revert text for processes which are not lightweight
                if(localTextLabel.getText().equals(FINISHING_UP)){
                    Platform.runLater(()->localTextLabel.setText(revertText));
                }

                //todo: would be nice if this was bound to a hasHandler property
                shaderName.setVisible(MultithreadModels.getInstance().getIOModel().hasValidHandler());

                Platform.runLater(()->cancelButton.setText("Cancel"));
                updateShaderList();
            }

            @Override
            public void fail(Throwable e)
            {
                complete();
            }

            @Override
            public boolean isConflictingProcess(){
                if (!ProgressBarsController.getInstance().isProcessing()){
                    return false;
                }

                Platform.runLater(() ->
                {
                    ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                    //ButtonType stopProcess = new ButtonType("Start New Process", ButtonBar.ButtonData.YES);
                    Alert alert = new Alert(AlertType.NONE, "Cannot run multiple tasks at the same time.\n" +
                            "Either wait for the current task to complete or cancel it." /*+
                            "Press OK to finish the current process."*/, ok/*, stopProcess*/);
                    alert.setHeaderText("Conflicting Tasks");

//                    //continue current process, don't start a new one
//                    ((Button) alert.getDialogPane().lookupButton(ok)).setOnAction(event -> {
//                    });
//
//                    //cancel current process and start new one
//                    ((Button) alert.getDialogPane().lookupButton(stopProcess)).setOnAction(event -> {
//                        cancelRequested.set(true);
//                    });

                    alert.showAndWait();
                });

                return true;
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

        RecentProjects.updateAllControlStructures();

        toggleableShaders.add(materialMetallicity);
        toggleableShaders.add(materialReflectivity);
        toggleableShaders.add(materialBasis);
        toggleableShaders.add(imgBasedWithTextures);
        toggleableShaders.add(weightmapCombination);

        toggleableShaders.add(heatmapMenu);
        toggleableShaders.add(superimposeMenu);

        updateShaderList();

        shaderName.textProperty().bind(Bindings.createStringBinding(()->
                ((RadioMenuItem)renderGroup.getSelectedToggle()).getText(), renderGroup.selectedToggleProperty()));

        KeyCombination ctrlUp = new KeyCodeCombination(KeyCode.UP, KeyCombination.CONTROL_DOWN);
        instance.window.getScene().getAccelerators().put(ctrlUp, () -> {
            List<RadioMenuItem> availableShaders = getRadioMenuItems(shadingMenu).stream()
                    .filter(item -> !item.isDisable()).collect(Collectors.toList());
            int numAvailableShaders = availableShaders.size();

            RadioMenuItem curr = (RadioMenuItem) renderGroup.getSelectedToggle();
            int idx = availableShaders.indexOf(curr);

            //there's probably a better way to do this but whatever
            idx = (idx - 1);
            if (idx < 0){
                idx = numAvailableShaders - 1;
            }
            availableShaders.get(idx).setSelected(true);
        });

        KeyCombination ctrlDown = new KeyCodeCombination(KeyCode.DOWN, KeyCombination.CONTROL_DOWN);
        instance.window.getScene().getAccelerators().put(ctrlDown, () -> {
            List<RadioMenuItem> availableShaders = getRadioMenuItems(shadingMenu).stream()
                    .filter(item -> !item.isDisable()).collect(Collectors.toList());
            int numAvailableShaders = availableShaders.size();

            RadioMenuItem curr = (RadioMenuItem) renderGroup.getSelectedToggle();
            int idx = availableShaders.indexOf(curr);
            idx = (idx + 1) % numAvailableShaders;
            availableShaders.get(idx).setSelected(true);
        });


        setToggleableShaderDisable(true);

        //add tooltips to recent projects list modifiers
        Tooltip tip = new Tooltip("Remove references to items not found in file explorer. " +
                "Will not modify your file system.");
        Tooltip.install(removeSomeRefsCustMenuItem.getContent(), tip);

        tip = new Tooltip("Remove references to all recent projects. Will not modify your file system.");
        Tooltip.install(removeAllRefsCustMenuItem.getContent(), tip);
    }

    private List<RadioMenuItem> getRadioMenuItems(Menu menu){
       List<RadioMenuItem> list = new ArrayList<>();
       getRadioMenuItemsHelper(list, menu.getItems());
       return list;
    }

    private void getRadioMenuItemsHelper(List<RadioMenuItem> radioMenuItems, List<MenuItem> menuItems){
        for (MenuItem item : menuItems){
            if (item instanceof RadioMenuItem) {
                radioMenuItems.add((RadioMenuItem) item);
            }
            if (item instanceof Menu) {
                Menu menu = (Menu) item;
                getRadioMenuItemsHelper(radioMenuItems, menu.getItems());
            }
        }
    }

    private void hideAllProgress() {
        ProgressBarsController.getInstance().hideStage();
        dismissMiniProgressBar();
    }

    private void setReadyToDismissMiniProgBar() {
        setLighterMiniBar();
        miniProgressBar.setVisible(false);
        dismissButton.setVisible(true);

        if(!ProgressBarsController.getInstance().getStage().isShowing()){
            miniProgressPane.setVisible(true);
        }
    }

    // Populate menu based on a given input number
    public void updateShaderList() {
        heatmapMenu.getItems().clear();
        superimposeMenu.getItems().clear();
        paletteMaterialMenu.getItems().clear();

        int basisCount = 0;
        try
        {
            ViewSet viewSet = MultithreadModels.getInstance().getIOModel().getLoadedViewSet();
            MaterialBasis basis = SpecularFitSerializer.deserializeBasisFunctions(viewSet.getSupportingFilesFilePath());
            basisCount = basis.getMaterialCount();
        }
        catch (IOException | NullPointerException e)
        {
            log.error("Error attempting to load previous solution basis count:", e);
        }

        Map<String, Optional<Object>> comboDefines = new HashMap<>();
        comboDefines.put("WEIGHTMAP_INDEX", Optional.of(0));
        comboDefines.put("WEIGHTMAP_COUNT", Optional.of(basisCount));
        weightmapCombination.setUserData(new RenderingShaderUserData("rendermodes/weightmaps/weightmapCombination.frag", comboDefines));

        for (int i = 0; i < basisCount; ++i) {
            RadioMenuItem heatmap = new RadioMenuItem("Weight map " + i);
            RadioMenuItem b = new RadioMenuItem("Weight map " + i);
            RadioMenuItem paletteMaterial = new RadioMenuItem("Palette material " + i);

            Map<String, Optional<Object>> defines = new HashMap<>();
            defines.put("WEIGHTMAP_INDEX", Optional.of(i));

            heatmap.setToggleGroup(renderGroup);
            heatmap.setUserData(new RenderingShaderUserData("rendermodes/weightmaps/weightmapSingle.frag", defines));

            b.setToggleGroup(renderGroup);
            b.setUserData(new RenderingShaderUserData("rendermodes/weightmaps/weightmapOverlay.frag", defines));

            paletteMaterial.setToggleGroup(renderGroup);
            paletteMaterial.setUserData(new RenderingShaderUserData("rendermodes/basisMaterialSingle.frag", defines));

            heatmapMenu.getItems().add(i, heatmap);
            superimposeMenu.getItems().add(i, b);
            paletteMaterialMenu.getItems().add(i, paletteMaterial);
            // when attempting to redefine 'heatmap' and use for superimposeMenu, K3D would crash
        }
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
                handleException("Failed to parse shader data for rendering option.", new RuntimeException("shaderData is null!"));
                return;
            }

            MultithreadModels.getInstance().getIOModel()
                .requestFragmentShader(new File("shaders", shaderData.getShaderName()), shaderData.getShaderDefines());
        });
    }

    private void bindCheckMenuItems()
    {
        visibleLightWidgetsCheckMenuItem.disableProperty().bind(relightingCheckMenuItem.selectedProperty().not());

        //value binding
        is3DGridCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("is3DGridEnabled"));
        isCameraVisualCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("isCameraVisualEnabled"));
        compassCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("compassEnabled"));
        relightingCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("relightingEnabled"));
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
        multiSamplingCheckMenuItem.selectedProperty().bindBidirectional(
            internalModels.getSettingsModel().getBooleanProperty("multisamplingEnabled"));
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
    private void file_exit()
    {
        WindowSynchronization.getInstance().quit();
    }

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
                                setShaderNameVisibility(MultithreadModels.getInstance().getIOModel().hasValidHandler());
                            });
                        });

                // Must wait until the controllers is created to add this additional window close event handler.
                stageCapture.stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,
                    e -> lightCalibrationController.unbind(internalModels.getSettingsModel()));

                // Bind controller to settings model to synchronize with "currentLightCalibration".
                lightCalibrationController.bind(internalModels.getSettingsModel());

                if (MultithreadModels.getInstance().getIOModel().hasValidHandler())
                {
                    // Set the "currentLightCalibration" to the existing calibration values in the view set.
                    ViewSet loadedViewSet = MultithreadModels.getInstance().getIOModel().getLoadedViewSet();

                    internalModels.getSettingsModel().set("currentLightCalibration",
                        loadedViewSet.getLightPosition(loadedViewSet.getLightIndex(0)).getXY());
                }

                cameraViewListController.rebindSearchableListView();

                // Enables light calibration mode when the window is opened.
                internalModels.getSettingsModel().set("lightCalibrationMode", true);

                //shader name doesn't change like it should when opening light calibration, so hide it for now
                //TODO: figure out how to show the correct name instead of hiding the text?
                setShaderNameVisibility(false);
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
            ArrayList<FXMLPage> pages = new ArrayList<>();

            FXMLLoader eyedropLoader = new FXMLLoader(getClass().getResource("/fxml/menubar/EyedropperColorChecker.fxml"));
            eyedropLoader.load();
            FXMLPage eyedropPage = new FXMLPage("/fxml/menubar/EyedropperColorChecker.fxml", eyedropLoader);

            FXMLLoader viewLoader = new FXMLLoader(getClass().getResource("/fxml/menubar/createnewproject/PrimaryViewSelect.fxml"));

            // Override controller class
            viewLoader.setControllerFactory(c -> new LightCalibrationViewSelectController());

            viewLoader.load();

            CurrentProjectInputSource inputSource = getCurrentProjectInputSource();

            FXMLPage viewPage = new FXMLPage("/fxml/menubar/createnewproject/PrimaryViewSelect.fxml", viewLoader);
            pages.add(viewPage);

            FXMLLoader imageSelectorLoader = new FXMLLoader(getClass().getResource("/fxml/menubar/SelectToneCalibrationImage.fxml"));
            imageSelectorLoader.load();
            FXMLPage imageSelectorPage = new FXMLPage("/fxml/menubar/SelectToneCalibrationImage.fxml", imageSelectorLoader);
            pages.add(imageSelectorPage);
            viewPage.setNextPage(imageSelectorPage);

            pages.add(eyedropPage);
            imageSelectorPage.setNextPage(eyedropPage);

            FXMLPageScrollerController scrollerController = makeWindow("Tone Calibration", colorCheckerWindowOpen,
                "fxml/menubar/FXMLPageScroller.fxml");
            scrollerController.setPages(pages, "/fxml/menubar/createnewproject/PrimaryViewSelect.fxml");
            scrollerController.addInfo(ShareInfo.Info.INPUT_SOURCE, inputSource);
            scrollerController.init();
        }
        catch (IOException|RuntimeException e)
        {
            handleException("An error occurred opening color checker window", e);
        }
    }

    private static CurrentProjectInputSource getCurrentProjectInputSource()
    {
        CurrentProjectInputSource inputSource = new CurrentProjectInputSource()
        {
            // Override this method to set the initial selection to the primary view instead of orientation view
            @Override
            public void setOrientationViewDefaultSelections(PrimaryViewSelectController controller)
            {
                ViewSet currentViewSet = MultithreadModels.getInstance().getIOModel().getLoadedViewSet();

                if (currentViewSet == null)
                    return;

                // Set the initial selection to what is currently being used
                TreeItem<String> selectionItem = InputSource.NONE_ITEM;

                if (currentViewSet.getPrimaryViewIndex() >= 0)
                {
                    String viewName = currentViewSet.getImageFileName(currentViewSet.getPrimaryViewIndex());

                    for (int i = 0; i < searchableTreeView.getTreeView().getExpandedItemCount(); i++)
                    {
                        TreeItem<String> item = searchableTreeView.getTreeView().getTreeItem(i);
                        if (Objects.equals(item.getValue(), viewName))
                        {
                            selectionItem = item;
                            break;
                        }
                    }
                }

                searchableTreeView.getTreeView().getSelectionModel().select(selectionItem);
            }
        };

        inputSource.setIncludeNoneItem(false);
        return inputSource;
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
        miniProgressPane.setVisible(false);
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
    //TODO: bind these to some property instead of manually changing values
    public void setToggleableShaderDisable(boolean b) {
        toggleableShaders.forEach(menuItem -> menuItem.setDisable(b));
    }

    public Window getWindow(){return window;} //useful for creating alerts in back-end classes

    public void showWelcomeWindow() {
        WelcomeWindowController.getInstance().show();
    }

    public void handleMiniProgressBar(MouseEvent event) {
        double relX = event.getX() - swapControlsStackPane.getLayoutX();
        double relY = event.getY() - swapControlsStackPane.getLayoutY();

        if(!ProgressBarsController.getInstance().isProcessing() &&
            swapControlsStackPane.contains(relX, relY)){
            dismissMiniProgressBar();
        }
        else {
            showProgressBars();
        }
    }

    private void resetMiniProgressBar() {
        miniProgressBar.setVisible(true);
        dismissButton.setVisible(false);
        setDarkestMiniBar();
    }

    public void mouseEnterMiniBar(MouseEvent event){
        double relX;
        double relY;

        if(!event.getSource().equals(swapControlsStackPane)){
            relX = event.getX() - swapControlsStackPane.getLayoutX();
            relY = event.getY() - swapControlsStackPane.getLayoutY();
        }
        else{
            relX = event.getX();
            relY = event.getY();
        }

        setLightestMiniBar();

        //don't highlight individual elements if still processing
        if(ProgressBarsController.getInstance().isProcessing()){
            return;
        }

        if(!swapControlsStackPane.contains(relX, relY)){
            //highlight label if it's hovered over
            miniProgBarBoundingHBox.setStyle("-fx-background-color: #CECECE");
            swapControlsStackPane.setStyle("-fx-background-color: #ADADAD;");

        }
        else{
            //highlight dismiss button area if it's hovered over
            miniProgBarBoundingHBox.setStyle("-fx-background-color: #ADADAD;");
            swapControlsStackPane.setStyle("-fx-background-color: #CECECE;");
        }
    }

    public void mouseExitMiniBar() {
        if(ProgressBarsController.getInstance().isProcessing()){
            setDarkestMiniBar();
        }
        else{
            setLighterMiniBar();
        }
    }

    private void setLighterMiniBar() {
        miniProgBarBoundingHBox.setStyle("-fx-background-color: #ADADAD;");
        miniProgressLabel.setStyle("-fx-text-fill: #202020;");
        swapControlsStackPane.setStyle("fx-fill: #ADADAD");

        miniProgressBar.lookup(".track").setStyle("-fx-background-color: #383838");
    }
    private void setLightestMiniBar(){
        miniProgBarBoundingHBox.setStyle("-fx-background-color: #CECECE");
        miniProgressLabel.setStyle("-fx-text-fill: #202020;");
        swapControlsStackPane.setStyle("-fx-background-color: #CECECE;");

        miniProgressBar.lookup(".track").setStyle("-fx-background-color: #383838");
    }

    public void setDarkestMiniBar() {
        miniProgBarBoundingHBox.setStyle("-fx-background-color: none;");
        miniProgressLabel.setStyle("-fx-text-fill: #CECECE;");
        swapControlsStackPane.setStyle("fx-fill: none");

        miniProgressBar.lookup(".track").setStyle("-fx-background-color: #CECECE");
    }

    public void dismissMiniProgressBar() {
        Platform.runLater(()->miniProgressPane.setVisible(false));
        WelcomeWindowController.getInstance().showIfNoModelLoaded();
    }

    public void file_hotSwap(ActionEvent actionEvent)
    {
        ProjectIO.getInstance().hotSwap(window);
    }

    public void selectMaterialBasisShader(){
        Platform.runLater(()-> materialBasis.setSelected(true));
    }

    public void selectImageBasedShader(){
        Platform.runLater(()->imageBased.setSelected(true));
    }

    public void setShaderNameVisibility(boolean b) {
        shaderName.setVisible(b);
    }
}
