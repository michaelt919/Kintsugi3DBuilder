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

package kintsugi3d.builder.resources.ibr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.function.Supplier;
import javax.imageio.ImageIO;

import kintsugi3d.builder.core.*;
import kintsugi3d.gl.builders.ColorTextureBuilder;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.geometry.GeometryMode;
import kintsugi3d.gl.geometry.GeometryTextures;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.material.TextureLoadOptions;
import kintsugi3d.gl.vecmath.IntVector2;
import kintsugi3d.util.ImageFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IBRResourcesTextureSpace<ContextType extends Context<ContextType>> extends IBRResourcesBase<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(IBRResourcesTextureSpace.class);
    /**
     * Array of image data pre-projected into texture space
     */
    private Texture3D<ContextType> textureArray;

    /**
     * Simple rectangle for draw calls
     */
    private VertexBuffer<ContextType> rectangle;

    /**
     * Textures storing pre-computed geometry information per-texel
     */
    private GeometryTextures<ContextType> geometryTextures;

    private final int texWidth;
    private final int texHeight;


    IBRResourcesTextureSpace(
        ContextType context, ViewSet viewSet, VertexGeometry geometry, TextureLoadOptions loadOptions,
        int texWidth, int texHeight, ProgressMonitor progressMonitor) throws IOException
    {
        super(new IBRSharedResources<>(context, viewSet, geometry, loadOptions), true);

        // Deferred rendering: draw to geometry textures initially and then just draw using a rectangle and
        // the geometry info cached in the textures
        geometryTextures = getGeometryResources().createGeometryFramebuffer(texWidth, texHeight);

        rectangle = context.createRectangle();

        this.texWidth = texWidth;
        this.texHeight = texHeight;

        // TODO load images in texture space
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param sharedResources
     * @param geometryTextureFactory This instance will take ownership of the textures produced
     * @param textureDirectory
     * @param loadOptions
     * @param texWidth
     * @param texHeight
     * @param progressMonitor
     * @throws IOException
     */
    IBRResourcesTextureSpace(IBRSharedResources<ContextType> sharedResources, Supplier<GeometryTextures<ContextType>> geometryTextureFactory,
         File textureDirectory, TextureLoadOptions loadOptions, int texWidth, int texHeight, ProgressMonitor progressMonitor)
             throws IOException, UserCancellationException
    {
        super(sharedResources, false);

        this.texWidth = texWidth;
        this.texHeight = texHeight;

        Date timestamp = new Date();

        ColorTextureBuilder<ContextType, ? extends Texture3D<ContextType>> builder = getContext().getTextureFactory()
                .build2DColorTextureArray(texWidth, texHeight, getViewSet().getCameraPoseCount())
                .setLinearFilteringEnabled(loadOptions.isLinearFilteringRequested())
                .setMipmapsEnabled(loadOptions.areMipmapsRequested());

        if (loadOptions.isCompressionRequested())
        {
            this.textureArray = builder
                .setInternalFormat(CompressionFormat.RGB_PUNCHTHROUGH_ALPHA1_4BPP)
                .createTexture();
        }
        else
        {
            this.textureArray = builder
                .setInternalFormat(ColorFormat.RGBA8)
                .createTexture();
        }

        if(progressMonitor != null)
        {
            progressMonitor.setMaxProgress(getViewSet().getCameraPoseCount());
            progressMonitor.setStage(0, "Loading textures...");
        }

        try
        {
            // Iterate over the layers to load in the texture array
            for (int k = 0; k < getViewSet().getCameraPoseCount(); k++)
            {
                if (progressMonitor != null)
                {
                    progressMonitor.setProgress(k, MessageFormat.format("{0} ({1}/{2})", getViewSet().getImageFileName(k), k+1, getViewSet().getCameraPoseCount()));
                    progressMonitor.allowUserCancellation();
                }

                textureArray.loadLayer(k,
                    ImageFinder.getInstance().findImageFile(new File(textureDirectory, getViewSet().getImageFileName(k))),
                    true);
            }

            if (progressMonitor != null)
            {
                progressMonitor.setProgress(getViewSet().getCameraPoseCount(), "All images loaded.");
            }
        }
        catch (IOException e)
        {
            // Release any resources prior to rethrowing the exception
            super.close();
            textureArray.close();
            throw e;
        }

        log.info("View Set textures loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

        if (progressMonitor != null)
        {
            progressMonitor.setStage(1, "Finished loading textures.");
        }

        this.geometryTextures = geometryTextureFactory.get();
        this.rectangle = sharedResources.getContext().createRectangle();
    }

    /**
     * Generates geometry textures using default implementation (rendering the geometry to an offscreen framebuffer)
     * @param sharedResources
     * @param textureDirectory
     * @param loadOptions
     * @param texWidth
     * @param texHeight
     * @param progressMonitor
     * @throws IOException
     */
    IBRResourcesTextureSpace(IBRSharedResources<ContextType> sharedResources, File textureDirectory,
         TextureLoadOptions loadOptions, int texWidth, int texHeight, ProgressMonitor progressMonitor) throws IOException, UserCancellationException
    {
        this(sharedResources, () -> sharedResources.getGeometryResources().createGeometryFramebuffer(texWidth, texHeight),
            textureDirectory, loadOptions, texWidth, texHeight, progressMonitor);
    }

    IBRResourcesTextureSpace(IBRSharedResources<ContextType> sharedResources, File textureDirectory,
         TextureLoadOptions loadOptions, IntVector2 dimensions, ProgressMonitor progressMonitor) throws IOException, UserCancellationException
    {
        this(sharedResources, textureDirectory, loadOptions, dimensions.x, dimensions.y, progressMonitor);
    }

    private static IntVector2 readDimensionsFromFile(File imageFile) throws IOException
    {
        // Read an image to get the width and height
        BufferedImage image = ImageIO.read(imageFile);
        return new IntVector2(image.getWidth(), image.getHeight());
    }

    /**
     * Loads an image an extra time to infer the width and height
     * @param sharedResources
     * @param textureDirectory
     * @throws IOException
     */
    IBRResourcesTextureSpace(IBRSharedResources<ContextType> sharedResources, File textureDirectory,
         TextureLoadOptions loadOptions, ProgressMonitor progressMonitor) throws IOException, UserCancellationException
    {
        this(sharedResources, textureDirectory, loadOptions,
            readDimensionsFromFile(ImageFinder.getInstance().findImageFile(new File(textureDirectory,
                sharedResources.getViewSet().getImageFileName(0)))),
            progressMonitor);
    }

    @Override
    public ProgramBuilder<ContextType> getShaderProgramBuilder()
    {
        return getSharedResources().getShaderProgramBuilder()
            .define("GEOMETRY_TEXTURES_ENABLED", true)
            .define("GEOMETRY_MODE", GeometryMode.RECTANGLE)
            .define("COLOR_APPEARANCE_MODE", ColorAppearanceMode.TEXTURE_SPACE);
    }

    @Override
    public void setupShaderProgram(Program<ContextType> program)
    {
        getSharedResources().setupShaderProgram(program);
        geometryTextures.setupShaderProgram(program);
        program.setTexture("viewImages", textureArray);
    }

    @Override
    public Drawable<ContextType> createDrawable(Program<ContextType> program)
    {
        Drawable<ContextType> drawable = getSharedResources().getContext().createDrawable(program);
        drawable.addVertexBuffer("position", rectangle);
        drawable.setDefaultPrimitiveMode(PrimitiveMode.TRIANGLE_FAN);
        return drawable;
    }

    public TextureResolution getTextureResolution()
    {
        return new TextureResolution(texWidth, texHeight);
    }

    @Override
    public void close()
    {
        super.close();

        if (this.geometryTextures != null)
        {
            this.geometryTextures.close();
            this.geometryTextures = null;
        }

        if (this.rectangle != null)
        {
            this.rectangle.close();
            this.rectangle = null;
        }

        if (this.textureArray != null)
        {
            this.textureArray.close();
            this.textureArray = null;
        }
    }
}
