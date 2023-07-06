package tetzlaff.ibrelight.javafx.controllers.menubar;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import tetzlaff.gl.util.UnzipHelper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MetashapeObject {
    private String psxFilePath;

    //key is chunk name, value is path to chunk's zip file
    private HashMap<String, String> chunkZipPathPairs;

    public List<String> getChunkNamesFromPSX(String psxFilePath) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //TODO: MAY BE PRONE TO XXE ATTACKS

        if (isValidPSXFilePath(psxFilePath)) {
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();

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
                ArrayList<String> chunkNames = new ArrayList<>();
                chunkZipPathPairs.clear();
                for (int i = 0; i < chunkList.getLength(); ++i) {//add all chunks to chunkNames list
                    Node chunk = chunkList.item(i);

                    if (chunk.getNodeType() == Node.ELEMENT_NODE) {
                        Element chunkElement = (Element) chunk;

                        //open doc.xml within each chunk and read the chunk's label attribute --> display it to user
                        chunkZipPath = chunkElement.getAttribute("path"); //gives xx/chunk.zip where xx is a number

                        //append this path to the psxFilePath (without that path's .psx)
                        chunkZipPath = psxFilePath.substring(0, psxFilePath.length() - 4) + ".files\\" + chunkZipPath;

                        Document chunkDocument = UnzipHelper.unzipToDocument(chunkZipPath);//path --> XML as String --> XML document

                        NodeList innerChunkList = chunkDocument.getElementsByTagName("chunk");
                        for (int j = 0; j < innerChunkList.getLength(); ++j) {
                            Node innerChunk = innerChunkList.item(j);

                            if (innerChunk.getNodeType() == Node.ELEMENT_NODE) {
                                Element innerChunkElement = (Element) innerChunk;
                                String chunkName = innerChunkElement.getAttribute("label");
                                chunkNames.add(chunkName);
                                chunkZipPathPairs.put(chunkName, chunkZipPath);
                            }
                        }
                    }
                }
                return chunkNames;
            } catch (ParserConfigurationException | IOException | SAXException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        } else {//invalid .psx path
            return new ArrayList<>();
        }
    }

    private boolean isValidPSXFilePath(String path) {
        File file = new File(path);
        return file.exists() && file.getAbsolutePath().endsWith(".psx");
    }
}
