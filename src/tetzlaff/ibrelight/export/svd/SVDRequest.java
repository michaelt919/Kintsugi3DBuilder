package tetzlaff.ibrelight.export.svd;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import javax.imageio.ImageIO;

import org.ejml.data.FMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

public class SVDRequest implements IBRRequest
{
    private static final boolean DEBUG = false;

    private static final int BLOCK_SIZE = 64;
    private static final int SAVED_SINGULAR_VALUES = 16;

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
        return (int) Math.max(1, Math.min(255, Math.round(value * 127 + 128)));
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

        float[][] textureData = new float[SAVED_SINGULAR_VALUES][texWidth * texHeight];
        float[][][] viewData = new float[resources.viewSet.getCameraPoseCount()][blockCountX * blockCountY * svLayoutWidth * svLayoutHeight][3];

        try
        (
            Program<ContextType> projTexProgram = resources.context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texspace.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/relight/roughnessresid.frag"))
                .createProgram();

            FramebufferObject<ContextType> framebuffer = resources.context.buildFramebufferObject(BLOCK_SIZE, BLOCK_SIZE)
                .addColorAttachment(ColorFormat.RGBA32F)
                .createFramebufferObject()
        )
        {

            for (int blockY = 0; blockY < blockCountY; blockY++)
            {
                for (int blockX = 0; blockX < blockCountX; blockX++)
                {
                    boolean proceed = false;
                    while (!proceed)
                    {
                        synchronized (activeBlockCountChangeHandle)
                        {
                            if (activeBlockCount < 8)
                            {
                                proceed = true;
                                activeBlockCount++;
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

                    System.out.println("Starting block " + blockX + ", " + blockY + "...");

                    Vector2 minTexCoords = new Vector2(
                        blockX * BLOCK_SIZE * 1.0f / texWidth,
                        blockY * BLOCK_SIZE * 1.0f / texHeight);

                    Vector2 maxTexCoords = new Vector2(
                        Math.min(1.0f, (blockX + 1) * BLOCK_SIZE * 1.0f / texWidth),
                        Math.min(1.0f, (blockY + 1) * BLOCK_SIZE * 1.0f / texHeight));

                    SimpleMatrix matrix = getMatrix(resources, projTexProgram, framebuffer, minTexCoords, maxTexCoords);

                    int currentBlockX = blockX;
                    int currentBlockY = blockY;

                    Thread svdThread = new Thread(() ->
                    {
                        try
                        {
                            SimpleSVD<SimpleMatrix> svd = matrix.svd(true);
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

                                            viewData[k][texturePixelIndex] = new float[3];
                                            viewData[k][texturePixelIndex][0] =
                                                (float)(vMatrix.get(3 * k, svIndex) / scale[svIndex]);
                                            viewData[k][texturePixelIndex][1] =
                                                (float)(vMatrix.get(3 * k + 1, svIndex) / scale[svIndex]);
                                            viewData[k][texturePixelIndex][2] =
                                                (float)(vMatrix.get(3 * k + 2, svIndex) / scale[svIndex]);
                                        }
                                    }
                                }
                            }

                            for (int svIndex = 0; svIndex < effectiveSingularValues; svIndex++)
                            {
                                for (int y = 0; y < BLOCK_SIZE; y++)
                                {
                                    for (int x = 0; x < BLOCK_SIZE; x++)
                                    {
                                        int blockPixelIndex = y * BLOCK_SIZE + x;
                                        int texturePixelIndex =
                                            (texHeight - currentBlockY * BLOCK_SIZE - y - 1) * texWidth + currentBlockX * BLOCK_SIZE + x;

                                        textureData[svIndex][texturePixelIndex] = (float)(uMatrix.get(blockPixelIndex, svIndex)
                                                                                            * scale[svIndex] * singularValues[svIndex]);
                                    }
                                }
                            }

                            System.out.println("Finished block " + currentBlockX + ", " + currentBlockY + '.');
                        }
                        catch(RuntimeException e)
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

                    svdThread.start();
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
                        int fixedPointValue = convertToFixedPoint(textureData[svIndex][pixelIndex]);
                        textureDataPacked[pixelIndex] = new Color(fixedPointValue, fixedPointValue, fixedPointValue).getRGB();
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
                viewDataPacked[pixelIndex] = new Color(
                    convertToFixedPoint(viewData[k][pixelIndex][0]),
                    convertToFixedPoint(viewData[k][pixelIndex][1]),
                    convertToFixedPoint(viewData[k][pixelIndex][2])).getRGB();
            }
            viewImg.setRGB(0, 0, viewImg.getWidth(), viewImg.getHeight(), viewDataPacked, 0, viewImg.getWidth());
            ImageIO.write(viewImg, "PNG", new File(exportPath, resources.viewSet.getImageFileName(k)));
        }
    }

    private <ContextType extends Context<ContextType>> SimpleMatrix getMatrix(
        IBRResources<ContextType> resources, Program<ContextType> projTexProgram, Framebuffer<ContextType> framebuffer,
        Vector2 minTexCoord, Vector2 maxTexCoord)
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

        SimpleMatrix result = new SimpleMatrix(BLOCK_SIZE * BLOCK_SIZE, resources.viewSet.getCameraPoseCount() * 3, FMatrixRMaj.class);

        for (int k = 0; k < resources.viewSet.getCameraPoseCount(); k++)
        {
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

            projTexProgram.setUniform("viewIndex", k);

            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

            float[] colors = framebuffer.readFloatingPointColorBufferRGBA(0);

            for (int i = 0; 4 * i + 3 < colors.length; i++)
            {
                if (colors[4 * i + 3] > 0.0 && !Double.isNaN(colors[4 * i]) && !Double.isNaN(colors[4 * i + 1]) && !Double.isNaN(colors[4 * i + 2]))
                {
                    result.set(i, 3 * k, colors[4 * i]);
                    result.set(i, 3 * k + 1, colors[4 * i + 1]);
                    result.set(i, 3 * k + 2, colors[4 * i + 2]);
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
