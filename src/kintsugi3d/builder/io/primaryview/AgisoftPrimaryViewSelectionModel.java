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

package kintsugi3d.builder.io.primaryview;

import javafx.scene.image.Image;
import kintsugi3d.builder.javafx.controllers.menubar.metashape.MetashapeChunk;
import kintsugi3d.builder.javafx.controllers.menubar.metashape.MetashapeModel;
import kintsugi3d.util.ImageFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class AgisoftPrimaryViewSelectionModel implements PrimaryViewSelectionModel
{
    private static final Logger log = LoggerFactory.getLogger(AgisoftPrimaryViewSelectionModel.class);
    private final String chunkName;
    private final List<View> views;
    private final List<Image> thumbnails;
    private Document cameraDocument;
    private final List<Element> cameras;
    private File fullResSearchDir;

    private Map<Integer, String> cameraIdAndPaths;

    //custom import path
    private AgisoftPrimaryViewSelectionModel(File cameraFile, File fullResSearchDir) throws ParserConfigurationException, IOException, SAXException
    {
        this.fullResSearchDir = fullResSearchDir;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        builder = factory.newDocumentBuilder();

        cameraDocument = builder.parse(cameraFile);

        //get chunk name
        Element chunkElem = (Element) cameraDocument.getElementsByTagName("chunk").item(0);
        chunkName = chunkElem.getAttribute("label");

        //get enabled cameras
        NodeList cameraNodes = cameraDocument.getElementsByTagName("camera");

        cameras = IntStream.range(0, cameraNodes.getLength())
                .mapToObj(cameraNodes::item)
                .filter(camera -> camera.getNodeType() == Node.ELEMENT_NODE)
                .map(camera -> (Element) camera)
                .collect(Collectors.toUnmodifiableList());

        views = getViews(IntStream.range(0, cameraNodes.getLength())
            .mapToObj(cameraNodes::item)
            .filter(camera -> camera.getNodeType() == Node.ELEMENT_NODE)
            .map(camera -> (Element) camera));

        //prev-res images haven't been generated and no thumbnails are present,
        //so leave thumbnails list empty
        thumbnails = new ArrayList<>(views.size());

        cameraIdAndPaths = new HashMap<>();
        for (View view : views){
            File imgFile = ImageFinder.getInstance().tryFindImageFile(new File(fullResSearchDir, view.name));
            if (imgFile != null && view.id != -1){
                cameraIdAndPaths.put(view.id, imgFile.getPath());
            }
        }
    }

    //metashape import path
    private AgisoftPrimaryViewSelectionModel(MetashapeModel model)
    {
        MetashapeChunk parentChunk = model.getChunk();
        this.chunkName = parentChunk.getLabel();
        this.cameras = MetashapeChunk.findEnabledCameras(parentChunk.findChunkXmlCameras());
        this.views = getViews(cameras.stream());
        this.thumbnails = parentChunk.loadThumbnailImageList();

        try{
            this.cameraIdAndPaths = parentChunk.buildCameraPathsMap(false);
        }
        catch(FileNotFoundException fnfe){
           log.warn("Failed to find source directories in Metashape project. Project images may not be found.", fnfe);
        }
    }

    private static List<View> getViews(Stream<Element> cameras)
    {
        return cameras
            .filter(camera ->
            {
                String enabled = camera.getAttribute("enabled");
                return "true".equals(enabled) ||
                    "1".equals(enabled) ||
                    enabled.isEmpty(); /*cam is enabled by default*/
            })
            .map(camera ->
            {
                Element parent = (Element) camera.getParentNode();
                String label = camera.getAttribute("label");
                String id = camera.getAttribute("id");
                int parsedId = !id.isBlank() ? Integer.parseInt(id) : -1;

                if("group".equals(parent.getTagName())) // (either a group or the root node)
                {
                    return new View(label, parsedId, parent.getAttribute("label"));
                }
                else
                {
                    return new View(label, parsedId, null);
                }
            })
            .collect(Collectors.toUnmodifiableList());
    }


    public static PrimaryViewSelectionModel createInstance(File cameraFile, File fullResOverride) throws ParserConfigurationException, IOException, SAXException
    {
        return new AgisoftPrimaryViewSelectionModel(cameraFile, fullResOverride);
    }

    public static PrimaryViewSelectionModel createInstance(MetashapeModel model)
    {
        return new AgisoftPrimaryViewSelectionModel(model);
    }

    @Override
    public String getName()
    {
        return chunkName;
    }

    @Override
    public List<View> getViews()
    {
        return views;
    }

    @Override
    public List<Image> getThumbnails()
    {
        return thumbnails;
    }

    @Override
    public Optional<String> findFullResImagePath(String imageName) {
        //find the camera (in chunk.xml) which holds the desired image
        //may have slightly different label from imageName --> "Processed\img123.jpg" vs. "img123.jpg"
        Element selectedItemCam = findTargetCamera(imageName);
        if(selectedItemCam == null){
            return Optional.empty();
        }

        //hopefully the camera is in the camera paths map and we don't really have to do much work
        String selectedCamId = selectedItemCam.getAttribute("id");
        if (!selectedCamId.isBlank()){
           try{
               int id = Integer.parseInt(selectedCamId);
               String path = cameraIdAndPaths.get(id);
               if (path != null){
                   return Optional.of(path);
               }
           }
           catch(NumberFormatException nfe){
               log.warn("Failed to parse camera id.", nfe);
           }
        }

        //need full label to find img path
        String pathAttribute = selectedItemCam.getAttribute("label");
        String pathAttributeName = new File(pathAttribute).getName();
        File imageFile = new File(fullResSearchDir, pathAttributeName);

        File finalImgFile;

        //return original file
        finalImgFile = ImageFinder.getInstance().tryFindImageFile(imageFile);
        if(finalImgFile != null){return Optional.of(finalImgFile.getPath());}

        //return file by searching canonical path
        try{
            finalImgFile = ImageFinder.getInstance().tryFindImageFile(new File(imageFile.getCanonicalPath()));
            if(finalImgFile != null){return Optional.of(finalImgFile.getPath());}
        }
        catch(IOException e) {
            log.warn("Error retrieving canonical file path for " + imageFile.getPath(), e);
        }

        //throw a hail mary and see if it sticks
        finalImgFile = ImageFinder.getInstance().tryFindImageFile(new File(imageFile.getAbsolutePath()
                .replace("..\\", "")));
        if(finalImgFile != null){return Optional.of(finalImgFile.getPath());}

        //give up
        return Optional.empty();
    }

    private Element findTargetCamera(String imageName) {
        Element selectedItemCam = null;
        for(Element camera : cameras) {
            if (camera.getAttribute("label").matches(".*" + imageName + ".*")) {
                selectedItemCam = camera;
                break;
            }
        }
        return selectedItemCam;
    }
}
