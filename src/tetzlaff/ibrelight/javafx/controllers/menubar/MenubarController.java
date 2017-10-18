package tetzlaff.ibrelight.javafx.controllers.menubar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
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
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.xml.sax.SAXException;
import tetzlaff.ibrelight.app.WindowSynchronization;
import tetzlaff.ibrelight.core.*;
import tetzlaff.ibrelight.javafx.InternalModels;
import tetzlaff.ibrelight.javafx.MultithreadModels;
import tetzlaff.ibrelight.javafx.controllers.scene.SceneModel;
import tetzlaff.util.Flag;

public class MenubarController
{
    private InternalModels internalModels;
    private SceneModel sceneModel;

    //Window open flags
    private final Flag ibrOptionsWindowOpen = new Flag(false);
    private final Flag loadOptionsWindowOpen = new Flag(false);
    private final Flag loaderWindowOpen = new Flag(false);

    @FXML private ProgressBar progressBar;

    //toggle groups
    @FXML private ToggleGroup renderGroup;

    //menu items
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

    public void init(Window injectedParentWindow, IBRRequestQueue<?> requestQueue, InternalModels injectedInternalModels, SceneModel injectedSceneModel)
    {
        this.parentWindow = injectedParentWindow;

        this.internalModels = injectedInternalModels;
        this.sceneModel = injectedSceneModel;

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
                                        requestUI.prompt(requestQueue::addRequest);
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
    }

    private void initToggleGroups()
    {
        renderGroup.selectedToggleProperty().addListener((ob, o, n) ->
        {
            if (n != null && n.getUserData() != null)
            {
                switch ((String) n.getUserData())
                {
                    case "None":
                        internalModels.getSettingsModel().set("renderingMode", RenderingMode.NONE);
                        break;
                    case "Wireframe":
                        internalModels.getSettingsModel().set("renderingMode", RenderingMode.WIREFRAME);
                        break;
                    case "Lambertian shaded":
                        internalModels.getSettingsModel().set("renderingMode", RenderingMode.LAMBERTIAN_SHADED);
                        break;
                    case "Specular shaded":
                        internalModels.getSettingsModel().set("renderingMode", RenderingMode.SPECULAR_SHADED);
                        break;
                    case "Solid textured":
                        internalModels.getSettingsModel().set("renderingMode", RenderingMode.SOLID_TEXTURED);
                        break;
                    case "Lambertian textured":
                        internalModels.getSettingsModel().set("renderingMode", RenderingMode.LAMBERTIAN_DIFFUSE_TEXTURED);
                        break;
                    case "Material shaded":
                        internalModels.getSettingsModel().set("renderingMode", RenderingMode.MATERIAL_SHADED);
                        break;
                    case "Image-based rendering":
                    default:
                        internalModels.getSettingsModel().set("renderingMode", RenderingMode.IMAGE_BASED);
                        break;
                }
            }
        });
    }

    private void bindCheckMenuItems()
    {
        //value binding
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
                        newVsetFile = sceneModel.openProjectFile(projectFile);
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

                    new Thread(() ->
                    {
                        try
                        {
                            MultithreadModels.getInstance().getLoadingModel().loadFromVSETFile(vsetFileRef.getPath(), vsetFileRef);
                        }
                        catch (FileNotFoundException e)
                        {
                            e.printStackTrace();
                        }
                    }).start();
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
                    sceneModel.saveProjectFile(projectFile, vsetFile);
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
            projectFileChooser.setInitialFileName(projectFile.toString());
        }
        else if (vsetFile != null)
        {
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
        stage.setTitle(title);
        stage.setScene(new Scene(root, width, height));
        stage.initOwner(parentWindow);
        stage.setResizable(false);
        flag.set(true);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, param -> flag.set(false));
        stage.show();

        return fxmlLoader.getController();
    }
}
