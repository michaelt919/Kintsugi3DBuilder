/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.scene.image.Image;
import kintsugi3d.util.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import kintsugi3d.gl.util.UnzipHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MetashapeObjectChunk {
    private static final Logger log = LoggerFactory.getLogger(MetashapeObjectChunk.class);
    private Integer modelID;

    //contains a metashape object and a specific chunk
    private MetashapeObject metashapeObject;
    private String chunkZipPath;
    private String chunkName;
    private int chunkID;

    private Document chunkXML;
    private Document frameZip;

    private ArrayList<Triplet<Integer, String, String>> modelInfo = new ArrayList<>();
    private Integer activeModelID; //TODO: is this equivalent to default model id?

    public String getChunkZipPath() { return chunkZipPath; }
    public Document getChunkXML() { return chunkXML; }
    public Document getFrameZip() { return frameZip; }

    public String getChunkDirectoryPath() {
        String psxFilePath = this.metashapeObject.getPsxFilePath();
        String psxPathBase = psxFilePath.substring(0, psxFilePath.length() - 4); //remove ".psx" from path
        return new File(new File(psxPathBase + ".files"), Integer.toString(chunkID)).getPath();
    }
    public String getFrameDirectoryPath() { return new File(new File(getChunkDirectoryPath()), "0").getPath(); }
    public String getFramePath() { return new File(new File(getFrameDirectoryPath()), "frame.zip").getPath(); }


    private MetashapeObjectChunk(){
        metashapeObject = new MetashapeObject();
        chunkName = "";
        chunkID = -1;//TODO: GOOD NULL CHUNK ID?
        chunkXML = null;
        frameZip = null;
        modelID = -1;
    }

    public MetashapeObjectChunk(MetashapeObject metashapeObject, String chunkName, Integer modelID) {
        this.metashapeObject = metashapeObject;
        this.modelID = modelID;

        updateChunk(chunkName);
    }

    public void updateChunk(String chunkName) {
        this.chunkName = chunkName;

        this.chunkZipPath = metashapeObject.getChunkZipPathPairs().get(chunkName);

        //set chunk xml
        try {
            this.chunkXML = UnzipHelper.unzipToDocument(this.chunkZipPath);
        } catch (IOException e) {
            this.chunkXML = null;
            throw new RuntimeException(e);
        }

        //set chunkID
        this.chunkID = getChunkIdFromZipPath();

        //unzip frame.zip
        String psxFilePath = metashapeObject.getPsxFilePath();
        String psxPathBase = psxFilePath.substring(0, psxFilePath.length() - 4); //remove ".psx" from path

        //the 0 means that the program searches for info regarding frame 0
        String frameZipPath = getFramePath();

        try {
            this.frameZip = UnzipHelper.unzipToDocument(frameZipPath);
        } catch (IOException e) {
            log.error("An error occurred loading Metashape chunk:", e);
        }

        //set model info (model num/id, path, and label)
        setModelInfo();
    }

    private void setModelInfo() {
        //open up chunk xml
        //loop through models in chunk xml
        //if id isn't null, pull label info from chunk xml and pull path info from framezip

        //get active id
        try {
            Element elem = (Element) chunkXML.getElementsByTagName("models").item(0);
            this.activeModelID = Integer.parseInt(elem.getAttribute("active_id"));
        }
        catch(NumberFormatException | NullPointerException e){
            log.warn("Could not find active id for " + this.getPsxFilePath(), e);
        }
        
        NodeList modelList = chunkXML.getElementsByTagName("model");

        modelInfo = new ArrayList<>();
        for(int i = 0; i < modelList.getLength(); ++i){
            Element elem = (Element) modelList.item(i);

            Integer tempModelID = null;
            String tempLabel = null;
            String tempPath;
            try{
                tempModelID = Integer.parseInt(elem.getAttribute("id"));
            }
            catch(NumberFormatException nfe){
                log.warn("Model has no id", nfe);
            }
            
            try{
                tempLabel = elem.getAttribute("label");
            }
            catch(NumberFormatException nfe){
                log.warn("Model has no label", nfe);
            }
            
            tempPath = getModelPathFromXML(tempModelID);
            
            modelInfo.add(new Triplet<>(tempModelID, tempLabel, tempPath));
        }

        //if no model info yet, might be a single model in chunk which isn't labeled in chunk xml
        //need to look in frame xml instead
        //default to looking in frame xml first?
        //ex. mia arrowhead
        if (modelInfo.isEmpty()){
            //TODO: needs more work, this is a quick hack to get arrowhead to work
            String path = getModelPathFromXML(null);
            if (!path.isBlank()) {
                modelInfo.add(new Triplet<>(null, "", path));
            }
        }
    }


    public String getChunkName() {
        return this.chunkName;
    }

    private int getChunkIdFromZipPath() {
        //example chunk zip path ----> ...GuanYu_with_ground_truth.files\0\chunk.zip
        //want to extract the 0 in this example because that number denotes the chunkID
        File file = new File(chunkZipPath);

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
        return this.metashapeObject.getPsxFilePath();
    }

    public MetashapeObject getMetashapeObject() {
        return this.metashapeObject;
    }

    public List<Image> loadThumbnailImageList() {
        //unzip thumbnail folder
        String psxFilePath = this.metashapeObject.getPsxFilePath();
        String psxPathBase = psxFilePath.substring(0, psxFilePath.length() - 4);//remove ".psx" from path

        //Note: the 0 denotes that these thumbnails are for frame 0
        //TODO: can get this info from thumbnails tag in frameXML instead of hard coding
        String thumbnailPath = new File(new File(getFrameDirectoryPath(), "thumbnails"), "thumbnails.zip").getPath();
        return UnzipHelper.unzipImages(thumbnailPath);
    }

    public List<Element> findThumbnailCameras() {
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

    public Element matchImageToCam(String imageName) {
        //takes in an image name, outputs the camera in frame.zip which took that image

        NodeList cameras = frameZip.getElementsByTagName("frame").
                item(0).getChildNodes().
                item(1).getChildNodes();

        for (int i = 0; i < cameras.getLength(); ++i) {
            //cameras also holds text fields associated with the cameras, so filter them out
            if (cameras.item(i).getNodeName().equals("camera")) {
                Element camera = (Element) cameras.item(i);

                Node photoNode = camera.getElementsByTagName("photo").item(0);
                Element photoElement = (Element) photoNode;

                //path in photo element contains "../../.." before the name of the image,
                // so we cannot test for an exact match
                //using regex to see if the image names are the same regardless of their paths
                //ex. "folder/anotherFolder/asdfghjk/imageName.png" matches with "imageName.png"
                if (photoElement.getAttribute("path").matches(".*" + imageName + ".*")) {
                    return camera;
                }
            }
        }

        return null;//no matching camera found
    }

    public File getImgFileFromCam(Element selectedItemCam) {
        Element photo = (Element) selectedItemCam.getElementsByTagName("photo").item(0);
        String path = photo.getAttribute("path");
        //example path: "../../../160518_mia337_114828_a_ding/160517_mia337_2013_9_7a_ding_nearFocus_R1_C4_0_30.jpg"

        //need to replace ../../../ with the parent of the .psx file
        File psxFile = new File(this.metashapeObject.getPsxFilePath());
        String parentPath = psxFile.getParentFile().getAbsolutePath();
        path = new File(new File(parentPath), path.substring(9)).getPath();

        //String path now holds the full path to the selected thumbnail's full-res image
        return new File(path);
    }

    public String getCurrentModelPath() {
        return getModelPathFromXML(modelID);
    }

    private String getModelPathFromXML(Integer mID) {
        //  <model id="0" path="model.1/model.zip"/> --> returns "model.1/model.zip"

        try{
            NodeList elems = ((Element) frameZip.getElementsByTagName("frame").item(0))
                    .getElementsByTagName("model");

            //this if statement triggers if chunk has one model and that model has no id
            if (elems.getLength() == 1 &&
                    ((Element) elems.item(0)).getAttribute("id").isEmpty()){
                return ((Element) elems.item(0)).getAttribute("path");
            }

            for (int i = 0; i < elems.getLength(); i++) {
                Element element = (Element) elems.item(i);

                if (Integer.parseInt(element.getAttribute("id")) == mID){
                    return element.getAttribute("path");
                }
            }

        }
        catch(NullPointerException e){
            return "";
        }

        return "";
    }

    public ArrayList<Triplet<Integer, String, String>> getModelInfo(){
        return modelInfo;
    }

    public Integer getActiveModelID(){return activeModelID;}
}
