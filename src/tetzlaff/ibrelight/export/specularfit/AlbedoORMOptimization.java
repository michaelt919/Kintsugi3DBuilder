/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.util.ShaderHoleFill;

public class AlbedoORMOptimization<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private static final Logger log = LoggerFactory.getLogger(AlbedoORMOptimization.class);

    // Graphics context
    private final ContextType context;

    // Estimation program
    private Program<ContextType> estimationProgram;

    // Framebuffer for storing the diffuse solution
    private FramebufferObject<ContextType> framebuffer;

    private VertexBuffer<ContextType> rect;
    private final Drawable<ContextType> drawable;

    public AlbedoORMOptimization(ContextType context, TextureFitSettings settings)
        throws FileNotFoundException
    {
        this.context = context;
        estimationProgram = createProgram(context);
        framebuffer = context.buildFramebufferObject(settings.width, settings.height)
            .addColorAttachment(ColorFormat.RGBA32F) // total albedo
            .addColorAttachment(ColorFormat.RGBA32F) // ORM
            .createFramebufferObject();

        // Create basic rectangle vertex buffer
        rect = context.createRectangle();
        drawable = context.createDrawable(estimationProgram);
        drawable.setDefaultPrimitiveMode(PrimitiveMode.TRIANGLE_FAN);
        drawable.addVertexBuffer("position", rect);

        estimationProgram.setUniform("gamma", settings.gamma);
    }

    public void execute(SpecularResources<ContextType> specularFit)
    {
        // Set up shader program
        estimationProgram.setTexture("diffuseEstimate", specularFit.getDiffuseMap());
        estimationProgram.setTexture("specularEstimate", specularFit.getSpecularReflectivityMap());
        estimationProgram.setTexture("roughnessEstimate", specularFit.getSpecularRoughnessMap());

//        // Second framebuffer for filling holes (used to double-buffer the first framebuffer)
//        // Placed outside of try-with-resources since it might end up being the primary framebuffer after filling holes.
//        FramebufferObject<ContextType> framebuffer2 = context.buildFramebufferObject(settings.width, settings.height)
//            .addColorAttachment(ColorFormat.RGBA32F)
//            .createFramebufferObject();

//        // Will reference the framebuffer that is in front after hole filling if everything is successful.
//        FramebufferObject<ContextType> finalDiffuse = null;

        try (ShaderHoleFill<ContextType> holeFill = new ShaderHoleFill<>(context))
        {
            // Perform diffuse fit
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
            framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
            framebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
            drawable.draw(framebuffer);

//            // Fill holes
//            finalDiffuse = holeFill.execute(framebuffer, framebuffer2);
        }
        catch (FileNotFoundException e)
        {
            log.error("An error occurred:", e);
        }
//        finally
//        {
//            if (Objects.equals(finalDiffuse, framebuffer2))
//            {
                // New framebuffer is the front framebuffer after filling holes;
                // close the old one and make the new one the primary framebuffer
//                framebuffer.close();
//                framebuffer = framebuffer2;
//            }
//            else
//            {
//                // New framebuffer is the back framebuffer after filling holes (or an exception occurred);
//                // either way; just close it and leave the primary framebuffer the same.
//                framebuffer2.close();
//            }
//        }
    }

    @Override
    public void close()
    {
        if (estimationProgram != null)
        {
            estimationProgram.close();
            estimationProgram = null;
        }

        if (framebuffer != null)
        {
            framebuffer.close();
            framebuffer = null;
        }

        if (rect != null)
        {
            rect.close();
            rect = null;
        }
    }

    public Texture2D<ContextType> getAlbedoMap()
    {
        return framebuffer.getColorAttachmentTexture(0);
    }

    public Texture2D<ContextType> getORMMap()
    {
        return framebuffer.getColorAttachmentTexture(1);
    }

    public void saveTextures(File outputDirectory)
    {
        try
        {
            framebuffer.saveColorBufferToFile(0, "PNG", new File(outputDirectory, "albedo.png"));
            framebuffer.saveColorBufferToFile(1, "PNG", new File(outputDirectory, "orm.png"));
        }
        catch (IOException e)
        {
            log.error("An error occurred while saving textures:", e);
        }
    }

    private static <ContextType extends Context<ContextType>>
        Program<ContextType> createProgram(ContextType context) throws FileNotFoundException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/estimateAlbedoORM.frag"))
            .createProgram();
    }
}
