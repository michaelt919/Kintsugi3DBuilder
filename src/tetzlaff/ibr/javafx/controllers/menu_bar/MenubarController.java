package tetzlaff.ibr.javafx.controllers.menu_bar;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Scanner;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import tetzlaff.ibr.app.WindowSynchronization;
import tetzlaff.ibr.core.*;
import tetzlaff.ibr.javafx.controllers.scene.camera.CameraSetting;
import tetzlaff.ibr.javafx.controllers.scene.environment_map.EnvironmentSetting;
import tetzlaff.ibr.javafx.controllers.scene.lights.LightGroupSetting;
import tetzlaff.ibr.javafx.controllers.scene.object.ObjectPoseSetting;
import tetzlaff.ibr.javafx.models.JavaFXModelAccess;
import tetzlaff.ibr.javafx.models.JavaFXSceneModel;
import tetzlaff.ibr.javafx.models.JavaFXSettingsModel;
import tetzlaff.util.Flag;

public class MenubarController
{
    private JavaFXSettingsModel getSettings()
    {
        return JavaFXModelAccess.getInstance().getSettingsModel();
    }

    //Window open flags
    Flag iBROptionsWindowOpen = new Flag(false);
    Flag loadOptionsWindowOpen = new Flag(false);
    Flag loaderWindowOpen = new Flag(false);

    @FXML private ProgressBar progressBar;

    //toggle groups
    @FXML private ToggleGroup renderGroup;

    //menu items
    @FXML private CheckMenuItem d3GridCheckMenuItem;
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

    public void init(Window parentWindow, IBRRequestQueue<?> requestQueue)
    {
        this.parentWindow = parentWindow;

        projectFileChooser = new FileChooser();

        projectFileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        projectFileChooser.getExtensionFilters().add(new ExtensionFilter("Full projects", "*.ibr"));
        projectFileChooser.getExtensionFilters().add(new ExtensionFilter("Standalone view sets", "*.vset"));

        JavaFXModelAccess.getInstance().getLoadingModel().setLoadingMonitor(new LoadingMonitor()
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
                Platform.runLater(() -> progressBar.setProgress(progress / maximum));
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
                            Method createMethod = requestUIClass.getDeclaredMethod("create", Window.class, IBRelightModelAccess.class);
                            if (IBRRequestUI.class.isAssignableFrom(createMethod.getReturnType())
                                && ((createMethod.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) == (Modifier.PUBLIC | Modifier.STATIC)))
                            {
                                MenuItem newItem = new MenuItem(menuName);
                                newItem.setOnAction(event ->
                                {
                                    try
                                    {
                                        IBRRequestUI requestUI = (IBRRequestUI) createMethod.invoke(null, parentWindow, JavaFXModelAccess.getInstance());
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
                    case "Wireframe":
                        JavaFXModelAccess.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.WIREFRAME);
                        return;
                    case "Lambertian shaded":
                        JavaFXModelAccess.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.LAMBERTIAN_SHADED);
                        return;
                    case "Specular shaded":
                        JavaFXModelAccess.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.SPECULAR_SHADED);
                        return;
                    case "Solid textured":
                        JavaFXModelAccess.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.SOLID_TEXTURED);
                        return;
                    case "Lambertian textured":
                        JavaFXModelAccess.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.LAMBERTIAN_DIFFUSE_TEXTURED);
                        return;
                    case "Material shaded":
                        JavaFXModelAccess.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.MATERIAL_SHADED);
                        return;
                    case "Image-based rendering":
                        JavaFXModelAccess.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.IMAGE_BASED);
                        return;
                    case "None":
                        JavaFXModelAccess.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.NONE);
                        return;
                }
            }
        });
    }

    private void bindCheckMenuItems()
    {
        //value binding
        d3GridCheckMenuItem.selectedProperty().bindBidirectional(getSettings().is3DGridEnabledProperty());
        compassCheckMenuItem.selectedProperty().bindBidirectional(getSettings().compassEnabledProperty());
        relightingCheckMenuItem.selectedProperty().bindBidirectional(getSettings().relightingProperty());
        shadowsCheckMenuItem.selectedProperty().bindBidirectional(getSettings().shadowsProperty());
        visibleLightsCheckMenuItem.selectedProperty().bindBidirectional(getSettings().visibleLightsProperty());
        visibleLightWidgetsCheckMenuItem.selectedProperty().bindBidirectional(getSettings().visibleLightWidgetsProperty());
        visibleCameraPoseCheckMenuItem.selectedProperty().bindBidirectional(getSettings().visibleCameraPoseProperty());
        visibleSavedCameraPoseCheckMenuItem.selectedProperty().bindBidirectional(getSettings().visibleSavedCameraPoseProperty());

        phyMaskingCheckMenuItem.selectedProperty().bindBidirectional(getSettings().pbrGeometricAttenuationProperty());
        fresnelEffectCheckMenuItem.selectedProperty().bindBidirectional(getSettings().fresnelProperty());

        halfResolutionCheckMenuItem.selectedProperty().bindBidirectional(getSettings().halfResolutionEnabledProperty());
        multiSamplingCheckMenuItem.selectedProperty().bindBidirectional(getSettings().multisamplingEnabledProperty());
    }

    //Menubar->File

    @FXML
    private void file_createProject()
    {

        if (loaderWindowOpen.get())
        {
            return;
        }

        LoaderController loaderController = makeWindow("Load Files", loaderWindowOpen, 750, 330, "fxml/menu_bar/Loader.fxml");
        if (loaderController != null)
        {
            loaderController.setUnloadFunction(this::file_closeProject);
        }
    }

    @FXML
    private void file_openProject()
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
                    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(projectFile);

                    Node vsetNode = document.getElementsByTagName("ViewSet").item(0);
                    if (vsetNode instanceof Element)
                    {
                        newVsetFile = new File(projectFile.getParent(), ((Element) vsetNode).getAttribute("src"));

                        JavaFXSceneModel sceneModel = JavaFXModelAccess.getInstance().getSceneModel();

                        Node cameraListNode = document.getElementsByTagName("CameraList").item(0);
                        if (cameraListNode != null)
                        {
                            NodeList cameraNodes = cameraListNode.getChildNodes();
                            sceneModel.getCameraList().clear();
                            for (int i = 0; i < cameraNodes.getLength(); i++)
                            {
                                Node cameraNode = cameraNodes.item(i);
                                if (cameraNode instanceof Element)
                                {
                                    sceneModel.getCameraList().add(CameraSetting.fromDOMElement((Element) cameraNode));
                                }
                            }
                        }

                        Node environmentListNode = document.getElementsByTagName("EnvironmentList").item(0);
                        if (environmentListNode != null)
                        {
                            NodeList environmentNodes = environmentListNode.getChildNodes();
                            sceneModel.getEnvironmentList().clear();
                            for (int i = 0; i < environmentNodes.getLength(); i++)
                            {
                                Node environmentNode = environmentNodes.item(i);
                                if (environmentNode instanceof Element)
                                {
                                    sceneModel.getEnvironmentList().add(EnvironmentSetting.fromDOMElement((Element) environmentNode));
                                }
                            }
                        }

                        Node lightGroupListNode = document.getElementsByTagName("LightGroupList").item(0);
                        if (lightGroupListNode != null)
                        {
                            NodeList lightGroupNodes = lightGroupListNode.getChildNodes();
                            sceneModel.getLightGroupList().clear();
                            for (int i = 0; i < lightGroupNodes.getLength(); i++)
                            {
                                Node lightGroupNode = lightGroupNodes.item(i);
                                if (lightGroupNode instanceof Element)
                                {
                                    sceneModel.getLightGroupList().add(LightGroupSetting.fromDOMElement((Element) lightGroupNode));
                                }
                            }
                        }

                        Node objectPoseListNode = document.getElementsByTagName("ObjectPoseList").item(0);
                        if (objectPoseListNode != null)
                        {
                            NodeList objectPoseNodes = objectPoseListNode.getChildNodes();
                            sceneModel.getObjectPoseList().clear();
                            for (int i = 0; i < objectPoseNodes.getLength(); i++)
                            {
                                Node objectPoseNode = objectPoseNodes.item(i);
                                if (objectPoseNode instanceof Element)
                                {
                                    sceneModel.getObjectPoseList().add(ObjectPoseSetting.fromDOMElement((Element) objectPoseNode));
                                }
                            }
                        }
                    }
                    else
                    {
                        System.err.println("Error while processing the ViewSet element.");
                    }
                }
                catch (SAXException|IOException|ParserConfigurationException e)
                {
                    e.printStackTrace();
                }
            }

            if (newVsetFile != null)
            {
                JavaFXModelAccess.getInstance().getLoadingModel().unload();

                this.vsetFile = newVsetFile;
                File vsetFileRef = newVsetFile;

                new Thread(() ->
                {
                    try
                    {
                        JavaFXModelAccess.getInstance().getLoadingModel().loadFromVSETFile(vsetFileRef.getPath(), vsetFileRef);
                    }
                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                }).start();
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
                    JavaFXModelAccess.getInstance().getLoadingModel().saveToVSETFile(projectFile);
                    this.vsetFile = projectFile;
                    this.projectFile = null;
                }
                else
                {
                    this.vsetFile = new File(projectFile + ".vset");

                    JavaFXModelAccess.getInstance().getLoadingModel().saveToVSETFile(vsetFile);

                    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                    Element rootElement = document.createElement("Project");
                    document.appendChild(rootElement);

                    Element vsetElement = document.createElement("ViewSet");
                    vsetElement.setAttribute("src", projectFile.getParentFile().toPath().relativize(vsetFile.toPath()).toString());
                    rootElement.appendChild(vsetElement);

                    JavaFXSceneModel sceneModel = JavaFXModelAccess.getInstance().getSceneModel();

                    Element cameraListElement = document.createElement("CameraList");
                    rootElement.appendChild(cameraListElement);

                    for (CameraSetting camera : sceneModel.getCameraList())
                    {
                        cameraListElement.appendChild(camera.toDOMElement(document));
                    }

                    Element environmentListElement = document.createElement("EnvironmentList");
                    rootElement.appendChild(environmentListElement);

                    for (EnvironmentSetting environment : sceneModel.getEnvironmentList())
                    {
                        environmentListElement.appendChild(environment.toDOMElement(document));
                    }

                    Element lightGroupListElement = document.createElement("LightGroupList");
                    rootElement.appendChild(lightGroupListElement);

                    for (LightGroupSetting lightGroup : sceneModel.getLightGroupList())
                    {
                        lightGroupListElement.appendChild(lightGroup.toDOMElement(document));
                    }

                    Element objectPoseListElement = document.createElement("ObjectPoseList");
                    rootElement.appendChild(objectPoseListElement);

                    for (ObjectPoseSetting objectPose : sceneModel.getObjectPoseList())
                    {
                        objectPoseListElement.appendChild(objectPose.toDOMElement(document));
                    }

                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                    try(OutputStream out = new FileOutputStream(projectFile))
                    {
                        transformer.transform(new DOMSource(document), new StreamResult(out));
                    }
                    catch(FileNotFoundException|TransformerException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            catch(ParserConfigurationException|TransformerConfigurationException|IOException e)
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
        projectFile = null;
        vsetFile = null;

        JavaFXModelAccess.getInstance().getLoadingModel().unload();
    }

    @FXML
    private void file_loadOptions()
    {

        if (loadOptionsWindowOpen.get())
        {
            return;
        }

        LoadOptionsController loadOptionsController = makeWindow("Load Options", loadOptionsWindowOpen, "fxml/menu_bar/LoadOptions.fxml");
        if (loadOptionsController != null)
        {
            loadOptionsController.bind(JavaFXModelAccess.getInstance().getLoadOptionsModel());
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

        if (iBROptionsWindowOpen.get())
        {
            return;
        }

        IBROptionsController ibrOptionsController = makeWindow("IBR Settings", iBROptionsWindowOpen,
            "fxml/menu_bar/IBROptions.fxml");
        if (ibrOptionsController != null)
        {
            ibrOptionsController.bind(JavaFXModelAccess.getInstance().getSettingsModel());
        }
    }

    //window helpers
    private <ControllerType> ControllerType makeWindow(String title, Flag flag, String urlString)
    {
        try
        {
            URL url = MenubarController.class.getClassLoader().getResource(urlString);
            if (url == null)
            {
                throw new IOException("Cant find file " + urlString);
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
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private <ControllerType> ControllerType makeWindow(String title, Flag flag, int width, int height, String urlString)
    {
        try
        {
            URL url = MenubarController.class.getClassLoader().getResource(urlString);
            if (url == null)
            {
                throw new IOException("Cant find file " + urlString);
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
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
