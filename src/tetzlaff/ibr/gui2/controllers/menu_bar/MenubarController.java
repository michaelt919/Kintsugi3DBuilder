package tetzlaff.ibr.gui2.controllers.menu_bar;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.app2.TheApp;
import tetzlaff.ibr.rendering2.ToolModel3;
import tetzlaff.ibr.rendering2.tools2.ToolBox;
import tetzlaff.ibr.util.Flag;

public class MenubarController implements Initializable {
    //toolModel
    private final ToolModel3 toolModel = TheApp.getRootModel().getToolModel3();
    private IBRSettingsUIImpl getSettings(){
        return toolModel.getIbrSettingsUIImpl();
    }
    private LoadSettings getLoadSettings(){
        return toolModel.getLoadSettings();
    }
    private IBRRenderable<?> getRenderable(){
        return toolModel.getIBRRenderable();
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


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initToggleGroups();
        bindCheckMenuItems();
    }

    private void initToggleGroups(){
        toolGroup.selectedToggleProperty().addListener((ob,o,n)->{
            if(n != null){

                int indexInToolList = toolGroup.getToggles().indexOf(n);
                switch (indexInToolList){
                    case 0: toolModel.setTool(ToolBox.TOOL.ORBIT); return;
                    case 1: toolModel.setTool(ToolBox.TOOL.PAN); return;
                    case 2: toolModel.setTool(ToolBox.TOOL.DOLLY); return;
                    case 11: toolModel.setTool(ToolBox.TOOL.LIGHT_DRAG); return;
                    default: toolModel.setTool(ToolBox.TOOL.ORBIT);
                }
            }
        });
        renderGroup.selectedToggleProperty().addListener((ob,o,n)->{
            if(n!=null && n.getUserData() != null){

                switch ((String) n.getUserData()){
                    case "Wireframe": toolModel.getIbrSettingsUIImpl().setRenderingType(RenderingType.WIREFRAME); return;
                    case "Lambertian shaded":toolModel.getIbrSettingsUIImpl().setRenderingType(RenderingType.LAMBERTIAN_SHADED); return;
                    case "Phong shaded":toolModel.getIbrSettingsUIImpl().setRenderingType(RenderingType.PHONG_SHADED); return;
                    case "Solid textured":toolModel.getIbrSettingsUIImpl().setRenderingType(RenderingType.SOLID_TEXTURED); return;
                    case "Lambertian textured":toolModel.getIbrSettingsUIImpl().setRenderingType(RenderingType.LAMBERTIAN_TEXTURED); return;
                    case "Material shaded":toolModel.getIbrSettingsUIImpl().setRenderingType(RenderingType.MATERIAL_SHADED); return;
                    case "Image-based rendering":toolModel.getIbrSettingsUIImpl().setRenderingType(RenderingType.IMAGE_BASED_RENDERING); return;
                    case "None": toolModel.getIbrSettingsUIImpl().setRenderingType(RenderingType.NONE); return;
                }
            }
        });
    }

    private void bindCheckMenuItems(){
        //value binding
        d3GridCheckMenuItem.selectedProperty().bindBidirectional(getSettings().d3GridEnabledProperty());
        compassCheckMenuItem.selectedProperty().bindBidirectional(getSettings().compassEnabledProperty());

        //onAction Binding
        halfResolutionCheckMenuItem.setOnAction(param-> getRenderable().setHalfResolution(halfResolutionCheckMenuItem.isSelected()));
        multiSamplingCheckMenuItem.setOnAction(param -> getRenderable().setMultisampling(multiSamplingCheckMenuItem.isSelected()));

    }

    //Menubar->File

    @FXML private void file_createProject(){

        if(loaderWindowOpen.get())return;

        LoaderController loaderController = makeWindow("Load Files", loaderWindowOpen, 750, 330,"fxml/menu_bar/Loader.fxml");
        if (loaderController != null) {
            loaderController.setToolModel3(toolModel);
        }
    }

    @FXML private void file_openProject(){
        System.out.println("TODO: open project");
    }

    @FXML private void file_saveProject(){
        System.out.println("TODO: save project");
    }

    @FXML private void file_saveProjectAs(){
        System.out.println("TODO: save project as...");
    }

    @FXML private void file_closeProject(){
        file_exit();
    }

    @FXML private void file_export_reSample(){
        System.out.println("TODO: export re-sample...");
    }
    @FXML private void file_export_fidelityMetric(){
        System.out.println("TODO: export fidelity metric...");
    }
    @FXML private void file_export_BTF(){
        System.out.println("TODO: export BTF...");
    }
    @FXML private void file_export_Other(){
        System.out.println("TODO: export Other...");
    }
    @FXML private void file_loadSettingsConfiguration(){
        System.out.println("TODO: load settings configuration");
    }

    @FXML private void file_loadOptions(){

        if(loadOptionsWindowOpen.get())return;

        LoadOptionsController loadOptionsController = makeWindow("Load Options", loadOptionsWindowOpen, "fxml/menu_bar/LoadOptions.fxml");
        if (loadOptionsController != null) {
            loadOptionsController.bind(toolModel.getLoadSettings());
        }

    }
    @FXML private void file_exit(){
        System.exit(0);
    }

    @FXML private void shading_IBRSettings(){

        if(iBROptionsWindowOpen.get())return;

        IBROptionsController ibrOptionsController = makeWindow("IBRL Settings", iBROptionsWindowOpen,
                "fxml/menu_bar/IBROptions.fxml");
        if (ibrOptionsController != null) {
            ibrOptionsController.bind(toolModel.getIbrSettingsUIImpl());
        }
    }



    //window helpers
    private static <CONTROLLER_CLASS> CONTROLLER_CLASS makeWindow(String title, String urlString){
        try {
            URL url = MenubarController.class.getClassLoader().getResource(urlString);
            if (url == null) {
                throw new IOException("Cant find file " + urlString);
            }
            FXMLLoader fxmlLoader = new FXMLLoader(url);
            Parent root = fxmlLoader.load();
            Stage stage= new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));

            stage.setResizable(false);

            stage.show();

            return fxmlLoader.getController();

        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    private static <CONTROLLER_CLASS> CONTROLLER_CLASS makeWindow(String title, Flag flag, String urlString){
        try {
            URL url = MenubarController.class.getClassLoader().getResource(urlString);
            if (url == null) {
                throw new IOException("Cant find file " + urlString);
            }
            FXMLLoader fxmlLoader = new FXMLLoader(url);
            Parent root = fxmlLoader.load();
            Stage stage= new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));

            stage.setResizable(false);

            flag.set(true);
            flag.addFalseToClose(stage);

            stage.show();

            return fxmlLoader.getController();

        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }
    private static <CONTROLLER_CLASS> CONTROLLER_CLASS makeWindow(String title, int width, int height, String urlString){
        try {
            URL url = MenubarController.class.getClassLoader().getResource(urlString);
            if (url == null) {
                throw new IOException("Cant find file " + urlString);
            }
            FXMLLoader fxmlLoader = new FXMLLoader(url);
            Parent root = fxmlLoader.load();
            Stage stage= new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));

            stage.setResizable(false);

            stage.show();

            return fxmlLoader.getController();

        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }
    private static <CONTROLLER_CLASS> CONTROLLER_CLASS makeWindow(String title, Flag flag, int width, int height, String urlString){
        try {
            URL url = MenubarController.class.getClassLoader().getResource(urlString);
            if (url == null) {
                throw new IOException("Cant find file " + urlString);
            }
            FXMLLoader fxmlLoader = new FXMLLoader(url);
            Parent root = fxmlLoader.load();
            Stage stage= new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));
            stage.setResizable(false);
            flag.set(true);
            flag.addFalseToClose(stage);
            stage.show();

            return fxmlLoader.getController();

        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

}
