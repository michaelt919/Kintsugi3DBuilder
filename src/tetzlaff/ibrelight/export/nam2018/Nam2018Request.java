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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.gl.vecmath.DoubleVector4;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;
import tetzlaff.util.NonNegativeLeastSquares;

public class Nam2018Request implements IBRRequest
{
    static final int BASIS_COUNT = 8;
    static final int MICROFACET_DISTRIBUTION_RESOLUTION = 90;

    private static final int BRDF_MATRIX_SIZE = BASIS_COUNT * (MICROFACET_DISTRIBUTION_RESOLUTION + 1);
    private static final double K_MEANS_TOLERANCE = 0.000001;
    private static final double GAMMA = 2.2;

    private static final boolean DEBUG = true;
    private static final int MAX_RUNNING_THREADS = 5;

    private final int width;
    private final int height;
    private final File outputDirectory;
    private final ReadonlySettingsModel settingsModel;

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
        System.out.println("Clustering to initialize weights...");

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

        DoubleVector3[] centers = new DoubleVector3[BASIS_COUNT];
        centers[0] = new DoubleVector3(averages[4 * firstCenterIndex], averages[4 * firstCenterIndex + 1], averages[4 * firstCenterIndex + 2]);

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
                    minDistance = Math.min(minDistance, centers[b2].distance(new DoubleVector3(averages[4 * p], averages[4 * p + 1], averages[4 * p + 2])));
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

            assert index < cdf.length;

            // If the index was actually positive to begin with, that's probably fine; just make sure that it's a valid location.
            // It's also possible in theory for the index to be zero if the random number generator produced 0.0.
            while (index < 0 || averages[4 * index + 3] == 0.0)
            {
                // Search forward until a valid index is found.
                index++;

                // We shouldn't ever fail to find an index since x should have been less than the final (un-normalized) CDF total.
                // This means that there has to be some place where the CDF went up, corresponding to a valid index.
                assert index < cdf.length;
            }

            // We've found a new center.
            centers[b] = new DoubleVector3(averages[4 * index], averages[4 * index + 1], averages[4 * index + 2]);
        }

        System.out.println("Initial centers:");
        for (int b = 0; b < BASIS_COUNT; b++)
        {
            System.out.println(centers[b]);
        }

        // Initialization is done; now it's time to iterate.
        boolean changed;
        do
        {
            // Initialize sums to zero.
            DoubleVector4[] sums = IntStream.range(0, BASIS_COUNT).mapToObj(i -> DoubleVector4.ZERO_DIRECTION).toArray(DoubleVector4[]::new);

            for (int p = 0; p < width * height; p++)
            {
                if (averages[4 * p + 3] > 0.0)
                {
                    int bMin = -1;

                    double minDistance = Double.MAX_VALUE;

                    for (int b = 0; b < BASIS_COUNT; b++)
                    {
                        double distance = centers[b].distance(new DoubleVector3(averages[4 * p], averages[4 * p + 1], averages[4 * p + 2]));
                        if (distance < minDistance)
                        {
                            minDistance = distance;
                            bMin = b;
                        }
                    }

                    sums[bMin] = sums[bMin].plus(new DoubleVector4(averages[4 * p], averages[4 * p + 1], averages[4 * p + 2], 1.0f));
                }
            }

            changed = false;
            for (int b = 0; b < BASIS_COUNT; b++)
            {
                DoubleVector3 newCenter = sums[b].getXYZ().dividedBy(sums[b].w);
                changed = changed || newCenter.distance(centers[b]) < K_MEANS_TOLERANCE;
                centers[b] = newCenter;
            }
        }
        while (changed);

        weightSolution = new SimpleMatrix(BASIS_COUNT, width * height, DMatrixRMaj.class);
        for (int p = 0; p < width * height; p++)
        {
            if (averages[4 * p + 3] > 0.0)
            {
                int bMin = -1;

                double minDistance = Double.MAX_VALUE;

                for (int b = 0; b < BASIS_COUNT; b++)
                {
                    double distance = centers[b].distance(new DoubleVector3(averages[4 * p], averages[4 * p + 1], averages[4 * p + 2]));
                    if (distance < minDistance)
                    {
                        minDistance = distance;
                        bMin = b;
                    }
                }

                weightSolution.set(bMin, p, 1.0);
            }
        }

        System.out.println("Refined centers:");
        for (int b = 0; b < BASIS_COUNT; b++)
        {
            System.out.println(centers[b]);
        }

        if (DEBUG)
        {
            BufferedImage weightImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] weightDataPacked = new int[width * height];
            for (int p = 0; p < width * height; p++)
            {
                if (averages[4 * p + 3] > 0.0)
                {
                    int bMin = -1;

                    double minDistance = Double.MAX_VALUE;

                    for (int b = 0; b < BASIS_COUNT; b++)
                    {
                        double distance = centers[b].distance(new DoubleVector3(averages[4 * p], averages[4 * p + 1], averages[4 * p + 2]));
                        if (distance < minDistance)
                        {
                            minDistance = distance;
                            bMin = b;
                        }
                    }

                    // Flip vertically
                    int weightDataIndex = p % width + width * (height - p / width - 1);

                    switch(bMin)
                    {
                        case 0: weightDataPacked[weightDataIndex] = Color.RED.getRGB(); break;
                        case 1: weightDataPacked[weightDataIndex] = Color.GREEN.getRGB(); break;
                        case 2: weightDataPacked[weightDataIndex] = Color.BLUE.getRGB(); break;
                        case 3: weightDataPacked[weightDataIndex] = Color.YELLOW.getRGB(); break;
                        case 4: weightDataPacked[weightDataIndex] = Color.CYAN.getRGB(); break;
                        case 5: weightDataPacked[weightDataIndex] = Color.MAGENTA.getRGB(); break;
                        case 6: weightDataPacked[weightDataIndex] = Color.WHITE.getRGB(); break;
                        case 7: weightDataPacked[weightDataIndex] = Color.GRAY.getRGB(); break;
                        default: weightDataPacked[weightDataIndex] = Color.BLACK.getRGB(); break;
                    }
                }
            }

            weightImg.setRGB(0, 0, weightImg.getWidth(), weightImg.getHeight(), weightDataPacked, 0, weightImg.getWidth());

            try
            {
                ImageIO.write(weightImg, "PNG", new File(outputDirectory, "k-means.png"));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private <ContextType extends Context<ContextType>> void reconstructBRDFs(
        Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int viewCount)
    {
        System.out.println("Building reflectance fitting matrix...");
        buildReflectanceMatrix(drawable, framebuffer, viewCount);
        System.out.println("Finished building matrix; solving now...");
        brdfSolutionRed = NonNegativeLeastSquares.solvePremultiplied(brdfATA, brdfATyRed, 0.001);
        brdfSolutionGreen = NonNegativeLeastSquares.solvePremultiplied(brdfATA, brdfATyGreen, 0.001);
        brdfSolutionBlue = NonNegativeLeastSquares.solvePremultiplied(brdfATA, brdfATyBlue, 0.001);
        System.out.println("DONE!");

        if (DEBUG)
        {
            // write out diffuse texture for debugging
            BufferedImage diffuseImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] diffuseDataPacked = new int[width * height];
            for (int p = 0; p < width * height; p++)
            {
                DoubleVector4 diffuseSum = DoubleVector4.ZERO_DIRECTION;

                for (int b = 0; b < BASIS_COUNT; b++)
                {
                    diffuseSum = diffuseSum.plus(new DoubleVector4(
                            brdfSolutionRed.get(b),
                            brdfSolutionGreen.get(b),
                            brdfSolutionBlue.get(b), 1.0)
                        .times(weightSolution.get(b, p)));
                }

                if (diffuseSum.w > 0)
                {
                    DoubleVector3 diffuseAvgGamma = diffuseSum.getXYZ().dividedBy(diffuseSum.w).applyOperator(x -> Math.min(1.0, Math.pow(x, 1.0 / GAMMA)));

                    // Flip vertically
                    int dataBufferIndex = p % width + width * (height - p / width - 1);
                    diffuseDataPacked[dataBufferIndex] = new Color((float)diffuseAvgGamma.x, (float)diffuseAvgGamma.y, (float)diffuseAvgGamma.z).getRGB();
                }
            }

            diffuseImg.setRGB(0, 0, diffuseImg.getWidth(), diffuseImg.getHeight(), diffuseDataPacked, 0, diffuseImg.getWidth());

            try
            {
                ImageIO.write(diffuseImg, "PNG", new File(outputDirectory, "diffuse.png"));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            for (int b = 0; b < BASIS_COUNT; b++)
            {
                DoubleVector3 diffuseColor = new DoubleVector3(
                    brdfSolutionRed.get(b),
                    brdfSolutionGreen.get(b),
                    brdfSolutionBlue.get(b));
                System.out.println("Diffuse #" + b + ": " + diffuseColor);
            }

            System.out.println("Basis BRDFs:");

            for (int b = 0; b < BASIS_COUNT; b++)
            {
                System.out.print("Red#" + b);
                double redTotal = 0.0;
                for (int m = MICROFACET_DISTRIBUTION_RESOLUTION - 1; m >= 0; m--)
                {
                    System.out.print(", ");
                    redTotal += brdfSolutionRed.get((m + 1) * BASIS_COUNT + b);
                    System.out.print(redTotal);
                }

                System.out.println();

                System.out.print("Green#" + b);
                double greenTotal = 0.0;
                for (int m = MICROFACET_DISTRIBUTION_RESOLUTION - 1; m >= 0; m--)
                {
                    System.out.print(", ");
                    greenTotal += brdfSolutionGreen.get((m + 1) * BASIS_COUNT + b);
                    System.out.print(greenTotal);
                }
                System.out.println();

                System.out.print("Blue#" + b);
                double blueTotal = 0.0;
                for (int m = MICROFACET_DISTRIBUTION_RESOLUTION - 1; m >= 0; m--)
                {
                    System.out.print(", ");
                    blueTotal += brdfSolutionBlue.get((m + 1) * BASIS_COUNT + b);
                    System.out.print(blueTotal);
                }
                System.out.println();
            }
        }
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

//            if (DEBUG)
//            {
//                try
//                {
//                    framebuffer.saveColorBufferToFile(0, "PNG", new File(outputDirectory, String.format("%02d.png", k)));
//                    framebuffer.saveColorBufferToFile(1, "PNG", new File(outputDirectory, String.format("%02d_geom.png", k)));
//                }
//                catch (IOException e)
//                {
//                    e.printStackTrace();
//                }
//            }

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
                    new ReflectanceMatrixBuilder(colorAndVisibility, halfwayAndGeom, weightSolution, contributionATA,
                        contributionATyRed, contributionATyGreen, contributionATyBlue).execute();

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

        for (int b = 0; b < BASIS_COUNT; b++)
        {
            System.out.print("RHS, red for BRDF #" + b + ": ");

            System.out.print(brdfATyRed.get(b));
            for (int m = 0; m < MICROFACET_DISTRIBUTION_RESOLUTION; m++)
            {
                System.out.print(", ");
                System.out.print(brdfATyRed.get((m + 1) * BASIS_COUNT + b));
            }
            System.out.println();

            System.out.print("RHS, green for BRDF #" + b + ": ");

            System.out.print(brdfATyGreen.get(b));
            for (int m = 0; m < MICROFACET_DISTRIBUTION_RESOLUTION; m++)
            {
                System.out.print(", ");
                System.out.print(brdfATyGreen.get((m + 1) * BASIS_COUNT + b));
            }
            System.out.println();

            System.out.print("RHS, blue for BRDF #" + b + ": ");

            System.out.print(brdfATyBlue.get(b));
            for (int m = 0; m < MICROFACET_DISTRIBUTION_RESOLUTION; m++)
            {
                System.out.print(", ");
                System.out.print(brdfATyBlue.get((m + 1) * BASIS_COUNT + b));
            }
            System.out.println();

            System.out.println();
        }
    }

    private void reconstructWeights()
    {
        // write out weight textures for debugging
        for (int b = 0; b < BASIS_COUNT; b++)
        {
            BufferedImage weightImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] weightDataPacked = new int[width * height];

            for (int p = 0; p < width * height; p++)
            {
                float weight = (float)weightSolution.get(b, p);

                // Flip vertically
                int dataBufferIndex = p % width + width * (height - p / width - 1);
                weightDataPacked[dataBufferIndex] = new Color(weight, weight, weight).getRGB();
            }

            weightImg.setRGB(0, 0, weightImg.getWidth(), weightImg.getHeight(), weightDataPacked, 0, weightImg.getWidth());

            try
            {
                ImageIO.write(weightImg, "PNG", new File(outputDirectory, String.format("weights%02d.png", b)));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
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
