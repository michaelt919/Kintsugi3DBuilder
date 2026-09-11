/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.io.metashape;

import kintsugi3d.builder.io.TextureSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.IntStream;

public class MetashapeTextures implements TextureSupplier
{
    private static final Logger LOG = LoggerFactory.getLogger(MetashapeTextures.class);

    private final File modelXMLFile;

    public MetashapeTextures(File modelXMLFile)
    {
        this.modelXMLFile = modelXMLFile;
    }

    @Override
    public Map<String, File> getTextures()
    {
        try
        {
            // Initialize document builder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Create a new document from the doc.xml
            Document document = builder.parse(modelXMLFile);
            document.getDocumentElement().normalize();

            // Get all the textures
            NodeList textures = document.getElementsByTagName("texture");

            return Map.ofEntries(IntStream.range(0, textures.getLength())
                .<Entry<String, File>>mapToObj(i ->
                {
                    Element e = (Element) textures.item(i);

                    // Get some needed metadata
                    String texType = e.getAttribute("type");
                    String texName = ((Element) e.getElementsByTagName("page").item(0)).getAttribute("path");

                    if ("normals".equals(texType))
                    {
                        texType = "normal";
                    }

                    return new SimpleEntry<>(texType, new File(modelXMLFile.getParentFile(), texName));
                })
                .toArray(length -> (Entry<String, File>[]) new Entry[length]));
        }
        catch (ParserConfigurationException | IOException | SAXException e)
        {
            LOG.error("Could not copy textures from Metashape project using model XML {}.", modelXMLFile);
            return null;
        }
    }
}
