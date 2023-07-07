package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javafx.scene.image.ImageView;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;


public class ChunkViewerController implements Initializable {
    //TODO: --> "INFO: index exceeds maxCellCount. Check size calculations for class javafx.scene.control.skin.TreeViewSkin$1"
    //suppress warning?

    @FXML
    public TreeView<String> chunkTreeView;

    @FXML
    public ImageView chunkViewerImgView;
    public Text imgViewLabel;
    private Document selectedChunkXML;

    static final String[] VALID_EXTENSIONS = {"*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.png", "*.bmp", "*.wbmp"};
    private String psxFilePath;
    private int chunkID;
    private String chunkName;

    static final int THUMBNAIL_SIZE = 30;
    @FXML
    public ChoiceBox<String> newChunkSelectionChoiceBox;//allows the user to select a new chunk to view

    @FXML
    public Button selectChunkButton;

    @FXML
    public TextFlow textFlow;

    private Scene scene;
    private Parent root;
    private Stage stage;
    //key is chunk name, value is path to chunk's zip file
    private HashMap<String, String> chunkZipPathPairs;
    private MetashapeObjectChunk metashapeObjectChunk;
    private MetashapeObject metashapeObject;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //nothing needed here so far
    }

    public void initializeChunkSelectionAndTreeView(MetashapeObjectChunk metashapeObjectChunk) {
        
        //TODO: REMOVE THESE ASSIGNMENTS BECAUSE THEY'RE STORED IN METASHAPE OBJECT CHUNK?
        this.metashapeObjectChunk = metashapeObjectChunk;
        this.metashapeObject = metashapeObjectChunk.getMetashapeObject();

        this.selectedChunkXML = metashapeObjectChunk.getChunkXML();
        this.psxFilePath = metashapeObjectChunk.getPsxFilePath();
        this.chunkID = metashapeObjectChunk.getChunkID();
        this.chunkName = metashapeObjectChunk.getChunkName();
        
        //TODO: SHOULD DO CASTING?
        this.chunkZipPathPairs = (HashMap<String, String>) metashapeObjectChunk.getChunkZipPathPairs();

        //add chunk name to tree
        TreeItem<String> rootItem = new TreeItem<>(chunkName);
        chunkTreeView.setRoot(rootItem);

        //initialize options in new chunk selection choice box (cannot be done before chunkName is set)
        initializeChoiceBox();
        this.newChunkSelectionChoiceBox.setOnAction(this::updateSelectChunkButton);

        //disable select chunk button if selected chunk (in choice box) matches the current chunk
        updateSelectChunkButton();

        //fill thumbnail list
        ArrayList <Image> thumbnailImageList = metashapeObjectChunk.getThumbnailImageList();

        //add full-res images as children to the chunk name in treeview
        ArrayList<Element> cameras = (ArrayList<Element>) metashapeObjectChunk.getThumbnailCameras();

        for (int i = 0; i < cameras.size(); ++i) {
            Element camera = cameras.get(i);
            String imageName = camera.getAttribute("label");

            ImageView thumbnailImgView;
            try {
                thumbnailImgView = new ImageView(thumbnailImageList.get(i));
            } catch (IndexOutOfBoundsException e) {
                //thumbnail not found in thumbnailImageList
                thumbnailImgView = new ImageView(new Image(new File("question-mark.png").toURI().toString()));
            }
            thumbnailImgView.setFitWidth(THUMBNAIL_SIZE);
            thumbnailImgView.setFitHeight(THUMBNAIL_SIZE);

            TreeItem<String> imageTreeItem = new TreeItem<>(imageName, thumbnailImgView);
            rootItem.getChildren().add(imageTreeItem);

        }

        //unroll treeview
        chunkTreeView.getRoot().setExpanded(true);
    }

    public void selectChunk(ActionEvent actionEvent) throws IOException {
        String selectedChunk = this.newChunkSelectionChoiceBox.getValue();

        if (!selectedChunk.equals(chunkName)) {//only change scene if switching to new chunk
            String selectedChunkZip = chunkZipPathPairs.get(selectedChunk);

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/menubar/ChunkViewer.fxml"));
            root = fxmlLoader.load();
            ChunkViewerController chunkViewerController = fxmlLoader.getController();

            MetashapeObjectChunk newMetashapeObjectChunk = new MetashapeObjectChunk(this.metashapeObject, selectedChunkZip);
            chunkViewerController.initializeChunkSelectionAndTreeView(newMetashapeObjectChunk);

            stage = (Stage) ((javafx.scene.Node) actionEvent.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void selectImageInTreeView() {
        //selectedItem holds the cameraID associated with the image
        TreeItem<String> selectedItem = chunkTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            String imageName = selectedItem.getValue();
            try {
                //goal of this try block is to find the camera which is associated with the image name in selectedItem
                //then take that camera's image and put it into the imageview to show to the user
                Element selectedItemCam = metashapeObjectChunk.matchImageToCam(imageName);

                if (selectedItemCam != null) {
                    File imgFile = metashapeObjectChunk.getImgFileFromCam(selectedItemCam);

                    //set imageview to selected image
                    if (imgFile.exists()) {
                        chunkViewerImgView.setImage(new Image(imgFile.toURI().toString()));

//                      set label to: psx name + chunk name + cameraID
                        File psxFile = new File(this.psxFilePath);

                        imgViewLabel.setText("File: " + psxFile.getName() +
                                            "\nChunk: " + chunkName +
                                            "\nImage: " + imageName);

                        textFlow.setTextAlignment(TextAlignment.LEFT);
                    } else {
                        setThumbnailAsFullImage(selectedItem);
                    }
                }
                else{
                    //camera not found
                    setThumbnailAsFullImage(selectedItem);
                }
                //TODO: .TIF (and maybe other image types) ARE NOT SUPPORTED. CONVERT THESE?
                //TODO: AFTER SELECTION, PROGRAM WILL FREEZE TO DOWNLOAD IMAGES FROM ONEDRIVE
            } catch (IllegalArgumentException e) {//could not find image
                e.printStackTrace();
            }
        }
    }

    private void setThumbnailAsFullImage(TreeItem<String> selectedItem) {
        //use thumbnail as main image if main image not found
        chunkViewerImgView.setImage(selectedItem.getGraphic().
                snapshot(new SnapshotParameters(), null));
        imgViewLabel.setText("Image not found.");
        textFlow.setTextAlignment(TextAlignment.CENTER);
    }

    private void initializeChoiceBox() {
        //add all items from old checkbox to new checkbox
        this.newChunkSelectionChoiceBox.getItems().addAll(metashapeObject.getChunkNames());

        //initialize checkbox to selected chunk (instead of blank) if possible
        //otherwise, set to first item in list
        try {
            this.newChunkSelectionChoiceBox.setValue(chunkName);
        } catch (Exception e) {
            if (this.newChunkSelectionChoiceBox.getItems() != null) {
                this.newChunkSelectionChoiceBox.setValue(this.newChunkSelectionChoiceBox.getItems().get(0));
            }
        }

        if (this.newChunkSelectionChoiceBox.getItems().size() <= 1) {
            selectChunkButton.setDisable(true);
        }
    }

    private boolean isValidImageType(String path) {
        for (String extension : VALID_EXTENSIONS) {
            if (path.matches("." + extension)) {
                return true;
            }
        }
        return false;
    }

    public void updateSelectChunkButton(ActionEvent actionEvent) {
        //need to keep the unused ActionEvent so we can link this method to the choice box
        String selectedChunk = this.newChunkSelectionChoiceBox.getValue();

        if (selectedChunk == null){
            return;
        }

        if (selectedChunk.equals(chunkName)) {
            selectChunkButton.setDisable(true);
            selectChunkButton.setText("Chunk already selected");
        } else {
            selectChunkButton.setDisable(false);
            selectChunkButton.setText("Select Chunk");
        }
    }
    public void updateSelectChunkButton() {
        updateSelectChunkButton(new ActionEvent());
    }
}