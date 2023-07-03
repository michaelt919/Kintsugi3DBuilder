package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javafx.scene.image.ImageView;
import tetzlaff.gl.util.UnzipHelper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;


public class ChunkViewerController implements Initializable {
    //TODO: --> "INFO: index exceeds maxCellCount. Check size calculations for class javafx.scene.control.skin.TreeViewSkin$1"
    //suppress warning?

    @FXML
    public TreeView chunkTreeView;

    @FXML
    public ImageView chunkViewerImgView;
    public Label imgViewLabel;
    private Document selectedChunkXML;

    static final String[] validExtensions = {"*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.png", "*.bmp", "*.wbmp"};
    private String psxFilePath;
    private Document frameZip;

    private int chunkID;//TODO: MOVE THIS IF IT IS ONLY USED IN ONE FUNCTION

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //nothing needed here so far
    }

    public void initializeChunkSelectionAndTreeView(Document selectedChunkXML, String psxFilePath, int chunkID){
        this.selectedChunkXML = selectedChunkXML;
        this.psxFilePath = psxFilePath;
        this.chunkID = chunkID;

        //add chunk name to tree
        Node chunk = this.selectedChunkXML.getElementsByTagName("chunk").item(0);
        Element chunkElement = (Element) chunk;
        TreeItem<String> rootItem = new TreeItem<>(chunkElement.getAttribute("label"));
        chunkTreeView.setRoot(rootItem);

        //unzip thumbnail folder
        String psxPathBase = psxFilePath.substring(0, psxFilePath.length() - 4);//remove ".psx" from path

        String thumbnailPath = psxPathBase + ".files\\" + chunkID + "\\0\\thumbnails\\thumbnails.zip";
        ArrayList<Image> thumbnailImageList = UnzipHelper.unzipImages(thumbnailPath);

        //unzip frame.zip
        String frameZipPath = psxPathBase + ".files\\" + chunkID + "\\0\\frame.zip";
        try {
            this.frameZip = UnzipHelper.unzipToDocument(frameZipPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //add full-res images as children to the chunk name
        NodeList cameras = this.selectedChunkXML.getElementsByTagName("camera");
        for(int i = 0; i < cameras.getLength(); ++i) {
            Node camera = cameras.item(i);

            if (camera.getNodeType() == Node.ELEMENT_NODE) {
                Element cameraElement = (Element) camera;
                String imageName = cameraElement.getAttribute("id");

                ImageView thumbnailImgView;
                try{
                    thumbnailImgView = new ImageView(thumbnailImageList.get(i));
                }
                catch (Exception e){//TODO: DIFFERENT EXCEPTION?
                    thumbnailImgView = new ImageView(new Image(new File("question-mark.png").toURI().toString()));
                }
                thumbnailImgView.setFitWidth(30);//TODO: SCALE SIZE INSTEAD OF HARD CODING?
                thumbnailImgView.setFitHeight(30);

                TreeItem<String> imageTreeItem = new TreeItem<>(imageName, thumbnailImgView);
                rootItem.getChildren().add(imageTreeItem);
            }
        }
    }

    public void selectItem() {
        //selectedItem holds the cameraID associated with the image
        TreeItem<String> selectedItem = (TreeItem<String>) chunkTreeView.getSelectionModel().getSelectedItem();
        if(selectedItem != null){
            String cameraID = selectedItem.getValue();
                try{
                    NodeList cameras = frameZip.getElementsByTagName("frame").
                            item(0).getChildNodes().
                            item(1).getChildNodes();


                    Element selectedItemCam = null;
                    for (int i = 0; i < cameras.getLength(); ++i){
                        //cameras also holds text fields associated with the cameras, so filter them out
                        if (cameras.item(i).getNodeName().equals("camera")) {
                            Element camera = (Element) cameras.item(i);

                            if (camera.getAttribute("camera_id").equals(cameraID)) {
                                selectedItemCam = camera;
                            }
                        }
                    }

                    if (selectedItemCam != null){
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
                            imgViewLabel.setText(cameraID);
                        }
                        else{
                            //use thumbnail as main image if main image not found
                            chunkViewerImgView.setImage(selectedItem.getGraphic().
                                    snapshot(new SnapshotParameters(), null));
                            imgViewLabel.setText("Image not found.");
                        }
                    }
                    //TODO: .TIF (and maybe other image types) ARE NOT SUPPORTED. CONVERT THESE?
                    //TODO: AFTER SELECTION, PROGRAM WILL FREEZE TO DOWNLOAD IMAGES FROM ONEDRIVE
                }
                catch(IllegalArgumentException e){//could not find image
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

}