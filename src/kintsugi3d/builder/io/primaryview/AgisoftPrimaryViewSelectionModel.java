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

package kintsugi3d.builder.io.primaryview;

import javafx.scene.image.Image;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AgisoftPrimaryViewSelectionModel implements PrimaryViewSelectionModel
{
    private final String chunkName;
    private final List<View> views;
    private final List<Image> thumbnails;

    private AgisoftPrimaryViewSelectionModel(File cameraFile) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        builder = factory.newDocumentBuilder();

        Document cameraDocument = builder.parse(cameraFile);

        //get chunk name
        Element chunkElem = (Element) cameraDocument.getElementsByTagName("chunk").item(0);
        chunkName = chunkElem.getAttribute("label");

        //get enabled cameras
        NodeList cameraNodes = cameraDocument.getElementsByTagName("camera");

        views = getViews(IntStream.range(0, cameraNodes.getLength())
            .mapToObj(cameraNodes::item)
            .filter(camera -> camera.getNodeType() == Node.ELEMENT_NODE)
            .map(camera -> (Element) camera));

        //prev-res images haven't been generated and no thumbnails are present,
        //so leave thumbnails list empty
        thumbnails = new ArrayList<>(views.size());
    }

    private AgisoftPrimaryViewSelectionModel(String chunkName, ArrayList<Element> cameras, ArrayList<Image> thumbnailImageList)
    {
        this.chunkName = chunkName;
        this.views = getViews(cameras.stream());
        this.thumbnails = thumbnailImageList;
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


    public static PrimaryViewSelectionModel createInstance(File cameraFile) throws ParserConfigurationException, IOException, SAXException
    {
        return new AgisoftPrimaryViewSelectionModel(cameraFile);
    }

    public static PrimaryViewSelectionModel createInstance(String chunkName, ArrayList<Element> cameras, ArrayList<Image> thumbnailImageList)
    {
        return new AgisoftPrimaryViewSelectionModel(chunkName, cameras, thumbnailImageList);
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
}
