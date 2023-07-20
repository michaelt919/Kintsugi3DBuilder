/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.rendering.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.gl.geometry.*;
import tetzlaff.gl.material.TextureLoadOptions;
import tetzlaff.gl.vecmath.IntVector2;
import tetzlaff.ibrelight.core.*;
import tetzlaff.util.ImageFinder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.function.Supplier;

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
        int texWidth, int texHeight, LoadingMonitor loadingMonitor) throws IOException
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
     * @param loadingMonitor
     * @throws IOException
     */
    IBRResourcesTextureSpace(IBRSharedResources<ContextType> sharedResources, Supplier<GeometryTextures<ContextType>> geometryTextureFactory,
         File textureDirectory, TextureLoadOptions loadOptions, int texWidth, int texHeight, LoadingMonitor loadingMonitor) throws IOException
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

        if(loadingMonitor != null)
        {
            loadingMonitor.setMaximum(getViewSet().getCameraPoseCount());
        }

        // Iterate over the layers to load in the texture array
        for (int k = 0; k < getViewSet().getCameraPoseCount(); k++)
        {
            log.info("Loading camera pose {}/{}", k, getViewSet().getCameraPoseCount());

            textureArray.loadLayer(k,
                    ImageFinder.getInstance().findImageFile(new File(textureDirectory, getViewSet().getImageFileName(k))),
                    true);

            if(loadingMonitor != null)
            {
                loadingMonitor.setProgress(k+1);
            }
        }

        log.info("View Set textures loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

        if (loadingMonitor != null)
        {
            loadingMonitor.setMaximum(0.0);
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
     * @param loadingMonitor
     * @throws IOException
     */
    IBRResourcesTextureSpace(IBRSharedResources<ContextType> sharedResources, File textureDirectory,
         TextureLoadOptions loadOptions, int texWidth, int texHeight, LoadingMonitor loadingMonitor) throws IOException
    {
        this(sharedResources, () -> sharedResources.getGeometryResources().createGeometryFramebuffer(texWidth, texHeight),
            textureDirectory, loadOptions, texWidth, texHeight, loadingMonitor);
    }

    IBRResourcesTextureSpace(IBRSharedResources<ContextType> sharedResources, File textureDirectory,
         TextureLoadOptions loadOptions, IntVector2 dimensions, LoadingMonitor loadingMonitor) throws IOException
    {
        this(sharedResources, textureDirectory, loadOptions, dimensions.x, dimensions.y, loadingMonitor);
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
         TextureLoadOptions loadOptions, LoadingMonitor loadingMonitor) throws IOException
    {
        this(sharedResources, textureDirectory, loadOptions,
            readDimensionsFromFile(ImageFinder.getInstance().findImageFile(new File(textureDirectory,
                sharedResources.getViewSet().getImageFileName(sharedResources.getViewSet().getPrimaryViewIndex())))),
            loadingMonitor);
    }

    @Override
    public ProgramBuilder<ContextType> getShaderProgramBuilder(StandardRenderingMode renderingMode)
    {
        return getSharedResources().getShaderProgramBuilder(renderingMode)
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

    public TextureFitSettings getTextureFitSettings(float gamma)
    {
        return new TextureFitSettings(texWidth, texHeight, gamma);
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
