/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.reflectancefit;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

import umn.gl.util.VertexGeometry;
import umn.imagedata.ViewSet;

/**
 * This interface defines the methods necessary for an object that is responsible for providing access to the geometry and color information
 * that will be used to estimate reflectance parameters.  Information that an implementation is responsible for includes all the images of the
 * reflecting surface (and optionally, image masks), the camera calibration information for all these images,  and a representation of the
 * geometry of the reflecting surface.  An implementation of this interface can be passed to the constructor for ParameterFittingResourcesImpl
 * to generate the GPU resources needed to run the reflectance parameter estimation computation.
 */
public interface ReflectanceDataAccess
{
    /**
     * Gets a default material name that will be used if a material name is not explicitly provided in a material file.
     * (This name is generally  a name associated with the geometry, i.e. the name of the Wavefront OBJ file without the .obj file extension.)
     * @return The default material name.
     */
    String getDefaultMaterialName();

    /**
     * Retrieves a data structure containing the positions, texture coordinates, normals and tangents for the geometry of the reflecting surface.
     * @return The vertex geometry data structure.
     * @throws IOException An IO exception may or may not be thrown, depending on the implementation.
     */
    VertexGeometry retrieveMesh() throws IOException;

    /**
     * Ensures that the view set is loaded and available for use.  Must be called before any calls to getViewSet().
     * @throws IOException An IO exception may or may not be thrown, depending on the implementation.
     */
    void initializeViewSet() throws IOException;

    /**
     * Returns a data structure containing camera information for each photograph.
     * The results of this method are undefined until after initializeViewSet() has been called.
     * @return The view set data structure.
     */
    ViewSet getViewSet();

    /**
     * Retrieves a particular image of the reflecting surface.
     * @param index The index of the image to be retrieved.
     * @return The image corresponding to the specified index.
     * @throws IOException An IO exception may or may not be thrown, depending on the implementation.
     */
    BufferedImage retrieveImage(int index) throws IOException;


    /**
     * Retrieves an optional "mask" image that is white for pixels that should be considered for reflectance parameter estimation,
     * and black for pixels that should be ignored.  If no masks are available, this method returns Optional.empty().
     * @param index The index of the image for which a mask to be retrieved.
     * @return The mask corresponding to the specified index, or Optional.empty() if a mask is not defined.
     * @throws IOException An IO exception may or may not be thrown, depending on the implementation.
     */
    Optional<BufferedImage> retrieveMask(int index) throws IOException;
}
