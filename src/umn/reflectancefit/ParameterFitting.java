/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.reflectancefit;

import java.util.Date;

import umn.gl.core.*;

/**
 * An object that drives the sequence of steps for estimating reflectance parameters.
 * @param <ContextType> The typ of the graphics context to use for reflectance estimation steps.
 */
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class ParameterFitting<ContextType extends Context<ContextType>>
{
    private final ContextType context;
    private final ParameterFittingResources<ContextType> resources;
    private final Options options;

    /**
     * Creates a new object for performing reflectance parameter fitting.
     * @param context The graphics context to use for each reflectance estimation step.
     * @param resources The graphics resources required for reflectance estimation.
     * @param options Options that may control the behavior of various steps of the reflectance parameter fitting process.
     */
    public ParameterFitting(ContextType context, ParameterFittingResources<ContextType> resources, Options options)
    {
        this.context = context;
        this.resources = resources;
        this.options = options;
    }

    /**
     * Runs the reflectance parameter fitting algorithm.
     * @return The results of reflectance parameter fitting.
     */
    public ParameterFittingResult fit()
    {
        try
        (
            // Create three framebuffers
            FramebufferObject<ContextType> framebuffer1 =
                this.context.buildFramebufferObject(this.options.getTextureSize(), this.options.getTextureSize())
                    .addColorAttachments(ColorFormat.RGBA32F, 5)
                    .createFramebufferObject();

            FramebufferObject<ContextType> framebuffer2 =
                this.context.buildFramebufferObject(this.options.getTextureSize(), this.options.getTextureSize())
                    .addColorAttachments(ColorFormat.RGBA32F, 5)
                    .createFramebufferObject();

            FramebufferObject<ContextType> frontFramebufferSpecular =
                this.context.buildFramebufferObject(this.options.getTextureSize(), this.options.getTextureSize())
                    .addColorAttachments(ColorFormat.RGBA32F, 5)
                    .createFramebufferObject()
        )
        {
            FramebufferObject<ContextType> diffuseFitFramebuffer = framebuffer1;

            // Fit the diffuse reflectance, if requested.
            if (this.options.isDiffuseTextureEnabled())
            {
                System.out.println("Beginning diffuse fit (" + (this.options.getTextureSubdivision() * this.options.getTextureSubdivision()) + " blocks)...");
                Date timestamp = new Date();

                diffuseFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                diffuseFitFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
                diffuseFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                diffuseFitFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);

                DiffuseFit<ContextType> diffuseFit = resources.createDiffuseFit(diffuseFitFramebuffer, this.options.getTextureSubdivision());

                diffuseFit.fitImageSpace(resources.getViewTextures(), resources.getDepthTextures(),
                    (row, col) ->
                        System.out.println("Block " + (row * this.options.getTextureSubdivision() + col + 1) + '/' +
                            (this.options.getTextureSubdivision() * this.options.getTextureSubdivision()) + " completed."));

                System.out.println("Diffuse fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

                System.out.println("Filling empty regions...");
            }
            else
            {
                System.out.println("Skipping diffuse fit.");
            }

            Date timestamp = new Date();

            // Fill holes in the diffuse fit.
            try(VertexBuffer<ContextType> rectBuffer = this.context.createRectangle())
            {
                Drawable<ContextType> holeFillRenderable = this.context.createDrawable(resources.getHoleFillProgram());
                holeFillRenderable.addVertexBuffer("position", rectBuffer);
                resources.getHoleFillProgram().setUniform("minFillAlpha", 0.5f);

                FramebufferObject<ContextType> backFramebuffer = framebuffer2;

                if (this.options.isDiffuseTextureEnabled())
                {
                    System.out.println("Diffuse fill...");

                    // Diffuse
                    FramebufferObject<ContextType> frontFramebuffer = diffuseFitFramebuffer;
                    for (int i = 0; i < this.options.getTextureSize() / 2; i++)
                    {
                        backFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
                        backFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);

                        resources.getHoleFillProgram().setTexture("input0", frontFramebuffer.getColorAttachmentTexture(0));
                        resources.getHoleFillProgram().setTexture("input1", frontFramebuffer.getColorAttachmentTexture(1));
                        resources.getHoleFillProgram().setTexture("input2", frontFramebuffer.getColorAttachmentTexture(2));
                        resources.getHoleFillProgram().setTexture("input3", frontFramebuffer.getColorAttachmentTexture(3));

                        holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, backFramebuffer);
                        this.context.finish();

                        FramebufferObject<ContextType> tmp = frontFramebuffer;
                        frontFramebuffer = backFramebuffer;
                        backFramebuffer = tmp;
                    }

                    diffuseFitFramebuffer = frontFramebuffer;

                    System.out.println("Empty regions filled in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
                }

                // Fit the specular reflectance parameters.
                if (this.options.isSpecularTextureEnabled())
                {
                    System.out.println("Fitting specular residual...");
                    System.out.println("Creating specular reflectivity texture...");

                    frontFramebufferSpecular.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                    frontFramebufferSpecular.clearColorBuffer(1, 0.5f, 0.5f, 1.0f, 1.0f); // normal map
                    frontFramebufferSpecular.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                    frontFramebufferSpecular.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
                    frontFramebufferSpecular.clearColorBuffer(4, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, 1.0f);

                    backFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                    backFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
                    backFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                    backFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
                    backFramebuffer.clearColorBuffer(4, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, 1.0f);

                    SpecularFit<ContextType> specularFit = resources.createSpecularFit(backFramebuffer, this.options.getTextureSubdivision());

                    specularFit.fitImageSpace(resources.getViewTextures(), resources.getDepthTextures(),
                        (this.options.isDiffuseTextureEnabled() ? diffuseFitFramebuffer : frontFramebufferSpecular).getColorAttachmentTexture(0),
                        (this.options.isDiffuseTextureEnabled() ? diffuseFitFramebuffer : frontFramebufferSpecular).getColorAttachmentTexture(1),
                        (row, col) ->
                            System.out.println("Block " + (row * this.options.getTextureSubdivision() + col + 1) + '/' +
                                (this.options.getTextureSubdivision() * this.options.getTextureSubdivision()) + " completed."));

                    FramebufferObject<ContextType> frontFramebufferHoleFill = backFramebuffer;
                    FramebufferObject<ContextType> backFramebufferHoleFill = frontFramebufferSpecular;

                    // Fill holes in the specular parameters.
                    for (int i = 0; i < this.options.getTextureSize() / 2; i++)
                    {
                        backFramebufferHoleFill.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebufferHoleFill.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebufferHoleFill.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebufferHoleFill.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
                        backFramebufferHoleFill.clearColorBuffer(4, 0.0f, 0.0f, 0.0f, 0.0f);

                        resources.getHoleFillProgram().setTexture("input0", frontFramebufferHoleFill.getColorAttachmentTexture(0));
                        resources.getHoleFillProgram().setTexture("input1", frontFramebufferHoleFill.getColorAttachmentTexture(1));
                        resources.getHoleFillProgram().setTexture("input2", frontFramebufferHoleFill.getColorAttachmentTexture(2));
                        resources.getHoleFillProgram().setTexture("input3", frontFramebufferHoleFill.getColorAttachmentTexture(3));
                        resources.getHoleFillProgram().setTexture("input4", frontFramebufferHoleFill.getColorAttachmentTexture(4));

                        holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, backFramebufferHoleFill);
                        this.context.finish();

                        FramebufferObject<ContextType> tmp = frontFramebufferHoleFill;
                        frontFramebufferHoleFill = backFramebufferHoleFill;
                        backFramebufferHoleFill = tmp;
                    }

                    return ParameterFittingResult.fromFramebuffer(frontFramebufferHoleFill, this.options);
                }
                else
                {
                    return ParameterFittingResult.fromFramebuffer(diffuseFitFramebuffer, this.options);
                }
            }
        }
    }
}
