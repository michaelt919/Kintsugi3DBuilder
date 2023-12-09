/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.rendering;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.metrics.ColorAppearanceRMSE;
import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.builders.framebuffer.FramebufferObjectBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.vecmath.DoubleVector3;
import kintsugi3d.util.SRGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageReconstruction<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private static final Logger log = LoggerFactory.getLogger(ImageReconstruction.class);
    private final ProgramObject<ContextType> program;

    private final Drawable<ContextType> drawable;

    private final FramebufferObject<ContextType> framebuffer;

    public ImageReconstruction(ReadonlyIBRResources<ContextType> resources, ProgramBuilder<ContextType> programBuilder,
                               FramebufferObjectBuilder<ContextType> framebufferObjectBuilder, Consumer<Program<ContextType>> programSetup)
        throws FileNotFoundException
    {
        this.program = programBuilder.createProgram();
        resources.setupShaderProgram(program);
        programSetup.accept(program);
        this.drawable = resources.createDrawable(program);

        this.framebuffer = framebufferObjectBuilder.createFramebufferObject();
    }

    /**
     *
     * @param viewSet
     * @param viewIndex
     * @param reconstructionAction
     * @param groundTruth
     * @return x-component stores rmse, y-component stores pixel count after masking
     */
    public ColorAppearanceRMSE execute(ReadonlyViewSet viewSet, int viewIndex, Consumer<Framebuffer<ContextType>> reconstructionAction,
        IntFunction<DoubleVector3> incidentRadianceLookup, BufferedImage groundTruth)
        throws IOException
    {
        float gamma = viewSet.getGamma();  // TODO: use proper sRGB, not gamma correction

        // Reconstruct the view
        drawable.program().setUniform("model_view", viewSet.getCameraPose(viewIndex));
        drawable.program().setUniform("projection",
            viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex)).getProjectionMatrix(
                viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
        drawable.program().setUniform("reconstructionCameraPos",
            viewSet.getCameraPoseInverse(viewIndex).getColumn(3).getXYZ());
        drawable.program().setUniform("reconstructionLightPos",
            viewSet.getCameraPoseInverse(viewIndex).times(viewSet.getLightPosition(viewSet.getLightIndex(viewIndex)).asPosition()).getXYZ());
        drawable.program().setUniform("reconstructionLightIntensity",
            viewSet.getLightIntensity(viewSet.getLightIndex(viewIndex)));
        drawable.program().setUniform("gamma", gamma);

        for (int i = 0; i < framebuffer.getColorAttachmentCount(); i++)
        {
            // Clear to black
            framebuffer.clearColorBuffer(i, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        // Also clear the depth buffer
        framebuffer.clearDepthBuffer();

        // Draw the view into the framebuffer.
        drawable.draw(framebuffer);

        // Give the callback an opportunity to do something with the view.
        if (reconstructionAction != null)
        {
            reconstructionAction.accept(framebuffer);
        }

        float[] reconstruction = framebuffer.getTextureReaderForColorAttachment(0).readFloatingPointRGBA();

        log.info("View " + viewIndex + ':');

        long sampleCount = IntStream.range(0, reconstruction.length / 4)
            .parallel()
            .filter(p -> reconstruction[4 * p + 3] > 0.0) // only count pixels where we have geometry (mask out the rest)
            .count();

        ColorModel colorModel = groundTruth.getColorModel();

        DoubleVector3 totalRMSEPacked =
            IntStream.range(0, reconstruction.length / 4)
                .parallel()
                .filter(p -> reconstruction[4 * p + 3] > 0.0) // only count pixels where we have geometry (mask out the rest)
                .mapToObj(p ->
                {
                    int x = p % groundTruth.getWidth();
                    int y = groundTruth.getHeight() - 1 - p / groundTruth.getWidth();
                    int rgb = groundTruth.getRGB(x, y);

                    DoubleVector3 groundTruthEncoded =
                        new DoubleVector3(colorModel.getRed(rgb) / 255.0, colorModel.getGreen(rgb) / 255.0, colorModel.getBlue(rgb) / 255.0);
                    DoubleVector3 incidentRadiance = incidentRadianceLookup.apply(p);
                    DoubleVector3 reconstructedLinear = new DoubleVector3(reconstruction[4 * p], reconstruction[4 * p + 1], reconstruction[4 * p + 2]);

                    // Handle NaN values -- replace with black
                    reconstructedLinear = reconstructedLinear.applyOperator(z -> Double.isNaN(z) ? 0.0 : z);

                    DoubleVector3 reconstructedSRGB = SRGB.fromLinear(reconstructedLinear);

                    DoubleVector3 reconstructedEncoded;
                    DoubleVector3 groundTruthLinear;
                    DoubleVector3 groundTruthSRGB;

                    if (viewSet.getLuminanceEncoding() != null)
                    {
                        reconstructedEncoded = viewSet.getLuminanceEncoding().encode(reconstructedLinear.times(incidentRadiance));
                        groundTruthLinear = viewSet.getLuminanceEncoding().decode(groundTruthEncoded).dividedBy(incidentRadiance);
                        groundTruthSRGB = SRGB.fromLinear(groundTruthLinear);
                    }
                    else
                    {
                        reconstructedEncoded = reconstructedSRGB;
                        groundTruthSRGB = groundTruthEncoded;
                        groundTruthLinear = SRGB.toLinear(groundTruthSRGB);
                    }

                    DoubleVector3 encodedError = groundTruthEncoded.minus(reconstructedEncoded);
                    double encodedSqError = encodedError.dot(encodedError) / 3; // mean squared error for the three channels

                    DoubleVector3 sRGBError = groundTruthSRGB.minus(reconstructedSRGB);
                    double sRGBSqError = sRGBError.dot(sRGBError) / 3; // mean squared error for the three channels

                    DoubleVector3 linearError = groundTruthLinear.minus(reconstructedLinear);
                    double linearSqError = linearError.dot(linearError) / 3; // mean squared error for the three channels

                    return new DoubleVector3(encodedSqError, sRGBSqError, linearSqError); // pack results into a Vector3
                })
                .reduce(DoubleVector3.ZERO, DoubleVector3::plus).dividedBy(sampleCount) // mean over pixels
                .applyOperator(Math::sqrt); // root

        ColorAppearanceRMSE totalRMSE = new ColorAppearanceRMSE();
        totalRMSE.setEncodedGroundTruth(totalRMSEPacked.x);
        totalRMSE.setNormalizedSRGB(totalRMSEPacked.y);
        totalRMSE.setNormalizedLinear(totalRMSEPacked.z);

        log.info("Raw sRGB RMSE: " + totalRMSE.getNormalizedSRGB());
        log.info("Raw linear RMSE: " + totalRMSE.getNormalizedLinear());

        // Multiply by the true whitepoint luminance (for an encoded value of 1.0) divided by pi, gamma corrected.
        // To match the original images dynamic range, the cosine-weighted reflectance values in the framebuffer
        // will have been pre-divided by this value / PI, then gamma corrected.
        // Multiplying will reconvert the RMSE back to being in terms of cosine-weighted, normalized reflectance.
        double decodedWhitePoint = viewSet.getLuminanceEncoding().decodeFunction.applyAsDouble(255.0);
        log.info("Decoded white point (as reflectance * pi): " + decodedWhitePoint);

        double normalizedSRGBRMSE = totalRMSE.getNormalizedSRGB() * Math.pow(decodedWhitePoint / Math.PI, 1.0 / gamma);
        log.info("Normalized sRGB RMSE (* white point / pi): " + normalizedSRGBRMSE);

        double normalizedLinearRMSE = totalRMSE.getNormalizedLinear() * decodedWhitePoint / Math.PI;
        log.info("Normalized linear RMSE (* white point / pi): " + normalizedLinearRMSE);

        ColorAppearanceRMSE finalRMSE = new ColorAppearanceRMSE();
        finalRMSE.setNormalizedSRGB(normalizedSRGBRMSE);
        finalRMSE.setNormalizedLinear(normalizedLinearRMSE);
        finalRMSE.setSampleCount(sampleCount);

        return finalRMSE;
    }

    @Override
    public void close()
    {
        program.close();
        framebuffer.close();
    }
}
