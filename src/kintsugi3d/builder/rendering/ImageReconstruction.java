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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.metrics.ColorAppearanceRMSE;
import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.builders.framebuffer.FramebufferObjectBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.vecmath.DoubleVector3;
import kintsugi3d.util.SRGB;
import org.lwjgl.*;

public class ImageReconstruction<ContextType extends Context<ContextType>> implements AutoCloseable, Iterable<ReconstructionView<ContextType>>
{
    private final ReadonlyViewSet viewSet;
    private final IntFunction<IntFunction<DoubleVector3>> groundTruthLoader;

    private final FramebufferObject<ContextType> incidentRadianceFramebuffer;
    private final ProgramObject<ContextType> incidentRadianceProgram;
    private final Drawable<ContextType> incidentRadianceDrawable;

    private final FloatBuffer incidentRadianceBuffer;

    private final FramebufferObject<ContextType> reconstructionFramebuffer;
    private final FloatBuffer reconstructionBuffer;

    public ImageReconstruction(
        ReadonlyViewSet viewSet,
        FramebufferObjectBuilder<ContextType> framebufferObjectBuilder,
        FramebufferObjectBuilder<ContextType> incidentRadianceFramebufferObjectBuilder,
        ProgramBuilder<ContextType> incidentRadianceProgramBuilder,
        ReadonlyIBRResources<ContextType> resources)
        throws FileNotFoundException
    {
        this(viewSet, framebufferObjectBuilder, incidentRadianceFramebufferObjectBuilder, incidentRadianceProgramBuilder, resources,
            viewIndex ->
            {
                // load new ground truth
                try
                {
                    BufferedImage groundTruthImage = ImageIO.read(viewSet.findFullResImageFile(viewIndex));

                    return p ->
                    {
                        int x = p % groundTruthImage.getWidth();
                        int y = groundTruthImage.getHeight() - 1 - p / groundTruthImage.getWidth();
                        int rgb = groundTruthImage.getRGB(x, y);

                        return new DoubleVector3(((rgb >>> 16) & 0xFF) / 255.0, ((rgb >>> 8) & 0xFF) / 255.0, (rgb & 0xFF) / 255.0);
                    };
                }
                catch (IOException e)
                {
                    //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException,ProhibitedExceptionThrown
                    throw new RuntimeException(e.toString());
                }
            }
        );
    }

    public ImageReconstruction(
            ReadonlyViewSet viewSet,
            FramebufferObjectBuilder<ContextType> framebufferObjectBuilder,
            FramebufferObjectBuilder<ContextType> incidentRadianceFramebufferObjectBuilder,
            ProgramBuilder<ContextType> incidentRadianceProgramBuilder,
            ReadonlyIBRResources<ContextType> resources,
            IntFunction<IntFunction<DoubleVector3>> groundTruthLoader)
        throws FileNotFoundException
    {
        this.viewSet = viewSet;
        this.reconstructionFramebuffer = framebufferObjectBuilder.createFramebufferObject();
        this.incidentRadianceFramebuffer = incidentRadianceFramebufferObjectBuilder.createFramebufferObject();
        this.incidentRadianceProgram = incidentRadianceProgramBuilder.createProgram();
        this.incidentRadianceDrawable = resources.createDrawable(incidentRadianceProgram);
        this.groundTruthLoader = groundTruthLoader;

        FramebufferSize reconstructionSize = reconstructionFramebuffer.getSize();
        reconstructionBuffer = BufferUtils.createFloatBuffer(reconstructionSize.width * reconstructionSize.height * 4);

        FramebufferSize incidentRadianceSize = incidentRadianceFramebuffer.getSize(); // should be the same as reconstruction size
        incidentRadianceBuffer = BufferUtils.createFloatBuffer(incidentRadianceSize.width * incidentRadianceSize.height * 4);
    }

    private static <ContextType extends Context<ContextType>> void render(
        ReadonlyViewSet viewSet, int viewIndex, Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
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

        for (int i = 0; i < framebuffer.getColorAttachmentCount(); i++)
        {
            // Clear to black
            framebuffer.clearColorBuffer(i, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        // Also clear the depth buffer
        framebuffer.clearDepthBuffer();

        // Draw the view into the framebuffer.
        drawable.draw(framebuffer);
    }

    public final class ReconstructionIterator implements ListIterator<ReconstructionView<ContextType>>
    {
        private int viewIndex = 0;
        private IntFunction<DoubleVector3> currentGroundTruth;

        private void refresh()
        {
            // load new ground truth
            currentGroundTruth = groundTruthLoader.apply(viewIndex);

            // render incident radiance for this view
            render(viewSet, viewIndex, incidentRadianceDrawable, incidentRadianceFramebuffer);
            incidentRadianceFramebuffer.getTextureReaderForColorAttachment(0).readFloatingPointRGBA(incidentRadianceBuffer);
        }

        private ReconstructionView<ContextType> makeReconstructionView()
        {
            return new ReconstructionView<>()
            {
                private final int index = ReconstructionIterator.this.viewIndex;

                @Override
                public int getIndex()
                {
                    return index;
                }

                @Override
                public Framebuffer<ContextType> getReconstructionFramebuffer()
                {
                    return reconstructionFramebuffer;
                }

                /**
                 * @param drawable
                 * @return x-component stores rmse, y-component stores pixel count after masking
                 */
                @Override
                public ColorAppearanceRMSE reconstruct(Drawable<ContextType> drawable)
                {
                    // Provide gamma to shader in case it's necessary for reconstruction
                    // TODO: use proper sRGB when possible, not gamma correction
                    float gamma = viewSet.getGamma();
                    drawable.program().setUniform("gamma", gamma);

                    render(viewSet, index, drawable, reconstructionFramebuffer);

                    reconstructionFramebuffer.getTextureReaderForColorAttachment(0).readFloatingPointRGBA(reconstructionBuffer);

                    long sampleCount = IntStream.range(0, reconstructionBuffer.limit() / 4)
                        .parallel()
                        .filter(p -> reconstructionBuffer.get(4 * p + 3) > 0.0) // only count pixels where we have geometry (mask out the rest)
                        .count();

                    DoubleVector3 totalRMSEPacked =
                        IntStream.range(0, reconstructionBuffer.limit() / 4)
                            .parallel()
                            .filter(p -> reconstructionBuffer.get(4 * p + 3) > 0.0) // only count pixels where we have geometry (mask out the rest)
                            .mapToObj(p ->
                            {
                                DoubleVector3 groundTruthEncoded = currentGroundTruth.apply(p);
                                DoubleVector3 incidentRadiance =
                                    new DoubleVector3(incidentRadianceBuffer.get(4 * p), incidentRadianceBuffer.get(4 * p + 1), incidentRadianceBuffer.get(4 * p + 2));
                                DoubleVector3 reconstructedLinear =
                                    new DoubleVector3(reconstructionBuffer.get(4 * p), reconstructionBuffer.get(4 * p + 1), reconstructionBuffer.get(4 * p + 2));

                                // Handle NaN values -- replace with black
                                reconstructedLinear = reconstructedLinear.applyOperator(z -> Double.isNaN(z) ? 0.0 : z);

                                DoubleVector3 reconstructedSRGB = SRGB.fromLinear(reconstructedLinear);
                                DoubleVector3 reconstructedEncoded = viewSet.getLuminanceEncoding().encode(reconstructedLinear.times(incidentRadiance)).dividedBy(255.0);
                                DoubleVector3 groundTruthLinear = viewSet.getLuminanceEncoding().decode(groundTruthEncoded.times(255.0)).dividedBy(incidentRadiance);
                                DoubleVector3 groundTruthSRGB = SRGB.fromLinear(groundTruthLinear);

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
                    totalRMSE.setSampleCount(sampleCount);

                    return totalRMSE;
                }
            };
        }

        @Override
        public boolean hasNext()
        {
            return viewIndex < viewSet.getCameraPoseCount();
        }

        @Override
        public ReconstructionView<ContextType> next()
        {
            if (hasNext())
            {
                refresh();
                ReconstructionView<ContextType> view = makeReconstructionView();
                viewIndex++;
                return view;
            }
            else
            {
                throw new NoSuchElementException("Reached the end of the view set");
            }
        }

        @Override
        public boolean hasPrevious()
        {
            return viewIndex > 0;
        }

        @Override
        public ReconstructionView<ContextType> previous()
        {
            if (hasPrevious())
            {
                viewIndex--;
                refresh();
                return makeReconstructionView();
            }
            else
            {
                throw new NoSuchElementException("Reached the beginning of the view set");
            }
        }

        public boolean canJump(int index)
        {
            return index >= 0 && index < viewSet.getCameraPoseCount();
        }

        public ReconstructionView<ContextType> jump(int index)
        {
            if (index >= 0 && index < viewSet.getCameraPoseCount())
            {
                this.viewIndex = index;
                refresh();
                return makeReconstructionView();
            }
            else
            {
                throw new NoSuchElementException("Illegal index for view set: " + index);
            }
        }

        @Override
        public int nextIndex()
        {
            return viewIndex + 1;
        }

        @Override
        public int previousIndex()
        {
            return viewIndex - 1;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("remove");
        }

        @Override
        public void set(ReconstructionView<ContextType> contextTypeReconstructionView)
        {
            throw new UnsupportedOperationException("set");
        }

        @Override
        public void add(ReconstructionView<ContextType> contextTypeReconstructionView)
        {
            throw new UnsupportedOperationException("add");
        }
    }

    @Override
    public ReconstructionIterator iterator()
    {
        //noinspection ReturnOfInnerClass
        return new ReconstructionIterator();
    }

    @Override
    public void close()
    {
        reconstructionFramebuffer.close();
        incidentRadianceFramebuffer.close();
        incidentRadianceProgram.close();
    }
}
