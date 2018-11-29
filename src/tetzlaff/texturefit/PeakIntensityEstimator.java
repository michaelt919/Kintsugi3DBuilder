package tetzlaff.texturefit;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import org.lwjgl.*;
import tetzlaff.gl.core.*;
import tetzlaff.gl.vecmath.*;
import tetzlaff.ibrelight.core.ViewSet;

class PeakIntensityEstimator<ContextType extends Context<ContextType>>
{
    private final Context<ContextType> context;
    private final ViewSet viewSet;

    private Consumer<Drawable<ContextType>> shaderSetup;
    private Program<ContextType> imgSpaceProgram;
    private Program<ContextType> texSpaceProgram;

    private Texture3D<ContextType> viewImages;
    private Texture3D<ContextType> depthImages;
    private Texture3D<ContextType> shadowImages;
    private Texture<ContextType> diffuseTexture;
    private Texture<ContextType> normalTexture;

    PeakIntensityEstimator(Context<ContextType> context, ViewSet viewSet)
    {
        this.context = context;
        this.viewSet = viewSet;
    }

    void init(Consumer<Drawable<ContextType>> shaderSetup, Texture3D<ContextType> viewImages, Texture3D<ContextType> depthImages,
        Texture3D<ContextType> shadowImages, Texture<ContextType> diffuseTexture, Texture<ContextType> normalTexture)
    {
        this.shaderSetup = shaderSetup;
        this.viewImages = viewImages;
        this.depthImages = depthImages;
        this.shadowImages = shadowImages;
        this.diffuseTexture = diffuseTexture;
        this.normalTexture = normalTexture;
    }

    void compileShaders(boolean visibilityTest, boolean shadowTest) throws IOException
    {
        imgSpaceProgram = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "imgspace.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders","texturefit", "specularpeakinfo_imgspace.frag").toFile())
            .define("LUMINANCE_MAP_ENABLED", viewSet.hasCustomLuminanceEncoding())
            .define("INFINITE_LIGHT_SOURCES", viewSet.areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", visibilityTest)
            .define("SHADOW_TEST_ENABLED", shadowTest)
            .createProgram();

        texSpaceProgram = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace_noscale.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders","texturefit", "specularpeakinfo_imgspace.frag").toFile())
            .define("LUMINANCE_MAP_ENABLED", viewSet.hasCustomLuminanceEncoding())
            .define("INFINITE_LIGHT_SOURCES", viewSet.areLightSourcesInfinite())
            .define("VISIBILITY_TEST_ENABLED", visibilityTest)
            .define("SHADOW_TEST_ENABLED", shadowTest)
            .createProgram();
    }

    private static class CharacteristicBin
    {
        final IntVector3 offPeakSum;
        final IntVector3 position;

        CharacteristicBin(IntVector3 offPeakSum, IntVector3 position)
        {
            this.offPeakSum = offPeakSum;
            this.position = position;
        }

        static CharacteristicBin fromContinuous(Vector3 offPeakSum, Vector3 position, float objSpaceRadius, float colorSpaceRadius)
        {
            return new CharacteristicBin(
                new IntVector3(Math.round(offPeakSum.x / colorSpaceRadius),
                    Math.round(offPeakSum.y / colorSpaceRadius),
                    Math.round(offPeakSum.z / colorSpaceRadius)),
                new IntVector3(Math.round(position.x / objSpaceRadius),
                    Math.round(position.y / objSpaceRadius),
                    Math.round(position.z / objSpaceRadius)));
        }

        public boolean equals(Object obj)
        {
            if (obj instanceof CharacteristicBin)
            {
                CharacteristicBin other = (CharacteristicBin)obj;
                return Objects.equals(offPeakSum, other.offPeakSum) && Objects.equals(position, other.position);
            }
            else
            {
                return false;
            }
        }

        @Override
        public int hashCode()
        {
            return 31 * (31 + (offPeakSum == null ? 0 : offPeakSum.hashCode()))
                + (position == null ? 0 : position.hashCode());
        }

        Collection<CharacteristicBin> getSurrounding()
        {
            return IntStream.range(0, 729)
                .mapToObj(i ->
                {
                    int current = i;
                    int d1 = (current + 1) % 3 - 1;
                    current /= 3;
                    int d2 = (current + 1) % 3 - 1;
                    current /= 3;
                    int d3 = (current + 1) % 3 - 1;
                    current /= 3;
                    int d4 = (current + 1) % 3 - 1;
                    current /= 3;
                    int d5 = (current + 1) % 3 - 1;
                    current /= 3;
                    int d6 = (current + 1) % 3 - 1;

                    return new CharacteristicBin(
                        this.offPeakSum.plus(new IntVector3(d1, d2, d3)),
                        this.position.plus(new IntVector3(d4, d5, d6)));
                })
                .collect(Collectors.toList());
        }
    }

    private static class PeakCandidate
    {
        final Vector3 peak;
        final Vector3 offPeakSum;
        final Vector3 position;
        final float nDotH;
        final Vector3 threshold;
        final float nDotV;
        final Vector2 texCoord;

        PeakCandidate(Vector3 peak, Vector3 offPeakSum, Vector3 position, float nDotH, Vector3 threshold, float nDotV, Vector2 texCoord)
        {
            this.peak = peak;
            this.offPeakSum = offPeakSum;
            this.position = position;
            this.nDotH = nDotH;
            this.threshold = threshold;
            this.nDotV = nDotV;
            this.texCoord = texCoord;
        }

        CharacteristicBin characteristicBin(float objSpaceRadius, float colorSpaceRadius)
        {
            return CharacteristicBin.fromContinuous(offPeakSum, position, objSpaceRadius, colorSpaceRadius);
        }
    }

    Vector4[] estimate(int texWidth, int texHeight, float objSpaceRadius, float colorSpaceRadius)
    {
        new File("debug").mkdir(); // debug
//        new File("debug", "peak").mkdirs(); // debug
//        new File("debug", "offPeak").mkdirs(); // debug
//        new File("debug", "position").mkdirs(); // debug

        Drawable<ContextType> imgSpaceDrawable = context.createDrawable(imgSpaceProgram);
        shaderSetup.accept(imgSpaceDrawable);
        imgSpaceDrawable.program().setTexture("viewImages", viewImages);
        imgSpaceDrawable.program().setTexture("depthImages", depthImages);
        imgSpaceDrawable.program().setTexture("shadowImages",
            shadowImages == null ? context.getTextureFactory().getNullTexture(SamplerType.FLOAT_2D_ARRAY) : shadowImages);
        imgSpaceDrawable.program().setTexture("diffuseEstimate", diffuseTexture);
        imgSpaceDrawable.program().setTexture("normalEstimate", normalTexture);

        FloatBuffer peakBuffer =
            BufferUtils.createFloatBuffer(4 * viewImages.getWidth() * viewImages.getHeight());
        FloatBuffer offPeakBuffer =
            BufferUtils.createFloatBuffer(4 * viewImages.getWidth() * viewImages.getHeight());
        FloatBuffer positionBuffer =
            BufferUtils.createFloatBuffer(4 * viewImages.getWidth() * viewImages.getHeight());
        FloatBuffer thresholdBuffer =
            BufferUtils.createFloatBuffer(4 * viewImages.getWidth() * viewImages.getHeight());
        FloatBuffer texCoordBuffer =
            BufferUtils.createFloatBuffer(4 * viewImages.getWidth() * viewImages.getHeight());

        System.out.println("Generating peak reflectance samples...");

        float maxOffPeak = 0.0f;

        Map<CharacteristicBin, List<PeakCandidate>> peakCandidates = new HashMap<>(100000);
        Queue<PeakCandidate> expectedPeaks = new PriorityQueue<>(256 * viewSet.getCameraPoseCount(),
            Comparator.comparingDouble(peakCandidate -> peakCandidate.nDotH));
        Map<CharacteristicBin, Vector4> residualSums = new HashMap<>(100000);
        long residualTotal = 0;

        try(FramebufferObject<ContextType> fbo = context.buildFramebufferObject(
            viewImages.getWidth(), viewImages.getHeight())
            .addColorAttachments(ColorFormat.RGBA32F, 5)
            .createFramebufferObject())
        {

            for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
            {
                System.out.println((i + 1) + "/" + viewSet.getCameraPoseCount());

                imgSpaceProgram.setUniform("model_view", viewSet.getCameraPose(i));
                imgSpaceProgram.setUniform("projection",
                    viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(i))
                        .getProjectionMatrix(viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
                imgSpaceProgram.setUniform("viewIndex", i);

                fbo.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
                fbo.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
                fbo.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
                fbo.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
                fbo.clearColorBuffer(4, 0.0f, 0.0f, 0.0f, 0.0f);
                imgSpaceDrawable.draw(PrimitiveMode.TRIANGLES, fbo);

                fbo.readFloatingPointColorBufferRGBA(0, peakBuffer);
                fbo.readFloatingPointColorBufferRGBA(1, offPeakBuffer);
                fbo.readFloatingPointColorBufferRGBA(2, positionBuffer);
                fbo.readFloatingPointColorBufferRGBA(3, thresholdBuffer);
                fbo.readFloatingPointColorBufferRGBA(4, texCoordBuffer);

//                // debug
//                try
//                {
//                    fbo.saveColorBufferToFile(0, "PNG",
//                        new File(new File("debug", "peak"), viewSet.getImageFileName(i)));
//                    fbo.saveColorBufferToFile(1, "PNG",
//                        new File(new File("debug", "offPeak"), viewSet.getImageFileName(i)));
//                    fbo.saveColorBufferToFile(2, "PNG",
//                        new File(new File("debug", "position"), viewSet.getImageFileName(i)));
//                }
//                catch (IOException e)
//                {
//                    e.printStackTrace();
//                }

                for (int y = 0; y < viewImages.getHeight(); y += 8)
                {
                    for (int x = 0; x < viewImages.getHeight(); x += 8)
                    {
                        PeakCandidate maxPeak = null;

                        for (int dy = 0; dy < 8 && y + dy < viewImages.getHeight(); dy++)
                        {
                            for (int dx = 0; dx < 8 && x + dx < viewImages.getWidth(); dx++)
                            {
                                int k = (y + dy) * viewImages.getWidth() + x + dx;

                                if (Float.isFinite(peakBuffer.get(4 * k)) && Float.isFinite(peakBuffer.get(4 * k + 1))
                                    && Float.isFinite(peakBuffer.get(4 * k + 2)) && peakBuffer.get(4 * k + 3) > 0.0)
                                {
                                    PeakCandidate peakCandidate = new PeakCandidate(
                                        new Vector3(peakBuffer.get(4 * k), peakBuffer.get(4 * k + 1), peakBuffer.get(4 * k + 2)),
                                        new Vector3(offPeakBuffer.get(4 * k), offPeakBuffer.get(4 * k + 1), offPeakBuffer.get(4 * k + 2)),
                                        new Vector3(positionBuffer.get(4 * k), positionBuffer.get(4 * k + 1), positionBuffer.get(4 * k + 2)),
                                        positionBuffer.get(4 * k + 3), /* n dot h */
                                        new Vector3(thresholdBuffer.get(4 * k), thresholdBuffer.get(4 * k + 1), thresholdBuffer.get(4 * k + 2)),
                                        thresholdBuffer.get(4 * k + 3), /* n dot v */
                                        new Vector2(texCoordBuffer.get(4 * k), texCoordBuffer.get(4 * k + 1)));

                                    if (maxPeak == null || maxPeak.peak.y < peakCandidate.peak.y)
                                    {
                                        maxPeak = peakCandidate;
                                    }

                                    CharacteristicBin residualSumBin = CharacteristicBin.fromContinuous(
                                        peakCandidate.offPeakSum, Vector3.ZERO, 1.0f, colorSpaceRadius);

                                    if (residualSums.containsKey(residualSumBin))
                                    {
                                        residualSums.put(residualSumBin, residualSums.get(residualSumBin)
                                            .plus(peakCandidate.peak.minus(peakCandidate.threshold).asVector4(1.0f)));
                                    }
                                    else
                                    {
                                        residualSums.put(residualSumBin, peakCandidate.peak.minus(peakCandidate.threshold).asVector4(1.0f));
                                    }

                                    residualTotal++;
                                }

                                if (Float.isFinite(offPeakBuffer.get(4 * k)) && Float.isFinite(offPeakBuffer.get(4 * k + 1))
                                        && Float.isFinite(offPeakBuffer.get(4 * k + 2)))
                                {
                                    maxOffPeak = Math.max(maxOffPeak, Math.max(offPeakBuffer.get(4 * k),
                                        Math.max(offPeakBuffer.get(4 * k + 1), offPeakBuffer.get(4 * k + 2))));
                                }
                            }
                        }

                        if (maxPeak != null)
                        {
                            CharacteristicBin binID = maxPeak.characteristicBin(objSpaceRadius, colorSpaceRadius);
                            List<PeakCandidate> bin = peakCandidates.get(binID);
                            if (bin == null)
                            {
                                bin = new ArrayList<>(1);
                                peakCandidates.put(binID, new ArrayList<>(1));
                            }
                            bin.add(maxPeak);

                            if (maxPeak.nDotH > 0.999)
                            {
                                if (expectedPeaks.size() < 256 * viewSet.getCameraPoseCount())
                                {
                                    expectedPeaks.add(maxPeak);
                                }
                                else if (maxPeak.nDotH > expectedPeaks.peek().nDotH)
                                {
                                    expectedPeaks.remove();
                                    expectedPeaks.add(maxPeak);
                                }
                            }
                        }
                    }
                }
            }
        }

        System.out.println("Max off peak: " + maxOffPeak);

        Drawable<ContextType> texSpaceDrawable = context.createDrawable(texSpaceProgram);
        shaderSetup.accept(texSpaceDrawable);
        texSpaceDrawable.program().setTexture("viewImages", viewImages);
        texSpaceDrawable.program().setTexture("depthImages", depthImages);
        texSpaceDrawable.program().setTexture("shadowImages",
            shadowImages == null ? context.getTextureFactory().getNullTexture(SamplerType.FLOAT_2D_ARRAY) : shadowImages);
        texSpaceDrawable.program().setTexture("diffuseEstimate", diffuseTexture);
        texSpaceDrawable.program().setTexture("normalEstimate", normalTexture);

        float[] offPeakTexSpace;
        float[] positionsTexSpace;
        float[] thresholdTexSpace;

        try(FramebufferObject<ContextType> fbo = context.buildFramebufferObject(texWidth, texHeight)
            .addColorAttachments(ColorFormat.RGBA32F, 4)
            .createFramebufferObject())
        {
            fbo.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
            fbo.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
            fbo.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
            texSpaceDrawable.draw(PrimitiveMode.TRIANGLES, fbo);

            offPeakTexSpace = fbo.readFloatingPointColorBufferRGBA(1);
            positionsTexSpace = fbo.readFloatingPointColorBufferRGBA(2);
            thresholdTexSpace = fbo.readFloatingPointColorBufferRGBA(3);
        }

        System.out.println("Sorting samples...");

        Map<CharacteristicBin, List<PeakCandidate>> redSorted = new HashMap<>(peakCandidates.size());
        Map<CharacteristicBin, List<PeakCandidate>> greenSorted = new HashMap<>(peakCandidates.size());
        Map<CharacteristicBin, List<PeakCandidate>> blueSorted = new HashMap<>(peakCandidates.size());

        for (Entry<CharacteristicBin, List<PeakCandidate>> bin : peakCandidates.entrySet())
        {
            redSorted.put(bin.getKey(), bin.getValue().stream()
                .parallel()
                .sorted(Comparator.<PeakCandidate, Float>comparing(candidate -> candidate.peak.x).reversed())
                .collect(Collectors.toList()));

            greenSorted.put(bin.getKey(), bin.getValue().stream()
                .parallel()
                .sorted(Comparator.<PeakCandidate, Float>comparing(candidate -> candidate.peak.y).reversed())
                .collect(Collectors.toList()));

            blueSorted.put(bin.getKey(), bin.getValue().stream()
                .parallel()
                .sorted(Comparator.<PeakCandidate, Float>comparing(candidate -> candidate.peak.z).reversed())
                .collect(Collectors.toList()));
        }

        System.out.println("Estimating residual...");
        System.out.println("____________________________________________________________________________________________________");

        Vector3[] estimatedResidual = IntStream.range(0, texWidth * texHeight)
            .parallel()
            .mapToObj(i ->
            {
                if ((100 * i) / (texWidth * texHeight) > (100 * (i-1)) / (texWidth * texHeight))
                {
                    System.out.print(".");
                }

                float[] totals = new float[4];

                Vector3 offPeak = new Vector3(offPeakTexSpace[4 * i], offPeakTexSpace[4 * i + 1], offPeakTexSpace[4 * i + 2]);

                CharacteristicBin residualSumBin = CharacteristicBin.fromContinuous(offPeak, Vector3.ZERO, 1.0f, colorSpaceRadius);

                for (CharacteristicBin bin : residualSumBin.getSurrounding())
                {
                    Vector4 localSum = residualSums.get(bin);

                    if (localSum != null)
                    {
                        float weight = (colorSpaceRadius - offPeak.distance(bin.offPeakSum.asFloatingPoint().times(colorSpaceRadius))) / colorSpaceRadius;
                        totals[0] += weight * localSum.x;
                        totals[1] += weight * localSum.y;
                        totals[2] += weight * localSum.z;
                        totals[3] += weight * localSum.w;
                    }
                }

                return new Vector3(2 * totals[0] / (viewSet.getCameraPoseCount() * totals[3]),
                    2 * totals[1] / (viewSet.getCameraPoseCount() * totals[3]),
                    2 * totals[2] / (viewSet.getCameraPoseCount() * totals[3]));
            })
            .toArray(Vector3[]::new);

        System.out.println();
        System.out.println("Estimating peak reflectance...");
        System.out.println("____________________________________________________________________________________________________");

        Vector3[] estimatedPeaks = IntStream.range(0, texWidth * texHeight)
            .parallel()
            .mapToObj(i ->
            {
                if ((100 * i) / (texWidth * texHeight) > (100 * (i-1)) / (texWidth * texHeight))
                {
                    System.out.print(".");
                }

                if (offPeakTexSpace[4 * i + 3] > 0)
                {
                    Vector3 position = new Vector3(
                        positionsTexSpace[4 * i],
                        positionsTexSpace[4 * i + 1],
                        positionsTexSpace[4 * i + 2]);

                    Vector3 offPeakSum = new Vector3(
                        offPeakTexSpace[4 * i],
                        offPeakTexSpace[4 * i + 1],
                        offPeakTexSpace[4 * i + 2]);

                    Collection<CharacteristicBin> binNeighborhood =
                        CharacteristicBin.fromContinuous(offPeakSum, position, objSpaceRadius, colorSpaceRadius).getSurrounding();

                    float lastPeak = Float.MAX_VALUE;
                    float red = 0;
                    float green = 0;
                    float blue = 0;

                    float distanceToExpectedPeak = (float)Math.sqrt(0.5);

                    for (PeakCandidate expectedPeak : expectedPeaks)
                    {
                        float scaledObjSpaceDistance = expectedPeak.position.distance(position) / objSpaceRadius;
                        float scaledColorSpaceDistance = expectedPeak.offPeakSum.distance(offPeakSum) / colorSpaceRadius;

                        distanceToExpectedPeak = Math.min(distanceToExpectedPeak,
                            (float)Math.sqrt(scaledObjSpaceDistance * scaledObjSpaceDistance + scaledColorSpaceDistance * scaledColorSpaceDistance));
                    }

                    for (CharacteristicBin bin : binNeighborhood)
                    {
                        List<PeakCandidate> redBin = redSorted.get(bin);

                        if (redBin != null)
                        {
                            for (int k = 0; k < redBin.size() && lastPeak >= red; k++)
                            {
                                PeakCandidate candidate = redBin.get(k);

                                float scaledObjSpaceDistance = candidate.position.distance(position) / objSpaceRadius;
                                float scaledColorSpaceDistance = candidate.offPeakSum.distance(offPeakSum) / colorSpaceRadius;

                                float weight = (float)Math.max(0, Math.min(1,
                                    (1.0 - Math.sqrt(0.5 * scaledObjSpaceDistance * scaledObjSpaceDistance
                                                + 0.5 * scaledColorSpaceDistance * scaledColorSpaceDistance)
                                            / distanceToExpectedPeak)
                                        / (1.0 - Math.sqrt(0.5))));

                                red = Math.max(red, weight * candidate.peak.x);
                                lastPeak = candidate.peak.x;
                            }
                        }

                        lastPeak = Float.MAX_VALUE;

                        List<PeakCandidate> greenBin = greenSorted.get(bin);

                        if (greenBin != null)
                        {
                            for (int k = 0; k < greenBin.size() && lastPeak >= green; k++)
                            {
                                PeakCandidate candidate = greenBin.get(k);

                                float scaledObjSpaceDistance = candidate.position.distance(position) / objSpaceRadius;
                                float scaledColorSpaceDistance = candidate.offPeakSum.distance(offPeakSum) / colorSpaceRadius;

                                float weight = (float)Math.max(0, Math.min(1,
                                    (1.0 - Math.sqrt(0.5 * scaledObjSpaceDistance * scaledObjSpaceDistance
                                                + 0.5 * scaledColorSpaceDistance * scaledColorSpaceDistance)
                                            / distanceToExpectedPeak)
                                        / (1.0 - Math.sqrt(0.5))));

                                green = Math.max(green, weight * candidate.peak.y);
                                lastPeak = candidate.peak.y;
                            }
                        }

                        lastPeak = Float.MAX_VALUE;

                        List<PeakCandidate> blueBin = blueSorted.get(bin);

                        if (blueBin != null)
                        {
                            for (int k = 0; k < blueBin.size() && lastPeak >= blue; k++)
                            {
                                PeakCandidate candidate = blueBin.get(k);

                                float scaledObjSpaceDistance = candidate.position.distance(position) / objSpaceRadius;
                                float scaledColorSpaceDistance = candidate.offPeakSum.distance(offPeakSum) / colorSpaceRadius;

                                float weight = (float)Math.max(0, Math.min(1,
                                    (1.0 - Math.sqrt(0.5 * scaledObjSpaceDistance * scaledObjSpaceDistance
                                                + 0.5 * scaledColorSpaceDistance * scaledColorSpaceDistance)
                                            / distanceToExpectedPeak)
                                        / (1.0 - Math.sqrt(0.5))));

                                blue = Math.max(blue, weight * candidate.peak.z);
                                lastPeak = candidate.peak.z;
                            }
                        }
                    }

                    return new Vector3(red, green, blue);
                }
                else
                {
                    return Vector3.ZERO;
                }
            })
            .toArray(Vector3[]::new);

        System.out.println();
        System.out.println("Filling holes...");

        Queue<IntVector2> holes = new ArrayDeque<>(texHeight * texWidth / 4);
        Map<IntVector2, Vector3> changes = new HashMap<>(texHeight * texWidth / 16);

        for (int y = 0; y < texHeight; y++)
        {
            for (int x = 0; x < texWidth; x++)
            {
                if (Objects.equals(estimatedPeaks[y * texWidth + x], Vector3.ZERO))
                {
                    holes.add(new IntVector2(x, y));
                }
            }
        }

        Collection<IntVector2> remainingHoles = new ArrayDeque<>(holes.size());

        do
        {
            while (!holes.isEmpty())
            {
                IntVector2 hole = holes.poll();

                Vector3 sum = Vector3.ZERO;
                int count = 0;

                if (hole.y > 0)
                {
                    Vector3 sample = estimatedPeaks[(hole.y - 1) * texWidth + hole.x];

                    if (!Objects.equals(sample, Vector3.ZERO))
                    {
                        sum = sum.plus(sample);
                        count++;
                    }
                }

                if (hole.y < texHeight - 1)
                {
                    Vector3 sample = estimatedPeaks[(hole.y + 1) * texWidth + hole.x];

                    if (!Objects.equals(sample, Vector3.ZERO))
                    {
                        sum = sum.plus(sample);
                        count++;
                    }
                }

                if (hole.x > 0)
                {
                    Vector3 sample = estimatedPeaks[hole.y * texWidth + hole.x - 1];

                    if (!Objects.equals(sample, Vector3.ZERO))
                    {
                        sum = sum.plus(sample);
                        count++;
                    }
                }

                if (hole.x < texWidth - 1)
                {
                    Vector3 sample = estimatedPeaks[hole.y * texWidth + hole.x + 1];

                    if (!Objects.equals(sample, Vector3.ZERO))
                    {
                        sum = sum.plus(sample);
                        count++;
                    }
                }

                if (count == 0)
                {
                    remainingHoles.add(hole);
                }
                else
                {
                    changes.put(hole, sum.dividedBy(count));
                }
            }

            for (Entry<IntVector2, Vector3> change : changes.entrySet())
            {
                estimatedPeaks[change.getKey().y * texWidth + change.getKey().x] = change.getValue();
            }

            holes.addAll(remainingHoles);
            remainingHoles.clear();
            changes.clear();
        }
        while(!holes.isEmpty());

        System.out.println("Filtering samples...");

        System.out.println("Estimating preliminary roughness...");
        System.out.println("____________________________________________________________________________________________________");

        double[] estimatedRoughness = IntStream.range(0, texWidth * texHeight)
            .parallel()
            .mapToDouble(i ->
            {
                if ((100 * i) / (texWidth * texHeight) > (100 * (i-1)) / (texWidth * texHeight))
                {
                    System.out.print(".");
                }

                if (offPeakTexSpace[4 * i + 3] > 0)
                {
                    Vector3 position = new Vector3(
                        positionsTexSpace[4 * i],
                        positionsTexSpace[4 * i + 1],
                        positionsTexSpace[4 * i + 2]);

                    Vector3 offPeakSum = new Vector3(
                        offPeakTexSpace[4 * i],
                        offPeakTexSpace[4 * i + 1],
                        offPeakTexSpace[4 * i + 2]);

                    Collection<CharacteristicBin> binNeighborhood =
                        CharacteristicBin.fromContinuous(offPeakSum, position, objSpaceRadius, colorSpaceRadius).getSurrounding();

                    float distanceToExpectedPeak = (float)Math.sqrt(0.5);

//                    for (PeakCandidate expectedPeak : expectedPeaks)
//                    {
//                        float scaledObjSpaceDistance = expectedPeak.position.distance(position) / objSpaceRadius;
//                        float scaledColorSpaceDistance = expectedPeak.offPeakSum.distance(offPeakSum) / colorSpaceRadius;
//
//                        distanceToExpectedPeak = Math.min(distanceToExpectedPeak,
//                            (float)Math.sqrt(scaledObjSpaceDistance * scaledObjSpaceDistance + scaledColorSpaceDistance * scaledColorSpaceDistance));
//                    }

                    double roughnessSum = 0.0;
                    double weightSum = 0.0;

//                    double minRoughness = 1.0;

                    for (CharacteristicBin bin : binNeighborhood)
                    {
                        List<PeakCandidate> peakBin = peakCandidates.get(bin);

                        if (peakBin != null)
                        {
                            for (int k = 0; k < peakBin.size(); k++)
                            {
                                PeakCandidate candidate = peakBin.get(k);

                                double scaledObjSpaceDistance = candidate.position.distance(position) / objSpaceRadius;
                                double scaledColorSpaceDistance = candidate.offPeakSum.distance(offPeakSum) / colorSpaceRadius;

                                int x = Math.max(0, Math.min(texWidth - 1, (int)Math.floor(candidate.texCoord.x * texWidth)));
                                int y = Math.max(0, Math.min(texHeight - 1, (int)Math.floor(candidate.texCoord.y * texHeight)));
                                float estimatedPeak = estimatedPeaks[y * texWidth + x].y;

                                double weight = Math.max(0, Math.min(1,
                                    (1.0 - Math.sqrt(0.5 * scaledObjSpaceDistance * scaledObjSpaceDistance
                                                + 0.5 * scaledColorSpaceDistance * scaledColorSpaceDistance)
                                            / distanceToExpectedPeak)
                                        / (1.0 - Math.sqrt(0.5))))
                                    * Math.max(0, Math.min(1, 2 * (candidate.peak.y - candidate.threshold.y) / (estimatedPeak - candidate.threshold.y)));

                                double nDotHSquared = candidate.nDotH * candidate.nDotH;

                                double numerator = Math.sqrt(Math.max(0.0, (1 - nDotHSquared) * Math.sqrt(candidate.peak.y * candidate.nDotV)));
                                double denominatorSq = Math.max(0.0, Math.sqrt(estimatedPeak) - nDotHSquared * Math.sqrt(candidate.peak.y * candidate.nDotV));
                                double denominator = Math.sqrt(denominatorSq);

//                                double roughnessEstimate = weight * (numerator * denominator) / Math.max(Math.sqrt(estimatedPeak) * 0.5, denominatorSq)
//                                    + (1 - weight * Math.min(1, denominatorSq / (Math.sqrt(estimatedPeak) * 0.5)));
//                                minRoughness = Math.min(minRoughness, roughnessEstimate);

                                roughnessSum += weight * numerator * denominator;
                                weightSum += weight * denominatorSq;
                            }
                        }
                    }

                    return
//                        minRoughness;
//                        roughnessSum / Math.max(0.001, weightSum);

                        Math.sqrt(0.25 * estimatedResidual[i].y / (estimatedPeaks[i].y - thresholdTexSpace[4 * i + 1]));
                }
                else
                {
                    return 0.0;
                }
            })
            .toArray();

        System.out.println();
        System.out.println("Filling holes...");

        Queue<IntVector2> roughnessHoles = new ArrayDeque<>(texHeight * texWidth / 4);
        Map<IntVector2, Double> roughnessChanges = new HashMap<>(texHeight * texWidth / 16);

        for (int y = 0; y < texHeight; y++)
        {
            for (int x = 0; x < texWidth; x++)
            {
                if (estimatedRoughness[y * texWidth + x] == 0.0)
                {
                    roughnessHoles.add(new IntVector2(x, y));
                }
            }
        }

        Collection<IntVector2> remainingRoughnessHoles = new ArrayDeque<>(roughnessHoles.size());

        do
        {
            while (!roughnessHoles.isEmpty())
            {
                IntVector2 hole = roughnessHoles.poll();

                double sum = 0.0;
                int count = 0;

                if (hole.y > 0)
                {
                    double sample = estimatedRoughness[(hole.y - 1) * texWidth + hole.x];

                    if (sample > 0.0)
                    {
                        sum += sample;
                        count++;
                    }
                }

                if (hole.y < texHeight - 1)
                {
                    double sample = estimatedRoughness[(hole.y + 1) * texWidth + hole.x];

                    if (sample > 0.0)
                    {
                        sum += sample;
                        count++;
                    }
                }

                if (hole.x > 0)
                {
                    double sample = estimatedRoughness[hole.y * texWidth + hole.x - 1];

                    if (sample > 0.0)
                    {
                        sum += sample;
                        count++;
                    }
                }

                if (hole.x < texWidth - 1)
                {
                    double sample = estimatedRoughness[hole.y * texWidth + hole.x + 1];

                    if (sample > 0.0)
                    {
                        sum += sample;
                        count++;
                    }
                }

                if (count == 0)
                {
                    remainingRoughnessHoles.add(hole);
                }
                else
                {
                    roughnessChanges.put(hole, sum / count);
                }
            }

            for (Entry<IntVector2, Double> change : roughnessChanges.entrySet())
            {
                estimatedRoughness[change.getKey().y * texWidth + change.getKey().x] = change.getValue();
            }

            roughnessHoles.addAll(remainingRoughnessHoles);
            remainingRoughnessHoles.clear();
            roughnessChanges.clear();
        }
        while(!roughnessHoles.isEmpty());

        System.out.println("Finished.");

        // debug

        BufferedImage residualSumImg = new BufferedImage(texWidth, texHeight, BufferedImage.TYPE_INT_ARGB);
        residualSumImg.setRGB(0, 0, texWidth, texHeight,
            Arrays.stream(estimatedResidual)
                .mapToInt(residual ->
                {
                    return residualSumImg.getColorModel().getDataElement(new float[] {
                            Math.min(1.0f, (float)Math.pow(residual.x, 1.0 / 2.2)),
                            Math.min(1.0f, (float)Math.pow(residual.y, 1.0 / 2.2)),
                            Math.min(1.0f, (float)Math.pow(residual.z, 1.0 / 2.2)),
                        1.0f },0);
                })
                .toArray(),
            0, texWidth);

        try
        {
            ImageIO.write(residualSumImg, "PNG", new File("debug", "residualTotal.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        BufferedImage outImg = new BufferedImage(texWidth, texHeight, BufferedImage.TYPE_INT_ARGB);
        outImg.setRGB(0, 0, texWidth, texHeight, Arrays.stream(estimatedPeaks).mapToInt(
            peak -> outImg.getColorModel().getDataElement(new float[] {
                    Math.min(1.0f, (float)Math.pow(peak.x, 1.0 / 2.2)),
                    Math.min(1.0f, (float)Math.pow(peak.y, 1.0 / 2.2)),
                    Math.min(1.0f, (float)Math.pow(peak.z, 1.0 / 2.2)),
                    1.0f },
                0)).toArray(), 0, texWidth);

        try
        {
            ImageIO.write(outImg, "PNG", new File("debug", "peak.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        BufferedImage roughnessImg = new BufferedImage(texWidth, texHeight, BufferedImage.TYPE_INT_ARGB);
        roughnessImg.setRGB(0, 0, texWidth, texHeight,
            Arrays.stream(estimatedRoughness)
                .mapToInt(roughness ->
                {
                    float sqrtRoughness = (float)Math.min(1.0f, Math.sqrt(roughness));
                    return roughnessImg.getColorModel().getDataElement(new float[] { sqrtRoughness, sqrtRoughness, sqrtRoughness, 1.0f },0);
                })
                .toArray(),
            0, texWidth);

        try
        {
            ImageIO.write(roughnessImg, "PNG", new File("debug", "roughness.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return IntStream.range(0, texWidth * texHeight)
            .mapToObj(i -> estimatedPeaks[i].asVector4((float)estimatedRoughness[i]))
            .toArray(Vector4[]::new);
    }
}
