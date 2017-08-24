package tetzlaff.fidelity;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.imageio.ImageIO;

import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.*;
import tetzlaff.gl.vecmath.IntVector3;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.core.ReadonlySettingsModel;
import tetzlaff.ibr.rendering.IBRResources;
import tetzlaff.util.NonNegativeLeastSquares;

import static org.ejml.dense.row.CommonOps_DDRM.multTransA;

public class LinearSystemFidelityTechnique<ContextType extends Context<ContextType>> implements FidelityEvaluationTechnique<ContextType>
{
    private Function<Vector3, IntVector3> encodeFunction;
    private Function<IntVector3, Vector3> pixelEvaluationFunction;
    private int imgWidth;
    private int imgHeight;
    private ReadonlySettingsModel settings;
    private final boolean usePerceptuallyLinearError;

    private final File debugDirectory;

    private NonNegativeLeastSquares solver;
    private List<SimpleMatrix> listATA;
    private List<SimpleMatrix> listATb;
    private byte[][] images;
    private byte[][] weights;

    private List<Integer> viewIndexList;

    public LinearSystemFidelityTechnique(boolean usePerceptuallyLinearError, File debugDirectory)
    {
        this.usePerceptuallyLinearError = usePerceptuallyLinearError;
        this.debugDirectory = debugDirectory;
    }

    private static class MatrixSystem
    {
        SimpleMatrix mA;
        SimpleMatrix b;
        List<Integer> activePixels;
    }

    @Override
    public boolean isGuaranteedMonotonic()
    {
        return true;
    }

    @Override
    public boolean isGuaranteedInterpolating()
    {
        return true;
    }

    @Override
    public double evaluateBaselineError(int targetViewIndex, File debugFile)
    {
        return 0.0;
    }

    @Override
    public void initialize(IBRResources<ContextType> resources, ReadonlySettingsModel settings, int size) throws IOException
    {
        this.settings = settings;
        this.imgWidth = this.imgHeight = size;

        encodeFunction = color ->
        {
            if (color.x <= 0 || color.y <= 0 || color.z <= 0)
            {
                return new IntVector3(0);
            }
            else
            {
                float luminance = color.dot(new Vector3(0.2126729f, 0.7151522f, 0.0721750f));
                double maxLuminance = resources.viewSet.getLuminanceEncoding().decodeFunction.applyAsDouble(255.0);
                if (luminance >= maxLuminance)
                {
                    Vector3 result = color.dividedBy((float)maxLuminance).applyOperator(x -> Math.pow(x, 1.0 / 2.2));
                    return new IntVector3(Math.round(255 * result.x), Math.round(255 * result.y), Math.round(255 * result.z));
                }
                else
                {
                    // Step 2: determine the ratio between the tonemapped and linear luminance
                    // Remove implicit gamma correction from the lookup table
                    double tonemappedGammaCorrected =
                            resources.viewSet.getLuminanceEncoding().encodeFunction.applyAsDouble(luminance) / 255.0;
                    double tonemappedNoGamma = Math.pow(tonemappedGammaCorrected, 2.2);
                    double scale = tonemappedNoGamma / luminance;

                    // Step 3: return the color, scaled to have the correct luminance,
                    // but the original saturation and hue.
                    // Step 4: apply gamma correction
                    Vector3 colorScaled = color.times((float)scale);
                    Vector3 result = colorScaled.applyOperator(x -> Math.pow(x, 1.0 / 2.2));
                    return new IntVector3(Math.round(255 * result.x), Math.round(255 * result.y), Math.round(255 * result.z));
                }
            }
        };

        Function<IntVector3, Vector3> fullDecodeFunction = color ->
        {
            if (color.x <= 0 || color.y <= 0 || color.z <= 0)
            {
                return Vector3.ZERO;
            }
            else
            {
                // Step 1: remove gamma correction
                Vector3 colorGamma = color.asFloatingPointNormalized().applyOperator(x -> Math.pow(x, 2.2));

                // Step 2: convert to CIE luminance
                // Clamp to 1 so that the ratio computed in step 3 is well defined
                // if the luminance value somehow exceeds 1.0
                double luminanceNonlinear = colorGamma.dot(new Vector3(0.2126729f, 0.7151522f, 0.0721750f));

                double maxLuminance = resources.viewSet.getLuminanceEncoding().decodeFunction.applyAsDouble(255.0);

                if (luminanceNonlinear > 1.0)
                {
                    return colorGamma.times((float)maxLuminance);
                }
                else
                {
                    // Step 3: determine the ratio between the linear and nonlinear luminance
                    // Reapply gamma correction to the single luminance value
                    float scale = (float)Math.min(5.0 * maxLuminance,
                            resources.viewSet.getLuminanceEncoding().decodeFunction.applyAsDouble(
                                    255.0 * Math.pow(luminanceNonlinear, 1.0 / 2.2)) / luminanceNonlinear);

                    // Step 4: return the color, scaled to have the correct luminance,
                    // but the original saturation and hue.
                    return colorGamma.times(scale);
                }
            }
        };

        Vector3 primaryViewDisplacement = resources.viewSet.getCameraPose(resources.viewSet.getPrimaryViewIndex())
                .times(resources.geometry.getCentroid().asPosition()).getXYZ();

        // TODO should this change when operating in perceptually linear space (gamma-corrected)?
        float unitReflectanceEncoding = 255.0f
                / fullDecodeFunction.apply(new IntVector3(255)).y
                * resources.viewSet.getLightIntensity(resources.viewSet.getLightIndex(resources.viewSet.getPrimaryViewIndex())).y
                / primaryViewDisplacement.dot(primaryViewDisplacement);

            //encodeFunction.apply(new Vector3(1.0f)).y;

        images = new byte[resources.viewSet.getCameraPoseCount()][size * size * 3];
        weights = new byte[resources.viewSet.getCameraPoseCount()][size * size];

        try
        (
            Program<ContextType> projTexProgram = resources.context.getShaderProgramBuilder()
                .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/colorappearance/projtex_multi.frag"))
                .createProgram();

            FramebufferObject<ContextType> framebuffer = resources.context.buildFramebufferObject(size, size)
                .addColorAttachment(ColorFormat.RGB8)
                .addColorAttachment(ColorFormat.RGBA8)
                .createFramebufferObject()
        )
        {
            Drawable<ContextType> drawable = resources.context.createDrawable(projTexProgram);
            drawable.addVertexBuffer("position", resources.positionBuffer);
            drawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
            drawable.addVertexBuffer("normal", resources.normalBuffer);
            drawable.addVertexBuffer("tangent", resources.tangentBuffer);

            resources.setupShaderProgram(projTexProgram, false);
            if (!this.settings.isOcclusionEnabled())
            {
                projTexProgram.setUniform("occlusionEnabled", false);
            }
            else
            {
                projTexProgram.setUniform("occlusionBias", this.settings.getOcclusionBias());
            }

            drawable.program().setUniform("lightIntensityCompensation", true);

            resources.context.getState().disableBackFaceCulling();

            for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
            {
                framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);

                projTexProgram.setUniform("viewIndex", i);

                drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
                int[] colors = framebuffer.readColorBufferARGB(0);
                for (int j = 0; j < colors.length; j++)
                {
                    Color color = new Color(colors[j], true);
                    IntVector3 colorVector = new IntVector3(color.getRed(), color.getGreen(), color.getBlue());

                    Vector3 decodedColor = colorVector.asFloatingPoint().dividedBy(255.0f);
                    if (!usePerceptuallyLinearError)
                    {
                        decodedColor = decodedColor.applyOperator(x -> Math.pow(x, 2.2));
                        //decodedColor = fullDecodeFunction.apply(colorVector);
                    }
                    images[i][3 * j] = (byte)Math.round(decodedColor.x * unitReflectanceEncoding);
                    images[i][3 * j + 1] = (byte)Math.round(decodedColor.y * unitReflectanceEncoding);
                    images[i][3 * j + 2] = (byte)Math.round(decodedColor.z * unitReflectanceEncoding);
                }

                if (debugDirectory != null)
                {
                    try
                    {
                        framebuffer.saveColorBufferToFile(0, "PNG",
                                new File(debugDirectory, resources.viewSet.getImageFileName(i).split("\\.")[0] + ".png"));

                        framebuffer.saveColorBufferToFile(1, "PNG",
                                new File(debugDirectory, resources.viewSet.getImageFileName(i).split("\\.")[0] + "_geometry.png"));
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }

                int[] geometry = framebuffer.readColorBufferARGB(1);
                for (int j = 0; j < geometry.length; j++)
                {
                    weights[i][j] = (byte) new Color(geometry[j], true).getGreen(); // n dot v
                }
            }
        }

        pixelEvaluationFunction = encodedVector -> new Vector3(
            encodedVector.x / unitReflectanceEncoding,
            encodedVector.y / unitReflectanceEncoding,
            encodedVector.z / unitReflectanceEncoding);

        solver = new NonNegativeLeastSquares();
        listATA = new ArrayList<>();
        listATb = new ArrayList<>();
        initializeMatrices(pixelEvaluationFunction);
    }

    @Override
    public void setMask(File maskFile) throws IOException
    {
        if (maskFile != null)
        {
            throw new UnsupportedOperationException("Masks are not currently supported.");
        }
    }

    private MatrixSystem getMatrixSystem(int targetViewIndex, List<Integer> viewIndexList, Function<IntVector3, Vector3> decodeFunction)
    {
        MatrixSystem result = new MatrixSystem();

        result.activePixels = new ArrayList<Integer>();
        for (int i = 0; i < weights[targetViewIndex].length; i++)
        {
            if ((0x000000FF & weights[targetViewIndex][i]) > 0)
            {
                result.activePixels.add(i);
            }
        }

        result.mA = new SimpleMatrix(result.activePixels.size() * 3, viewIndexList == null ? images.length : viewIndexList.size());
        result.b = new SimpleMatrix(result.activePixels.size() * 3, 1);

        for (int i = 0; i < result.activePixels.size(); i++)
        {
            double weight = (0x000000FF & weights[targetViewIndex][result.activePixels.get(i)]) / 255.0;

            Vector3 targetColor = decodeFunction.apply(new IntVector3(
                    0x000000FF & images[targetViewIndex][3 * result.activePixels.get(i)],
                    0x000000FF & images[targetViewIndex][3 * result.activePixels.get(i) + 1],
                    0x000000FF & images[targetViewIndex][3 * result.activePixels.get(i) + 2]));

            result.b.set(3 * i, weight * targetColor.x);
            result.b.set(3 * i + 1, weight * targetColor.y);
            result.b.set(3 * i + 2, weight * targetColor.z);

            if (viewIndexList == null)
            {
                for (int j = 0; j < images.length; j++)
                {
                    Vector3 color = decodeFunction.apply(new IntVector3(
                            0x000000FF & images[j][3 * result.activePixels.get(i)],
                            0x000000FF & images[j][3 * result.activePixels.get(i) + 1],
                            0x000000FF & images[j][3 * result.activePixels.get(i) + 2]));

                    result.mA.set(3 * i, j, weight * color.x);
                    result.mA.set(3 * i + 1, j, weight * color.y);
                    result.mA.set(3 * i + 2, j, weight * color.z);
                }
            }
            else
            {
                for (int j = 0; j < viewIndexList.size(); j++)
                {
                    Vector3 color = decodeFunction.apply(new IntVector3(
                            0x000000FF & images[viewIndexList.get(j)][3 * result.activePixels.get(i)],
                            0x000000FF & images[viewIndexList.get(j)][3 * result.activePixels.get(i) + 1],
                            0x000000FF & images[viewIndexList.get(j)][3 * result.activePixels.get(i) + 2]));

                    result.mA.set(3 * i, j, weight * color.x);
                    result.mA.set(3 * i + 1, j, weight * color.y);
                    result.mA.set(3 * i + 2, j, weight * color.z);
                }
            }
        }

        return result;
    }

    private void initializeMatrices(Function<IntVector3, Vector3> decodeFunction)
    {
        for (int k = 0; k < images.length; k++)
        {
            MatrixSystem system = getMatrixSystem(k, null, decodeFunction);

            SimpleMatrix mATA = new SimpleMatrix(system.mA.numCols(), system.mA.numCols());
            SimpleMatrix vATb = new SimpleMatrix(system.mA.numCols(), 1);

            // Low level operations to avoid using unnecessary memory.
            multTransA(system.mA.getMatrix(), system.mA.getMatrix(), mATA.getMatrix());
            multTransA(system.mA.getMatrix(), system.b.getMatrix(), vATb.getMatrix());

            listATA.add(mATA);
            listATb.add(vATb);
        }
    }

    @Override
    public void updateActiveViewIndexList(List<Integer> viewIndexList)
    {
        this.viewIndexList = new ArrayList<Integer>(viewIndexList);
    }

    @Override
    public double evaluateError(int targetViewIndex, File debugFile)
    {
        MatrixSystem system = getMatrixSystem(targetViewIndex, viewIndexList, pixelEvaluationFunction);

        SimpleMatrix mATA = new SimpleMatrix(viewIndexList.size(), viewIndexList.size());
        SimpleMatrix vATb = new SimpleMatrix(viewIndexList.size(), 1);

        SimpleMatrix mATA_full = listATA.get(targetViewIndex);
        SimpleMatrix vATb_full = listATb.get(targetViewIndex);

        for (int i = 0; i < viewIndexList.size(); i++)
        {
            vATb.set(i, vATb_full.get(viewIndexList.get(i)));

            for (int j = 0; j < viewIndexList.size(); j++)
            {
                mATA.set(i, j, mATA_full.get(viewIndexList.get(i), viewIndexList.get(j)));
            }
        }

        SimpleMatrix solution = solver.solvePremultiplied(mATA, vATb, 0.001);
        SimpleMatrix recon = system.mA.mult(solution);

        if (debugFile != null)
        {
            BufferedImage outImg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);

            int[] pixels = new int[imgWidth * imgHeight];
            for (int i = 0; i < imgWidth * imgHeight; i++)
            {
                pixels[i] = new Color(0,0,0).getRGB();
            }

            for (int i = 0; i < system.activePixels.size(); i++)
            {
                int pixelIndex = system.activePixels.get(i);
                double weight = (0x000000FF & weights[targetViewIndex][pixelIndex]) / 255.0;

                IntVector3 encodedColor = encodeFunction.apply(
                        new Vector3((float)(recon.get(3 * i) / weight),
                                    (float)(recon.get(3 * i + 1) / weight),
                                    (float)(recon.get(3 * i + 2) / weight)));

                pixels[pixelIndex] = new Color(
                        Math.max(0, Math.min(255, encodedColor.x)),
                        Math.max(0, Math.min(255, encodedColor.y)),
                        Math.max(0, Math.min(255, encodedColor.z))).getRGB();
            }

            // Flip the array vertically
            for (int y = 0; y < imgHeight / 2; y++)
            {
                int limit = (y + 1) * imgWidth;
                for (int i1 = y * imgWidth, i2 = (imgHeight - y - 1) * imgWidth; i1 < limit; i1++, i2++)
                {
                    int tmp = pixels[i1];
                    pixels[i1] = pixels[i2];
                    pixels[i2] = tmp;
                }
            }

            outImg.setRGB(0, 0, imgWidth, imgHeight, pixels, 0, imgWidth);

            try
            {
                ImageIO.write(outImg, "PNG", debugFile);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        SimpleMatrix error = recon.minus(system.b);

        return error.normF() / Math.sqrt(system.b.numRows() / 3);
    }

    @Override
    public void close() throws Exception
    {
    }
}
