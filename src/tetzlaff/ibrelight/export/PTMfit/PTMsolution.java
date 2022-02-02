package tetzlaff.ibrelight.export.PTMfit;

import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.ibrelight.core.TextureFitSettings;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

public class PTMsolution {
    public BiConsumer<Integer, SimpleMatrix> setWeights;
    private PolynomialTextureMapModel PTMmodel;
    private TextureFitSettings settings;
    private SimpleMatrix[] weightsByTexel;
    private final boolean[] weightsValidity;
    private final DoubleVector3[] diffuseAlbedos;

    public PTMsolution(TextureFitSettings setting) {
        PTMmodel = new PolynomialTextureMapModel(setting.width,setting.height);
        settings=setting;
        weightsByTexel= IntStream.range(0, settings.width * settings.height*3)
                .mapToObj(p -> new SimpleMatrix(6 + 1, 1, DMatrixRMaj.class))
                .toArray(SimpleMatrix[]::new);
        weightsValidity = new boolean[settings.width * settings.height*3];
        diffuseAlbedos = new DoubleVector3[8];
        for (int i = 0; i < 8; i++)
        {
            diffuseAlbedos[i] = DoubleVector3.ZERO;
        }
    }
    public void setWeights(int texelIndex, SimpleMatrix weights)
    {
        weightsByTexel[texelIndex] = weights;
    }
    public SimpleMatrix getWeights(int texelIndex)
    {
        return weightsByTexel[texelIndex];
    }
    public DoubleVector3 getDiffuseAlbedo(int basisIndex)
    {
        return diffuseAlbedos[basisIndex];
    }

    public void setDiffuseAlbedo(int basisIndex, DoubleVector3 diffuseAlbedo)
    {
        diffuseAlbedos[basisIndex] = diffuseAlbedo;
    }


    public PolynomialTextureMapModel getPTMmodel(){
        return PTMmodel;
    }
    public void saveWeightMaps() {
        for (int b = 0; b < 6; b++)
        {
            BufferedImage weightImg = new BufferedImage(settings.width, settings.height, BufferedImage.TYPE_INT_ARGB);
            int[] weightDataPacked = new int[settings.width * settings.height];

            for (int p = 0; p < settings.width * settings.height; p++)
            {
                float weight1 = Math.max(-1, Math.min(1, (float)weightsByTexel[p].get(b)));
                float weight2 = Math.max(-1, Math.min(1, (float)weightsByTexel[p+settings.width * settings.height].get(b)));
                float weight3 = Math.max(-1, Math.min(1, (float)weightsByTexel[p+2*settings.width * settings.height].get(b)));

                // Flip vertically
                int dataBufferIndex = p % settings.width + settings.width * (settings.height - p / settings.width - 1);
                weightDataPacked[dataBufferIndex] = new Color(weight1*0.5f+0.5f, weight2*0.5f+0.5f, weight3*0.5f+0.5f).getRGB();
            }

            weightImg.setRGB(0, 0, weightImg.getWidth(), weightImg.getHeight(), weightDataPacked, 0, weightImg.getWidth());

            try
            {
                ImageIO.write(weightImg, "PNG", new File(settings.outputDirectory, String.format("weights%02d.png", b)));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

    }
    public boolean areWeightsValid(int texelIndex)
    {
        return weightsValidity[texelIndex];
    }

    public void invalidateWeights()
    {
        // Quickly invalidate all the weights
        Arrays.fill(weightsValidity, false);
    }

    public void setWeightsValidity(int texelIndex, boolean validity)
    {
        weightsValidity[texelIndex] = validity;
    }
}
