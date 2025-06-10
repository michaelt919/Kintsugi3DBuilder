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

import javafx.scene.image.Image;
import kintsugi3d.gl.util.UnzipHelper;
import kintsugi3d.util.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public class MetashapeChunk {
    private static final Logger log = LoggerFactory.getLogger(MetashapeChunk.class);

    //contains a metashape object and a specific chunk
    private MetashapeDocument metashapeDocument;
    private String chunkZipXmlPath;
    private String chunkName;
    private int chunkID;

    private Document chunkXML;
    private Document frameXML;

    private List<MetashapeModel> models = new ArrayList<>();

    private Optional<Integer> defaultModelID;
    private String currModelID;
    private LoadPreferences loadPreferences;

    public MetashapeChunk(MetashapeDocument metashapeDocument, String chunkName, String currModelID) {
        this.metashapeDocument = metashapeDocument;
        this.currModelID = currModelID;
        this.loadPreferences = new LoadPreferences();

        updateChunk(chunkName);
    }

    public void updateChunk(String chunkName) {
        this.chunkName = chunkName;

        this.chunkZipXmlPath = metashapeDocument.getChunkZipPathPairs().get(chunkName);

        //set chunk xml
        try {
            this.chunkXML = UnzipHelper.unzipToDocument(this.chunkZipXmlPath);
        } catch (IOException e) {
            this.chunkXML = null;
            throw new RuntimeException(e);
        }

        //set chunkID
        this.chunkID = getChunkIdFromZipPath();

        //unzip frame.zip

        //the 0 means that the program searches for info regarding frame 0
        String frameZipPath = getFramePath();

        try {
            this.frameXML = UnzipHelper.unzipToDocument(frameZipPath);
        } catch (IOException e) {
            log.error("An error occurred loading Metashape chunk:", e);
        }

        loadModelInfo();
    }

    public String getChunkZipXmlPath() { return chunkZipXmlPath; }
    public Document getChunkXML() { return chunkXML; }
    public Document getFrameXML() { return frameXML; }

    public String getChunkDirectoryPath() {
        String psxFilePath = this.metashapeDocument.getPsxFilePath();
        String psxPathBase = psxFilePath.substring(0, psxFilePath.length() - 4); //remove ".psx" from path
        return new File(new File(psxPathBase + ".files"), Integer.toString(chunkID)).getPath();
    }
    public String getFrameDirectoryPath() { return new File(new File(getChunkDirectoryPath()), "0").getPath(); }
    public String getFramePath() { return new File(new File(getFrameDirectoryPath()), "frame.zip").getPath(); }


    private MetashapeChunk(){
        //hide useless constructor
    }


    public class LoadPreferences{
        public File fullResOverride;
        public boolean doSkipMissingCams;
        public String orientationViewName;
        public double orientationViewRotateDegrees;
    }

    public LoadPreferences getLoadPreferences(){return loadPreferences;}

    private void loadModelInfo() {
        //get default model id if the chunk has one
        try {
            Element elem1 = (Element) chunkXML.getElementsByTagName("models").item(0);
            this.defaultModelID = Optional.of(Integer.parseInt(elem1.getAttribute("active_id")));
        }
        catch(NumberFormatException | NullPointerException e){
            log.warn("Could not find active id for " + this.getPsxFilePath(), e);
            this.defaultModelID = Optional.empty();
        }

        NodeList modelList = chunkXML.getElementsByTagName("model");

        models = new ArrayList<>();
        for(int i = 0; i < modelList.getLength(); ++i){
            Element elem = (Element) modelList.item(i);
            models.add(MetashapeModel.parseFromElement(this, elem));
        }
    }


    public String getChunkName() {
        return this.chunkName;
    }

    private int getChunkIdFromZipPath() {
        //example chunk zip path ----> ...GuanYu_with_ground_truth.files\0\chunk.zip
        //want to extract the 0 in this example because that number denotes the chunkID
        File file = new File(chunkZipXmlPath);

        //parent file would give "...GuanYu_with_ground_truth.files\0" in this case
        File parentFile = new File(file.getParent());

        try{
            this.chunkID = Integer.parseInt(parentFile.getName());
            return this.chunkID;
        }
        catch (NumberFormatException nfe){
            log.error("An error occurred parsing chunk:", nfe);
            return -1;
        }
    }

    public String getPsxFilePath() {
        return this.metashapeDocument.getPsxFilePath();
    }

    public MetashapeDocument getMetashapeObject() {
        return this.metashapeDocument;
    }

    public List<Image> loadThumbnailImageList() {
        //unzip thumbnail folder

        //Note: the 0 denotes that these thumbnails are for frame 0
        //TODO: can get this info from thumbnails tag in frameXML instead of hard coding
        String thumbnailPath = new File(new File(getFrameDirectoryPath(), "thumbnails"), "thumbnails.zip").getPath();
        return UnzipHelper.unzipImages(thumbnailPath);
    }

    public List<Element> findAllCameras() {
        NodeList cams = this.chunkXML.getElementsByTagName("camera");
        ArrayList<Element> cameras = new ArrayList<>();
        for (int i = 0; i < cams.getLength(); ++i) {
            Node camera = cams.item(i);
            if (camera.getNodeType() == Node.ELEMENT_NODE) {
                cameras.add((Element)camera);
            }
        }
        return cameras;
    }

    public List<Element> findEnabledCameras() {
        List<Element> allCams = findAllCameras();
        List<Element> enabledCams = new ArrayList<>();

        for(Element cam : allCams){
            String enabled = cam.getAttribute("enabled");

            if (enabled.equals("true") ||
                enabled.equals("1") ||
                    enabled.isEmpty() /*cam is enabled by default*/){
                enabledCams.add(cam);
            }
        }
        return enabledCams;
    }

    public File findFullResImgDirectory(){
        //get first camera path from frame xml
        //assume that path is relative to parent of .psx file path

        try{
            NodeList frameCams = frameXML.getElementsByTagName("camera");

            //this will probably exit after the first camera
            for(int i = 0; i < frameCams.getLength(); ++i){
                Element cam = (Element) frameCams.item(i);

                if(cam.getNodeType() != Node.ELEMENT_NODE){continue;}

                String pathAttribute = ((Element) cam.getElementsByTagName("photo").item(0)).getAttribute("path");
                return new File(getPsxFile().getParent(), pathAttribute).getParentFile();
            }
        }
        catch(NumberFormatException nfe){
            log.warn("Failed to find full res directory for Metashape Project.", nfe);
        }

        return null;
    }

    public String getCurrentModelPath() {
        for (MetashapeModel model : models){
            if (model.getId().isPresent() &&
            model.getId().equals(defaultModelID)){
                return model.getPath();
            }
        }
        return "";
    }

    public Optional<Integer> getDefaultModelID(){return defaultModelID;}

    public String getCurrModelID(){return currModelID;}

    public File getPsxFile() {
        return metashapeDocument.getPsxFile();
    }

    public boolean equals(Object rhs){
        if (!(rhs instanceof MetashapeChunk)){
            return false;
        }

        MetashapeChunk moc = (MetashapeChunk) rhs;
        //chunk name is the same
        //psx path is the same

        //TODO: may need to revisit this method if more precise criteria are needed

        return this.chunkName.equals(moc.getChunkName()) &&
                this.getPsxFilePath().equals(moc.getPsxFilePath());
    }
    public List<MetashapeModel> getModels() {
        return models;
    }
}
