/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.core.UserCancellationException;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.fit.ReconstructionShaders;
import kintsugi3d.builder.fit.SpecularFitProcess;
import kintsugi3d.builder.fit.SpecularFitProgramFactory;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.builder.fit.settings.SpecularFitRequestParams;
import kintsugi3d.builder.io.ViewSetReaderFromVSET;
import kintsugi3d.builder.metrics.ColorAppearanceRMSE;
import kintsugi3d.builder.rendering.ImageReconstruction;
import kintsugi3d.builder.rendering.ReconstructionView;
import kintsugi3d.builder.resources.ibr.IBRResourcesAnalytic;
import kintsugi3d.builder.resources.ibr.IBRResourcesCacheable;
import kintsugi3d.builder.state.DefaultSettings;
import kintsugi3d.builder.state.SettingsModel;
import kintsugi3d.builder.state.impl.SimpleSettingsModel;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.opengl.OpenGLContext;
import kintsugi3d.gl.opengl.OpenGLContextFactory;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.ColorArrayImage;
import kintsugi3d.util.Potato;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageReconstructionTest
{
    private static final File TEST_OUTPUT_DIR = new File("test-output");

    private ViewSet potatoViewSet;
    private ViewSet potatoViewSetTonemapped;
    private OpenGLContext context;
    private VertexGeometry potatoGeometry;
    private BiConsumer<ColorAppearanceRMSE, Float> validationLinear;
    private BiConsumer<ColorAppearanceRMSE, Float> validationSRGB;
    private BiConsumer<ColorAppearanceRMSE, Float> validationEncoded;
    private Consumer<Program<OpenGLContext>> setupColor;
    private Consumer<Program<OpenGLContext>> setupMetallic;

    private ProgressMonitor progressMonitor;

    private static class ProgressMonitorImpl implements ProgressMonitor
    {
        private double maxProgress;
        private int stageCount;

        @Override
        public void allowUserCancellation()
        {
        }

        @Override
        public void cancelComplete(UserCancellationException e)
        {
        }

        @Override
        public void start()
        {
        }

        @Override
        public void setStageCount(int count)
        {
            this.stageCount = count;
        }

        @Override
        public void setStage(int stage, String message)
        {
            System.out.println(MessageFormat.format("[{0}/{1}] {2}", stage, stageCount, message));
        }

        @Override
        public void setMaxProgress(double maxProgress)
        {
            this.maxProgress = maxProgress;
        }

        @Override
        public void setProgress(double progress, String message)
        {
            System.out.println(MessageFormat.format("[{0}%] {1}", progress / maxProgress, message));
        }

        @Override
        public void complete()
        {
            System.out.println("COMPLETE!");
        }

        @Override
        public void fail(Throwable e)
        {
            e.printStackTrace();
        }
    }

    @BeforeEach
    void setUp() throws Exception
    {
        progressMonitor = new ProgressMonitorImpl();

        potatoViewSet = ViewSetReaderFromVSET.getInstance().readFromStream(getClass().getClassLoader().getResourceAsStream("test/Structured34View.vset"), null);
        potatoViewSetTonemapped = potatoViewSet.copy();

        // Using tonemapping from the Guan Yu dataset
        potatoViewSetTonemapped.setTonemapping(potatoViewSet.getGamma(), new double [] { 0.031, 0.090, 0.198, 0.362, 0.591, 0.900 },
            new byte [] { 50, 105, (byte)140, (byte)167, (byte)176, (byte)185 });

        context = OpenGLContextFactory.getInstance().buildWindow("Kintsugi 3D Builder Tests", 1, 1).create().getContext();
        context.getState().enableDepthTest();

        Potato potato = new Potato(50, 0.75f, 0.1f, 250000);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        potato.writeToStream(new PrintStream(out, false, StandardCharsets.UTF_8));

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        potatoGeometry = VertexGeometry.createFromOBJStream(in);

        validationLinear = (rmse, noiseScale) -> assertEquals(noiseScale / (float) Math.sqrt(12.0f), rmse.getNormalizedLinear(), 0.001);
        validationSRGB = (rmse, noiseScale) -> assertEquals(noiseScale / (float) Math.sqrt(12.0f), rmse.getNormalizedSRGB(), 0.001);

        // TODO figure out why we need a higher validation delta for this one?
        validationEncoded = (rmse, noiseScale) -> assertEquals(noiseScale / (float) Math.sqrt(12.0f), rmse.getEncodedGroundTruth(), 0.005);

        setupColor = program -> program.setUniform("diffuseColor", new Vector3(1.0f, 0.8f, 0.2f));
        setupMetallic = program ->
        {
            program.setUniform("diffuseColor", new Vector3(0.0f, 0.0f, 0.0f));
            program.setUniform("specularColor", new Vector3(1.0f, 0.8f, 0.2f));
        };
    }

    @AfterEach
    void tearDown()
    {
        context.close();
    }

    private float noiseScaleToRMSE(float noiseScale)
    {
        return noiseScale / (float) Math.sqrt(12.0f);
    }

    private BiFunction<SpecularFitProgramFactory<OpenGLContext>, IBRResourcesAnalytic<OpenGLContext>, ProgramObject<OpenGLContext>>
        getProgramCreator(String testShaderName, Consumer<Program<OpenGLContext>> setupShader)
    {
        return
            (programFactory, resources) ->
            {
                try
                {
                    ProgramObject<OpenGLContext> program = programFactory.getShaderProgramBuilder(resources,
                            new File("shaders/common/imgspace.vert"),
                            new File("shaders/test/" + testShaderName + ".frag"))
                        .createProgram();
                    setupShader.accept(program);
                    return program;
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            };
    }

    @Test
    @DisplayName("Normalized linear reconstruction error, grayscale synthetic data")
    void normalizedLinear_grayscale() throws IOException
    {
        // TODO switch from gamma to sRGB decoding for the cases without ColorChecker values
        multiTest(potatoViewSet, getProgramCreator("syntheticWithLinearNoise", p->{}),
            (factory, resources) -> createGroundTruthProgramGamma(factory, resources, p->{}), validationLinear);
    }

    @Test
    @DisplayName("Normalized linear reconstruction error, grayscale tonemapped synthetic data")
    void normalizedLinear_grayscaleTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithLinearNoise", p->{}),
            (factory, resources) -> createGroundTruthProgramSRGB(factory, resources, p->{}), validationLinear);
    }

    @Test
    @DisplayName("Normalized linear reconstruction error, color synthetic data")
    void normalizedLinear_color() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithLinearColorNoise", setupColor),
            (factory, resources) -> createGroundTruthProgramGamma(factory, resources, setupColor), validationLinear);
    }

    @Test
    @DisplayName("Normalized linear reconstruction error, color tonemapped synthetic data")
    void normalizedLinear_colorTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithLinearColorNoise", setupColor),
            (factory, resources) -> createGroundTruthProgramSRGB(factory, resources, setupColor), validationLinear);
    }

    @Test
    @DisplayName("Normalized linear reconstruction error, metallic synthetic data")
    void normalizedLinear_metallic() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithLinearColorNoise", setupMetallic),
            (factory, resources) -> createGroundTruthProgramGamma(factory, resources, setupMetallic), validationLinear);
    }

    @Test
    @DisplayName("Normalized linear reconstruction error, metallic tonemapped synthetic data")
    void normalizedLinear_metallicTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithLinearColorNoise", setupMetallic),
            (factory, resources) -> createGroundTruthProgramSRGB(factory, resources, setupMetallic), validationLinear);
    }

    @Test
    @DisplayName("Normalized sRGB reconstruction error, grayscale synthetic data")
    void normalizedSRGB_grayscale() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithSRGBNoise", p->{}),
            (factory, resources) -> createGroundTruthProgramGamma(factory, resources, p->{}), validationSRGB);
    }

    @Test
    @DisplayName("Normalized sRGB reconstruction error, grayscale tonemapped synthetic data")
    void normalizedSRGB_grayscaleTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithSRGBNoise", p->{}),
            (factory, resources) -> createGroundTruthProgramSRGB(factory, resources, p->{}), validationSRGB);
    }

    @Test
    @DisplayName("Normalized sRGB reconstruction error, color synthetic data")
    void normalizedSRGB_color() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithSRGBColorNoise", setupColor),
            (factory, resources) -> createGroundTruthProgramGamma(factory, resources, setupColor), validationSRGB);
    }

    @Test
    @DisplayName("Normalized sRGB reconstruction error, color tonemapped synthetic data")
    void normalizedSRGB_colorTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithSRGBColorNoise", setupColor),
            (factory, resources) -> createGroundTruthProgramSRGB(factory, resources, setupColor), validationSRGB);
    }

    @Test
    @DisplayName("Normalized sRGB reconstruction error, metallic synthetic data")
    void normalizedSRGB_metallic() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithSRGBColorNoise", setupMetallic),
            (factory, resources) -> createGroundTruthProgramGamma(factory, resources, setupMetallic), validationSRGB);
    }

    @Test
    @DisplayName("Normalized sRGB reconstruction error, metallic tonemapped synthetic data")
    void normalizedSRGB_metallicTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithSRGBColorNoise", setupMetallic),
            (factory, resources) -> createGroundTruthProgramSRGB(factory, resources, setupMetallic), validationSRGB);
    }

    @Test
    @DisplayName("Tonemapped and lit reconstruction error, grayscale synthetic data")
    void tonemappedLit_grayscale() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithTonemappedLitNoise", p->{}),
            (factory, resources) -> createGroundTruthProgramGamma(factory, resources, p->{}), validationEncoded);
    }

    @Test
    @DisplayName("Tonemapped and lit reconstruction error, grayscale tonemapped synthetic data")
    void tonemappedLit_grayscaleTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithTonemappedLitNoise", p->{}),
            (factory, resources) -> createGroundTruthProgramSRGB(factory, resources, p->{}), validationEncoded);
    }

    @Test
    @DisplayName("Tonemapped and lit reconstruction error, color synthetic data")
    void tonemappedLit_color() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithTonemappedLitColorNoise", setupColor),
            (factory, resources) -> createGroundTruthProgramGamma(factory, resources, setupColor), validationEncoded);
    }

    @Test
    @DisplayName("Tonemapped and lit reconstruction error, color tonemapped synthetic data")
    void tonemappedLit_colorTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithTonemappedLitColorNoise", setupColor),
            (factory, resources) -> createGroundTruthProgramSRGB(factory, resources, setupColor), validationEncoded);
    }

    @Test
    @DisplayName("Tonemapped and lit reconstruction error, metallic synthetic data")
    void tonemappedLit_metallic() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithTonemappedLitColorNoise", setupMetallic),
            (factory, resources) -> createGroundTruthProgramGamma(factory, resources, setupMetallic), validationEncoded);
    }

    @Test
    @DisplayName("Tonemapped and lit reconstruction error, metallic tonemapped synthetic data")
    void tonemappedLit_metallicTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithTonemappedLitColorNoise", setupMetallic),
            (factory, resources) -> createGroundTruthProgramSRGB(factory, resources, setupMetallic), validationEncoded);
    }

    static ProgramObject<OpenGLContext> createGroundTruthProgramGamma(
        SpecularFitProgramFactory<OpenGLContext> programFactory, IBRResourcesAnalytic<OpenGLContext> resources, Consumer<Program<OpenGLContext>> setupShader)
    {
        try
        {
            ProgramObject<OpenGLContext> program = programFactory.getShaderProgramBuilder(resources,
                    new File("shaders/common/imgspace.vert"),
                    new File("shaders/test/syntheticTonemapped.frag"))
                .define("SRGB_DECODING_ENABLED", 0)
                .define("SRGB_ENCODING_ENABLED", 0)
                .createProgram();
            setupShader.accept(program);
            return program;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    static ProgramObject<OpenGLContext> createGroundTruthProgramSRGB(
        SpecularFitProgramFactory<OpenGLContext> programFactory, IBRResourcesAnalytic<OpenGLContext> resources, Consumer<Program<OpenGLContext>> setupShader)
    {
        try
        {
            ProgramObject<OpenGLContext> program = programFactory.getShaderProgramBuilder(resources,
                    new File("shaders/common/imgspace.vert"),
                    new File("shaders/test/syntheticTonemapped.frag"))
                .define("SRGB_DECODING_ENABLED", 1)
                .define("SRGB_ENCODING_ENABLED", 1)
                .createProgram();
            setupShader.accept(program);
            return program;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    void multiTest(
        ViewSet viewSet,
        BiFunction<SpecularFitProgramFactory<OpenGLContext>, IBRResourcesAnalytic<OpenGLContext>, ProgramObject<OpenGLContext>> testProgramCreator,
        BiFunction<SpecularFitProgramFactory<OpenGLContext>, IBRResourcesAnalytic<OpenGLContext>, ProgramObject<OpenGLContext>> groundTruthProgramCreator,
        BiConsumer<ColorAppearanceRMSE, Float> validationByNoiseScale)  throws IOException
    {
        float[] noiseScaleTests = { 0.0f, 0.1f, 0.25f, 0.5f, 1.0f };

        for (float noiseScale : noiseScaleTests)
        {
            testSynthetic(
                viewSet,
                (programFactory, resources) ->
                {
                    ProgramObject<OpenGLContext> program = testProgramCreator.apply(programFactory, resources);
                    program.setUniform("noiseScale", noiseScale);
                    return program;
                },
                groundTruthProgramCreator,
                rmse -> validationByNoiseScale.accept(rmse, noiseScale));
        }
    }

    private void testSynthetic(
        ViewSet viewSet,
        BiFunction<SpecularFitProgramFactory<OpenGLContext>, IBRResourcesAnalytic<OpenGLContext>, ProgramObject<OpenGLContext>> testProgramCreator,
        BiFunction<SpecularFitProgramFactory<OpenGLContext>, IBRResourcesAnalytic<OpenGLContext>, ProgramObject<OpenGLContext>> groundTruthProgramCreator,
        Consumer<ColorAppearanceRMSE> validation) throws IOException
    {
        SimpleSettingsModel ibrSettings = new SimpleSettingsModel();
        ibrSettings.createBooleanSetting("shadowsEnabled", false);
        ibrSettings.createBooleanSetting("occlusionEnabled", false);

        SpecularBasisSettings specularBasisSettings = new SpecularBasisSettings();
        specularBasisSettings.setBasisCount(1);

        SpecularFitProgramFactory<OpenGLContext> programFactory = new SpecularFitProgramFactory<>(ibrSettings, specularBasisSettings);

        try (IBRResourcesAnalytic<OpenGLContext> resources = new IBRResourcesAnalytic<>(context, viewSet, potatoGeometry);
            ProgramObject<OpenGLContext> groundTruthProgram = groundTruthProgramCreator.apply(programFactory, resources);
            Drawable<OpenGLContext> groundTruthDrawable = resources.createDrawable(groundTruthProgram))
        {
            groundTruthProgram.setUniform("noiseScale", 0.0f);
            resources.setupShaderProgram(groundTruthProgram);

            try (FramebufferObject<OpenGLContext> groundTruthFBO = context.buildFramebufferObject(256, 256)
                .addColorAttachment(ColorFormat.RGBA32F)
                .addDepthAttachment()
                .createFramebufferObject();
                ImageReconstruction<OpenGLContext> reconstruction = new ImageReconstruction<>(
                    viewSet,
                    builder -> builder
                        .addColorAttachment(ColorFormat.RGBA32F)
                        .addDepthAttachment(),
                    builder -> builder
                        .addColorAttachment(ColorFormat.RGBA32F)
                        .addDepthAttachment(),
                    ReconstructionShaders.getIncidentRadianceProgramBuilder(resources, programFactory),
                    resources,
                    viewIndex ->
                    {
                        // TODO: use proper sRGB when possible, not gamma correction
                        float gamma = viewSet.getGamma();
                        groundTruthProgram.setUniform("gamma", gamma);
                        groundTruthProgram.setUniform("renderGamma", gamma);

                        groundTruthProgram.setUniform("model_view", viewSet.getCameraPose(viewIndex));
                        groundTruthProgram.setUniform("projection",
                            viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex)).getProjectionMatrix(
                                viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
                        groundTruthProgram.setUniform("reconstructionCameraPos",
                            viewSet.getCameraPoseInverse(viewIndex).getColumn(3).getXYZ());
                        groundTruthProgram.setUniform("reconstructionLightPos",
                            viewSet.getCameraPoseInverse(viewIndex).times(viewSet.getLightPosition(viewSet.getLightIndex(viewIndex)).asPosition()).getXYZ());
                        groundTruthProgram.setUniform("reconstructionLightIntensity",
                            viewSet.getLightIntensity(viewSet.getLightIndex(viewIndex)));

                        groundTruthFBO.clearColorBuffer(0, 0, 0, 0, 0);
                        groundTruthFBO.clearDepthBuffer();
                        groundTruthDrawable.draw(groundTruthFBO);

                        float[] groundTruth = groundTruthFBO.getTextureReaderForColorAttachment(0).readFloatingPointRGBA();
                        return new ColorArrayImage(groundTruth, 256, 256);
                    });
                ProgramObject<OpenGLContext> syntheticWithNoise = testProgramCreator.apply(programFactory, resources);
                Drawable<OpenGLContext> drawable = resources.createDrawable(syntheticWithNoise))
            {
                resources.setupShaderProgram(syntheticWithNoise);

                TEST_OUTPUT_DIR.mkdirs();

                for (ReconstructionView<OpenGLContext> view : reconstruction)
                {
                    groundTruthFBO.getTextureReaderForColorAttachment(0).saveToFile("PNG", new File(TEST_OUTPUT_DIR, "reference.png"));

                    // Pass light intensity for noise generation methods that depend on it.
                    syntheticWithNoise.setUniform("reconstructionLightIntensity", viewSet.getLightIntensity(viewSet.getLightIndex(view.getIndex())));

                    ColorAppearanceRMSE rmse = view.reconstruct(drawable);
                    view.getReconstructionFramebuffer().getTextureReaderForColorAttachment(0).saveToFile("PNG", new File(TEST_OUTPUT_DIR, "test.png"));
                    validation.accept(rmse);
                }
            }
        }
    }

    private void testSyntheticFit(ViewSet viewSet)
    {
        try (IBRResourcesCacheable<OpenGLContext> resources = new IBRResourcesAnalytic<>(context, viewSet, potatoGeometry))
        {
            SettingsModel settings = new SimpleSettingsModel();
            DefaultSettings.apply(settings);
            SpecularFitRequestParams params = new SpecularFitRequestParams(new TextureResolution(512, 512), settings);
            params.setOutputDirectory(TEST_OUTPUT_DIR);

            // Perform the specular fit
            new SpecularFitProcess(params).optimizeFit(resources, progressMonitor);
        }
        catch (UserCancellationException | IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}