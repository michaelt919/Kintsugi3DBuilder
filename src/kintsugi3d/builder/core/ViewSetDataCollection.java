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

package kintsugi3d.builder.core;

import kintsugi3d.util.ImageFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewSetDataCollection
{
    private final ArrayList<ViewSetData> viewSetData;

    /**
     * The absolute file path to be used for loading all resources.
     */
    private File rootDirectory;

    /**
     * The directory to be used for loading images. It is an absolute file path.
     */
    private File fullResImageDirectory;

    /**
     * The directory to be used for saving preview images
     */
    private File previewImageDirectory;

    /**
     * The directory where thumbnail images are stored.
     */
    private File thumbnailImageDirectory;

    public ViewSetDataCollection(int initialSize)
    {
        this.viewSetData= new ArrayList<>(initialSize);
    }

    public ArrayList<ViewSetData> getViewSetData()
    {
        return this.viewSetData;
    }

    public File getRootDirectory() { return this.rootDirectory; }

    public void setRootDirectory(File dir) { this.rootDirectory = dir; }

    public File getFullResImageDirectory()
    {
        if (this.fullResImageDirectory == null)
        {
            // If no full res images, just use preview images as full res, or root directory as last fallback
            return this.previewImageDirectory == null ? this.rootDirectory : this.previewImageDirectory;
        }
        else
        {
            return this.fullResImageDirectory;
        }
    }

    public void setFullResImageDirectory(File dir) { this.fullResImageDirectory = dir; }

    public File getPreviewImageDirectory()
    {
        if (this.previewImageDirectory == null)
        {
            // If no preview images, default to just using full res images, or root directory as last fallback
            return this.fullResImageDirectory == null ? this.rootDirectory : this.fullResImageDirectory;
        }
        else
        {
            return this.previewImageDirectory;
        }
    }

    public void setPreviewImageDirectory(File dir) { this.previewImageDirectory = dir; }

    public File getThumbnailImageDirectory()
    {
        if (this.thumbnailImageDirectory == null)
        {
            // If no thumbnail images, default to just using full res images, or root directory as last fallback
            return this.fullResImageDirectory == null ? this.rootDirectory : this.fullResImageDirectory;
        }
        else
        {
            return this.thumbnailImageDirectory;
        }
    }

    public void setThumbnailImageDirectory(File dir)
    {
        this.thumbnailImageDirectory = dir;
    }

    public File getFullResImageFile(int poseIndex)
    {
        return new File(this.fullResImageDirectory, this.viewSetData.get(poseIndex).imageFile.getPath());
    }

    public File findFullResImageFile(int index) throws FileNotFoundException
    {
        return ImageFinder.getInstance().findImageFile(getFullResImageFile(index));
    }

   public File findThumbnailImageFile(int index) throws FileNotFoundException
   {
       return ImageFinder.getInstance().findImageFile(getThumbnailImageFile(index));
   }

   public File getThumbnailImageFile(int poseIndex)
   {
       return getThumbnailImageFile(poseIndex, "png");
   }

   public File getThumbnailImageFile(int poseIndex, String extension)
   {
       return new File(this.getThumbnailImageDirectory(), ImageFinder.getInstance().getImageFileNameWithExtension(
           viewSetData.get(poseIndex).imageFile.getName(), extension));
   }

   public List<File> getImageFiles()
   {
       List<File> imageFiles = new ArrayList<>(viewSetData.size());
       for (ViewSetData v : viewSetData)
       {
           imageFiles.add(v.imageFile);
       }
       return Collections.unmodifiableList(imageFiles);
   }

}
