/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.io.metashape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.*;

public class MetashapeModel
{
    private static final Logger LOG = LoggerFactory.getLogger(MetashapeModel.class);
    private final MetashapeChunk chunk;
    private final Optional<Integer> id;
    private final String label;
    private final String path;

    private final LoadPreferences loadPreferences = new LoadPreferences();

    private MetashapeModel(MetashapeChunk chunk, Optional<Integer> id, String label, String path)
    {
        this.id = id;
        this.label = label;
        this.path = path;
        this.chunk = chunk;
    }

    public static MetashapeModel parseFromElement(MetashapeChunk chunk, Element elem)
    {
        Optional<Integer> modelID = Optional.empty();
        String tempLabel = null;
        try
        {
            modelID = Optional.of(Integer.parseInt(elem.getAttribute("id")));
        }
        catch (NumberFormatException nfe)
        {
            LOG.warn("Model has no id", nfe);
        }

        try
        {
            tempLabel = elem.getAttribute("label");
        }
        catch (NumberFormatException nfe)
        {
            LOG.warn("Model has no label", nfe);
        }

        String path = findModelPath(chunk, modelID);

        return new MetashapeModel(chunk, modelID, tempLabel, path);
    }

    private static String findModelPath(MetashapeChunk chunk, Optional<Integer> modelID)
    {
        try
        {
            NodeList elems = ((Element) chunk.getFrameXML()
                .getElementsByTagName("frame").item(0))
                .getElementsByTagName("model");

            //this if statement triggers if chunk has one model and that model has no id
            if (elems.getLength() == 1 &&
                ((Element) elems.item(0)).getAttribute("id").isEmpty())
            {
                return ((Element) elems.item(0)).getAttribute("path");
            }

            //now we check to see if id's match
            for (int i = 0; i < elems.getLength(); i++)
            {
                Element element = (Element) elems.item(i);

                if (Objects.equals(element.getAttribute("id"), String.valueOf(modelID.get())))
                {
                    return element.getAttribute("path");
                }
            }

        }
        catch (NullPointerException e)
        {
            //ignore, no path was found
        }
        return "";
    }

    public LoadPreferences getLoadPreferences()
    {
        return loadPreferences;
    }

    public Optional<Integer> getId()
    {
        return id;
    }

    public String getPath()
    {
        return path;
    }

    public String getLabel()
    {
        return label;
    }

    public MetashapeChunk getChunk()
    {
        return chunk;
    }

    @Override
    public boolean equals(Object obj)
    {

        if (!(obj instanceof MetashapeModel))
        {
            return false;
        }

        MetashapeModel other = (MetashapeModel) obj;

        return this.id.equals(other.id) &&
            this.label.equals(other.label) &&
            this.path.equals(other.path);
    }
}
