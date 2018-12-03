package tetzlaff.reflectancefit;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import javax.imageio.ImageIO;

import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.FramebufferSize;

public final class ParameterFittingResult
{
    private final float[] diffuseColorDataRGBA;
    private final float[] normalDataRGBA;
    private final float[] specularColorDataRGBA;
    private final float[] roughnessDataRGBA;
    private final float[] roughnessErrorDataRGBA;

    private final int[] diffuseColorPackedIntRGBA;
    private final int[] normalPackedIntRGBA;
    private final int[] specularColorPackedIntRGBA;
    private final int[] roughnessPackedIntRGBA;
    private final int[] roughnessErrorPackedIntRGBA;

    private final FramebufferSize framebufferSize;
    private final Options params;

    private ParameterFittingResult(Framebuffer<?> framebuffer, Options params)
    {
        framebufferSize = framebuffer.getSize();
        this.params = params;

        diffuseColorDataRGBA = framebuffer.readFloatingPointColorBufferRGBA(0);
        normalDataRGBA = framebuffer.readFloatingPointColorBufferRGBA(1);
        specularColorDataRGBA = framebuffer.readFloatingPointColorBufferRGBA(2);
        roughnessDataRGBA = framebuffer.readFloatingPointColorBufferRGBA(3);
        roughnessErrorDataRGBA = framebuffer.readFloatingPointColorBufferRGBA(4);

        diffuseColorPackedIntRGBA = framebuffer.readColorBufferARGB(0);
        normalPackedIntRGBA = framebuffer.readColorBufferARGB(1);
        specularColorPackedIntRGBA = framebuffer.readColorBufferARGB(2);
        roughnessPackedIntRGBA = framebuffer.readColorBufferARGB(3);
        roughnessErrorPackedIntRGBA = framebuffer.readColorBufferARGB(4);
    }

    static ParameterFittingResult fromFramebuffer(Framebuffer<?> framebuffer, Options params)
    {
        return new ParameterFittingResult(framebuffer, params);
    }

    private void writeMTLFile(File mtlFile, String materialName) throws FileNotFoundException
    {
        try(PrintStream materialStream = new PrintStream(mtlFile))
        {
            materialStream.println("newmtl " + materialName);

            if (params.isDiffuseTextureEnabled())
            {
                materialStream.println("Kd 1.0 1.0 1.0");
                materialStream.println("map_Kd " + materialName + "_Kd.png");
                materialStream.println("Ka 1.0 1.0 1.0");
                materialStream.println("map_Ka " + materialName + "_Kd.png");
            }
            else
            {
                materialStream.println("Kd 0.0 0.0 0.0");

                if (params.isSpecularTextureEnabled())
                {
                    materialStream.println("Ka 1.0 1.0 1.0");
                    materialStream.println("map_Ka " + materialName + "_Ks.png");
                }
                else
                {
                    materialStream.println("Ka 0 0 0");
                }
            }

            if (params.isSpecularTextureEnabled())
            {
                materialStream.println("Ks 1.0 1.0 1.0");
                materialStream.println("map_Ks " + materialName + "_Ks.png");

                materialStream.println("Pr 1.0");
                materialStream.println("map_Pr " + materialName + "_Pr.png");
            }
            else
            {
                materialStream.println("Ks 0 0 0");
            }

            if ((params.isDiffuseTextureEnabled() && params.getDiffuseComputedNormalWeight() > 0) || params.isSpecularTextureEnabled())
            {
                materialStream.println("norm " + materialName + "_norm.png");
            }

            materialStream.println("d 1.0");
            materialStream.println("Tr 0.0");
            materialStream.println("illum 5");

            materialStream.flush();
        }
    }

    private void writeTextureDataToOutputFile(int[] textureData, File outputFile) throws IOException
    {
        // Flip the array vertically
        FramebufferSize size = this.framebufferSize;
        for (int y = 0; y < size.height / 2; y++)
        {
            int limit = (y + 1) * size.width;
            for (int i1 = y * size.width, i2 = (size.height - y - 1) * size.width; i1 < limit; i1++, i2++)
            {
                int tmp = textureData[i1];
                textureData[i1] = textureData[i2];
                textureData[i2] = tmp;
            }
        }

        BufferedImage outImg = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        outImg.setRGB(0, 0, size.width, size.height, textureData, 0, size.width);
        ImageIO.write(outImg, "PNG", outputFile);
    }

    public void writeToFiles(File materialFile, String materialName) throws IOException
    {
        File outputDirectory = materialFile.getParentFile();
        outputDirectory.mkdirs();

        writeMTLFile(materialFile, materialName);

        if (params.isDiffuseTextureEnabled())
        {
            writeTextureDataToOutputFile(diffuseColorPackedIntRGBA, new File(outputDirectory, materialName + "_Kd.png"));
        }

        if ((params.isDiffuseTextureEnabled() && params.getDiffuseComputedNormalWeight() > 0) || params.isSpecularTextureEnabled())
        {
            writeTextureDataToOutputFile(normalPackedIntRGBA, new File(outputDirectory, materialName + "_norm.png"));
        }

        if (params.isSpecularTextureEnabled())
        {
            writeTextureDataToOutputFile(specularColorPackedIntRGBA, new File(outputDirectory, materialName + "_Ks.png"));
            writeTextureDataToOutputFile(roughnessPackedIntRGBA, new File(outputDirectory, materialName + "_Pr.png"));
            writeTextureDataToOutputFile(roughnessErrorPackedIntRGBA, new File(outputDirectory, materialName + "_Pr_error.png"));
        }
    }

    public float[] getDiffuseColorDataRGBA()
    {
        return diffuseColorDataRGBA;
    }

    public float[] getNormalDataRGBA()
    {
        return normalDataRGBA;
    }

    public float[] getSpecularColorDataRGBA()
    {
        return specularColorDataRGBA;
    }

    public float[] getRoughnessDataRGBA()
    {
        return roughnessDataRGBA;
    }

    public float[] getRoughnessErrorDataRGBA()
    {
        return roughnessErrorDataRGBA;
    }
}
