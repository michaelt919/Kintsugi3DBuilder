/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar.metashape;

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
import java.util.*;

public class MetashapeDocument {
    private static final Logger log = LoggerFactory.getLogger(MetashapeDocument.class);

    private String psxFilePath;

    private List<MetashapeChunk> chunks;

    private int activeChunkID;
    private Document projectZipXML;

    //hide useless constructor
    private MetashapeDocument(){
    }

    public MetashapeDocument(String path){
        this.psxFilePath = path;

        if (!isValidPSXFilePath(psxFilePath)) {return;}

        try {
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

            documentPathInfo = documentPathInfo.substring(13);
            documentPathInfo = getPSXPathBase() + documentPathInfo;

            //extract project.zip and open the doc.xml
            String projectZipString = UnzipHelper.unzipToString(documentPathInfo);
            projectZipXML = UnzipHelper.convertStringToDocument(projectZipString);

            if (projectZipXML == null){return ;}

            //set active chunk id if the project has one
            NodeList chunksWrapper = projectZipXML.getElementsByTagName("chunks");
            if (chunksWrapper.getLength()>0){
                Element chunkWrapperElem = (Element) chunksWrapper.item(0);
                String activeID = chunkWrapperElem.getAttribute("active_id");
                if (!activeID.isBlank()){
                    activeChunkID = Integer.parseInt(activeID);
                }
            }

            //find the chunks and open the .zip for each chunk
            NodeList chunkList = projectZipXML.getElementsByTagName("chunk");

            loadChunks(chunkList);
        }
        catch (ParserConfigurationException | IOException | SAXException e) {
            log.error("An error occurred:", e);
        }
    }

    public String getChunkNameFromID(int id) {
        Optional<MetashapeChunk> chunk = chunks.stream()
                .filter(c -> c.getID() == id)
                .findFirst();

        if (chunk.isPresent()){
            return chunk.get().getLabel();
        }
        return "";
//        //open project.zip
//        //get path from appropriate chunk id
//        //open chunk.zip
//        //TODO: do better logging here
//        NodeList chunks = projectZipXML.getElementsByTagName("chunk");
//        if (chunks == null || chunks.getLength() == 0){return "";}
//
//        String chunkPath = "";
//        for (int i = 0; i < chunks.getLength(); ++i){
//            Element elem = (Element) chunks.item(i);
//            String chunkElemID = elem.getAttribute("id");
//            if (Integer.parseInt(chunkElemID) == id){
//                chunkPath = elem.getAttribute("path");
//                break;
//            }
//        }
//
//        if (chunkPath.isBlank()){return "";}
//
//        chunkPath = new File(new File(getPSXPathBase() + ".files"), chunkPath).getPath();
//        try {
//            Document chunkXML = UnzipHelper.unzipToDocument(chunkPath);
//            NodeList chunkWrapper = chunkXML.getElementsByTagName("chunk");
//            if (chunkWrapper!= null && chunkWrapper.getLength() >0){
//                Element chunkElem = (Element) chunkWrapper.item(0);
//                return chunkElem.getAttribute("label");
//            }
//            else{
//                return "";
//            }
//        } catch (IOException e) {
//            log.error("Could not open chunk zip document", e);
//            return "";
//        }
    }

    String getPSXPathBase() {
        return psxFilePath.substring(0, psxFilePath.length() - 4);
    }

    private void loadChunks(NodeList chunkList) throws IOException {
        chunks = new ArrayList<>();
        for (int i = 0; i < chunkList.getLength(); ++i) {
            Node chunk = chunkList.item(i);

            if (chunk.getNodeType() == Node.ELEMENT_NODE) {
                Element chunkElement = (Element) chunk;

                //TODO: set info about which chunk is active here
                chunks.add(MetashapeChunk.parseFromElement(this, chunkElement));
            }
        }
    }

    public Integer getActiveChunkID(){
        return activeChunkID;
    }

    private boolean isValidPSXFilePath(String path) {
        File file = new File(path);
        return file.exists() && file.getAbsolutePath().endsWith(".psx");
    }

    public File getPsxFile() {
        return new File(psxFilePath);
    }

    public String getPsxFilePath() {
        return psxFilePath;
    }

    public List<MetashapeChunk> getChunks() {
        return chunks;
    }

    public void selectChunk(String selectedChunkLabel) {
        Optional<MetashapeChunk> selectedChunk = chunks.stream()
                .filter(chunk -> chunk.getLabel().equals(selectedChunkLabel))
                .findFirst();

        if (selectedChunk.isEmpty()){
            return;
        }

        this.activeChunkID = selectedChunk.get().getID();
    }

    public MetashapeChunk getSelectedChunk() {
        return chunks.stream()
                .filter(chunk -> chunk.getID() == activeChunkID)
                .findFirst()
                .orElse(null);
    }
}
