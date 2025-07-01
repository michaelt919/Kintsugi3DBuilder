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
import kintsugi3d.builder.resources.ibr.MissingImagesException;
import kintsugi3d.gl.util.UnzipHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;


public class MetashapeChunk {
    private static final Logger log = LoggerFactory.getLogger(MetashapeChunk.class);

    private MetashapeDocument metashapeDocument;
    private String label;
    private int id;//TODO: is this optional?

    private Document chunkXML;
    private Document frameXML;

    private List<MetashapeModel> models = new ArrayList<>();
    private Optional<Integer> defaultModelID;
    private MetashapeModel currModel;
    private File thumbnailsDir;
    private File masksDir = null;

    public static MetashapeChunk parseFromElement(MetashapeDocument document, Element chunkElement) throws IOException {
        Optional<Integer> chunkID = Optional.empty();
        String tempChunkID = chunkElement.getAttribute("id");
        if (!tempChunkID.isBlank()){
            chunkID = Optional.of(Integer.parseInt(tempChunkID));
        }

        String chunkZipPath = chunkElement.getAttribute("path"); //gives xx/chunk.zip where xx is a number

        //append this path to the psxFilePath (without ".psx" at the end)
        chunkZipPath = new File(new File(document.getPSXPathBase() + ".files"), chunkZipPath).getPath();

        //fullChunkDocument has info about chunk name, cameras, images, etc.
        Document fullChunkDocument = UnzipHelper.unzipToDocument(new File(chunkZipPath));

        //get default model id if the chunk has one
        Optional<Integer> defaultModelID = Optional.empty();
        try {
            Element modelsElem = (Element) fullChunkDocument.getElementsByTagName("models").item(0);
            defaultModelID = Optional.of(Integer.parseInt(modelsElem.getAttribute("active_id")));
        } catch (NumberFormatException | NullPointerException e) {
            log.warn("Could not find active id for " + document.getPsxFilePath(), e);
        }

        //only one chunk in this inner chunk document, so no need for a for loop
        Element fullChunkElement = (Element) fullChunkDocument.getElementsByTagName("chunk").item(0);

        Optional<String> chunkName = Optional.of(fullChunkElement.getAttribute("label"));
        if (chunkName.get().isBlank()){
            chunkName = Optional.empty();
        }

        //unzip frame.zip, use frame 0 by default
        NodeList frames =  fullChunkDocument.getElementsByTagName("frame");
        String frameZipPath = "";
        if (frames.getLength() > 0){
            Element frameElem = (Element) frames.item(0);
            String subPath = frameElem.getAttribute("path");
            frameZipPath = new File(new File(chunkZipPath).getParent(), subPath).getAbsolutePath();
        }

        Document frameXML = null;
        try {
            frameXML = UnzipHelper.unzipToDocument(new File(frameZipPath));
        } catch (IOException e) {
            log.error("An error occurred loading frame.xml for Metashape chunk:", e);
        }

        MetashapeChunk returned = new MetashapeChunk()
                .setParentDocument(document)
                .setChunkXml(fullChunkDocument)
                .setFrameXml(frameXML)
                .setLabel(chunkName)
                .setChunkID(chunkID)
                .setDefaultModelID(defaultModelID);

        if (frameXML != null){
            //parse thumbnail info
            NodeList list = frameXML.getElementsByTagName("thumbnails");
            if (list.getLength() > 0){
                Element thumbnailsElem = (Element) list.item(0);
                //thumbnails path is relative to frame.zip's parent directory
                File thumbnailsDir = new File(new File(frameZipPath).getParent(), thumbnailsElem.getAttribute("path"));
                if (thumbnailsDir.exists()){
                    returned.setThumbnailsDir(thumbnailsDir);
                }
            }

            //parse mask info
            list = frameXML.getElementsByTagName("masks");
            if (list.getLength() > 0){
                Element masksElem = (Element) list.item(0);
                //masks path is relative to frame.zip's parent directory
                File masksDir = new File(new File(frameZipPath).getParent(), masksElem.getAttribute("path"));
                if (masksDir.exists()){
                    returned.setMasksDir(masksDir);
                }
            }
        }

        List<MetashapeModel> models = new ArrayList<>();

        NodeList modelList = fullChunkDocument.getElementsByTagName("model");

        //if model list is empty, then there is likely a single model listed in frameXml
        if (modelList.getLength() == 0){
            NodeList list = frameXML.getElementsByTagName("model");
            Element modelElem = (Element) list.item(0);

            if (modelElem != null){
                MetashapeModel model = MetashapeModel.parseFromElement(returned, modelElem);
                returned.selectModel(model);
                models.add(model);
            }
        }

        boolean defaultIdFound = false;
        for (int i = 0; i < modelList.getLength(); ++i) {
            Element elem = (Element) modelList.item(i);
            MetashapeModel model = MetashapeModel.parseFromElement(returned, elem);
            if (model.getId().isPresent() && model.getId().equals(defaultModelID)){
                returned.selectModel(model);
                defaultIdFound = true;
            }
            models.add(model);
        }

        if (!defaultIdFound && !models.isEmpty()){
            returned.selectModel(models.get(0));
        }

        returned.setModels(models);

        return returned;
    }

    private void setMasksDir(File masksDir) { this.masksDir = masksDir; }

    public boolean hasModels(){
        return !models.isEmpty();
    }

    private void setThumbnailsDir(File thumbnailsDir) {
        this.thumbnailsDir = thumbnailsDir;
    }

    public void selectModel(MetashapeModel model) {
        currModel = model;
    }

    private MetashapeChunk setChunkID(Optional<Integer> chunkID) {
        this.id = chunkID.orElse(-1);
        return this;
    }

    private MetashapeChunk setDefaultModelID(Optional<Integer> defaultModelID) {
        this.defaultModelID = defaultModelID;
        return this;
    }

    private MetashapeChunk setFrameXml(Document frameXML) {
        this.frameXML = frameXML;
        return this;
    }

    private MetashapeChunk setParentDocument(MetashapeDocument document) {
        this.metashapeDocument = document;
        return this;
    }

    private MetashapeChunk setLabel(Optional<String> chunkName) {
        this.label = chunkName.orElse(null);
        return this;
    }

    private MetashapeChunk setChunkXml(Document doc) {
        this.chunkXML = doc;
        return this;
    }

    private void setModels(List<MetashapeModel> models) {
        this.models = models;
    }

    public Document getFrameXML() {
        return frameXML;
    }

    public String getChunkDirectoryPath() {
        String psxFilePath = this.metashapeDocument.getPsxFilePath();
        String psxPathBase = psxFilePath.substring(0, psxFilePath.length() - 4); //remove ".psx" from path
        return new File(new File(psxPathBase + ".files"), Integer.toString(id)).getPath();
    }

    public String getFrameDirectoryPath() {
        return new File(new File(getChunkDirectoryPath()), "0").getPath();
    }

    public String getFramePath() {
        return new File(new File(getFrameDirectoryPath()), "frame.zip").getPath();
    }


    private MetashapeChunk() {
        //intentionally left blank
    }

    public Integer getID() {
        return id;
    }

    public MetashapeModel getSelectedModel() {
        return currModel;
    }

    public Map<Integer, Image> loadThumbnailImageList() {
        return UnzipHelper.unzipImagesToMap(thumbnailsDir);
    }

    public List<Element> findChunkXmlCameras() {
        NodeList cams = this.chunkXML.getElementsByTagName("camera");
        ArrayList<Element> cameras = new ArrayList<>();
        for (int i = 0; i < cams.getLength(); ++i) {
            Node camera = cams.item(i);
            if (camera.getNodeType() == Node.ELEMENT_NODE) {
                cameras.add((Element) camera);
            }
        }
        return cameras;
    }

    public static List<Element> findEnabledCameras(List<Element> cams) {
        List<Element> enabledCams = new ArrayList<>();

        for (Element cam : cams) {
            String enabled = cam.getAttribute("enabled");

            if (enabled.equals("true") ||
                    enabled.equals("1") ||
                    enabled.isEmpty() /*cam is enabled by default*/) {
                enabledCams.add(cam);
            }
        }
        return enabledCams;
    }

    public Map<Integer, String> buildCameraPathsMap(boolean useRelativePaths) throws FileNotFoundException
    {
        File rootDirectory = new File(getParentDocument().getPsxFilePath()).getParentFile();
        if (!rootDirectory.exists())
        {
            throw new FileNotFoundException(MessageFormat.format("Root directory does not exist: {0}", rootDirectory));
        }

        Map<Integer, String> cameraPathsMap = new HashMap<>(128);

        // Open the xml files that contains all the cameras' ids and file paths
        if (frameXML == null || frameXML.getDocumentElement() == null){
            throw new FileNotFoundException("No frame document found");
        }

        // Loop through the cameras and store each pair of id and path in the map
        NodeList cameraList = ((Element) frameXML.getElementsByTagName("frame").item(0))
                .getElementsByTagName("camera");

        int numMissingFiles = 0;
        File modelOverride = this.getSelectedModel().getLoadPreferences().fullResOverride;
        File fullResSearchDirectory = modelOverride == null ?
                //full res image paths in frame.xml are relative to the frame document itself
                new File(getFramePath()).getParentFile() :
                modelOverride;


        File exceptionFolder = null;

        for (int i = 0; i < cameraList.getLength(); i++) {
            Element cameraElement = (Element) cameraList.item(i);
            int cameraId = Integer.parseInt(cameraElement.getAttribute("camera_id"));

            String pathAttribute = ((Element) cameraElement.getElementsByTagName("photo").item(0)).getAttribute("path");

            File imageFile;
            String path = "";
            if (modelOverride == null){
                imageFile = new File(fullResSearchDirectory, pathAttribute);
                path = rootDirectory.toPath().relativize(imageFile.toPath()).toString();
            }
            else{
                String pathAttributeName = new File(pathAttribute).getName();
                imageFile = new File(fullResSearchDirectory, pathAttributeName);
                path = imageFile.getName();
            }

            if (imageFile.exists() && !path.isBlank()) {
                // Add pair to the map
                String finalPath = useRelativePaths ? path : imageFile.getAbsolutePath();
                cameraPathsMap.put(cameraId, finalPath);
            }
            else{
                numMissingFiles++;
                exceptionFolder = imageFile.getParentFile();
            }
        }

        if (!getSelectedModel().getLoadPreferences().doSkipMissingCams && numMissingFiles > 0){
            throw new MissingImagesException("Project is missing images.", numMissingFiles, exceptionFolder);
        }

        return cameraPathsMap;
    }

    public String getCurrentModelPath() {
        return currModel.getPath();
    }

    public List<MetashapeModel> getModels() {
        return models;
    }

    public String getLabel() {
        return this.label;
    }

    public Optional<Integer> getDefaultModelID() {
        return defaultModelID;
    }

    public int getCurrModelID() {
        return currModel.getId().orElse(-1);
    }

    public File getPsxFile() {
        return metashapeDocument.getPsxFile();
    }

    public MetashapeDocument getParentDocument() {
        return this.metashapeDocument;
    }

    public File getMasksDirectory() {
        return this.masksDir;
    }

    public void setMasksDirectory(File dir) {
        this.masksDir = dir;
    }

    @Override
    public boolean equals(Object rhs) {
        if (!(rhs instanceof MetashapeChunk)) {
            return false;
        }

        MetashapeChunk other = (MetashapeChunk) rhs;

        //chunk name is the same
        //psx path is the same
        //selected models are equal

        return this.label.equals(other.label) &&
                this.metashapeDocument.getPsxFilePath().equals(other.metashapeDocument.getPsxFilePath()) &&
                this.getSelectedModel().equals(other.getSelectedModel());
    }
}
