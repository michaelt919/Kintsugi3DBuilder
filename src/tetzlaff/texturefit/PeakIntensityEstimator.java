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
import tetzlaff.gl.vecmath.IntVector3;
import tetzlaff.gl.vecmath.Vector3;
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

    PeakIntensityEstimator(Context<ContextType> context, ViewSet viewSet)
    {
        this.context = context;
        this.viewSet = viewSet;
    }

    void init(Consumer<Drawable<ContextType>> shaderSetup, Texture3D<ContextType> viewImages,
        Texture3D<ContextType> depthImages, Texture3D<ContextType> shadowImages)
    {
        this.shaderSetup = shaderSetup;
        this.viewImages = viewImages;
        this.depthImages = depthImages;
        this.shadowImages = shadowImages;
    }

    void compileShaders() throws IOException
    {
        imgSpaceProgram = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "imgspace.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders","texturefit", "specularpeakinfo_imgspace.frag").toFile())
            .createProgram();

        texSpaceProgram = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, Paths.get("shaders", "common", "texspace_noscale.vert").toFile())
            .addShader(ShaderType.FRAGMENT, Paths.get("shaders","texturefit", "specularpeakinfo_imgspace.frag").toFile())
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

        PeakCandidate(Vector3 peak, Vector3 offPeakSum, Vector3 position)
        {
            this.peak = peak;
            this.offPeakSum = offPeakSum;
            this.position = position;
        }

        CharacteristicBin characteristicBin(float objSpaceRadius, float colorSpaceRadius)
        {
            return CharacteristicBin.fromContinuous(offPeakSum, position, objSpaceRadius, colorSpaceRadius);
        }
    }

    Vector3[] estimate(int texWidth, int texHeight, float objSpaceRadius, float colorSpaceRadius)
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

        Map<CharacteristicBin, List<PeakCandidate>> peaks = new HashMap<>(100000);

        FloatBuffer peakBuffer =
            BufferUtils.createFloatBuffer(4 * viewImages.getWidth() * viewImages.getHeight());
        FloatBuffer offPeakBuffer =
            BufferUtils.createFloatBuffer(4 * viewImages.getWidth() * viewImages.getHeight());
        FloatBuffer positionBuffer =
            BufferUtils.createFloatBuffer(4 * viewImages.getWidth() * viewImages.getHeight());

        System.out.println("Generating peak reflectance samples...");

        float maxOffPeak = 0.0f;

        try(FramebufferObject<ContextType> fbo = context.buildFramebufferObject(
            viewImages.getWidth(), viewImages.getHeight())
            .addColorAttachments(ColorFormat.RGBA32F, 3)
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
                imgSpaceDrawable.draw(PrimitiveMode.TRIANGLES, fbo);

                fbo.readFloatingPointColorBufferRGBA(0, peakBuffer);
                fbo.readFloatingPointColorBufferRGBA(1, offPeakBuffer);
                fbo.readFloatingPointColorBufferRGBA(2, positionBuffer);

                // debug
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

                for (int k = 0; k < viewImages.getWidth() * viewImages.getHeight(); k++)
                {
                    if (peakBuffer.get(4 * k + 3) > 0.0)
                    {
                        PeakCandidate peak = new PeakCandidate(
                            new Vector3(peakBuffer.get(4 * k), peakBuffer.get(4 * k + 1), peakBuffer.get(4 * k + 2)),
                            new Vector3(offPeakBuffer.get(4 * k), offPeakBuffer.get(4 * k + 1), offPeakBuffer.get(4 * k + 2)),
                            new Vector3(positionBuffer.get(4 * k), positionBuffer.get(4 * k + 1), positionBuffer.get(4 * k + 2)));

                        CharacteristicBin binID = peak.characteristicBin(objSpaceRadius, colorSpaceRadius);
                        List<PeakCandidate> bin = peaks.get(binID);
                        if (bin == null)
                        {
                            bin = new ArrayList<>(1);
                            peaks.put(binID, new ArrayList<>(1));
                        }
                        bin.add(peak);

                        maxOffPeak = Math.max(maxOffPeak, Math.max(offPeakBuffer.get(4 * k), Math.max(offPeakBuffer.get(4 * k + 1), offPeakBuffer.get(4 * k + 2))));
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

        float[] offPeakTexSpace;
        float[] positionsTexSpace;

        try(FramebufferObject<ContextType> fbo = context.buildFramebufferObject(texWidth, texHeight)
            .addColorAttachments(ColorFormat.RGBA32F, 3)
            .createFramebufferObject())
        {
            fbo.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
            fbo.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
            fbo.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
            texSpaceDrawable.draw(PrimitiveMode.TRIANGLES, fbo);

            offPeakTexSpace = fbo.readFloatingPointColorBufferRGBA(1);
            positionsTexSpace = fbo.readFloatingPointColorBufferRGBA(2);
        }

        System.out.println("Sorting samples...");

        Map<CharacteristicBin, List<PeakCandidate>> redSorted = new HashMap<>(peaks.size());
        Map<CharacteristicBin, List<PeakCandidate>> greenSorted = new HashMap<>(peaks.size());
        Map<CharacteristicBin, List<PeakCandidate>> blueSorted = new HashMap<>(peaks.size());

        for (Entry<CharacteristicBin, List<PeakCandidate>> bin : peaks.entrySet())
        {
            redSorted.put(bin.getKey(), bin.getValue().stream()
                .sorted(Comparator.<PeakCandidate, Float>comparing(candidate -> candidate.peak.x).reversed())
                .collect(Collectors.toList()));

            greenSorted.put(bin.getKey(), bin.getValue().stream()
                .sorted(Comparator.<PeakCandidate, Float>comparing(candidate -> candidate.peak.y).reversed())
                .collect(Collectors.toList()));

            blueSorted.put(bin.getKey(), bin.getValue().stream()
                .sorted(Comparator.<PeakCandidate, Float>comparing(candidate -> candidate.peak.z).reversed())
                .collect(Collectors.toList()));
        }

        System.out.println("Estimating peak reflectance...");

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

                    for (CharacteristicBin bin : binNeighborhood)
                    {
                        List<PeakCandidate> redBin = redSorted.get(bin);

                        if (redBin != null)
                        {
                            for (int k = 0; k < redBin.size() && lastPeak >= red; k++)
                            {
                                PeakCandidate candidate = redBin.get(k);

                                float weight = Math.max(0, Math.min(1, 2 * (objSpaceRadius - candidate.position.distance(position)) / objSpaceRadius))
                                    * Math.max(0, Math.min(1, 2 * (colorSpaceRadius - candidate.offPeakSum.distance(offPeakSum)) / colorSpaceRadius));

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

                                float weight = Math.max(0, Math.min(1, 2 * (objSpaceRadius - candidate.position.distance(position)) / objSpaceRadius))
                                    * Math.max(0, Math.min(1, 2 * (colorSpaceRadius - candidate.offPeakSum.distance(offPeakSum)) / colorSpaceRadius));

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

                                float weight = Math.max(0, Math.min(1, 2 * (objSpaceRadius - candidate.position.distance(position)) / objSpaceRadius))
                                    * Math.max(0, Math.min(1, 2 * (colorSpaceRadius - candidate.offPeakSum.distance(offPeakSum)) / colorSpaceRadius));

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

        System.out.println("Finished.");

        // debug
        BufferedImage outImg = new BufferedImage(texWidth, texHeight, BufferedImage.TYPE_INT_ARGB);
        outImg.setRGB(0, 0, texWidth, texHeight, Arrays.stream(estimatedPeaks).mapToInt(
            peak -> outImg.getColorModel().getDataElement(new float[] {
                    (float)Math.pow(peak.x, 1.0 / 2.2), (float)Math.pow(peak.y, 1.0 / 2.2), (float)Math.pow(peak.z, 1.0 / 2.2), 1.0f },
                0)).toArray(), 0, texWidth);
        try
        {
            ImageIO.write(outImg, "PNG", new File("debug", "result.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return estimatedPeaks;
    }
}