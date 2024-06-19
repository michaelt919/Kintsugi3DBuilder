/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import kintsugi3d.gl.util.UnzipHelper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetashapeObject {
    private static final Logger log = LoggerFactory.getLogger(MetashapeObject.class);

    private String psxFilePath;

    //key is chunk name, value is path to chunk's zip file
    private HashMap<String, String> chunkZipPathPairs;
    private ArrayList<String> chunkNames;

    public MetashapeObject(){
        psxFilePath = "";
        chunkZipPathPairs = new HashMap<>();
        chunkNames = new ArrayList<>();
    }

    public MetashapeObject(String path){
        loadChunkNamesFromPSX(path);
    }

    public List<String> loadChunkNamesFromPSX(String psxFilePath) {
        //return the chunk names stored in the .psx file (and assigns them to this.chunkNames)
        //also initializes this.psxFilePath to psxFilePath
        //also puts values into this.chunkZipPathPairs

        if (isValidPSXFilePath(psxFilePath)) {
            try {
                this.psxFilePath = psxFilePath;
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                //TODO: MAY BE PRONE TO XXE ATTACKS

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

                //...chunk.zip holds the information we need to extract (chunk name, id, etc)

                String chunkZipPath;
                chunkNames = new ArrayList<>();
                chunkZipPathPairs = new HashMap<>();
                for (int i = 0; i < chunkList.getLength(); ++i) {//add all chunks to chunkNames list
                    Node chunk = chunkList.item(i);

                    if (chunk.getNodeType() == Node.ELEMENT_NODE) {
                        //chunkElement holds simple info
                        //<chunk id="0" path="0/chunk.zip"/>
                        Element chunkElement = (Element) chunk;

                        //open doc.xml within each chunk and read the chunk's label attribute --> display it to user
                        chunkZipPath = chunkElement.getAttribute("path"); //gives xx/chunk.zip where xx is a number

                        //append this path to the psxFilePath (without ".psx" at the end)
                        chunkZipPath = psxFilePath.substring(0, psxFilePath.length() - 4) + ".files\\" + chunkZipPath;

                        //fullChunkDocument has info about chunk name, cameras, images, etc.
                        Document fullChunkDocument = UnzipHelper.unzipToDocument(chunkZipPath);

                        //only one chunk in this inner chunk document, so no need for a for loop
                        Element fullChunkElement = (Element) fullChunkDocument.getElementsByTagName("chunk").item(0);

                        String chunkName = fullChunkElement.getAttribute("label");
                        chunkNames.add(chunkName);
                        chunkZipPathPairs.put(chunkName, chunkZipPath);
                    }
                }
            } catch (ParserConfigurationException | IOException | SAXException e) {
                log.error("An error occurred:", e);
            }
        }
        return chunkNames;
    }

    public List<String> getChunkNames(){
        return chunkNames;
    }

    public Map<String, String> getChunkZipPathPairs(){
        return chunkZipPathPairs;
    }

    public String getPsxFilePath() {
        return psxFilePath;
    }

    public void setPsxFilePath(String path){
        psxFilePath = path;
    }

    private boolean isValidPSXFilePath(String path) {
        File file = new File(path);
        return file.exists() && file.getAbsolutePath().endsWith(".psx");
    }
}
