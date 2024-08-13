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

package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import kintsugi3d.util.ImageFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

public class ImgSelectionThread implements Runnable{

    private static final Logger log = LoggerFactory.getLogger(ImgSelectionThread.class);

    private final String imageName;
    private final ImageView chunkViewerImgView;
    private final Text imgViewText;
    private final MetashapeObjectChunk metashapeObjectChunk;
    private final Document cameraDocument;
    private final File photosDir;

    private final PrimaryViewSelectController controller;
    private final File fullResOverride;
    private HashMap<String, Image> imgCache;
    private volatile boolean stopRequested = false;
    private volatile boolean isRunning = false;

    //TODO: look at this again
    public ImgSelectionThread(String imageName, PrimaryViewSelectController primaryViewSelectController) {
        this.imageName = imageName;

        this.chunkViewerImgView = primaryViewSelectController.getChunkViewerImgView();
        this.imgViewText = primaryViewSelectController.getImgViewText();
        this.metashapeObjectChunk = primaryViewSelectController.getMetashapeObjectChunk();
        this.cameraDocument = primaryViewSelectController.getCameraDocument();
        this.photosDir = primaryViewSelectController.getPhotosDir();
        this.fullResOverride = primaryViewSelectController.getFullResOverride();
        this.imgCache = primaryViewSelectController.getImgCache();
        this.controller = primaryViewSelectController;
    }

    @Override
    public void run() {
        isRunning = true;
        loadFullResImg(imageName);
        isRunning = false;
    }

    public boolean isActive(){return isRunning;}
    public void stopThread(){
        stopRequested = true;
    }

    private void loadFullResImg(String imageName) {
        Image image = null;//use cached img if possible
        if(imgCache.containsKey(imageName)){
            image = imgCache.get(imageName);
        }
        else{
            try {
                //goal of this try block is to find the camera which is associated with the image name in selectedItem
                //then take that camera's image and put it into the imageview to show to the user
                Document document = metashapeObjectChunk == null ? cameraDocument : metashapeObjectChunk.getFrameZip();
                boolean isMetashapeImport = document.getElementsByTagName("frame").getLength() != 0;

                Element selectedItemCam = null;

                NodeList cameras = document.getElementsByTagName("camera");

                for (int i = 0; i < cameras.getLength(); ++i) {
                    Element camera = (Element) cameras.item(i);

                    //(metashape import) --> if in frame.zip, each camera has an id and
                    //      the photo path is inside a photo node within the camera node

                    //(custom import) --> if in cameras.xml, img name is in camera node attribute "label"

                    //metashape import
                    if(isMetashapeImport){
                        Node photoNode = camera.getElementsByTagName("photo").item(0);
                        Element photoElement = (Element) photoNode;

                        //path in photo element contains "../../.." before the name of the image,
                        // so we cannot test for an exact match
                        //using regex to see if the image names are the same regardless of their paths
                        //ex. "folder/anotherFolder/asdfghjk/imageName.png" matches with "imageName.png"
                        if (photoElement.getAttribute("path").matches(".*" + imageName + ".*")) {
                            selectedItemCam = camera;
                            break;
                        }
                    }
                    //custom import
                    else if (camera.getAttribute("label").matches(".*" + imageName + ".*")) {
                        selectedItemCam = camera;
                        break;
                    }
                }

                if (selectedItemCam == null) {
                    imgViewText.setText(imgViewText.getText() +
                            " (matching camera not found)");
                    return;
                }

                String path = findFullResPath(isMetashapeImport, selectedItemCam, fullResOverride);

                File imgFile;
                try{
                    imgFile = ImageFinder.getInstance().findImageFile(new File(path));
                }
                catch(FileNotFoundException ignored){
                    //camera not found in xml document
                    imgViewText.setText(imgViewText.getText() +
                            " (full res image not found)");
                    return;
                }

                //set imageview to selected image
                if (!imgFile.getAbsolutePath().toLowerCase().matches(".*\\.tiff?")) {
                    int requestedWidth = (int) chunkViewerImgView.getFitWidth();
                    int requestedHeight = (int) chunkViewerImgView.getFitHeight();
                    boolean preserveRatio = true;
                    boolean smooth = true;        // Use a smoother resampling algorithm

                    image = new Image(imgFile.toURI().toString(), requestedWidth, requestedHeight, preserveRatio, smooth);
                } else {
                    //convert image if it is a .tif or .tiff
                    if(stopRequested){return;}
                    BufferedImage bufferedImage = ImageIO.read(imgFile);
                    if(stopRequested){return;}
                    image = SwingFXUtils.toFXImage(bufferedImage, null);
                    if(stopRequested){return;}
                }

                imgCache.put(imageName, image);

            } catch (IllegalArgumentException e) {//could not find image
                imgViewText.setText(imgViewText.getText() + " (full res image not found)");
                log.warn("Could not find full res image", e);
            } catch (IOException e) {
                log.warn("Failed to read image", e);
            }
        }

        Image finalImage = image;//copy here so a final version of image can be passed to lambda expression
        if (finalImage != null && !stopRequested) {
            Platform.runLater(() -> {
                //TODO: WITH LARGER IMAGES, FORMATTING IS BROKEN
                chunkViewerImgView.setImage(finalImage);
                controller.updateImageText(imageName);
            });
        }
    }

    private String findFullResPath(boolean isMetashapeImport, Element selectedItemCam, File fullResOverride) {
        String path;
        if(isMetashapeImport){
            String pathAttribute = ((Element) selectedItemCam.getElementsByTagName("photo").item(0)).getAttribute("path");

            File imageFile;
            File rootDirectory = metashapeObjectChunk.getPsxFile().getParentFile();
            File fullResSearchDirectory = fullResOverride == null ?
                    new File(metashapeObjectChunk.getFramePath()).getParentFile() :
                    fullResOverride;

            if (fullResOverride == null){
                imageFile = new File(fullResSearchDirectory, pathAttribute);
            }
            else{
                //if this doesn't work, then replace metashapeObjectChunk.getFramePath()).getParentFile()
                //    and the first part of path with the file that the user selected
                String pathAttributeName = new File(pathAttribute).getName();
                imageFile = new File(fullResOverride, pathAttributeName);
            }
            path = imageFile.getPath();
        }
        else{
            path = selectedItemCam.getAttribute("label");
            String parentPath = photosDir.getPath();
            path = new File(new File(parentPath), path).getPath();
        }
        return path;
    }
}
