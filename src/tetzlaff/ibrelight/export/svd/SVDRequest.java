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
        SimpleMatrix matrix = getMatrix(renderable.getResources());
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
                writer.print(renderable.getActiveViewSet().getImageFileName(j));

                for (int i = 0; i < singularValues.length; i++)
                {
                    writer.print("\t" + vMatrix.get(i, j));
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
                int convertedValue = (int)Math.max(0, Math.min(255, Math.round((uMatrix.get(i, j) * 0.5 + 0.5) * 255)));
                data[i] = new Color(convertedValue, convertedValue, convertedValue).getRGB();
            }

            outImg.setRGB(0, 0, texWidth, texHeight, data, 0, texWidth);

            ImageIO.write(outImg, "PNG", new File(exportPath, String.format("%04d.png", j)));
        }
    }

    private <ContextType extends Context<ContextType>> SimpleMatrix getMatrix(IBRResources<ContextType> resources)
        throws FileNotFoundException
    {
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

            SimpleMatrix result = new SimpleMatrix(texWidth * texHeight, resources.viewSet.getCameraPoseCount() * 3);

            for (int k = 0; k < resources.viewSet.getCameraPoseCount(); k++)
            {
                framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
//                framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);

                projTexProgram.setUniform("viewIndex", k);

                drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
                int[] colors = framebuffer.readColorBufferARGB(0);
                for (int i = 0; i < colors.length; i++)
                {
                    Color color = new Color(colors[i], true);
                    if (color.getAlpha() > 0)
                    {
                        result.set(i, 3 * k, (0x000000FF & color.getRed()) / 255.0f - 0.5f);
                        result.set(i, 3 * k + 1, (0x000000FF & color.getGreen()) / 255.0f - 0.5f);
                        result.set(i, 3 * k + 2, (0x000000FF & color.getBlue()) / 255.0f - 0.5f);
                    }
                }

                if (DEBUG)
                {
                    try
                    {
                        framebuffer.saveColorBufferToFile(0, "PNG",
                                new File(exportPath, resources.viewSet.getImageFileName(k).split("\\.")[0] + ".png"));

//                        framebuffer.saveColorBufferToFile(1, "PNG",
//                                new File(exportPath, resources.viewSet.getImageFileName(i).split("\\.")[0] + "_weights.png"));
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
}
