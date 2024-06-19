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

package kintsugi3d.optimization;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import kintsugi3d.gl.core.*;

public class ShaderBasedErrorCalculator<ContextType extends Context<ContextType>> implements Resource
{
    // Compare fitted models against actual photographs
    private ProgramObject<ContextType> program;
    private Drawable<ContextType> drawable;

    // Framebuffer for calculating error and reconstructing 3D renderings of the object
    private FramebufferObject<ContextType> framebuffer;

    private final ErrorReport report;

    public static <ContextType extends Context<ContextType>> ShaderBasedErrorCalculator<ContextType> create(
            ContextType context, Supplier<ProgramObject<ContextType>> programFactory,
            Function<Program<ContextType>, Drawable<ContextType>> drawableFactory, int imageWidth, int imageHeight)
    {
        return new ShaderBasedErrorCalculator<>(context, programFactory, drawableFactory, imageWidth, imageHeight);
    }

    private ShaderBasedErrorCalculator(ContextType context, Supplier<ProgramObject<ContextType>> programFactory,
        Function<Program<ContextType>, Drawable<ContextType>> drawableFactory, int imageWidth, int imageHeight)
    {
        this.program = programFactory.get();
        this.drawable = drawableFactory.apply(program);

        this.framebuffer = context.buildFramebufferObject(imageWidth, imageHeight)
                .addColorAttachment(ColorFormat.RGBA32F)
                .addDepthAttachment()
                .createFramebufferObject();
        this.report = new ErrorReport(imageWidth * imageHeight);
    }

    public Program<ContextType> getProgram()
    {
        return program;
    }

    public Framebuffer<ContextType> getFramebuffer()
    {
        return framebuffer;
    }

    public ReadonlyErrorReport getReport()
    {
        return report;
    }

    @SuppressWarnings("PackageVisibleField")
    private static class WeightedError
    {
        double error;
        double weight;

        WeightedError(double error, double weight)
        {
            this.error = error;
            this.weight = weight;
        }
    }

    public void update()
    {
        // Clear framebuffer
        framebuffer.clearDepthBuffer();
        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

        // Run shader program to fill framebuffer with per-pixel error.
        drawable.draw(framebuffer);

        // Copy framebuffer from GPU to main memory.
        float[] pixelErrors = framebuffer.getTextureReaderForColorAttachment(0).readFloatingPointRGBA();

        // Add up per-pixel error.
        WeightedError errorTotal = IntStream.range(0, report.getSampleCount())
            .parallel()
            .filter(p -> pixelErrors[4 * p + 3] > 0)
            .collect(() -> new WeightedError(0, 0),
                (total, p) ->
                {
                    total.error += pixelErrors[4 * p];
                    total.weight += pixelErrors[4 * p + 3];
                },
                (total1, total2) ->
                {
                    total1.error += total2.error;
                    total1.weight += total2.weight;
                });

        report.setError(Math.sqrt(errorTotal.error / Math.max(1.0, errorTotal.weight)));
    }

    public void reject()
    {
        // Roll back to previous error calculation.
        report.reject();
    }

    @Override
    public void close()
    {
        if (program != null)
        {
            program.close();
            program = null;
        }

        if (drawable != null)
        {
            drawable.close();
            drawable = null;
        }

        if (framebuffer != null)
        {
            framebuffer.close();
            framebuffer = null;
        }
    }
}
