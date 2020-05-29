/*
 *  Copyright (c) Michael Tetzlaff 2020
  ~  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.nam2018;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;
import tetzlaff.util.NonNegativeLeastSquares;

public class Nam2018Request implements IBRRequest
{
    private final int width;
    private final int height;
    private final File outputDirectory;
    private final ReadonlySettingsModel settingsModel;

    private static final double SQRT_PI_OVER_3 = Math.sqrt(Math.PI / 3);
    private static final double PI_SQUARED = Math.PI * Math.PI;

    private static final boolean DEBUG = true;
    private static final int MAX_RUNNING_THREADS = 12;

    private static final int BASIS_COUNT = 8;
    private static final int MICROFACET_DISTRIBUTION_RESOLUTION = 90;
    private static final int BRDF_MATRIX_SIZE = BASIS_COUNT * (MICROFACET_DISTRIBUTION_RESOLUTION + 1);

    private float error = Float.POSITIVE_INFINITY;

    private final SimpleMatrix brdfATA = new SimpleMatrix(BRDF_MATRIX_SIZE, BRDF_MATRIX_SIZE, DMatrixRMaj.class);
    private final SimpleMatrix brdfATyRed = new SimpleMatrix(BRDF_MATRIX_SIZE, 1, DMatrixRMaj.class);
    private final SimpleMatrix brdfATyGreen = new SimpleMatrix(BRDF_MATRIX_SIZE, 1, DMatrixRMaj.class);
    private final SimpleMatrix brdfATyBlue = new SimpleMatrix(BRDF_MATRIX_SIZE, 1, DMatrixRMaj.class);
    private SimpleMatrix brdfSolutionRed;
    private SimpleMatrix brdfSolutionGreen;
    private SimpleMatrix brdfSolutionBlue;
    private SimpleMatrix weightSolution;

    private final Object threadsRunningLock = new Object();
    private int threadsRunning = 0;

    public Nam2018Request(int width, int height, File outputDirectory, ReadonlySettingsModel settingsModel)
    {
        this.width = width;
        this.height = height;
        this.outputDirectory = outputDirectory;
        this.settingsModel = settingsModel;

        weightSolution = new SimpleMatrix(BASIS_COUNT, width * height, DMatrixRMaj.class);
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback)
    {
        IBRResources<ContextType> resources = renderable.getResources();

        try
        (
            Program<ContextType> averageProgram = createAverageProgram(resources);
            Program<ContextType> reflectanceProgram = createReflectanceProgram(resources);
            FramebufferObject<ContextType> framebuffer = resources.context.buildFramebufferObject(width, height)
                .addColorAttachment(ColorFormat.RGBA32F)
                .addColorAttachment(ColorFormat.RGBA32F)
                .createFramebufferObject();
            FramebufferObject<ContextType> normalFramebuffer = resources.context.buildFramebufferObject(width, height)
                .addColorAttachment(ColorFormat.RGB8)
                .createFramebufferObject()
        )
        {
            Drawable<ContextType> averageDrawable = createDrawable(averageProgram, resources);

            initializeClusters(averageDrawable, framebuffer);

            Drawable<ContextType> reflectanceDrawable = createDrawable(reflectanceProgram, resources);
            normalFramebuffer.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);
            reflectanceDrawable.program().setTexture("normalMap", normalFramebuffer.getColorAttachmentTexture(0));

            float previousError;

            do
            {
                previousError = error;

                reconstructBRDFs(reflectanceDrawable, framebuffer, resources.viewSet.getCameraPoseCount());
                reconstructWeights();
                reconstructNormals();
            }
            while (error < previousError);

            saveBasisFunctions();
            saveWeightMaps();
            saveNormalMaps();
        }
        catch(FileNotFoundException e) // thrown by createReflectanceProgram
        {
            e.printStackTrace();
        }
    }

    private <ContextType extends Context<ContextType>>
    Program<ContextType> createAverageProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/nam2018/average.frag"))
            .createProgram();

        program.setUniform("occlusionEnabled", resources.depthTextures != null && this.settingsModel.getBoolean("occlusionEnabled"));
        program.setUniform("occlusionBias", this.settingsModel.getFloat("occlusionBias"));

        resources.setupShaderProgram(program);

        return program;
    }

    private <ContextType extends Context<ContextType>>
        Program<ContextType> createReflectanceProgram(IBRResources<ContextType> resources) throws FileNotFoundException
    {
        Program<ContextType> program = resources.getIBRShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/nam2018/extractReflectance.frag"))
            .define("PHYSICALLY_BASED_MASKING_SHADOWING", 1)
            .define("SMITH_MASKING_SHADOWING", 0)
            .createProgram();

        program.setUniform("occlusionEnabled", resources.depthTextures != null && this.settingsModel.getBoolean("occlusionEnabled"));
        program.setUniform("occlusionBias", this.settingsModel.getFloat("occlusionBias"));

        resources.setupShaderProgram(program);

        return program;
    }

    private static <ContextType extends Context<ContextType>> Drawable<ContextType>
        createDrawable(Program<ContextType> program, IBRResources<ContextType> resources)
    {
        Drawable<ContextType> drawable = program.getContext().createDrawable(program);
        drawable.addVertexBuffer("position", resources.positionBuffer);
        drawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
        drawable.addVertexBuffer("normal", resources.normalBuffer);
        drawable.addVertexBuffer("tangent", resources.tangentBuffer);
        return drawable;
    }

    private <ContextType extends Context<ContextType>> void initializeClusters(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer)
    {
        // Clear framebuffer
        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

        // Run shader program to fill framebuffer with per-pixel information.
        drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

        if (DEBUG)
        {
            try
            {
                framebuffer.saveColorBufferToFile(0, "PNG", new File(outputDirectory, "average.png"));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        float[] averages = framebuffer.readFloatingPointColorBufferRGBA(0);

        // k-means++ initialization
        Random random = new SecureRandom();

        // Randomly choose the first center.
        int firstCenterIndex;

        do
        {
            firstCenterIndex = random.nextInt(width * height);
        }
        while(averages[4 * firstCenterIndex + 3] < 1.0); // Make sure the center chosen is valid.

        Vector3[] centers = new Vector3[BASIS_COUNT];
        centers[0] = new Vector3(averages[4 * firstCenterIndex], averages[4 * firstCenterIndex + 1], averages[4 * firstCenterIndex + 2]);

        // Populate a CDF for the purpose of randomly selecting from a weighted probability distribution.
        double[] cdf = new double[width * height + 1];

        for (int b = 1; b < BASIS_COUNT; b++)
        {
            cdf[0] = 0.0;

            for (int p = 0; p < width * height; p++)
            {
                double minDistance = Double.MAX_VALUE;

                for (int b2 = 0; b2 < b; b2++)
                {
                    minDistance = Math.min(minDistance, centers[b2].distance(new Vector3(averages[4 * p], averages[4 * p + 1], averages[4 * p + 2])));
                }

                cdf[p + 1] = cdf[p] + minDistance * minDistance;
            }

            double x = random.nextDouble() * cdf[width * height - 1];

            //noinspection FloatingPointEquality
            if (x >= cdf[width * height - 1]) // It's possible but extremely unlikely that floating-point rounding would cause this to happen.
            {
                // In that extremely rare case, just set x to 0.0.
                x = 0.0;
            }

            // binarySearch returns index of a match if its found, or -insertionPoint - 1 if not (the more likely scenario).
            // insertionPoint is defined to be the index of the first element greater than the number searched for (the randomly generated value).
            int index = Arrays.binarySearch(cdf, x);

            if (index < 0) // The vast majority of the time this condition will be true, since floating-point matches are unlikely.
            {
                // We actually want insertionPoint - 1 since the CDF is offset by one from the actual array of colors.
                // i.e. if the random value falls between indices 3 and 4 in the CDF, the differential between those two indices is due to the color
                // at index 3, so we want to use 3 as are index.
                index = -index - 2;
            }

            // If the index was actually positive to begin with, that's probably fine; just make sure that it's a valid location.
            // It's also possible in theory for the index to be zero if the random number generator produced 0.0.
            while (index < 0 || averages[4 * index + 3] < 1.0)
            {
                // Search forward until a valid index is found.
                index++;

                // We shouldn't ever fail to find an index since x should have been less than the final (un-normalized) CDF total.
                // This means that there has to be some place where the CDF went up, corresponding to a valid index.
            }

            // We've found a new center.
            centers[b] = new Vector3(averages[4 * index], averages[4 * index + 1], averages[4 * index + 2]);
        }

        weightSolution = new SimpleMatrix(BASIS_COUNT, width * height, DMatrixRMaj.class);
        for (int p = 0; p < width * height; p++)
        {
            if (averages[4 * p + 3] >= 1.0)
            {
                int bMin = -1;

                double minDistance = Double.MAX_VALUE;

                for (int b = 0; b < BASIS_COUNT; b++)
                {
                    double distance = centers[b].distance(new Vector3(averages[4 * p], averages[4 * p + 1], averages[4 * p + 2]));
                    if (distance < minDistance)
                    {
                        minDistance = distance;
                        bMin = b;
                    }
                }

                weightSolution.set(bMin, p, 1.0);
            }
        }
    }

    private <ContextType extends Context<ContextType>> void reconstructBRDFs(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int viewCount)
    {
        buildReflectanceMatrix(drawable, framebuffer, viewCount);
        System.out.println("Finished building matrix; solving now...");
        brdfSolutionRed = NonNegativeLeastSquares.solvePremultiplied(brdfATA, brdfATyRed, 0.001);
        brdfSolutionGreen = NonNegativeLeastSquares.solvePremultiplied(brdfATA, brdfATyGreen, 0.001);
        brdfSolutionBlue = NonNegativeLeastSquares.solvePremultiplied(brdfATA, brdfATyBlue, 0.001);
        System.out.println("DONE!");
    }

    private <ContextType extends Context<ContextType>> void buildReflectanceMatrix(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int viewCount)
    {
        // Reinitialize matrices to zero.
        brdfATA.zero();
        brdfATyRed.zero();
        brdfATyGreen.zero();
        brdfATyBlue.zero();

        for (int k = 0; k < viewCount; k++)
        {
            synchronized (threadsRunningLock)
            {
                // Make sure that we don't have too many threads running.
                // Wait until a thread finishes if we're at the max.
                while (threadsRunning >= MAX_RUNNING_THREADS)
                {
                    try
                    {
                        threadsRunningLock.wait(30000); // Double check every 30 seconds if notifyAll() was not called
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            // Clear framebuffer
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
            framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);

            // Run shader program to fill framebuffer with per-pixel information.
            drawable.program().setUniform("viewIndex", k);
            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

            if (DEBUG)
            {
                try
                {
                    framebuffer.saveColorBufferToFile(0, "PNG", new File(outputDirectory, String.format("%02d.png", k)));
                    framebuffer.saveColorBufferToFile(1, "PNG", new File(outputDirectory, String.format("%02d_geom.png", k)));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            // Copy framebuffer from GPU to main memory.
            float[] colorAndVisibility = framebuffer.readFloatingPointColorBufferRGBA(0);
            float[] halfwayAndGeom = framebuffer.readFloatingPointColorBufferRGBA(1);

            synchronized (threadsRunningLock)
            {
                threadsRunning++;
            }

            int kCopy = k;

            Thread contributionThread = new Thread(() ->
            {
                try
                {
                    // Create scratch space for the thread handling this view.
                    SimpleMatrix contributionATA = new SimpleMatrix(BRDF_MATRIX_SIZE, BRDF_MATRIX_SIZE, DMatrixRMaj.class);
                    SimpleMatrix contributionATyRed = new SimpleMatrix(BRDF_MATRIX_SIZE, 1, DMatrixRMaj.class);
                    SimpleMatrix contributionATyGreen = new SimpleMatrix(BRDF_MATRIX_SIZE, 1, DMatrixRMaj.class);
                    SimpleMatrix contributionATyBlue = new SimpleMatrix(BRDF_MATRIX_SIZE, 1, DMatrixRMaj.class);

                    // Get the contributions from the current view.
                    getReflectanceMatrixContribution(colorAndVisibility, halfwayAndGeom,
                        contributionATA, contributionATyRed, contributionATyGreen, contributionATyBlue);

                    // Add the contribution into the main matrix and vectors.
                    synchronized (brdfATA)
                    {
                        CommonOps_DDRM.addEquals(brdfATA.getMatrix(), contributionATA.getMatrix());
                    }

                    synchronized (brdfATyRed)
                    {
                        CommonOps_DDRM.addEquals(brdfATyRed.getMatrix(), contributionATyRed.getMatrix());
                    }

                    synchronized (brdfATyGreen)
                    {
                        CommonOps_DDRM.addEquals(brdfATyGreen.getMatrix(), contributionATyGreen.getMatrix());
                    }

                    synchronized (brdfATyBlue)
                    {
                        CommonOps_DDRM.addEquals(brdfATyBlue.getMatrix(), contributionATyBlue.getMatrix());
                    }
                }
                finally
                {
                    synchronized (threadsRunningLock)
                    {
                        threadsRunning--;
                        threadsRunningLock.notifyAll();
                    }

                    System.out.println("Finished view " + kCopy + '.');
                }
            });

            contributionThread.start();
        }

        // Wait for all the threads to finish.
        synchronized (threadsRunningLock)
        {
            while (threadsRunning > 0)
            {
                try
                {
                    threadsRunningLock.wait(30000); // Double check every 30 seconds if notifyAll() was not called
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private void getReflectanceMatrixContribution(float[] colorAndVisibility, float[] halfwayAndGeom,
        SimpleMatrix contributionATA, SimpleMatrix contributionATyRed, SimpleMatrix contributionATyGreen, SimpleMatrix contributionATyBlue)
    {
        // Iterate over each sampled texture coordinate.
        for (int p = 0; p < width * height; p++)
        {
            // Skip any coordinates that weren't visible from the current view.
            if (colorAndVisibility[4 * p + 3] > 0)
            {
                double mExact = halfwayAndGeom[4 * p] * MICROFACET_DISTRIBUTION_RESOLUTION / SQRT_PI_OVER_3;
                int mFloor = Math.min(MICROFACET_DISTRIBUTION_RESOLUTION - 1,
                    (int) Math.floor(halfwayAndGeom[4 * p] * MICROFACET_DISTRIBUTION_RESOLUTION / SQRT_PI_OVER_3));

                // When floor and exact are the same, t = 1.0.  When exact is almost a whole increment greater than floor, t approaches 0.0.
                // If mFloor is clamped to MICROFACET_DISTRIBUTION_RESOLUTION -1, then mExact will be much larger, so t = 0.0.
                double t = Math.max(0.0, 1.0 + mFloor - mExact);

                for (int b1 = 0; b1 < BASIS_COUNT; b1++)
                {
                    int iStart = BASIS_COUNT * (mFloor + 1) + b1;

                    double weightedReflectanceRed   = weightSolution.get(b1, p) * colorAndVisibility[4 * p];
                    double weightedReflectanceGreen = weightSolution.get(b1, p) * colorAndVisibility[4 * p + 1];
                    double weightedReflectanceBlue  = weightSolution.get(b1, p) * colorAndVisibility[4 * p + 2];

                    // For each basis function: update the vector.
                    // Top partition of the vector corresponds to diffuse coefficients
                    contributionATyRed.set(b1, 0, contributionATyRed.get(b1, 0) - weightedReflectanceRed);
                    contributionATyGreen.set(b1, 0, contributionATyGreen.get(b1, 0) - weightedReflectanceGreen);
                    contributionATyBlue.set(b1, 0, contributionATyBlue.get(b1, 0) - weightedReflectanceBlue);

                    double weightedGeomReflectanceRed   = halfwayAndGeom[4 * p + 1] * weightedReflectanceRed;
                    double weightedGeomReflectanceGreen = halfwayAndGeom[4 * p + 1] * weightedReflectanceGreen;
                    double weightedGeomReflectanceBlue  = halfwayAndGeom[4 * p + 1] * weightedReflectanceBlue;

                    // Bottom partition of the vector corresponds to specular coefficients.
                    // For the first non-zero element of the specular function, scale by t to account for linear interpolation.
                    contributionATyRed.set(iStart, 0, contributionATyRed.get(iStart, 0) - t * weightedGeomReflectanceRed);
                    contributionATyGreen.set(iStart, 0, contributionATyGreen.get(iStart, 0) - t * weightedGeomReflectanceGreen);
                    contributionATyBlue.set(iStart, 0, contributionATyBlue.get(iStart, 0) - t * weightedGeomReflectanceBlue);

                    // Iterate over all elements in the specular (microfacet distribution) functions whose angle is greater than or equal to the current angle.
                    // We are actually calculating the differential of the specular function, to be integrated at the end.
                    for (int m = mFloor + 1; m < MICROFACET_DISTRIBUTION_RESOLUTION; m++)
                    {
                        int i = BASIS_COUNT * (m + 1) + b1;

                        contributionATyRed.set(i, 0, contributionATyRed.get(i, 0) - weightedGeomReflectanceRed);
                        contributionATyGreen.set(i, 0, contributionATyGreen.get(i, 0) - weightedGeomReflectanceGreen);
                        contributionATyBlue.set(i, 0, contributionATyBlue.get(i, 0) - weightedGeomReflectanceBlue);
                    }

                    // Reset i back to the beginning:
                    int i = iStart;

                    // For each pair of basis functions: update the matrix.
                    for (int b2 = 0; b2 < BASIS_COUNT; b2++)
                    {
                        // Top left partition of the matrix: row and column both correspond to diffuse coefficients
                        contributionATA.set(b1, b2, contributionATA.get(b1, b2) + weightSolution.get(b1, p) * weightSolution.get(b2, p) / PI_SQUARED);

                        double weightedGeom = weightSolution.get(b1, p) * weightSolution.get(b2, p) * halfwayAndGeom[4 * p + 1];
                        double weightedGeomOverPi = weightedGeom / Math.PI;

                        // Top right and bottom left partitions of the matrix:
                        // row corresponds to diffuse coefficients and column corresponds to specular, or vice-versa.
                        // For the first non-zero element of the specular function, scale by t to account for linear interpolation.
                        contributionATA.set(i, b2, contributionATA.get(i, b2) + t * weightedGeomOverPi);
                        contributionATA.set(b2, i, contributionATA.get(b2, i) + t * weightedGeomOverPi);

                        int j = BASIS_COUNT * (mFloor + 1) + b2;
                        double weightedGeomSq = weightedGeom * weightedGeom;
                        double tTimesWeightedGeomSq = t * weightedGeomSq;

                        // Bottom right partition of the matrix: row and column both correspond to specular.
                        // The first non-zero specular element in BOTH row AND column is handled here.
                        // Scale by t squared to account for linear interpolation for both row and column.
                        contributionATA.set(i, j, contributionATA.get(i, j) + t * tTimesWeightedGeomSq);

                        // Iterate over all elements in the specular (microfacet distribution) functions whose angle is greater than or equal to the current angle.
                        // We are actually calculating the differential of the specular function, to be integrated at the end.
                        for (int m = mFloor + 1; m < MICROFACET_DISTRIBUTION_RESOLUTION; m++)
                        {
                            i = BASIS_COUNT * (m + 1) + b1;

                            // Top right and bottom left partitions of the matrix:
                            // row corresponds to diffuse coefficients and column corresponds to specular, or vice-versa.
                            contributionATA.set(i, b2, contributionATA.get(i, b2) + weightedGeomOverPi);
                            contributionATA.set(b2, i, contributionATA.get(b2, i) + weightedGeomOverPi);

                            // Bottom right partition of the matrix: row and column both correspond to specular.
                            // The first non-zero specular element in a row or column (but not both) is handled here.
                            // Scale by t to account for linear interpolation.
                            contributionATA.set(i, j, contributionATA.get(i, j) + tTimesWeightedGeomSq);

                            // The remaining elements are handled here.
                            for (int m2 = mFloor + 1; m2 < MICROFACET_DISTRIBUTION_RESOLUTION; m2++)
                            {
                                j = BASIS_COUNT * (m2 + 1) + b2;
                                contributionATA.set(i, j, contributionATA.get(i, j) + weightedGeomSq);
                            }
                        }
                    }
                }
            }
        }
    }


    private void reconstructWeights()
    {

    }

    private void reconstructNormals()
    {

    }

    private void saveBasisFunctions()
    {

    }

    private void saveWeightMaps()
    {

    }

    private void saveNormalMaps()
    {

    }
}
