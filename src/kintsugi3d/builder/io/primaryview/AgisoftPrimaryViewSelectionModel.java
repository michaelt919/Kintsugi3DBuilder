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

import com.agisoft.metashape.Camera;
import com.agisoft.metashape.CameraGroup;
import com.agisoft.metashape.Chunk;
import javafx.scene.image.Image;
import kintsugi3d.gl.util.UnzipHelper;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

    private final List<Element> cameraElements;
    private final List<Camera> cameras;
    private File fullResSearchDir;

    //custom import path
    private AgisoftPrimaryViewSelectionModel(File cameraFile) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        builder = factory.newDocumentBuilder();

        cameraDocument = builder.parse(cameraFile);

        //get chunk name
        Element chunkElem = (Element) cameraDocument.getElementsByTagName("chunk").item(0);
        chunkName = chunkElem.getAttribute("label");

        //get enabled cameras
        NodeList cameraNodes = cameraDocument.getElementsByTagName("camera");

        //camera file is .xml, so img name is in camera node attribute "label"
        cameraElements = IntStream.range(0, cameraNodes.getLength())
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
        //TODO: ^^^ this doesn't seem true, should at least check to see if they exist like we do for metashape import path
        thumbnails = new ArrayList<>(views.size());
        
        //TODO: need this?
        this.cameras = new ArrayList<>(cameraElements.size());
    }

    //metashape import path
    private AgisoftPrimaryViewSelectionModel(Chunk chunk)
    {
        this.chunkName = chunk.getLabel();
        this.cameras = Arrays.stream(chunk.getCameras()).collect(Collectors.toList());
        this.views = cameras.stream().filter(Camera::isEnabled)
                .map(camera -> {
                    Optional<CameraGroup> group = camera.getGroup();

                    if (group.isPresent()) {
                        return new View(camera.getLabel(), group.get().getLabel());
                    } else {
                        return new View(camera.getLabel(), null);
                    }
                })
                .collect(Collectors.toUnmodifiableList());

        thumbnails = loadThumbnails(chunk);

        this.cameraElements = new ArrayList<>(cameras.size());
    }

    private List<Image> loadThumbnails(Chunk chunk) {
        //get model path
        //get parent of parent of that
        //append thumbnails/thumbnails.zip and we should be there
        //TODO: make this better by reading the thumbnails path from frame.zip
        //why is this not supported by the api? Who knows :/

        File modelZip = new File(chunk.getModel().get().getPath());
        File frameDir = modelZip.getParentFile().getParentFile();
        File thumbnailsDir = new File(new File(frameDir, "thumbnails"), "thumbnails.zip");

        return UnzipHelper.unzipImages(thumbnailsDir.getPath());
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
                if("group".equals(parent.getTagName())) // (either a group or the root node)
                {
                    return new View(camera.getAttribute("label"), parent.getAttribute("label"));
                }
                else
                {
                    return new View(camera.getAttribute("label"), null);
                }
            })
            .collect(Collectors.toUnmodifiableList());
    }


    public static PrimaryViewSelectionModel createInstance(File cameraFile, File fullResOverride) throws ParserConfigurationException, IOException, SAXException
    {
        return new AgisoftPrimaryViewSelectionModel(cameraFile).setFullResSearchDir(fullResOverride);
    }

    public static PrimaryViewSelectionModel createInstance(Chunk chunk, File fullResOverride)
    {
        return new AgisoftPrimaryViewSelectionModel(chunk)
                .setFullResSearchDir(fullResOverride);
    }

    private PrimaryViewSelectionModel setFullResSearchDir(File fullResOverride) {
        this.fullResSearchDir = fullResOverride;
        return this;
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
        //find the camera which holds the desired image
        //may have slightly different label from imageName --> "Processed\img123.jpg" vs. "img123.jpg"
        Camera targetCam = cameras.stream().filter(camera -> camera.getLabel()
                .matches(".*" + imageName + ".*"))
                .findFirst()
                .orElse(null);

        if(targetCam == null){
            return Optional.empty();
        }

        //TODO: only works for metashape import, needs to also work for custom import
        //can probably reuse findTargetCamera()

        //need full label to find img path
        String pathAttribute = targetCam.getLabel();
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
        for(Element camera : cameraElements) {
            if (camera.getAttribute("label").matches(".*" + imageName + ".*")) {
                selectedItemCam = camera;
                break;
            }
        }
        return selectedItemCam;
    }

    public Optional<Document> getCamDocument(){return Optional.ofNullable(cameraDocument);}
}
