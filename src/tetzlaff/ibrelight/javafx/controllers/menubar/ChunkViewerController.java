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
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javafx.scene.image.ImageView;
import tetzlaff.gl.util.UnzipHelper;

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

    static final String[] validExtensions = {"*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.png", "*.bmp", "*.wbmp"};
    private String psxFilePath;
    private Document frameZip;

    private int chunkID;
    private String chunkName;
    @FXML
    public ChoiceBox<String> newChunkSelectionChoiceBox;//allows the user to select a new chunk to view

    @FXML
    public Button selectChunkButton;

    private Scene scene;
    private Parent root;
    private Stage stage;
    //key is chunk name, value is path to chunk's zip file
    private HashMap<String, String> chunkZipPathPairs;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //nothing needed here so far
    }

    public void initializeChunkSelectionAndTreeView(Document selectedChunkXML, String psxFilePath, int chunkID,
                                                    ChoiceBox<String> chunkSelectionChoiceBox, HashMap<String, String> chunkZipPathPairs) {
        this.selectedChunkXML = selectedChunkXML;
        this.psxFilePath = psxFilePath;
        this.chunkID = chunkID;
        this.chunkZipPathPairs = chunkZipPathPairs;

        //add chunk name to tree
        Node chunk = this.selectedChunkXML.getElementsByTagName("chunk").item(0);
        Element chunkElement = (Element) chunk;

        this.chunkName = chunkElement.getAttribute("label");
        TreeItem<String> rootItem = new TreeItem<>(this.chunkName);

        chunkTreeView.setRoot(rootItem);

        //initialize options in new chunk selection choice box (cannot be done before chunkName is set)
        initializeChoiceBox(chunkSelectionChoiceBox);
        this.newChunkSelectionChoiceBox.setOnAction(this::updateSelectChunkButton);

        //disable select chunk button if selected chunk (in choice box) matches the current chunk
        updateSelectChunkButton();

        //unzip thumbnail folder
        String psxPathBase = psxFilePath.substring(0, psxFilePath.length() - 4);//remove ".psx" from path

        String thumbnailPath = psxPathBase + ".files\\" + chunkID + "\\0\\thumbnails\\thumbnails.zip";
        ArrayList<Image> thumbnailImageList = UnzipHelper.unzipImages(thumbnailPath);

        //unzip frame.zip
        String frameZipPath = psxPathBase + ".files\\" + chunkID + "\\0\\frame.zip";
        try {
            this.frameZip = UnzipHelper.unzipToDocument(frameZipPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //add full-res images as children to the chunk name
        NodeList cameras = this.selectedChunkXML.getElementsByTagName("camera");
        for (int i = 0; i < cameras.getLength(); ++i) {
            Node camera = cameras.item(i);

            if (camera.getNodeType() == Node.ELEMENT_NODE) {
                Element cameraElement = (Element) camera;
                String imageName = cameraElement.getAttribute("id");

                ImageView thumbnailImgView;
                try {
                    thumbnailImgView = new ImageView(thumbnailImageList.get(i));
                } catch (IndexOutOfBoundsException e) {
                    //thumbnail not found in thumbnailImageList
                    thumbnailImgView = new ImageView(new Image(new File("question-mark.png").toURI().toString()));
                }
                thumbnailImgView.setFitWidth(30);//TODO: SCALE SIZE INSTEAD OF HARD CODING?
                thumbnailImgView.setFitHeight(30);

                TreeItem<String> imageTreeItem = new TreeItem<>(imageName, thumbnailImgView);
                rootItem.getChildren().add(imageTreeItem);
            }
        }

        //unroll treeview
        chunkTreeView.getRoot().setExpanded(true);
    }

    public void selectChunk(ActionEvent actionEvent) throws IOException {
        String selectedChunk = this.newChunkSelectionChoiceBox.getValue();

        if (!selectedChunk.equals(chunkName)) {//only change scene if switching to new chunk
            String selectedChunkZip = chunkZipPathPairs.get(selectedChunk);
            int newChunkID = UnzipHelper.getChunkIdFromZipPath(selectedChunkZip);
            Document newSelectedChunkXML = UnzipHelper.convertStringToDocument(
                    UnzipHelper.unzipToString(selectedChunkZip));//path --> XML as String --> XML document

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/menubar/ChunkViewer.fxml"));
            root = fxmlLoader.load();
            ChunkViewerController chunkViewerController = fxmlLoader.getController();
            chunkViewerController.initializeChunkSelectionAndTreeView(newSelectedChunkXML, psxFilePath, newChunkID, this.newChunkSelectionChoiceBox, chunkZipPathPairs);
            stage = (Stage) ((javafx.scene.Node) actionEvent.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }


    private void initializeChoiceBox(ChoiceBox<String> chunkSelectionChoiceBox) {
        //add all items from old checkbox to new checkbox
        this.newChunkSelectionChoiceBox.getItems().addAll(chunkSelectionChoiceBox.getItems());

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

    public void selectImageInTreeView() {
        //selectedItem holds the cameraID associated with the image
        TreeItem<String> selectedItem = chunkTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            String cameraID = selectedItem.getValue();
            try {
                NodeList cameras = frameZip.getElementsByTagName("frame").
                        item(0).getChildNodes().
                        item(1).getChildNodes();


                Element selectedItemCam = null;
                for (int i = 0; i < cameras.getLength(); ++i) {
                    //cameras also holds text fields associated with the cameras, so filter them out
                    if (cameras.item(i).getNodeName().equals("camera")) {
                        Element camera = (Element) cameras.item(i);

                        if (camera.getAttribute("camera_id").equals(cameraID)) {
                            selectedItemCam = camera;
                        }
                    }
                }

                if (selectedItemCam != null) {
                    Element photo = (Element) selectedItemCam.getElementsByTagName("photo").item(0);
                    String path = photo.getAttribute("path");
                    //example path: "../../../160518_mia337_114828_a_ding/160517_mia337_2013_9_7a_ding_nearFocus_R1_C4_0_30.jpg"

                    //need to replace ../../../ with the parent of the .psx file
                    File psxFile = new File(psxFilePath);
                    String parentPath = psxFile.getParentFile().getAbsolutePath();
                    path = parentPath + "\\" + path.substring(9, path.length());

                    //String path now holds the full path to the selected thumbnail's full-res image
                    File imgFile = new File(path);

                    //set imageview to selected image
                    if (imgFile.exists()) {
                        chunkViewerImgView.setImage(new Image(imgFile.toURI().toString()));

                        //set label to: psx name + chunk name + cameraID

                        imgViewLabel.setText(psxFile.getName() + "\n" + chunkName + ": Image " + cameraID);
                    } else {
                        //use thumbnail as main image if main image not found
                        chunkViewerImgView.setImage(selectedItem.getGraphic().
                                snapshot(new SnapshotParameters(), null));
                        imgViewLabel.setText("Image not found.");
                    }
                }
                //TODO: .TIF (and maybe other image types) ARE NOT SUPPORTED. CONVERT THESE?
                //TODO: AFTER SELECTION, PROGRAM WILL FREEZE TO DOWNLOAD IMAGES FROM ONEDRIVE
            } catch (IllegalArgumentException e) {//could not find image
                e.printStackTrace();
            }
        }
    }

    private boolean isValidImageType(String path) {
        for (String extension : validExtensions) {
            if (path.matches("." + extension)) {
                return true;
            }
        }
        return false;
    }

    public void updateSelectChunkButton(ActionEvent actionEvent) {
        //need to keep the unused ActionEvent so we can link this method to the choice box
        String selectedChunk = this.newChunkSelectionChoiceBox.getValue();

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