package tetzlaff.ibrelight.export.fidelity;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.lwjgl.*;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.DoubleVector2;
import tetzlaff.gl.vecmath.DoubleVector3;
import tetzlaff.gl.vecmath.IntVector3;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

public class HeuristicFidelityTechnique<ContextType extends Context<ContextType>> implements FidelityEvaluationTechnique<ContextType>
{
    private IBRResources<ContextType> resources;
    private float unitReflectanceEncoding;
    private final boolean usePerceptuallyLinearError;

    private final File debugDirectory;

    private byte[][] intensities;
    private byte[][] weights;
    private byte[][] geom;

    private List<Integer> viewIndexList;

    public HeuristicFidelityTechnique(boolean usePerceptuallyLinearError, File debugDirectory)
    {
        this.usePerceptuallyLinearError = usePerceptuallyLinearError;
        this.debugDirectory = debugDirectory;
    }

    @Override
    public boolean isGuaranteedMonotonic()
    {
        return false;
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

    private IntVector3 encode(Vector3 color)
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
    }

    private Vector3 decode(IntVector3 color)
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
    }

    @Override
    public void initialize(IBRResources<ContextType> resources, ReadonlySettingsModel settings, int size) throws IOException
    {
        this.resources = resources;

        Vector3 primaryViewDisplacement = resources.viewSet.getCameraPose(resources.viewSet.getPrimaryViewIndex())
                .times(resources.geometry.getCentroid().asPosition()).getXYZ();

        unitReflectanceEncoding = 255.0f
                / (float)Math.pow(
                    decode(new IntVector3(255)).y * primaryViewDisplacement.dot(primaryViewDisplacement)
                        / resources.viewSet.getLightIntensity(resources.viewSet.getLightIndex(resources.viewSet.getPrimaryViewIndex())).y,
                    usePerceptuallyLinearError ? (1.0 / 2.2) : 1.0);

        intensities = new byte[resources.viewSet.getCameraPoseCount()][size * size];
        weights = new byte[resources.viewSet.getCameraPoseCount()][size * size];
        geom = new byte[resources.viewSet.getCameraPoseCount()][size * size * 3];

        try
        (
            Program<ContextType> projTexProgram = resources.getIBRShaderProgramBuilder()
                .define("VISIBILITY_TEST_ENABLED", resources.depthTextures != null && settings.getBoolean("occlusionEnabled"))
                .define("SHADOW_TEST_ENABLED", resources.shadowTextures != null && settings.getBoolean("occlusionEnabled"))
                .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/relight/projtex_multi_fidelity.frag"))
                .createProgram();

            FramebufferObject<ContextType> framebuffer = resources.context.buildFramebufferObject(2048, 2048)
                .addColorAttachment(ColorFormat.RGBA32F)
                .addColorAttachment(ColorFormat.RGB32F)
                .createFramebufferObject()
        )
        {
            Drawable<ContextType> drawable = resources.context.createDrawable(projTexProgram);
            drawable.addVertexBuffer("position", resources.positionBuffer);
            drawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
            drawable.addVertexBuffer("normal", resources.normalBuffer);
            drawable.addVertexBuffer("tangent", resources.tangentBuffer);

            resources.setupShaderProgram(projTexProgram);
            if (settings.getBoolean("occlusionEnabled"))
            {
                projTexProgram.setUniform("occlusionBias", settings.getFloat("occlusionBias"));
            }

            drawable.program().setUniform("lightIntensityCompensation", true);

            resources.context.getState().disableBackFaceCulling();


            ByteBuffer colorByteBuffer = BufferUtils.createByteBuffer(2048 * 2048 * 4);
            ByteBuffer geomByteBuffer = BufferUtils.createByteBuffer(2048 * 2048 * 4);

            int[] pixelCounts = new int[size * size];
            int[] intensitySums = new int[size * size];
            int[] weightSums = new int[size * size];
            int[] geomSums = new int[size * size * 3];

            for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
            {
                framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);

                projTexProgram.setUniform("viewIndex", i);

                drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

                framebuffer.readColorBufferARGB(0, colorByteBuffer);
                Arrays.fill(intensitySums, 0);
                Arrays.fill(pixelCounts, 0);
                Arrays.fill(weightSums, 0);

                for (int j = 0; j < 2048 * 2048; j++)
                {
                    Color color = new Color(colorByteBuffer.asIntBuffer().get(j), true);

                    if (color.getAlpha() > 0)
                    {
                        float decodedIntensity = color.getRed() / 255.0f;
                        if (!usePerceptuallyLinearError)
                        {
                            decodedIntensity = (float)Math.pow(decodedIntensity, 2.2);
                            //decodedColor = decode(colorVector);
                        }

                        @SuppressWarnings("IntegerDivisionInFloatingPointContext")
                        int k = (int)Math.floor((j % 2048) * size / 2048.0) + size * (int)Math.floor((j / 2048) * size / 2048.0);

                        intensitySums[k] += Math.round(decodedIntensity * unitReflectanceEncoding);
                        weightSums[k] += Math.round(color.getGreen() / 255.0f);
                        pixelCounts[k]++;
                    }
                }

                if (debugDirectory != null)
                {
                    try
                    {
                        String[] filenameParts = resources.viewSet.getImageFileName(i).split("\\.");
                        filenameParts[filenameParts.length - 1] = "png";

                        framebuffer.saveColorBufferToFile(0, "PNG",
                            new File(debugDirectory, String.join(".", filenameParts)));

                        filenameParts[filenameParts.length - 2] += "_geom";
                        framebuffer.saveColorBufferToFile(1, "PNG",
                                new File(debugDirectory, String.join(".", filenameParts)));
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }

                framebuffer.readColorBufferARGB(1, geomByteBuffer);
                Arrays.fill(geomSums, 0);

                for (int j = 0; j < 2048 * 2048; j++)
                {
                    if (new Color(colorByteBuffer.asIntBuffer().get(j), true).getAlpha() > 0)
                    {
                        Color geomColor = new Color(geomByteBuffer.asIntBuffer().get(j), true);

                        @SuppressWarnings("IntegerDivisionInFloatingPointContext")
                        int k = (int)Math.floor((j % 2048) * size / 2048.0) + size * (int)Math.floor((j / 2048) * size / 2048.0);

                        geomSums[3 * k]     += Math.round(geomColor.getRed());
                        geomSums[3 * k + 1] += Math.round(geomColor.getGreen());
                        geomSums[3 * k + 2] += Math.round(geomColor.getBlue());
                    }
                }

                for (int k = 0; k < pixelCounts.length; k++)
                {
                    if (pixelCounts[k] > 0)
                    {
                        intensities[i][k]  = (byte)Math.min(255, Math.round((float)intensitySums[k]    / (float)pixelCounts[k]));
                        weights[i][k]      = (byte)Math.min(255, Math.round((float)weightSums[k]       / (float)pixelCounts[k]));
                        geom[i][3 * k]     = (byte)Math.min(255, Math.round((float)geomSums[3 * k]     / (float)pixelCounts[k]));
                        geom[i][3 * k + 1] = (byte)Math.min(255, Math.round((float)geomSums[3 * k + 1] / (float)pixelCounts[k]));
                        geom[i][3 * k + 2] = (byte)Math.min(255, Math.round((float)geomSums[3 * k + 2] / (float)pixelCounts[k]));
                    }
                }
            }
        }
    }

    @Override
    public void setMask(File maskFile)
    {
        if (maskFile != null)
        {
            throw new UnsupportedOperationException("Masks are not currently supported.");
        }
    }

    @Override
    public void updateActiveViewIndexList(List<Integer> activeViewIndexList)
    {
        this.viewIndexList = new ArrayList<>(activeViewIndexList);
    }

    private DoubleVector2 getGeom2(int viewIndex, int pixelIndex)
    {
        return new DoubleVector2(
            (0x000000FF & geom[viewIndex][3 * pixelIndex])     * (2.0 / 255.0) - 1.0,
            (0x000000FF & geom[viewIndex][3 * pixelIndex + 1]) * (2.0 / 255.0) - 1.0);
    }

    private DoubleVector3 getGeom3(int viewIndex, int pixelIndex)
    {
        return new DoubleVector3(
            (0x000000FF & geom[viewIndex][3 * pixelIndex])     * (2.0 / 255.0) - 1.0,
            (0x000000FF & geom[viewIndex][3 * pixelIndex + 1]) * (2.0 / 255.0) - 1.0,
            (0x000000FF & geom[viewIndex][3 * pixelIndex + 2]) * (2.0 / 255.0) - 1.0);
    }

    private float getIntensity(int viewIndex, int pixelIndex)
    {
        return 0x000000FF & intensities[viewIndex][pixelIndex];
    }

    private float getWeight(int viewIndex, int pixelIndex)
    {
        return 0x000000FF & weights[viewIndex][pixelIndex];
    }

    @Override
    public double evaluateError(int targetViewIndex, File debugFile)
    {
        int[] peakSpecularPixelIndices = IntStream.range(0, intensities[targetViewIndex].length - 1)
            .filter(i -> getIntensity(targetViewIndex,i) > 0
                && viewIndexList.stream().mapToInt(j -> j)
                    .allMatch(j -> j == targetViewIndex
                        || getWeight(targetViewIndex, i) * getIntensity(targetViewIndex,i)
                            > getWeight(j, i) * getIntensity(j,i)))
            .toArray();

//        Vector3 weightedNormalDirection = Arrays.stream(peakSpecularPixelIndices)
//            .mapToObj(k -> getGeom3(targetViewIndex, k).normalized()
//                .times((0x000000FF & intensities[targetViewIndex][k]) / unitReflectanceEncoding))
//            .reduce(Vector3.ZERO, Vector3::plus)
//            .normalized();
//
//        double weightedDifference = Arrays.stream(peakSpecularPixelIndices)
//            .mapToDouble(k ->
//            {
//                Vector3 diff = getGeom3(targetViewIndex, k).normalized().minus(weightedNormalDirection);
//                return diff.dot(diff) * (0x000000FF & intensities[targetViewIndex][k]) / unitReflectanceEncoding;
//            })
//            .average()
//            .orElse(1.0);
//
//        double unweightedDifference = Arrays.stream(peakSpecularPixelIndices)
//            .mapToDouble(k ->
//            {
//                Vector3 diff = getGeom3(targetViewIndex, k).normalized().minus(weightedNormalDirection);
//                return diff.dot(diff);
//            })
//            .average()
//            .orElse(1.0);
//
//        return 1.0 - weightedDifference / unweightedDifference;

        double sumWeights = IntStream.range(0, weights[targetViewIndex].length)
            .mapToDouble(i -> getWeight(targetViewIndex, i))
            .sum();

        DoubleVector2 center = IntStream.range(0, intensities[targetViewIndex].length - 1)
            .mapToObj(k -> getGeom2(targetViewIndex, k).times(getWeight(targetViewIndex, k)))
            .reduce(DoubleVector2.ZERO, DoubleVector2::plus).dividedBy(sumWeights);

        double xVariance = IntStream.range(0, weights[targetViewIndex].length)
            .mapToDouble(k ->
            {
                double x = getGeom2(targetViewIndex, k).x - center.x;
                return x * x * getWeight(targetViewIndex, k);
            })
            .sum();

        double yVariance = IntStream.range(0, weights[targetViewIndex].length)
            .mapToDouble(k ->
            {
                double y = getGeom2(targetViewIndex, k).y - center.y;
                return y * y * getWeight(targetViewIndex, k);
            })
            .sum();

        double covariance = IntStream.range(0, weights[targetViewIndex].length)
            .mapToDouble(k ->
            {
                double x = getGeom2(targetViewIndex, k).x - center.x;
                double y = getGeom2(targetViewIndex, k).y - center.y;
                return x * y * getWeight(targetViewIndex, k);
            })
            .sum();

        double sumIntensities = IntStream.range(0, intensities[targetViewIndex].length - 1)
            .mapToDouble(k -> getWeight(targetViewIndex, k) * getIntensity(targetViewIndex, k))
            .sum();

        double sumPeakIntensities = Arrays.stream(peakSpecularPixelIndices)
            .mapToDouble(k -> getWeight(targetViewIndex, k) * getIntensity(targetViewIndex, k))
            .sum();

        double[] diffs = Arrays.stream(peakSpecularPixelIndices)
            .mapToDouble(k ->
                getIntensity(targetViewIndex, k) * getWeight(targetViewIndex, k)
                    - viewIndexList.stream()
                        .filter(j -> j != targetViewIndex)
                        .mapToDouble(j -> getIntensity(j, k) * getWeight(j, k))
                        .max().orElse(0.0))
            .toArray();

        double sumDiffs = Arrays.stream(diffs).sum();

        DoubleVector2 diffWeightedCenter = IntStream.range(0, peakSpecularPixelIndices.length)
            .mapToObj(i -> getGeom2(targetViewIndex, peakSpecularPixelIndices[i]).times(diffs[i]))
            .reduce(DoubleVector2.ZERO, DoubleVector2::plus).dividedBy(sumDiffs);

        double diffWeightedXVariance = IntStream.range(0, peakSpecularPixelIndices.length)
            .mapToDouble(i ->
            {
                double x = getGeom2(targetViewIndex, peakSpecularPixelIndices[i]).x - diffWeightedCenter.x;
                return x * x * diffs[i];
            })
            .sum();

        double diffWeightedYVariance = IntStream.range(0, peakSpecularPixelIndices.length)
            .mapToDouble(i ->
            {
                double y = getGeom2(targetViewIndex, peakSpecularPixelIndices[i]).y - diffWeightedCenter.y;
                return y * y * diffs[i];
            })
            .sum();

        double diffWeightedCovariance = IntStream.range(0, peakSpecularPixelIndices.length)
            .mapToDouble(i ->
            {
                double x = getGeom2(targetViewIndex, peakSpecularPixelIndices[i]).x - diffWeightedCenter.x;
                double y = getGeom2(targetViewIndex, peakSpecularPixelIndices[i]).y - diffWeightedCenter.y;
                return x * y * diffs[i];
            })
            .sum();

        return Math.max(0.0, Math.min(1.0,
            (sumDiffs
                    - (1.0 - sumDiffs / sumIntensities)
                        * Math.sqrt((diffWeightedXVariance * diffWeightedYVariance - diffWeightedCovariance * diffWeightedCovariance)
                            / (xVariance * yVariance - covariance * covariance))
                        * sumWeights)
                / sumPeakIntensities));

//        DoubleVector2 secondaryIntensityWeightedDirection = new DoubleVector2(
//                0.5 * (diffWeightedXVariance - diffWeightedYVariance
//                    - Math.sqrt(diffWeightedXVariance * diffWeightedXVariance + diffWeightedYVariance * diffWeightedYVariance
//                        - 2 * diffWeightedXVariance * diffWeightedYVariance + 4 * diffWeightedCovariance)),
//                diffWeightedCovariance)
//            .normalized();
//
//        @SuppressWarnings("SuspiciousNameCombination")
//        DoubleVector2 primaryIntensityWeightedDirection =
//            new DoubleVector2(secondaryIntensityWeightedDirection.y, -secondaryIntensityWeightedDirection.x);
    }

    @Override
    public void close()
    {
    }
}
