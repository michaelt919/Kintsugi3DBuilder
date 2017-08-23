package tetzlaff.ibr.javafx.controllers.menu_bar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import tetzlaff.ibr.RenderingMode;
import tetzlaff.ibr.app.WindowSynchronization;
import tetzlaff.ibr.javafx.models.JavaFXModels;
import tetzlaff.ibr.javafx.models.JavaFXSettingsModel;
import tetzlaff.ibr.javafx.models.JavaFXToolSelectionModel;
import tetzlaff.ibr.tools.ToolType;
import tetzlaff.util.Flag;

public class MenubarController
{
    //toolModel
    private JavaFXToolSelectionModel toolModel;

    private JavaFXSettingsModel getSettings()
    {
        return JavaFXModels.getInstance().getSettingsModel();
    }

    //Window open flags
    Flag iBROptionsWindowOpen = new Flag(false);
    Flag loadOptionsWindowOpen = new Flag(false);
    Flag loaderWindowOpen = new Flag(false);

    //toggle groups
    @FXML private ToggleGroup toolGroup;
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
    @FXML private CheckMenuItem visibleCameraPoseCheckMenuItem;
    @FXML private CheckMenuItem visibleSavedCameraPoseCheckMenuItem;

    @FXML private CheckMenuItem materialsForIBRCheckMenuItem;
    @FXML private CheckMenuItem phyMaskingCheckMenuItem;
    @FXML private CheckMenuItem fresnelEffectCheckMenuItem;

    @FXML private FileChooser vSetFileChooser;

    public void init(JavaFXToolSelectionModel toolModel)
    {
        this.toolModel = toolModel;
        vSetFileChooser = new FileChooser();

        vSetFileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        vSetFileChooser.setTitle("Load V-Set File");
        vSetFileChooser.getExtensionFilters().add(
            new ExtensionFilter("V-Set Files", "*.vset")
        );

        initToggleGroups();
        bindCheckMenuItems();
    }

    private void initToggleGroups()
    {
        toolGroup.selectedToggleProperty().addListener((ob, o, n) ->
        {
            if (n != null && n.getUserData() != null)
            {
                switch ((String) n.getUserData())
                {
                    case "ORBIT":
                        toolModel.setTool(ToolType.ORBIT);
                        break;
                    case "DOLLY":
                        toolModel.setTool(ToolType.DOLLY);
                        break;
                    case "PAN":
                        toolModel.setTool(ToolType.PAN);
                        break;
                    case "LIGHT":
                        toolModel.setTool(ToolType.LIGHT);
                        break;
                    case "CENTER_POINT":
                        toolModel.setTool(ToolType.CENTER_POINT);
                        break;
                }
            }
        });

        toolModel.toolProperty().addListener((observable, oldValue, newValue) ->
        {
            String data;
            switch (newValue)
            {
                case ORBIT:
                    data = "ORBIT";
                    break;
                case DOLLY:
                    data = "DOLLY";
                    break;
                case PAN:
                    data = "PAN";
                    break;
                case LIGHT:
                    data = "LIGHT";
                    break;
                case CENTER_POINT:
                    data = "CENTER_POINT";
                    break;
                default:
                    data = "ERROR";
            }
            selectToggleWithData(toolGroup, data);
        });

        renderGroup.selectedToggleProperty().addListener((ob, o, n) ->
        {
            if (n != null && n.getUserData() != null)
            {

                switch ((String) n.getUserData())
                {
                    case "Wireframe":
                        JavaFXModels.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.WIREFRAME);
                        return;
                    case "Lambertian shaded":
                        JavaFXModels.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.LAMBERTIAN_SHADED);
                        return;
                    case "Phong shaded":
                        JavaFXModels.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.PHONG_SHADED);
                        return;
                    case "Solid textured":
                        JavaFXModels.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.SOLID_TEXTURED);
                        return;
                    case "Lambertian textured":
                        JavaFXModels.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.LAMBERTIAN_TEXTURED);
                        return;
                    case "Material shaded":
                        JavaFXModels.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.MATERIAL_SHADED);
                        return;
                    case "Image-based rendering":
                        JavaFXModels.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.IMAGE_BASED_RENDERING);
                        return;
                    case "None":
                        JavaFXModels.getInstance().getSettingsModel().renderingModeProperty().set(RenderingMode.NONE);
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
        visibleCameraPoseCheckMenuItem.selectedProperty().bindBidirectional(getSettings().visibleCameraPoseProperty());
        visibleSavedCameraPoseCheckMenuItem.selectedProperty().bindBidirectional(getSettings().visibleSavedCameraPoseProperty());

        materialsForIBRCheckMenuItem.selectedProperty().bindBidirectional(getSettings().useMaterialsProperty());
        phyMaskingCheckMenuItem.selectedProperty().bindBidirectional(getSettings().pBRGeometricAttenuationProperty());
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

        makeWindow("Load Files", loaderWindowOpen, 750, 330, "fxml/menu_bar/Loader.fxml");
    }

    @FXML
    private void file_openProject()
    {
        File vsetFile = vSetFileChooser.showOpenDialog(null);
        if (vsetFile != null)
        {
            new Thread(() ->
            {
                try
                {
                    JavaFXModels.getInstance().getLoadingModel().loadFromVSETFile(vsetFile.getPath(), vsetFile);
                }
                catch (FileNotFoundException e)
                {
                    //do nothing
                }
            }).start();
        }
    }

    @FXML
    private void file_saveProject()
    {
        System.out.println("TODO: save project");
    }

    @FXML
    private void file_saveProjectAs()
    {
        System.out.println("TODO: save project as...");
    }

    @FXML
    private void file_closeProject()
    {
        file_exit();
    }

    @FXML
    private void file_export_reSample()
    {
        System.out.println("TODO: export re-sample...");
    }

    @FXML
    private void file_export_fidelityMetric()
    {
        System.out.println("TODO: export fidelity metric...");
    }

    @FXML
    private void file_export_BTF()
    {
        System.out.println("TODO: export BTF...");
    }

    @FXML
    private void file_export_Other()
    {
        System.out.println("TODO: export Other...");
    }

    @FXML
    private void file_loadSettingsConfiguration()
    {
        System.out.println("TODO: load settings configuration");
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
            loadOptionsController.bind(JavaFXModels.getInstance().getLoadOptionsModel());
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
            ibrOptionsController.bind(JavaFXModels.getInstance().getSettingsModel());
        }
    }

    //window helpers
    private static <ControllerType> ControllerType makeWindow(String title, Flag flag, String urlString)
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

    private static <ControllerType> ControllerType makeWindow(String title, Flag flag, int width, int height, String urlString)
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

    //toggle group helpers
    private static void selectToggleWithData(ToggleGroup toggleGroup, String data)
    {
        ObservableList<Toggle> toggles = toggleGroup.getToggles();
        toggles.iterator().forEachRemaining(toggle ->
        {
            if (toggle.getUserData() instanceof String && toggle.getUserData().equals(data))
            {
                toggleGroup.selectToggle(toggle);
            }
        });
    }
}
