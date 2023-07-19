package tetzlaff.ibrelight.javafx.controllers.scene;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import main.resources.fxml.menubar.CreateProjectController;
import org.xml.sax.SAXException;
import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.core.IBRRequestManager;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.javafx.InternalModels;
import tetzlaff.ibrelight.javafx.MultithreadModels;
import tetzlaff.ibrelight.javafx.controllers.menubar.LoaderController;
import tetzlaff.ibrelight.javafx.controllers.menubar.MenubarController;
import tetzlaff.util.Flag;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;

public class WelcomeWindowController {
    private InternalModels internalModels;

    //Window open flags
    private final Flag ibrOptionsWindowOpen = new Flag(false);
    private final Flag jvmOptionsWindowOpen = new Flag(false);
    private final Flag loadOptionsWindowOpen = new Flag(false);
    private final Flag loaderWindowOpen = new Flag(false);
    private final Flag colorCheckerWindowOpen = new Flag(false);
    private final Flag unzipperOpen = new Flag(false);


    @FXML private ProgressBar progressBar;

    //toggle groups
    @FXML private ToggleGroup renderGroup;

    @FXML private FileChooser projectFileChooser;

    @FXML private Menu exportMenu;

    @FXML private SplitMenuButton recentProjectsSplitMenuButton;

    private Window parentWindow;

    private File projectFile;
    private File vsetFile;
    private boolean projectLoaded;

    private Runnable userDocumentationHandler;

    private IBRRequestManager<?> requestQueue;
    private File recentProjectsFile;


    public <ContextType extends Context<ContextType>> void init(
            Window injectedParentWindow, IBRRequestManager<ContextType> requestQueue, InternalModels injectedInternalModels,
            Runnable injectedUserDocumentationHandler) {
        this.parentWindow = injectedParentWindow;
        this.internalModels = injectedInternalModels;
        this.userDocumentationHandler = injectedUserDocumentationHandler;

        projectFileChooser = new FileChooser();

        this.requestQueue = requestQueue;

        projectFileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        projectFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Full projects", "*.ibr"));
        projectFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Standalone view sets", "*.vset"));

        recentProjectsFile = new File("src/main/resources/recentFiles.txt");//TODO: NEW LOCATION?

        updateRecentProjectsButton();

        MultithreadModels.getInstance().getLoadingModel().setLoadingMonitor(new LoadingMonitor() {
            private double maximum = 0.0;
            private double progress = 0.0;

            @Override
            public void startLoading() {
                progress = 0.0;
                Platform.runLater(() ->
                {
                    progressBar.setVisible(true);
                    progressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : 0.0);
                });
            }

            @Override
            public void setMaximum(double maximum) {
                this.maximum = maximum;
                Platform.runLater(() -> progressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : progress / maximum));
            }

            @Override
            public void setProgress(double progress) {
                this.progress = progress;
                Platform.runLater(() -> progressBar.setProgress(maximum == 0.0 ? ProgressIndicator.INDETERMINATE_PROGRESS : progress / maximum));
            }

            @Override
            public void loadingComplete() {
                this.maximum = 0.0;
                Platform.runLater(() -> progressBar.setVisible(false));
            }

            @Override
            public void loadingFailed(Exception e) {
                loadingComplete();
                projectLoaded = false;
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, e.toString()).show());
            }
        });
    }

    private void updateRecentProjectsButton() {//TODO: FORMAT ----- PROJECT NAME --> PATH
        recentProjectsSplitMenuButton.getItems().clear();

        List<String> items = getItemsFromRecentsFile();

        List<MenuItem> menuItems = new ArrayList<>();
        for (String item : items){
            menuItems.add(new MenuItem(item));
        }

        recentProjectsSplitMenuButton.getItems().addAll(menuItems);

        //disable button if there are no recent projects
        if(recentProjectsSplitMenuButton.getItems().isEmpty()){
            recentProjectsSplitMenuButton.setDisable(true);
        }

        //attach event handlers to all menu items
        for (MenuItem item : menuItems){
            item.setOnAction(event -> handleMenuItemSelection(item));
        }
    }

    public void handleMenuItemSelection(MenuItem item) {
        String projectName = item.getText();
        openProjectFromFile(new File(projectName));
    }

    public void splitMenuButtonActions(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();

        //user clicks on a menu item
        if (source.getClass() == MenuItem.class) {
            handleMenuItemSelection((MenuItem) actionEvent.getSource());
        }

        //user clicks on the button, so unroll the menu
        else{
            unrollMenu();
        }
    }

    private List<String> getItemsFromRecentsFile() {
        List<String> projectItems = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(recentProjectsFile.getAbsolutePath()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String projectItem = line;
                projectItems.add(projectItem);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //remove duplicates while maintaining the same order (regular HashSet does not maintain order)
        return new ArrayList<>(new LinkedHashSet<>(projectItems));
    }

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
        updateRecentProjectsButton();
    }

    public void createProject() {
        if (loaderWindowOpen.get())
        {
            return;
        }

        if (confirmClose("Are you sure you want to create a new project?"))
        {
            try
            {
                CreateProjectController createProjectController = makeWindow("Load Files", loaderWindowOpen, "fxml/menubar/CreateProject.fxml");
                createProjectController.setCallback(() ->
                {
                    this.file_closeProject();
                    projectLoaded = true;
                });
                createProjectController.init();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        updateRecentProjectsButton();
    }

    @FXML
    private void file_openProject()//TODO: CHANGE NAMING CONVENTION? (file_...)
    {
        if (confirmClose("Are you sure you want to open another project?"))
        {
            projectFileChooser.setTitle("Open project");
            File selectedFile = projectFileChooser.showOpenDialog(parentWindow);
            if (selectedFile != null)
            {
                openProjectFromFile(selectedFile);
            }
        }
        updateRecentProjectsButton();
    }

    private void openProjectFromFile(File selectedFile) {
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

            updateRecentFiles(projectFile.getAbsolutePath());//TODO: ADD TO CREATE()

            projectLoaded = true;

            new Thread(() -> MultithreadModels.getInstance().getLoadingModel().loadFromVSETFile(vsetFileRef.getPath(), vsetFileRef)).start();
        }
    }

    private void updateRecentFiles(String fileName) {
        // Read existing file content into a List
        List<String> existingFileNames = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(recentProjectsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                existingFileNames.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Check if the fileName is already present
        existingFileNames.remove(fileName); // Remove it from its current position

        // Add the fileName to the front of the List
        existingFileNames.add(0, fileName);

        // Write the updated content back to the file
        try (PrintWriter writer = new PrintWriter(new FileWriter(recentProjectsFile))) {
            for (String name : existingFileNames) {
                writer.println(name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //update list of recent projects
        updateRecentProjectsButton();
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
        stage.getIcons().add(new Image(new File("ibr-icon.png").toURI().toURL().toString()));
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
        stage.getIcons().add(new Image(new File("ibr-icon.png").toURI().toURL().toString()));
        stage.setTitle(title);
        stage.setScene(new Scene(root, width, height));
        stage.initOwner(parentWindow);
        stage.setResizable(false);
        flag.set(true);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, param -> flag.set(false));
        stage.show();

        return fxmlLoader.getController();
    }

    private boolean confirmClose(String text)
    {
        if (projectLoaded)
        {
            Dialog<ButtonType> confirmation = new Alert(Alert.AlertType.CONFIRMATION,
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
    private void file_closeProject()
    {
        //TODO: DISABLE THIS BUTTON IF NO PROJECT IS OPEN?
        if (confirmClose("Are you sure you want to close the current project?"))
        {
            projectFile = null;
            vsetFile = null;

            MultithreadModels.getInstance().getLoadingModel().unload();
            projectLoaded = false;
        }
    }



    @FXML
    private void help_userManual()
    {
        userDocumentationHandler.run();
    }

    public void unrollMenu() {
        recentProjectsSplitMenuButton.show();
    }

}
