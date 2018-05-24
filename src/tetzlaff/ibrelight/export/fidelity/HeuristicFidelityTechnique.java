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
    private byte[][] normals;

    private List<Integer> viewIndexList;

    public HeuristicFidelityTechnique(boolean usePerceptuallyLinearError, File debugDirectory)
    {
        this.usePerceptuallyLinearError = usePerceptuallyLinearError;
        this.debugDirectory = debugDirectory;
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
        normals = new byte[resources.viewSet.getCameraPoseCount()][size * size * 3];

        try
        (
            Program<ContextType> projTexProgram = resources.getIBRShaderProgramBuilder()
                .define("VISIBILITY_TEST_ENABLED", resources.depthTextures != null && settings.getBoolean("occlusionEnabled"))
                .define("SHADOW_TEST_ENABLED", resources.shadowTextures != null && settings.getBoolean("occlusionEnabled"))
                .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/colorappearance/projtex_multi_lab_normal.frag"))
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
            ByteBuffer normalByteBuffer = BufferUtils.createByteBuffer(2048 * 2048 * 4);

            int[] intensitySums = new int[size * size];
            int[] intensityCounts = new int[size * size];
            int[] normalSums = new int[size * size * 3];
            int[] normalCounts = new int[size * size];

            for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
            {
                framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);

                projTexProgram.setUniform("viewIndex", i);

                drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

                framebuffer.readColorBufferARGB(0, colorByteBuffer);
                Arrays.fill(intensitySums, 0);
                Arrays.fill(intensityCounts, 0);

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
                        intensityCounts[k]++;
                    }
                }

                for (int k = 0; k < intensityCounts.length; k++)
                {
                    intensities[i][k] = (byte)Math.min(255, Math.round((float)intensitySums[k] / (float)intensityCounts[k]));
                }

                if (debugDirectory != null)
                {
                    try
                    {
                        String[] filenameParts = resources.viewSet.getImageFileName(i).split("\\.");
                        filenameParts[filenameParts.length - 1] = "png";

                        framebuffer.saveColorBufferToFile(0, "PNG",
                            new File(debugDirectory, String.join(".", filenameParts)));

                        filenameParts[filenameParts.length - 2] += "_normal";
                        framebuffer.saveColorBufferToFile(1, "PNG",
                                new File(debugDirectory, String.join(".", filenameParts)));
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }

                framebuffer.readColorBufferARGB(1, normalByteBuffer);
                Arrays.fill(normalSums, 0);
                Arrays.fill(normalCounts, 0);

                for (int j = 0; j < 2048 * 2048; j++)
                {
                    if (new Color(colorByteBuffer.asIntBuffer().get(j), true).getAlpha() > 0)
                    {
                        Color normalColor = new Color(normalByteBuffer.asIntBuffer().get(j), true);

                        @SuppressWarnings("IntegerDivisionInFloatingPointContext")
                        int k = (int)Math.floor((j % 2048) * size / 2048.0) + size * (int)Math.floor((j / 2048) * size / 2048.0);

                        normalSums[3 * k]     += Math.round(normalColor.getRed());
                        normalSums[3 * k + 1] += Math.round(normalColor.getGreen());
                        normalSums[3 * k + 2] += Math.round(normalColor.getBlue());
                        normalCounts[k]++;
                    }
                }

                for (int k = 0; k < normalCounts.length; k++)
                {
                    normals[i][3 * k]     = (byte)Math.min(255, Math.round((float)normalSums[3 * k]     / (float)normalCounts[k]));
                    normals[i][3 * k + 1] = (byte)Math.min(255, Math.round((float)normalSums[3 * k + 1] / (float)normalCounts[k]));
                    normals[i][3 * k + 2] = (byte)Math.min(255, Math.round((float)normalSums[3 * k + 2] / (float)normalCounts[k]));
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

    private Vector3 decodeNormal(int targetViewIndex, int k)
    {
        return new Vector3(
            normals[targetViewIndex][3 * k]     * (2.0f / 255.0f) - 1.0f,
            normals[targetViewIndex][3 * k + 1] * (2.0f / 255.0f) - 1.0f,
            normals[targetViewIndex][3 * k + 2] * (2.0f / 255.0f) - 1.0f)
            .normalized();
    }

    @Override
    public double evaluateError(int targetViewIndex, File debugFile)
    {
        int[] peakSpecularPixelIndices = IntStream.range(0, intensities[targetViewIndex].length - 1)
            .filter(i -> (0x000000FF & intensities[targetViewIndex][i]) > 0
                && viewIndexList.stream().mapToInt(j -> j)
                    .allMatch(j -> j == targetViewIndex
                        || (0x000000FF & intensities[targetViewIndex][i]) > (0x000000FF & intensities[j][i])))
            .toArray();

        Vector3 weightedNormalDirection = Arrays.stream(peakSpecularPixelIndices)
            .mapToObj(k -> decodeNormal(targetViewIndex, k).times(intensities[targetViewIndex][k] / unitReflectanceEncoding))
            .reduce(Vector3.ZERO, Vector3::plus)
            .normalized();

        double weightedDifference = Arrays.stream(peakSpecularPixelIndices)
            .mapToDouble(k ->
            {
                Vector3 diff = decodeNormal(targetViewIndex, k).minus(weightedNormalDirection);
                return diff.dot(diff) * intensities[targetViewIndex][k] / unitReflectanceEncoding;
            })
            .average()
            .orElse(1.0);

        double unweightedDifference = Arrays.stream(peakSpecularPixelIndices)
            .mapToDouble(k ->
            {
                Vector3 diff = decodeNormal(targetViewIndex, k).minus(weightedNormalDirection);
                return diff.dot(diff) * intensities[targetViewIndex][k] / unitReflectanceEncoding;
            })
            .average()
            .orElse(1.0);

        return 1.0 - weightedDifference / unweightedDifference;
    }

    @Override
    public void close()
    {
    }
}
