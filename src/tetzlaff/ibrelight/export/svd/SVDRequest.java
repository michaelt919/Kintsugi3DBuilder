package tetzlaff.ibrelight.export.svd;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.FloatBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import org.ejml.data.FMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import org.lwjgl.*;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.RenderingMode;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;
import tetzlaff.util.FastPartialSVD;

public class SVDRequest implements IBRRequest
{
    private static final boolean DEBUG = false;

    private static final int BLOCK_SIZE = 32;
    private static final int SAVED_SINGULAR_VALUES = 16;
    private static final int MAX_RUNNING_THREADS = 3;
    private static final boolean PUT_COLOR_IN_VIEW_FACTOR = true;
    private static final boolean DIFFUSE_MODE = false;

    private final int texWidth;
    private final int texHeight;
    private final File exportPath;
    private final ReadonlySettingsModel settings;

    private int activeBlockCount = 0;
    private final Object activeBlockCountChangeHandle = new Object();

    private final Queue<SimpleMatrix> allocatedMatrices;
    private final Queue<FloatBuffer> allocatedFloatBuffers;

    public SVDRequest(int texWidth, int texHeight, File exportPath, ReadonlySettingsModel settings)
    {
        this.exportPath = exportPath;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.settings = settings;
        this.allocatedMatrices = new LinkedList<>();
        this.allocatedFloatBuffers = new LinkedList<>();
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

        for (int i = 0; i < 2 * MAX_RUNNING_THREADS; i++)
        {
            if (PUT_COLOR_IN_VIEW_FACTOR)
            {
                allocatedMatrices.add(new SimpleMatrix(BLOCK_SIZE * BLOCK_SIZE, resources.viewSet.getCameraPoseCount() * 3, FMatrixRMaj.class));
            }
            else
            {
                allocatedMatrices.add(new SimpleMatrix(BLOCK_SIZE * BLOCK_SIZE * 3, resources.viewSet.getCameraPoseCount(), FMatrixRMaj.class));
            }

            allocatedFloatBuffers.add(BufferUtils.createFloatBuffer(BLOCK_SIZE * BLOCK_SIZE * resources.viewSet.getCameraPoseCount() * 4));
        }

        System.out.println("Finished allocating memory.");

        try
        (
            Program<ContextType> deferredProgram = resources.context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/common/deferred.frag"))
                .createProgram();

            Program<ContextType> projTexProgram = resources.getIBRShaderProgramBuilder(RenderingMode.IMAGE_BASED_WITH_MATERIALS)
                .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/relight/resid.frag"))
                .createProgram();

            FramebufferObject<ContextType> geometryFramebuffer = resources.context.buildFramebufferObject(BLOCK_SIZE, BLOCK_SIZE)
                .addColorAttachment(ColorFormat.RGB32F)
                .addColorAttachment(ColorFormat.RGB32F)
                .createFramebufferObject();

            FramebufferObject<ContextType> colorFramebuffer = resources.context.buildFramebufferObject(BLOCK_SIZE, BLOCK_SIZE)
                .addColorAttachment(ColorFormat.RGBA32F)
                .createFramebufferObject()
        )
        {
            System.out.println("Finished compiling programs and creating framebuffer objects.");

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

                    FloatBuffer colorStorage;

                    synchronized (allocatedFloatBuffers)
                    {
                        while(allocatedFloatBuffers.isEmpty())
                        {
                            try
                            {
                                allocatedFloatBuffers.wait(30000); // Double check every 30 seconds if notifyAll() was not called
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }

                        colorStorage = allocatedFloatBuffers.poll();
                    }

                    boolean hasValidEntries = getMatrix(resources, deferredProgram, projTexProgram, geometryFramebuffer, colorFramebuffer,
                        minTexCoords, maxTexCoords, colorStorage);

                    if (hasValidEntries)
                    {
                        int currentBlockX = blockX;
                        int currentBlockY = blockY;

                        Thread svdThread = new Thread(() ->
                        {
                            if (currentBlockX == 0)
                            {
                                System.out.println("Started row " + currentBlockY + "...");
                            }

                            SimpleMatrix matrix;

                            synchronized (allocatedMatrices)
                            {
                                while(allocatedMatrices.isEmpty())
                                {
                                    try
                                    {
                                        allocatedMatrices.wait(30000); // Double check every 30 seconds if notifyAll() was not called
                                    }
                                    catch (InterruptedException e)
                                    {
                                        e.printStackTrace();
                                    }
                                }

                                matrix = allocatedMatrices.poll();
                            }

                            try
                            {
                                matrix.zero();

                                boolean[] pixelMasks = new boolean[BLOCK_SIZE * BLOCK_SIZE];
//                                int[] validPixelCounts = new int[resources.viewSet.getCameraPoseCount()];

                                for (int k = 0; k < resources.viewSet.getCameraPoseCount(); k++)
                                {
//                                    validPixelCounts[k] = 0;

                                    colorStorage.position(BLOCK_SIZE * BLOCK_SIZE * 4 * k);

                                    FloatBuffer colorSlice = colorStorage.slice();
                                    colorSlice.limit(BLOCK_SIZE * BLOCK_SIZE * 4);

                                    for (int i = 0; 4 * i + 3 < colorSlice.limit(); i++)
                                    {
                                        if (colorSlice.get(4 * i + 3) > 0.0 && !Double.isNaN(colorSlice.get(4 * i))
                                            && !Double.isNaN(colorSlice.get(4 * i + 1)) && !Double.isNaN(colorSlice.get(4 * i + 2)))
                                        {
                                            if (PUT_COLOR_IN_VIEW_FACTOR)
                                            {
                                                matrix.set(i, 3 * k, colorSlice.get(4 * i));
                                                matrix.set(i, 3 * k + 1, colorSlice.get(4 * i + 1));
                                                matrix.set(i, 3 * k + 2, colorSlice.get(4 * i + 2));
                                            }
                                            else
                                            {
                                                matrix.set(3 * i, k, colorSlice.get(4 * i));
                                                matrix.set(3 * i + 1, k, colorSlice.get(4 * i + 1));
                                                matrix.set(3 * i + 2, k, colorSlice.get(4 * i + 2));
                                            }

//                                            validPixelCounts[k]++;
                                            pixelMasks[i] = true;
                                        }
                                    }
                                }

                                //SimpleSVD<SimpleMatrix> svd = matrix.svd(true);
                                FastPartialSVD svd = FastPartialSVD.compute(matrix, SAVED_SINGULAR_VALUES, 0.05f, 16, 3);

                                for (int k = 0; k < resources.viewSet.getCameraPoseCount(); k++)
                                {
                                    double squaredError;
                                    SimpleMatrix error = svd.getError();

                                    if (PUT_COLOR_IN_VIEW_FACTOR)
                                    {
                                        int firstColumn = 3 * k;
                                        squaredError = IntStream.range(0, error.numRows())
                                            .filter(i -> pixelMasks[i])
                                            .mapToDouble(i -> error.get(i, firstColumn) * error.get(i, firstColumn)
                                                + error.get(i, firstColumn + 1) * error.get(i, firstColumn + 1)
                                                + error.get(i, firstColumn + 2) * error.get(i, firstColumn + 2))
                                            .sum();
                                    }
                                    else
                                    {
                                        int column = k;
                                        squaredError = IntStream.range(0, error.numRows())
                                            .filter(i -> pixelMasks[i / 3])
                                            .mapToDouble(i -> error.get(i, column) * error.get(i, column))
                                            .sum();
                                    }

                                    synchronized (squaredErrorByView)
                                    {
                                        if (PUT_COLOR_IN_VIEW_FACTOR)
                                        {
                                            squaredErrorByView[k] += squaredError / (double) (3 * texWidth * texHeight);
                                        }
                                        else
                                        {
                                            squaredErrorByView[k] += squaredError / (double) (texWidth * texHeight);
                                        }
                                    }
                                }

                                float[] singularValues = svd.getSingularValues();
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

                                    scale[svIndex] *= Math.signum(uMatrix.elementSum());

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

                                //System.out.println("Finished block " + currentBlockX + ", " + currentBlockY + '.');
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

                                synchronized(allocatedMatrices)
                                {
                                    allocatedMatrices.add(matrix);
                                    allocatedMatrices.notifyAll();
                                }

                                synchronized(allocatedFloatBuffers)
                                {
                                    allocatedFloatBuffers.add(colorStorage);
                                    allocatedFloatBuffers.notifyAll();
                                }
                            }
                        });

                        taskQueue.add(svdThread);

                        do
                        {
                            synchronized (activeBlockCountChangeHandle)
                            {
                                // Add any tasks that are in the queue as long as there are less than the max number of threads running.
                                while (!taskQueue.isEmpty() && activeBlockCount < MAX_RUNNING_THREADS)
                                {
                                    taskQueue.poll().start();
                                    activeBlockCount++;
                                }

                                // Two situations in which we need to sleep: if the task queue is full and we're waiting to start another thread
                                // before adding more to it, or if we've finished setting up the last block and are just running out the queue.
                                if (taskQueue.size() == MAX_RUNNING_THREADS || (blockX == blockCountX - 1 && blockY == blockCountY - 1))
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
                        // If the task queue is full and we had to wait for a running task to finish, then loop through a second time to start
                        // at least one task and free up space in the queue.
                        // Also, if we've just finished setting up the last block, keep looping until the task queue is empty.
                        while (taskQueue.size() == MAX_RUNNING_THREADS
                            || (!taskQueue.isEmpty() && blockX == blockCountX - 1 && blockY == blockCountY - 1));
                    }
                    else
                    {
                        synchronized(allocatedFloatBuffers)
                        {
                            allocatedFloatBuffers.add(colorStorage);
                            allocatedFloatBuffers.notifyAll();
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
            String[] parts = resources.viewSet.getImageFileName(k).split("\\.");
            if (parts.length >= 2)
            {
                parts[parts.length-1] = "png";
            }
            ImageIO.write(viewImg, "PNG", new File(exportPath, String.join(".", parts)));
        }
    }

    private <ContextType extends Context<ContextType>> boolean getMatrix(
        IBRResources<ContextType> resources, Program<ContextType> deferredProgram, Program<ContextType> projTexProgram,
        FramebufferObject<ContextType> geometryFramebuffer, Framebuffer<ContextType> colorFramebuffer, Vector2 minTexCoord, Vector2 maxTexCoord,
        FloatBuffer colorStorage)
    {
        resources.context.getState().disableBackFaceCulling();

        Drawable<ContextType> deferredDrawable = resources.context.createDrawable(deferredProgram);
        deferredDrawable.addVertexBuffer("position", resources.positionBuffer);
        deferredDrawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
        deferredDrawable.addVertexBuffer("normal", resources.normalBuffer);
        deferredDrawable.addVertexBuffer("tangent", resources.tangentBuffer);

        deferredProgram.setUniform("minTexCoord", minTexCoord);
        deferredProgram.setUniform("maxTexCoord", maxTexCoord);

//        deferredProgram.setTexture("normalMap", resources.normalTexture);
//        deferredProgram.setUniform("useNormalMap", !DIFFUSE_MODE && resources.normalTexture != null);

        geometryFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
        geometryFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);

        deferredDrawable.draw(PrimitiveMode.TRIANGLES, geometryFramebuffer);

//        float[] normals = geometryFramebuffer.readFloatingPointColorBufferRGBA(1);
//        boolean validBlock = false;
//        for (int i = 0; i < normals.length; i++)
//        {
//            if (i % 4 != 3) // Ignore alpha channel
//            {
//                validBlock = validBlock || normals[i] != 0;
//            }
//        }
        boolean validBlock = true;

        if (validBlock)
        {
            try (VertexBuffer<ContextType> rectangle = resources.context.createRectangle())
            {
                Drawable<ContextType> projTexDrawable = resources.context.createDrawable(projTexProgram);
                projTexDrawable.addVertexBuffer("position", rectangle);

                projTexProgram.setUniform("minTexCoord", minTexCoord);
                projTexProgram.setUniform("maxTexCoord", maxTexCoord);

                resources.setupShaderProgram(projTexProgram);

                projTexProgram.setTexture("positionMap", geometryFramebuffer.getColorAttachmentTexture(0));

                // NOTE: overrides normalMap assignment from resources.setupShaderProgram()
                projTexProgram.setTexture("normalMap", geometryFramebuffer.getColorAttachmentTexture(1));

                projTexProgram.setUniform("diffuseMode", DIFFUSE_MODE);

                if (settings.getBoolean("occlusionEnabled"))
                {
                    projTexProgram.setUniform("occlusionBias", settings.getFloat("occlusionBias"));
                }

                // Want to use raw pixel values since they represents roughness, not intensity
                projTexProgram.setUniform("lightIntensityCompensation", false);

                colorStorage.clear();

                for (int k = 0; k < resources.viewSet.getCameraPoseCount(); k++)
                {
                    colorFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

                    projTexProgram.setUniform("viewIndex", k);

                    projTexDrawable.draw(PrimitiveMode.TRIANGLE_FAN, colorFramebuffer);

                    colorStorage.position(4 * BLOCK_SIZE * BLOCK_SIZE * k);
                    FloatBuffer colorSlice = colorStorage.slice();
                    colorSlice.limit(4 * BLOCK_SIZE * BLOCK_SIZE);
                    colorFramebuffer.readFloatingPointColorBufferRGBA(0, colorSlice);

                    if (DEBUG)
                    {
                        try
                        {
                            colorFramebuffer.saveColorBufferToFile(0, "PNG",
                                new File(exportPath, resources.viewSet.getImageFileName(k).split("\\.")[0]
                                    + '_' + minTexCoord.x + '_' + minTexCoord.y + ".png"));
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }

                colorStorage.rewind();
            }
        }

        return validBlock;
    }
}
