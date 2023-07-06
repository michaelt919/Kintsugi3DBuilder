package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import tetzlaff.gl.util.UnzipHelper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class UnzipFileSelectionController {
    @FXML
    public Button unzipPSXButton;

    private File selectedFile;
    private Stage stage;

    private DirectoryChooser directoryChooser;

    @FXML
    public TextField psxPathTxtField;
    @FXML
    public ChoiceBox<String> chunkSelectionChoiceBox;

    @FXML
    public Button selectChunkButton;

    @FXML
    private TextField outputDirectoryPathTxtField;

    private Scene scene;
    private Parent root;

    //key is chunk name, value is path to chunk's zip file
    private HashMap<String, String> chunkZipPathPairs;

    public UnzipFileSelectionController() {
        chunkZipPathPairs = new HashMap<>();
    }

    public void init(){
        this.directoryChooser = new DirectoryChooser();
        chunkSelectionChoiceBox.setDisable(true);
        selectChunkButton.setDisable(true);
    }

    public void selectPSX() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose .psx file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Metashape files", "*.psx"));

        stage = (Stage) unzipPSXButton.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if(selectedFile != null){
            psxPathTxtField.setText(selectedFile.getAbsolutePath());
        }
    }

    public void unzipPSXAndParse() {
        //open .psx as an XML file and grab the path attribute from the document tag
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //TODO: MAY BE PRONE TO XXE ATTACKS

        if(isValidPSXFilePath()){
            try{
                DocumentBuilder builder = factory.newDocumentBuilder();

                String psxFilePath = psxPathTxtField.getText();

                //Get Document from .psx file
                Document psxDocument = builder.parse(new File(psxFilePath));

                //get the path attribute from the document tag
                NodeList nodes = psxDocument.getElementsByTagName("document");
                Element documentTag = (Element) nodes.item(0);

                //this gives "{projectname}.files/project.zip"
                //need to replace {projectname} with full path (except .psx)
                String documentPathInfo = documentTag.getAttribute("path");

                documentPathInfo = documentPathInfo.substring(14);
                documentPathInfo = psxFilePath.substring(0, psxFilePath.length() - 3) + documentPathInfo;

                //extract project.zip and open the doc.xml
                String projectZipString = UnzipHelper.unzipToString(documentPathInfo);
                Document docXML = UnzipHelper.convertStringToDocument(projectZipString);

                //find the chunks and open the .zip for each chunk
                NodeList chunkList = docXML.getElementsByTagName("chunk");
                String chunkZipPath;
                chunkSelectionChoiceBox.getItems().clear();
                chunkZipPathPairs.clear();
                for(int i = 0; i < chunkList.getLength(); ++i) {//add all chunks to choice box
                    Node chunk = chunkList.item(i);

                    if (chunk.getNodeType() == Node.ELEMENT_NODE) {
                        Element chunkElement = (Element) chunk;

                        //open doc.xml within each chunk and read the chunk's label attribute --> display it to user
                        chunkZipPath = chunkElement.getAttribute("path"); //gives xx/chunk.zip where xx is a number

                        //append this path to the psxFilePath (without that path's .psx)
                        chunkZipPath = psxFilePath.substring(0, psxFilePath.length() - 4) + ".files\\" + chunkZipPath;

                        Document chunkDocument = UnzipHelper.convertStringToDocument(
                                UnzipHelper.unzipToString(chunkZipPath));//path --> XML as String --> XML document

                        NodeList innerChunkList = chunkDocument.getElementsByTagName("chunk");
                        for(int j = 0; j < innerChunkList.getLength(); ++j) {
                            Node innerChunk = innerChunkList.item(j);

                            if (innerChunk.getNodeType() == Node.ELEMENT_NODE) {
                                Element innerChunkElement = (Element) innerChunk;
                                String chunkName = innerChunkElement.getAttribute("label");
                                chunkSelectionChoiceBox.getItems().add(chunkName);
                                chunkZipPathPairs.put(chunkName, chunkZipPath);
                            }
                        }
                    }
                    chunkSelectionChoiceBox.setDisable(false);
                    selectChunkButton.setDisable(false);

                    //initialize choice box to first option instead of null option
                    if (chunkSelectionChoiceBox.getItems() != null &&
                        chunkSelectionChoiceBox.getItems().get(0) != null){
                        chunkSelectionChoiceBox.setValue(chunkSelectionChoiceBox.getItems().get(0));
                    }
                }
            } catch (ParserConfigurationException|IOException|SAXException e) {
                e.printStackTrace();
            }
        }
        else{//invalid .psx path
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private boolean isValidPSXFilePath() {
        String path = psxPathTxtField.getText();
        File file = new File(path);
        return file.exists() && file.getAbsolutePath().endsWith(".psx");
    }

    public void selectOutputDirectory() {//TODO: REMOVE THIS?
        //if this is removed, also remove the necessary items from UnzipFileSelection.fxml
        this.directoryChooser.setTitle("Choose an output directory");

        stage = (Stage) outputDirectoryPathTxtField.getScene().getWindow();
        File file = this.directoryChooser.showDialog(stage.getOwner());

        if (file != null && file.exists()){
            directoryChooser.setInitialDirectory(file);
            outputDirectoryPathTxtField.setText(file.getAbsolutePath());
        }
        else{
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void selectChunk(ActionEvent actionEvent) {
        String selectedChunkZip = chunkZipPathPairs.get(chunkSelectionChoiceBox.getValue());
        int chunkID = UnzipHelper.getChunkIdFromZipPath(selectedChunkZip);

        Document selectedChunkXML;
        try {
            selectedChunkXML = UnzipHelper.convertStringToDocument(
                    UnzipHelper.unzipToString(selectedChunkZip));//path --> XML as String --> XML document

            //load chunk viewer window
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/menubar/ChunkViewer.fxml"));
            root = fxmlLoader.load();
            ChunkViewerController chunkViewerController = fxmlLoader.getController();
            chunkViewerController.initializeChunkSelectionAndTreeView(selectedChunkXML, psxPathTxtField.getText(),
                    chunkID, chunkSelectionChoiceBox, chunkZipPathPairs);
        }
        catch (Exception e){
            unzipPSXButton.fire();//selected .psx file and list of chunks may be referring to different objects
                                    //if chunk selection fails, try unzipping the file again
                                    //this action will also update the chunk selection choice box
            return;//do not load new window for chunk viewer
        }

        stage = (Stage) ((javafx.scene.Node) actionEvent.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void enterToRun(KeyEvent keyEvent) {//press the enter button while in the text field to unzip
        if (keyEvent.getCode() == KeyCode.ENTER) {
            unzipPSXButton.fire();
        }
    }
}
