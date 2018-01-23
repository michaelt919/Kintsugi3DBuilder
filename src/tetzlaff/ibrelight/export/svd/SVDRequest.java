package tetzlaff.ibrelight.export.svd;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;
import tetzlaff.util.FastPartialSVD;

public class SVDRequest implements IBRRequest
{
    private static final boolean DEBUG = false;

    private static final int BLOCK_SIZE = 32;
    private static final int SAVED_SINGULAR_VALUES = 4;
    private static final int MAX_RUNNING_THREADS = 6;
    private static final boolean PUT_COLOR_IN_VIEW_FACTOR = true;

    private final int texWidth;
    private final int texHeight;
    private final File exportPath;
    private final ReadonlySettingsModel settings;

    private int activeBlockCount = 0;
    private final Object activeBlockCountChangeHandle = new Object();

    public SVDRequest(int texWidth, int texHeight, File exportPath, ReadonlySettingsModel settings)
    {
        this.exportPath = exportPath;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.settings = settings;
    }

    private static int convertToFixedPoint(double value)
    {
        return Double.isNaN(value) ? 0 : (int) Math.max(1, Math.min(255, Math.round(value * 127 + 128)));
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback)
        throws IOException
    {
        System.out.println("Starting SVD...");
        Instant start = Instant.now();

        IBRResources<ContextType> resources = renderable.getResources();

        int blockCountX = (texWidth - 1) / BLOCK_SIZE + 1; // should equal ceil(texWidth / BLOCK_SIZE)
        int blockCountY = (texHeight - 1) / BLOCK_SIZE + 1; // should equal ceil(texHeight / BLOCK_SIZE)

        int svLayoutWidth = (int)Math.ceil(Math.sqrt(SAVED_SINGULAR_VALUES));
        int svLayoutHeight = (SAVED_SINGULAR_VALUES - 1) / svLayoutWidth + 1; // should equal ceil(SAVED_SINGULAR_VALUES / svLayoutWidth)

        System.out.println("SV Layout width: " + svLayoutWidth);
        System.out.println("SV Layout height: " + svLayoutHeight);

        float[][][] textureData;
        float[][][] viewData;

        if (PUT_COLOR_IN_VIEW_FACTOR)
        {
            textureData = new float[SAVED_SINGULAR_VALUES][texWidth * texHeight][1];
            viewData = new float[resources.viewSet.getCameraPoseCount()][blockCountX * blockCountY * svLayoutWidth * svLayoutHeight][3];
        }
        else
        {
            textureData = new float[SAVED_SINGULAR_VALUES][texWidth * texHeight][3];
            viewData = new float[resources.viewSet.getCameraPoseCount()][blockCountX * blockCountY * svLayoutWidth * svLayoutHeight][1];
        }

        double[] squaredErrorByView = new double[resources.viewSet.getCameraPoseCount()];
        double[] viewImportance = new double[resources.viewSet.getCameraPoseCount()];
        double[][] viewImportanceBySingularValues = new double[resources.viewSet.getCameraPoseCount()][SAVED_SINGULAR_VALUES];

        try
        (
            Program<ContextType> projTexProgram = resources.context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/relight/resid.frag"))
                .createProgram();

            FramebufferObject<ContextType> framebuffer = resources.context.buildFramebufferObject(BLOCK_SIZE, BLOCK_SIZE)
                .addColorAttachment(ColorFormat.RGBA32F)
                .createFramebufferObject()
        )
        {
            Queue<Thread> taskQueue = new LinkedList<>();

            for (int blockY = 0; blockY < blockCountY; blockY++)
            {
                for (int blockX = 0; blockX < blockCountX; blockX++)
                {
                    Vector2 minTexCoords = new Vector2(
                        blockX * BLOCK_SIZE * 1.0f / texWidth,
                        blockY * BLOCK_SIZE * 1.0f / texHeight);

                    Vector2 maxTexCoords = new Vector2(
                        Math.min(1.0f, (blockX + 1) * BLOCK_SIZE * 1.0f / texWidth),
                        Math.min(1.0f, (blockY + 1) * BLOCK_SIZE * 1.0f / texHeight));

                    int[] validPixelCounts = new int[resources.viewSet.getCameraPoseCount()];
                    boolean[] pixelMasks = new boolean[BLOCK_SIZE * BLOCK_SIZE];
                    SimpleMatrix matrix = getMatrix(resources, projTexProgram, framebuffer, minTexCoords, maxTexCoords, validPixelCounts, pixelMasks);

                    // Quickly test if the block is completely empty (i.e. no triangles were rendered in the block).
                    // If so, then don't even bother putting it in the queue and move on to try another block.
                    boolean hasValidEntries = false;
                    for (int i = 0; !hasValidEntries && i < validPixelCounts.length; i++)
                    {
                        hasValidEntries = validPixelCounts[i] != 0;
                    }

                    if (hasValidEntries)
                    {
                        int currentBlockX = blockX;
                        int currentBlockY = blockY;

                        Thread svdThread = new Thread(() ->
                        {
                            System.out.println("Starting block " + currentBlockX + ", " + currentBlockY + "...");

                            try
                            {
                                //SimpleSVD<SimpleMatrix> svd = matrix.svd(true);
                                FastPartialSVD svd = FastPartialSVD.compute(matrix, SAVED_SINGULAR_VALUES, 0.05, 16, 3);

                                for (int k = 0; k < resources.viewSet.getCameraPoseCount(); k++)
                                {
                                    double squaredError;

                                    if (PUT_COLOR_IN_VIEW_FACTOR)
                                    {
                                        int firstColumn = 3 * k;
                                        squaredError = IntStream.range(0, svd.getError().numRows())
                                            .filter(i -> pixelMasks[i])
                                            .mapToDouble(i -> svd.getError().get(i, firstColumn) * svd.getError().get(i, firstColumn)
                                                + svd.getError().get(i, firstColumn + 1) * svd.getError().get(i, firstColumn + 1)
                                                + svd.getError().get(i, firstColumn + 2) * svd.getError().get(i, firstColumn + 2))
                                            .sum();
                                    }
                                    else
                                    {
                                        int column = k;
                                        squaredError = IntStream.range(0, svd.getError().numRows())
                                            .filter(i -> pixelMasks[i / 3])
                                            .mapToDouble(i -> svd.getError().get(i, column) * svd.getError().get(i, column))
                                            .sum();
                                    }

                                    synchronized (squaredErrorByView)
                                    {
                                        squaredErrorByView[k] += squaredError / (double) (3 * texWidth * texHeight);
                                    }
                                }

                                double[] singularValues = svd.getSingularValues();
                                int effectiveSingularValues = Math.min(SAVED_SINGULAR_VALUES, singularValues.length);

                                SimpleMatrix uMatrix = svd.getU();
                                SimpleMatrix vMatrix = svd.getV();

                                double[] scale = new double[effectiveSingularValues];
                                for (int svIndex = 0; svIndex < effectiveSingularValues; svIndex++)
                                {
                                    for (int k = 0; k < vMatrix.numRows(); k++)
                                    {
                                        scale[svIndex] = Math.max(scale[svIndex], Math.abs(vMatrix.get(k, svIndex)));
                                    }

                                    for (int k = 0; k < uMatrix.numRows(); k++)
                                    {
                                        scale[svIndex] = Math.min(scale[svIndex], 1.0 / (singularValues[svIndex] * Math.abs(uMatrix.get(k, svIndex))));
                                    }

                                    for (int y = 0; y < BLOCK_SIZE; y++)
                                    {
                                        for (int x = 0; x < BLOCK_SIZE; x++)
                                        {
                                            int blockPixelIndex = y * BLOCK_SIZE + x;
                                            int texturePixelIndex =
                                                (texHeight - currentBlockY * BLOCK_SIZE - y - 1) * texWidth + currentBlockX * BLOCK_SIZE + x;

                                            if (PUT_COLOR_IN_VIEW_FACTOR)
                                            {
                                                if (pixelMasks[blockPixelIndex])
                                                {
                                                    textureData[svIndex][texturePixelIndex][0] =
                                                        (float) (uMatrix.get(blockPixelIndex, svIndex)
                                                            * scale[svIndex] * singularValues[svIndex]);
                                                }
                                                else
                                                {
                                                    textureData[svIndex][texturePixelIndex][0] = Float.NaN;
                                                }
                                            }
                                            else
                                            {
                                                if (pixelMasks[blockPixelIndex])
                                                {
                                                    textureData[svIndex][texturePixelIndex][0] =
                                                        (float) (uMatrix.get(3 * blockPixelIndex, svIndex)
                                                            * scale[svIndex] * singularValues[svIndex]);
                                                    textureData[svIndex][texturePixelIndex][1] =
                                                        (float) (uMatrix.get(3 * blockPixelIndex + 1, svIndex)
                                                            * scale[svIndex] * singularValues[svIndex]);
                                                    textureData[svIndex][texturePixelIndex][2] =
                                                        (float) (uMatrix.get(3 * blockPixelIndex + 2, svIndex)
                                                            * scale[svIndex] * singularValues[svIndex]);
                                                }
                                                else
                                                {
                                                    textureData[svIndex][texturePixelIndex][0] = Float.NaN;
                                                    textureData[svIndex][texturePixelIndex][1] = Float.NaN;
                                                    textureData[svIndex][texturePixelIndex][2] = Float.NaN;
                                                }
                                            }
                                        }
                                    }

                                    // Hole fill
                                    for (int subBlockSize = 2; subBlockSize <= BLOCK_SIZE; subBlockSize *= 2)
                                    {
                                        for (int y0 = 0; y0 < BLOCK_SIZE; y0 += subBlockSize)
                                        {
                                            for (int x0 = 0; x0 < BLOCK_SIZE; x0 += subBlockSize)
                                            {
                                                float sum = 0.0f;
                                                int count = 0;

                                                // First pass: compute average
                                                for (int y = y0; y < y0 + subBlockSize; y++)
                                                {
                                                    for (int x = x0; x < x0 + subBlockSize; x++)
                                                    {
                                                        int texturePixelIndex =
                                                            (texHeight - currentBlockY * BLOCK_SIZE - y - 1) * texWidth + currentBlockX * BLOCK_SIZE + x;

                                                        if (PUT_COLOR_IN_VIEW_FACTOR)
                                                        {
                                                            if (!Float.isNaN(textureData[svIndex][texturePixelIndex][0]))
                                                            {
                                                                sum += textureData[svIndex][texturePixelIndex][0];
                                                                count++;
                                                            }
                                                        }
                                                        else
                                                        {
                                                            if (!Float.isNaN(textureData[svIndex][texturePixelIndex][0]))
                                                            {
                                                                sum += textureData[svIndex][texturePixelIndex][0];
                                                                count++;
                                                            }

                                                            if (!Float.isNaN(textureData[svIndex][texturePixelIndex][1]))
                                                            {
                                                                sum += textureData[svIndex][texturePixelIndex][1];
                                                                count++;
                                                            }

                                                            if (!Float.isNaN(textureData[svIndex][texturePixelIndex][2]))
                                                            {
                                                                sum += textureData[svIndex][texturePixelIndex][2];
                                                                count++;
                                                            }
                                                        }
                                                    }
                                                }

                                                if (count > 0)
                                                {
                                                    // Second pass: replace NaN with average.
                                                    for (int y = y0; y < y0 + subBlockSize; y++)
                                                    {
                                                        for (int x = x0; x < x0 + subBlockSize; x++)
                                                        {
                                                            int texturePixelIndex =
                                                                (texHeight - currentBlockY * BLOCK_SIZE - y - 1) * texWidth + currentBlockX * BLOCK_SIZE + x;

                                                            if (PUT_COLOR_IN_VIEW_FACTOR)
                                                            {
                                                                if (Float.isNaN(textureData[svIndex][texturePixelIndex][0]))
                                                                {
                                                                    textureData[svIndex][texturePixelIndex][0] = sum / count;
                                                                }
                                                            }
                                                            else
                                                            {
                                                                if (Float.isNaN(textureData[svIndex][texturePixelIndex][0]))
                                                                {
                                                                    textureData[svIndex][texturePixelIndex][0] = sum / count;
                                                                }

                                                                if (Float.isNaN(textureData[svIndex][texturePixelIndex][1]))
                                                                {
                                                                    textureData[svIndex][texturePixelIndex][1] = sum / count;
                                                                }

                                                                if (Float.isNaN(textureData[svIndex][texturePixelIndex][2]))
                                                                {
                                                                    textureData[svIndex][texturePixelIndex][2] = sum / count;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                for (int svIndex = effectiveSingularValues; svIndex < SAVED_SINGULAR_VALUES; svIndex++)
                                {
                                    // Fill in with NaN for singular values that are zero.
                                    for (int y = 0; y < BLOCK_SIZE; y++)
                                    {
                                        for (int x = 0; x < BLOCK_SIZE; x++)
                                        {
                                            int texturePixelIndex =
                                                (texHeight - currentBlockY * BLOCK_SIZE - y - 1) * texWidth + currentBlockX * BLOCK_SIZE + x;

                                            if (PUT_COLOR_IN_VIEW_FACTOR)
                                            {
                                                textureData[svIndex][texturePixelIndex][0] = Float.NaN;
                                            }
                                            else
                                            {
                                                textureData[svIndex][texturePixelIndex][0] = Float.NaN;
                                                textureData[svIndex][texturePixelIndex][1] = Float.NaN;
                                                textureData[svIndex][texturePixelIndex][2] = Float.NaN;
                                            }
                                        }
                                    }
                                }

                                for (int k = 0; k < renderable.getActiveViewSet().getCameraPoseCount(); k++)
                                {
                                    for (int i = 0; i < svLayoutHeight; i++)
                                    {
                                        for (int j = 0; j < svLayoutWidth; j++)
                                        {
                                            int svIndex = i * svLayoutWidth + j;
                                            if (svIndex < effectiveSingularValues)
                                            {
                                                int texturePixelIndex =
                                                    ((svLayoutHeight * (blockCountY - currentBlockY) - i - 1) * blockCountX + currentBlockX)
                                                        * svLayoutWidth + j;

                                                double blockImportance;

                                                if (PUT_COLOR_IN_VIEW_FACTOR)
                                                {
                                                    viewData[k][texturePixelIndex][0] =
                                                        (float) (vMatrix.get(3 * k, svIndex) / scale[svIndex]);
                                                    viewData[k][texturePixelIndex][1] =
                                                        (float) (vMatrix.get(3 * k + 1, svIndex) / scale[svIndex]);
                                                    viewData[k][texturePixelIndex][2] =
                                                        (float) (vMatrix.get(3 * k + 2, svIndex) / scale[svIndex]);

                                                    blockImportance = 1.0 / (double) (texWidth * texHeight)
                                                        * singularValues[svIndex] * singularValues[svIndex]
                                                        * (vMatrix.get(3 * k, svIndex) * vMatrix.get(3 * k, svIndex)
                                                        + vMatrix.get(3 * k + 1, svIndex) * vMatrix.get(3 * k + 1, svIndex)
                                                        + vMatrix.get(3 * k + 2, svIndex) * vMatrix.get(3 * k + 2, svIndex)) / 3;
                                                }
                                                else
                                                {
                                                    viewData[k][texturePixelIndex][0] =
                                                        (float) (vMatrix.get(k, svIndex) / scale[svIndex]);

                                                    blockImportance = 1.0 / (double) (texWidth * texHeight)
                                                        * singularValues[svIndex] * singularValues[svIndex]
                                                        * vMatrix.get(k, svIndex) * vMatrix.get(k, svIndex);
                                                }

                                                synchronized (viewImportanceBySingularValues)
                                                {
                                                    viewImportanceBySingularValues[k][svIndex] += blockImportance;
                                                }

                                                synchronized (viewImportance)
                                                {
                                                    viewImportance[k] += blockImportance;
                                                }
                                            }
                                        }
                                    }
                                }

                                System.out.println("Finished block " + currentBlockX + ", " + currentBlockY + '.');
                            }
                            catch (RuntimeException e)
                            {
                                System.err.println("Block " + currentBlockX + ", " + currentBlockY + " failed.");
                                e.printStackTrace();
                            }
                            finally
                            {
                                synchronized (activeBlockCountChangeHandle)
                                {
                                    activeBlockCount--;
                                    activeBlockCountChangeHandle.notifyAll();
                                }
                            }
                        });

                        taskQueue.add(svdThread);

                        // Add any tasks that are in the queue as long as it looks like there are less than the max number of threads running.
                        // Otherwise, if the task queue is full, wait until at least one task starts, and then move on to generate another task to replace it.
                        // Finally, if we've just finished setting up the last block, keep looping until the task queue is empty.
                        boolean readyForNewBlock = true; // First time through the loop will initialize this variable.
                        while ((!taskQueue.isEmpty() && readyForNewBlock)
                            || taskQueue.size() == MAX_RUNNING_THREADS
                            || (!taskQueue.isEmpty() && blockX == blockCountX - 1 && blockY == blockCountY - 1))
                        {
                            synchronized (activeBlockCountChangeHandle)
                            {
                                if (activeBlockCount < MAX_RUNNING_THREADS)
                                {
                                    taskQueue.poll().start();
                                    activeBlockCount++;
                                }
                                // Two situations in which we need to sleep: if the task queue is full and we're waiting to start another thread
                                // before adding more to it, or if we've finished setting up the last block and are just running out the queue.
                                else if (taskQueue.size() == MAX_RUNNING_THREADS || (blockX == blockCountX - 1 && blockY == blockCountY - 1))
                                {
                                    try
                                    {
                                        activeBlockCountChangeHandle.wait(30000); // Double check every 30 seconds if notify() was not called
                                    }
                                    catch (InterruptedException e)
                                    {
                                        e.printStackTrace();
                                    }
                                }

                                readyForNewBlock = activeBlockCount < MAX_RUNNING_THREADS;
                            }
                        }
                    }
                }
            }
        }

        // Wait for all threads to finish
        boolean proceed = false;
        while (!proceed)
        {
            synchronized (activeBlockCountChangeHandle)
            {
                if (activeBlockCount == 0)
                {
                    proceed = true;
                }
                else
                {
                    try
                    {
                        activeBlockCountChangeHandle.wait(30000); // Double check every 30 seconds if notify() was not called
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        Duration duration = Duration.between(start, Instant.now());
        System.out.println("SVD finished in " + (duration.getSeconds() + duration.getNano() * 1.0e-9) + " seconds.");

        try(PrintStream importanceFilePrintStream = new PrintStream(new File(exportPath, "importance.txt")))
        {
            for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
            {
                importanceFilePrintStream.print(resources.viewSet.getImageFileName(i));
                importanceFilePrintStream.print('\t');
                importanceFilePrintStream.print(viewImportance[i]);

                for (int j = 0; j < SAVED_SINGULAR_VALUES; j++)
                {
                    importanceFilePrintStream.print('\t');
                    importanceFilePrintStream.print(viewImportanceBySingularValues[i][j]);
                }

                importanceFilePrintStream.print('\t');
                importanceFilePrintStream.print(squaredErrorByView[i]);

                importanceFilePrintStream.println();
            }
        }

        for (int i = 0; i < svLayoutHeight; i++)
        {
            for (int j = 0; j < svLayoutWidth; j++)
            {
                int svIndex = i * svLayoutWidth + j;
                if (svIndex < SAVED_SINGULAR_VALUES)
                {
                    BufferedImage textureImg = new BufferedImage(texWidth, texHeight, BufferedImage.TYPE_INT_ARGB);
                    int[] textureDataPacked = new int[textureData[svIndex].length];
                    for (int pixelIndex = 0; pixelIndex < textureData[svIndex].length; pixelIndex++)
                    {
                        if (PUT_COLOR_IN_VIEW_FACTOR)
                        {
                            int fixedPointValue = convertToFixedPoint(textureData[svIndex][pixelIndex][0]);
                            textureDataPacked[pixelIndex] = new Color(
                                fixedPointValue, fixedPointValue, fixedPointValue,
                                fixedPointValue == 0 ? 0 : 255).getRGB();
                        }
                        else
                        {
                            int fixedPointRed = convertToFixedPoint(textureData[svIndex][pixelIndex][0]);
                            int fixedPointGreen = convertToFixedPoint(textureData[svIndex][pixelIndex][1]);
                            int fixedPointBlue = convertToFixedPoint(textureData[svIndex][pixelIndex][2]);

                            textureDataPacked[pixelIndex] = new Color(
                                fixedPointRed, fixedPointGreen, fixedPointBlue,
                                fixedPointRed == 0 || fixedPointGreen == 0 || fixedPointBlue == 0 ? 0 : 255
                            ).getRGB();
                        }
                    }
                    textureImg.setRGB(0, 0, texWidth, texHeight, textureDataPacked, 0, texWidth);
                    ImageIO.write(textureImg, "PNG", new File(exportPath, String.format("sv_%04d_%02d_%02d.png", svIndex, i, j)));
                }
            }
        }

        for (int k = 0; k < resources.viewSet.getCameraPoseCount(); k++)
        {
            BufferedImage viewImg = new BufferedImage(blockCountX * svLayoutWidth, blockCountY * svLayoutHeight, BufferedImage.TYPE_INT_ARGB);
            int[] viewDataPacked = new int[viewData[k].length];
            for (int pixelIndex = 0; pixelIndex < viewData[k].length; pixelIndex++)
            {
                if (PUT_COLOR_IN_VIEW_FACTOR)
                {
                    viewDataPacked[pixelIndex] = new Color(
                        convertToFixedPoint(viewData[k][pixelIndex][0]),
                        convertToFixedPoint(viewData[k][pixelIndex][1]),
                        convertToFixedPoint(viewData[k][pixelIndex][2])
                    ).getRGB();
                }
                else
                {
                    int fixedPointValue = convertToFixedPoint(viewData[k][pixelIndex][0]);
                    viewDataPacked[pixelIndex] = new Color(fixedPointValue, fixedPointValue, fixedPointValue).getRGB();
                }
            }
            viewImg.setRGB(0, 0, viewImg.getWidth(), viewImg.getHeight(), viewDataPacked, 0, viewImg.getWidth());
            ImageIO.write(viewImg, "PNG", new File(exportPath, resources.viewSet.getImageFileName(k)));
        }
    }

    private <ContextType extends Context<ContextType>> SimpleMatrix getMatrix(
        IBRResources<ContextType> resources, Program<ContextType> projTexProgram, Framebuffer<ContextType> framebuffer,
        Vector2 minTexCoord, Vector2 maxTexCoord, int[] validPixelCountStorage, boolean[] pixelMaskStorage)
    {
        Drawable<ContextType> drawable = resources.context.createDrawable(projTexProgram);
        drawable.addVertexBuffer("position", resources.positionBuffer);
        drawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
        drawable.addVertexBuffer("normal", resources.normalBuffer);
        drawable.addVertexBuffer("tangent", resources.tangentBuffer);

        projTexProgram.setUniform("minTexCoord", minTexCoord);
        projTexProgram.setUniform("maxTexCoord", maxTexCoord);

        resources.setupShaderProgram(projTexProgram, false);
        if (settings.getBoolean("occlusionEnabled"))
        {
            projTexProgram.setUniform("occlusionBias", settings.getFloat("occlusionBias"));
        }
        else
        {
            projTexProgram.setUniform("occlusionEnabled", false);
        }

        // Want to use raw pixel values since they represents roughness, not intensity
        drawable.program().setUniform("lightIntensityCompensation", false);

        resources.context.getState().disableBackFaceCulling();

        SimpleMatrix result;
        if (PUT_COLOR_IN_VIEW_FACTOR)
        {
            result = new SimpleMatrix(BLOCK_SIZE * BLOCK_SIZE, resources.viewSet.getCameraPoseCount() * 3/*, FMatrixRMaj.class*/);
        }
        else
        {
            result = new SimpleMatrix(BLOCK_SIZE * BLOCK_SIZE * 3, resources.viewSet.getCameraPoseCount()/*, FMatrixRMaj.class*/);
        }

        for (int k = 0; k < resources.viewSet.getCameraPoseCount(); k++)
        {
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

            projTexProgram.setUniform("viewIndex", k);

            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

            validPixelCountStorage[k] = 0;
            float[] colors = framebuffer.readFloatingPointColorBufferRGBA(0);

            for (int i = 0; 4 * i + 3 < colors.length; i++)
            {
                if (colors[4 * i + 3] > 0.0 && !Double.isNaN(colors[4 * i]) && !Double.isNaN(colors[4 * i + 1]) && !Double.isNaN(colors[4 * i + 2]))
                {
                    if (PUT_COLOR_IN_VIEW_FACTOR)
                    {
                        result.set(i, 3 * k, colors[4 * i]);
                        result.set(i, 3 * k + 1, colors[4 * i + 1]);
                        result.set(i, 3 * k + 2, colors[4 * i + 2]);
                    }
                    else
                    {
                        result.set(3 * i, k, colors[4 * i]);
                        result.set(3 * i + 1, k, colors[4 * i + 1]);
                        result.set(3 * i + 2, k, colors[4 * i + 2]);
                    }

                    validPixelCountStorage[k]++;
                    pixelMaskStorage[i] = true;
                }
            }

            if (DEBUG)
            {
                try
                {
                    framebuffer.saveColorBufferToFile(0, "PNG",
                            new File(exportPath, resources.viewSet.getImageFileName(k).split("\\.")[0] + ".png"));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }
}
