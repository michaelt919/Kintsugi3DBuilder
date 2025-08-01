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

package kintsugi3d.builder.rendering;

import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.metrics.ColorAppearanceRMSE;
import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.builders.framebuffer.FramebufferObjectBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.vecmath.DoubleVector3;
import kintsugi3d.util.BufferedImageColorList;
import kintsugi3d.util.ColorImage;
import kintsugi3d.util.SRGB;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public class ImageReconstruction<ContextType extends Context<ContextType>> implements AutoCloseable, Iterable<ReconstructionView<ContextType>>
{
    private final ReadonlyViewSet viewSet;
    private final IntFunction<ColorImage> groundTruthLoader;
    private final ProgramObject<ContextType> incidentRadianceProgram;
    private final Drawable<ContextType> incidentRadianceDrawable;

    private final Consumer<FramebufferObjectBuilder<ContextType>> buildFramebufferAttachments;
    private final Consumer<FramebufferObjectBuilder<ContextType>> buildIncidentRadianceFramebufferAttachments;

    private FramebufferObject<ContextType> reconstructionFramebuffer;
    private FramebufferObject<ContextType> incidentRadianceFramebuffer;

    private FloatBuffer incidentRadianceBuffer;
    private FloatBuffer reconstructionBuffer;

    public ImageReconstruction(
        ReadonlyViewSet viewSet,
        Consumer<FramebufferObjectBuilder<ContextType>> buildFramebufferAttachments,
        Consumer<FramebufferObjectBuilder<ContextType>> buildIncidentRadianceFramebufferAttachments,
        ProgramBuilder<ContextType> incidentRadianceProgramBuilder,
        ReadonlyIBRResources<ContextType> resources)
        throws IOException
    {
        this(viewSet, buildFramebufferAttachments, buildIncidentRadianceFramebufferAttachments, incidentRadianceProgramBuilder, resources,
            viewIndex ->
            {
                // load new ground truth
                try
                {
                    BufferedImage groundTruthImage = ImageIO.read(viewSet.findFullResImageFile(viewIndex));
                    return new BufferedImageColorList(groundTruthImage);
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
            Consumer<FramebufferObjectBuilder<ContextType>> buildFramebufferAttachments,
            Consumer<FramebufferObjectBuilder<ContextType>> buildIncidentRadianceFramebufferAttachments,
            ProgramBuilder<ContextType> incidentRadianceProgramBuilder,
            ReadonlyIBRResources<ContextType> resources,
            IntFunction<ColorImage> groundTruthLoader)
        throws IOException
    {
        this.viewSet = viewSet;
        this.incidentRadianceProgram = incidentRadianceProgramBuilder.createProgram();
        this.incidentRadianceDrawable = resources.createDrawable(incidentRadianceProgram);
        this.groundTruthLoader = groundTruthLoader;

        this.buildFramebufferAttachments = buildFramebufferAttachments;
        this.buildIncidentRadianceFramebufferAttachments = buildIncidentRadianceFramebufferAttachments;
//
//        FramebufferSize reconstructionSize = reconstructionFramebuffer.getSize();
//        reconstructionBuffer = BufferUtils.createFloatBuffer(reconstructionSize.width * reconstructionSize.height * 4);
//
//        FramebufferSize incidentRadianceSize = incidentRadianceFramebuffer.getSize(); // should be the same as reconstruction size
//        incidentRadianceBuffer = BufferUtils.createFloatBuffer(incidentRadianceSize.width * incidentRadianceSize.height * 4);
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
        private ColorImage currentGroundTruth;

        private void refresh()
        {
            // load new ground truth
            currentGroundTruth = groundTruthLoader.apply(viewIndex);

            // Create new framebuffer if necessary.
            if (incidentRadianceFramebuffer == null
                || incidentRadianceFramebuffer.getSize().width != currentGroundTruth.getWidth()
                || incidentRadianceFramebuffer.getSize().height != currentGroundTruth.getHeight())
            {
                if (incidentRadianceFramebuffer != null)
                {
                    incidentRadianceFramebuffer.close();
                }

                var builder = incidentRadianceDrawable.getContext()
                    .buildFramebufferObject(currentGroundTruth.getWidth(), currentGroundTruth.getHeight());
                buildIncidentRadianceFramebufferAttachments.accept(builder);
                incidentRadianceFramebuffer = builder.createFramebufferObject();
            }

            // render incident radiance for this view
            render(viewSet, viewIndex, incidentRadianceDrawable, incidentRadianceFramebuffer);

            // create new native buffer if necessary
            FramebufferSize incidentRadianceSize = incidentRadianceFramebuffer.getSize(); // should be the same as reconstruction size
            if (incidentRadianceBuffer == null || incidentRadianceBuffer.capacity() != incidentRadianceSize.width * incidentRadianceSize.height * 4)
            {
                incidentRadianceBuffer = BufferUtils.createFloatBuffer(incidentRadianceSize.width * incidentRadianceSize.height * 4);
            }

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
                public DoubleVector3 getIncidentRadiance(int pixelIndex)
                {
                    return new DoubleVector3(
                        incidentRadianceBuffer.get(4 * pixelIndex),
                        incidentRadianceBuffer.get(4 * pixelIndex + 1),
                        incidentRadianceBuffer.get(4 * pixelIndex + 2));
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
                    // Create new framebuffer if necessary.
                    if (reconstructionFramebuffer == null
                        || reconstructionFramebuffer.getSize().width != currentGroundTruth.getWidth()
                        || reconstructionFramebuffer.getSize().height != currentGroundTruth.getHeight())
                    {
                        if (reconstructionFramebuffer != null)
                        {
                            reconstructionFramebuffer.close();
                        }

                        var builder = drawable.getContext()
                            .buildFramebufferObject(currentGroundTruth.getWidth(), currentGroundTruth.getHeight());
                        buildFramebufferAttachments.accept(builder);
                        reconstructionFramebuffer = builder.createFramebufferObject();
                    }

                    // render view
                    render(viewSet, index, drawable, reconstructionFramebuffer);

                    // create new native buffer if necessary
                    FramebufferSize framebufferSize = reconstructionFramebuffer.getSize(); // should be the same as reconstruction size
                    if (reconstructionBuffer == null || reconstructionBuffer.capacity() != framebufferSize.width * framebufferSize.height * 4)
                    {
                        reconstructionBuffer = BufferUtils.createFloatBuffer(framebufferSize.width * framebufferSize.height * 4);
                    }

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
                                DoubleVector3 groundTruthEncoded = currentGroundTruth.get(p).getXYZ().asDoublePrecision();
                                DoubleVector3 incidentRadiance = getIncidentRadiance(p);
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
        incidentRadianceProgram.close();
        incidentRadianceDrawable.close();
    }
}
