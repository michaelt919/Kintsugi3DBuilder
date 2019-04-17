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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;

import umn.gl.core.Context;

/**
 * The main entry point for the reflectance parameter fitting computation.
 * In the context of the user interface, this class contains the code that is run when the "Execute" button is pressed.
 * @param <ContextType>
 */
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class MainExecution<ContextType extends Context<ContextType>>
{
    private final ContextType context;
    private final File cameraFile;
    private final File modelFile;
    private final File imageDir;
    private final File maskDir;
    private final File rescaleDir;
    private final File outputDir;
    private final Options options;

    /**
     * The constructor for the MainExecution object.
     * @param context       A graphics context to use for GPU operations.
     * @param cameraFile    Either a VSET file or an XML camera definition file exported by PhotoScan using the “Export Cameras…” feature.
     *                      A VSET file is generated after running the reflectance parameter fitting.
     *                      Loading a VSET file instead of the original XML file can automatically initialize some settings (ColorChecker values and the primary view).
     * @param modelFile     A Wavefront OBJ file containing the geometry model (exported from PhotoScan using “Export Model…”).
     * @param imageDir      A directory containing undistorted images (exported from PhotoScan using “Undistort Photos…”).
     * @param maskDir       Optional.  A directory containing undistorted masks.
     *                      Masks can be used to restrict which pixels in an image will be used.
     *                      If this directory is not provided, the alpha channel of the images will be used as a mask if it exists.
     * @param rescaleDir    Optional.  A directory where rescaled images will be saved for future re-use if “Rescale Images” is enabled.
     *                      If you set this, you can use this directory for “Images” in the future to make processing on the same dataset finish faster.
     * @param outputDir     The directory where the final textures will be stored.
     * @param options       An object containing options for controlling aspects of the parameter fitting computation.
     */
    public MainExecution(ContextType context, File cameraFile, File modelFile, File imageDir, File maskDir, File rescaleDir, File outputDir, Options options)
    {
        this.context = context;
        this.cameraFile = cameraFile;
        this.modelFile = modelFile;
        this.imageDir = imageDir;
        this.maskDir = maskDir;
        this.rescaleDir = rescaleDir;
        this.outputDir = outputDir;
        this.options = options;
    }

    /**
     * Runs the main reflectance parameter fitting computation.
     * @throws IOException An IO exception may or may not be thrown, depending on the implementation.
     */
    public void execute() throws IOException
    {
        // Create a new instance of an implementation for access to the reflectance data on the hard drive.
        ReflectanceDataAccessImpl reflectanceDataAccess =
            new ReflectanceDataAccessImpl(cameraFile, modelFile, imageDir, maskDir);

        try(ParameterFittingResourcesImpl<ContextType> resources = new ParameterFittingResourcesImpl<>(context, reflectanceDataAccess, options))
        {
            // Print out some information about the graphics system for debugging purposes.
            System.out.println("Max vertex uniform components across all blocks:" + context.getState().getMaxCombinedVertexUniformComponents());
            System.out.println("Max fragment uniform components across all blocks:" + context.getState().getMaxCombinedFragmentUniformComponents());
            System.out.println("Max size of a uniform block in bytes:" + context.getState().getMaxUniformBlockSize());
            System.out.println("Max texture array layers:" + context.getState().getMaxArrayTextureLayers());

            // Load geometry, shaders, and camera information, and initialize basic graphics state
            resources.initialize();

            // Print a warning if color calibration data was not supplied either in the user interface or in the view set file.
            if(!reflectanceDataAccess.getViewSet().hasCustomLuminanceEncoding())
            {
                System.out.println("WARNING: no luminance mapping found.  Reflectance values are not physically grounded.");
            }

            // Set the primary view to the one selected in the user interface.
            reflectanceDataAccess.getViewSet().setPrimaryView(options.getPrimaryViewName());
            System.out.println("Primary view: " + options.getPrimaryViewName());
            System.out.println("Primary view index: " + reflectanceDataAccess.getViewSet().getPrimaryViewIndex());

            // Rescale images if requested.
            if (options.isImageRescalingEnabled())
            {
                reflectanceDataAccess.rescaleImages(context, options.getImageWidth(), options.getImageHeight(), rescaleDir);
            }

            // Load all the images and calibrate the intensity of the light source.
            resources.loadImagesAndCalibrateLight();

            // Update the geometry file name and the relative path to the image directory in the view set.
            reflectanceDataAccess.getViewSet().setGeometryFileName(modelFile.getName());

            if (options.isImageRescalingEnabled())
            {
                reflectanceDataAccess.getViewSet().setRelativeImagePathName(outputDir.toPath().relativize(rescaleDir.toPath()).toString());
            }
            else
            {
                reflectanceDataAccess.getViewSet().setRelativeImagePathName(outputDir.toPath().relativize(imageDir.toPath()).toString());
            }

            // Make sure the output directory exists.
            outputDir.mkdirs();

            // Write the updated view set file to the output directory.
            try(FileOutputStream outputStream = new FileOutputStream(new File(outputDir, cameraFile.getName().split("\\.")[0] + ".vset")))
            {
                reflectanceDataAccess.getViewSet().writeVSETFileToStream(outputStream);
                outputStream.flush();
            }

            if (options.isDiffuseTextureEnabled() || options.isSpecularTextureEnabled())
            {
                // Create a copy of the model in the output directory.
                Files.copy(modelFile.toPath(), new File(outputDir, modelFile.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Fit to the parameterized model.
                ParameterFittingResult result = new ParameterFitting<>(context, resources, options).fit();

                System.out.println("Saving textures...");
                Date timestamp = new Date();

                // Save the textures containing the parameters.
                result.writeToFiles(new File(outputDir, resources.getMaterialFileName()), resources.getMaterialName());

                System.out.println("Textures saved in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
            }
        }
    }
}
