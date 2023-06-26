package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import tetzlaff.gl.util.UnzipHelper;

import javax.tools.Tool;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class UnzipFileSelectionController {
    @FXML
    public Button runButton;

    private File selectedFile;
    private Stage stage;

    private DirectoryChooser directoryChooser;

    @FXML
    public TextField psxPathTxtField;
    @FXML
    private TextField outputDirectoryPathTxtField;
    public void init(){
        this.directoryChooser = new DirectoryChooser();
    }

    public void selectPSX(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose .psx file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Metashape files", "*.psx"));

        stage = (Stage) runButton.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if(selectedFile != null){
            psxPathTxtField.setText(selectedFile.getAbsolutePath());
        }
    }

    public void parseXML(ActionEvent actionEvent) {
        //TODO: NEED TO ADD NULL SAFETY AND GENERAL CLEANUP/USER INPUT PROTECTION
        //open .psx as an XML file and grab the path attribute from the document tag
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //TODO: SONARLIST IS COMPLAINING ABOUT XXE ATTACKS

        try{
            DocumentBuilder builder = factory.newDocumentBuilder();

            String psxFilePath = psxPathTxtField.getText();

            //Get Document from .psx file
            Document document = builder.parse(new File(psxFilePath));

            //get the path attribute from the document tag
            NodeList nodes = document.getElementsByTagName("document");
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
            for(int i = 0; i < chunkList.getLength(); ++i) {
                Node chunk = chunkList.item(i);

                if (chunk.getNodeType() == Node.ELEMENT_NODE) {
                    Element chunkElement = (Element) chunk;

                    //open doc.xml within each chunk and read the chunk's label attribute --> display it to user
                    chunkZipPath = chunkElement.getAttribute("path"); //gives xx/chunk.zip where xx is a number
                    chunkZipPath = psxFilePath.substring(0, psxFilePath.length() - 4) + ".files\\" + chunkZipPath;//append this path to the psxFilePath (without that path's .psx)

                    Document chunkDocument = UnzipHelper.convertStringToDocument(UnzipHelper.unzipToString(chunkZipPath));//path --> String --> XML document
                    chunkDocument.getDocumentElement().normalize();

                    NodeList innerChunkList = chunkDocument.getElementsByTagName("chunk");
                    for(int j = 0; j < innerChunkList.getLength(); ++j) {
                        Node innerChunk = innerChunkList.item(j);

                        if (innerChunk.getNodeType() == Node.ELEMENT_NODE) {
                            Element innerChunkElement = (Element) innerChunk;
                            System.out.println(innerChunkElement.getAttribute("label"));
                        }
                    }
                }
                //user selects a chunk to load --> treat that chunk's doc.xml as the camera's xml file
                //TODO: IMPLEMENT CHUNK SELECTION
            }

        } catch (ParserConfigurationException|IOException|SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean loadPSXFromTxtField() {
        String path = psxPathTxtField.getText();

        File tempFile = new File(path);

        if (tempFile.exists()) {
            selectedFile = tempFile;
            return true;//file exists
        }
        else{
            return false;//file does not exist
        }
    }

    public void selectOutputDirectory(ActionEvent actionEvent) {
        this.directoryChooser.setTitle("Choose an output directory");

        stage = (Stage) outputDirectoryPathTxtField.getScene().getWindow();
        File file = this.directoryChooser.showDialog(stage.getOwner());
//TODO: NULL POINTER EXCEPTION IF THE USER CANCELS THE REQUEST TO SPECIFY A DIRECTORY
        if (file.exists()){
            directoryChooser.setInitialDirectory(file);
            outputDirectoryPathTxtField.setText(file.getAbsolutePath());
        }
        else{
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
