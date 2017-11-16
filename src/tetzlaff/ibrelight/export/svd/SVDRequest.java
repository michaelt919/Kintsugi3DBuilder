package tetzlaff.ibrelight.export.svd;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import javax.imageio.ImageIO;

import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;
import tetzlaff.gl.*;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

public class SVDRequest implements IBRRequest
{
    private static final boolean DEBUG = true;

    private final int texWidth;
    private final int texHeight;
    private final File exportPath;
    private final ReadonlySettingsModel settings;

    private byte[][] images;
//    private byte[][] weights;

    public SVDRequest(int texWidth, int texHeight, File exportPath, ReadonlySettingsModel settings)
    {
        this.exportPath = exportPath;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.settings = settings;
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback)
        throws IOException
    {
        initialize(renderable.getResources());
        SimpleMatrix matrix = getMatrix();
        SimpleSVD<SimpleMatrix> svd = matrix.svd(true);
        double[] singularValues = svd.getSingularValues();

        try (PrintStream writer = new PrintStream(new File(exportPath, "svd.txt")))
        {
            writer.print("svd");

            for (double sv : singularValues)
            {
                writer.print("\t" + sv);
            }

            writer.println();

            SimpleMatrix vMatrix = svd.getV();

            for (int j = 0; j < renderable.getActiveViewSet().getCameraPoseCount(); j++)
            {
                writer.print(renderable.getActiveViewSet().getCameraPose(j));

                for (int i = 0; i < singularValues.length; i++)
                {
                    writer.print(vMatrix.get(i, j));
                }

                writer.println();
            }
        }

        SimpleMatrix uMatrix = svd.getU();

        for (int j = 0; j < singularValues.length; j++)
        {
            BufferedImage outImg = new BufferedImage(texWidth, texHeight, BufferedImage.TYPE_INT_ARGB);

            int[] data = new int[uMatrix.numRows()];

            for (int i = 0; i < texWidth * texHeight; i++)
            {
                long convertedValue = Math.max(0, Math.min(255, Math.round((uMatrix.get(i, j) * 0.5 - 0.5) * 255)));
                data[i] = new Color(convertedValue, convertedValue, convertedValue).getRGB();
            }

            outImg.setRGB(0, 0, texWidth, texHeight, data, 0, texWidth);

            ImageIO.write(outImg, "PNG", new File(exportPath, String.format("%04d.png", j)));
        }
    }

    private <ContextType extends Context<ContextType>> void initialize(IBRResources<ContextType> resources)
        throws FileNotFoundException
    {
        images = new byte[resources.viewSet.getCameraPoseCount() * 3][texWidth * texHeight];
//        weights = new byte[resources.viewSet.getCameraPoseCount() * 3][texWidth * texHeight];

        try
        (
            Program<ContextType> projTexProgram = resources.context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/colorappearance/projtex_multi.frag"))
                .createProgram();

            FramebufferObject<ContextType> framebuffer = resources.context.buildFramebufferObject(texWidth, texHeight)
                .addColorAttachment(ColorFormat.RGB8)
//                .addColorAttachment(ColorFormat.RGBA8)
                .createFramebufferObject()
        )
        {
            Drawable<ContextType> drawable = resources.context.createDrawable(projTexProgram);
            drawable.addVertexBuffer("position", resources.positionBuffer);
            drawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
            drawable.addVertexBuffer("normal", resources.normalBuffer);
            drawable.addVertexBuffer("tangent", resources.tangentBuffer);

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

            for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
            {
                framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
//                framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);

                projTexProgram.setUniform("viewIndex", i);

                drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
                int[] colors = framebuffer.readColorBufferARGB(0);
                for (int j = 0; j < colors.length; j++)
                {
                    Color color = new Color(colors[j], true);
                    images[3 * i][j] = (byte)color.getRed();
                    images[3 * i + 1][j] = (byte)color.getGreen();
                    images[3 * i + 2][j] = (byte)color.getBlue();
                }

                if (DEBUG)
                {
                    try
                    {
                        framebuffer.saveColorBufferToFile(0, "PNG",
                                new File(exportPath, resources.viewSet.getImageFileName(i).split("\\.")[0] + ".png"));

//                        framebuffer.saveColorBufferToFile(1, "PNG",
//                                new File(exportPath, resources.viewSet.getImageFileName(i).split("\\.")[0] + "_weights.png"));
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }

//                int[] weightData = framebuffer.readColorBufferARGB(1);
//                for (int j = 0; j < weightData.length; j++)
//                {
//                    Color weight = new Color(weightData[j], true);
//                    this.weights[3 * i][j] = (byte)weight.getRed();
//                    this.weights[3 * i + 1][j] = (byte)weight.getGreen();
//                    this.weights[3 * i + 2][j] = (byte)weight.getBlue();
//                }
            }
        }
    }

    private SimpleMatrix getMatrix()
    {
        int pixelCount = texWidth * texHeight;
        SimpleMatrix result = new SimpleMatrix(texWidth * texHeight, images.length);

        for (int i = 0; i < pixelCount; i++)
        {
            for (int j = 0; j < images.length; j++)
            {
//                double weight = (0x000000FF & weights[j][i]) / 255.0;
                double roughness = (0x000000FF & images[j][i]) / 255.0f - 0.5f;

                result.set(i, j, /*weight * */roughness);
            }
        }

        return result;
    }
}
