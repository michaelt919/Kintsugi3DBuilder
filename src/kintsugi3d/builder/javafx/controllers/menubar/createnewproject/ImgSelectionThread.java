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
import kintsugi3d.builder.io.primaryview.PrimaryViewSelectionModel;
import kintsugi3d.util.ImageFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public class ImgSelectionThread implements Runnable{

    private static final Logger log = LoggerFactory.getLogger(ImgSelectionThread.class);
    private final String imageName;
    private final ImageView chunkViewerImgView;
    private final Text imgViewText;
    private final PrimaryViewSelectController controller;
    private final PrimaryViewSelectionModel model;
    private Map<String, Image> imgCache;
    private volatile boolean stopRequested = false;
    private volatile boolean isRunning = false;

    public ImgSelectionThread(String imageName, PrimaryViewSelectController primaryViewSelectController, PrimaryViewSelectionModel model) {
        this.imageName = imageName;

        this.chunkViewerImgView = primaryViewSelectController.getChunkViewerImgView();
        this.imgViewText = primaryViewSelectController.getImgViewText();
        this.imgCache = primaryViewSelectController.getImgCache();
        this.controller = primaryViewSelectController;
        this.model = model;
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
                String path = model.findFullResImagePath(imageName).orElse("");

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
            } catch(Exception e){
                log.warn("Image selection thread failed to find " + imageName, e);
            }
        }

        Image finalImage = image;//copy here so a final version of image can be passed to lambda expression
        if (finalImage != null && !stopRequested) {
            Platform.runLater(() -> {
                chunkViewerImgView.setImage(finalImage);
                controller.updateImageText(imageName);
            });
        }
    }
}
