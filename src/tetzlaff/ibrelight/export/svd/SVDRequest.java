package tetzlaff.ibrelight.export.svd;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;
import tetzlaff.gl.*;
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
        IBRResources<ContextType> resources = renderable.getResources();

        int blockCountX = (texWidth - 1) / BLOCK_SIZE + 1; // should equal ceil(texWidth / BLOCK_SIZE)
        int blockCountY = (texHeight - 1) / BLOCK_SIZE + 1; // should equal ceil(texHeight / BLOCK_SIZE)

        int svLayoutWidth = (int)Math.ceil(Math.sqrt(SAVED_SINGULAR_VALUES));
        int svLayoutHeight = (SAVED_SINGULAR_VALUES - 1) / svLayoutWidth + 1; // should equal ceil(SAVED_SINGULAR_VALUES / svLayoutWidth)

        System.out.println("SV Layout width: " + svLayoutWidth);
        System.out.println("SV Layout height: " + svLayoutHeight);

        int[][] textureData = new int[SAVED_SINGULAR_VALUES][texWidth * texHeight];
        int[][] viewData = new int[resources.viewSet.getCameraPoseCount()][blockCountX * blockCountY * svLayoutWidth * svLayoutHeight];

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
            double[] weights = new double[resources.viewSet.getCameraPoseCount()];

            for (int blockY = 0; blockY < blockCountY; blockY++)
            {
                for (int blockX = 0; blockX < blockCountX; blockX++)
                {
                    System.out.println("Starting block " + blockX + ", " + blockY + "...");

                    Vector2 minTexCoords = new Vector2(
                        blockX * BLOCK_SIZE * 1.0f / texWidth,
                        blockY * BLOCK_SIZE * 1.0f / texHeight);

                    Vector2 maxTexCoords = new Vector2(
                        Math.min(1.0f, (blockX + 1) * BLOCK_SIZE * 1.0f / texWidth),
                        Math.min(1.0f, (blockY + 1) * BLOCK_SIZE * 1.0f / texHeight));

                    SimpleMatrix matrix = getMatrix(resources, projTexProgram, framebuffer, minTexCoords, maxTexCoords, weights);
                    SimpleSVD<SimpleMatrix> svd = matrix.svd(true);
                    double[] singularValues = svd.getSingularValues();
                    int effectiveSingularValues = Math.min(SAVED_SINGULAR_VALUES, singularValues.length);
                    double[] scale = new double[effectiveSingularValues];

                    SimpleMatrix uMatrix = svd.getU();
                    SimpleMatrix vMatrix = svd.getV();

                    for (int i = 0; i < effectiveSingularValues; i++)
                    {
                        double maxAbsValue = 0.0;
                        for (int j = 0; j < vMatrix.numCols(); j++)
                        {
                            maxAbsValue = Math.max(maxAbsValue, Math.abs(vMatrix.get(i, j)));
                        }

                        scale[i] = maxAbsValue;
                    }

                    for (int k = 0; k < renderable.getActiveViewSet().getCameraPoseCount(); k++)
                    {
                        if (weights[k] > 0)
                        {
                            for (int i = 0; i < svLayoutHeight; i++)
                            {
                                for (int j = 0; j < svLayoutWidth; j++)
                                {
                                    int svIndex = i * svLayoutWidth + j;
                                    if (svIndex < effectiveSingularValues && scale[svIndex] > 0)
                                    {
                                        double yValue = vMatrix.get(svIndex, 3 * k + 1) / (scale[svIndex] * weights[k]);

                                        viewData[k][((blockY * svLayoutHeight + i) * blockCountX + blockX) * svLayoutWidth + j]
                                            = new Color(
                                                convertToFixedPoint(vMatrix.get(svIndex, 3 * k) / (scale[svIndex] * weights[k]) - yValue),
                                                convertToFixedPoint(yValue),
                                                convertToFixedPoint(vMatrix.get(svIndex, 3 * k + 2) / (scale[svIndex] * weights[k]) - yValue))
                                            .getRGB();
                                    }
                                }
                            }
                        }
                    }

                    for (int i = 0; i < effectiveSingularValues; i++)
                    {
                        for (int y = 0; y < BLOCK_SIZE; y++)
                        {
                            for (int x = 0; x < BLOCK_SIZE; x++)
                            {
                                int blockPixelIndex = y * BLOCK_SIZE + x;
                                int texturePixelIndex = (blockY * BLOCK_SIZE + y) * texWidth + blockX * BLOCK_SIZE + x;

                                int convertedValue = convertToFixedPoint(uMatrix.get(blockPixelIndex, i) * scale[i] * singularValues[i]);
                                textureData[i][texturePixelIndex] = new Color(convertedValue, convertedValue, convertedValue).getRGB();
                            }
                        }
                    }
                }
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
                    textureImg.setRGB(0, 0, texWidth, texHeight, textureData[svIndex], 0, texWidth);
                    ImageIO.write(textureImg, "PNG", new File(exportPath, String.format("sv_%04d_%02d_%02d.png", svIndex, i, j)));
                }
            }
        }

        for (int k = 0; k < resources.viewSet.getCameraPoseCount(); k++)
        {
            BufferedImage viewImg = new BufferedImage(blockCountX * svLayoutWidth, blockCountY * svLayoutHeight, BufferedImage.TYPE_INT_ARGB);
            viewImg.setRGB(0, 0, viewImg.getWidth(), viewImg.getHeight(), viewData[k], 0, viewImg.getWidth());
            ImageIO.write(viewImg, "PNG", new File(exportPath, resources.viewSet.getImageFileName(k)));
        }
    }

    private <ContextType extends Context<ContextType>> SimpleMatrix getMatrix(
        IBRResources<ContextType> resources, Program<ContextType> projTexProgram, Framebuffer<ContextType> framebuffer,
        Vector2 minTexCoord, Vector2 maxTexCoord, double[] weightStorage)
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

        SimpleMatrix result = new SimpleMatrix(BLOCK_SIZE * BLOCK_SIZE, resources.viewSet.getCameraPoseCount() * 3);

        for (int k = 0; k < resources.viewSet.getCameraPoseCount(); k++)
        {
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

            projTexProgram.setUniform("viewIndex", k);

            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

            float[] colors = framebuffer.readFloatingPointColorBufferRGBA(0);

            double alphaSum = 0.0;
            for (int i = 3; i < colors.length; i += 4)
            {
                if (!Double.isNaN(colors[i]))
                {
                    alphaSum += colors[i];
                }
            }
            double weightAvg = alphaSum / colors.length;
            weightStorage[k] = weightAvg;

            for (int i = 0; 4 * i + 3 < colors.length; i++)
            {
                if (colors[4 * i + 3] > 0.0 && !Double.isNaN(colors[4 * i]) && !Double.isNaN(colors[4 * i + 1]) && !Double.isNaN(colors[4 * i + 2]))
                {
                    result.set(i, 3 * k, colors[4 * i] * weightAvg);
                    result.set(i, 3 * k + 1, colors[4 * i + 1] * weightAvg);
                    result.set(i, 3 * k + 2, colors[4 * i + 2] * weightAvg);
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
