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

package tetzlaff.optimization;

import java.util.stream.IntStream;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.PrimitiveMode;

public class ShaderBasedErrorCalculator
{
    private final ErrorReport report;

    public ShaderBasedErrorCalculator(int sampleCount)
    {
        this.report = new ErrorReport(sampleCount);
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

    public <ContextType extends Context<ContextType>> void update(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        // Clear framebuffer
        framebuffer.clearDepthBuffer();
        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

        // Run shader program to fill framebuffer with per-pixel error.
        drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

        // Copy framebuffer from GPU to main memory.
        float[] pixelErrors = framebuffer.readFloatingPointColorBufferRGBA(0);

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

        report.setError(Math.sqrt(errorTotal.error / errorTotal.weight));
    }

    public void reject()
    {
        // Roll back to previous error calculation.
        report.reject();
    }
}
