/*
 *  Copyright (c) Michael Tetzlaff 2024
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.test;

import java.io.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.fit.ReconstructionShaders;
import kintsugi3d.builder.fit.SpecularFitProgramFactory;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.builder.io.ViewSetReaderFromVSET;
import kintsugi3d.builder.metrics.ColorAppearanceRMSE;
import kintsugi3d.builder.rendering.ImageReconstruction;
import kintsugi3d.builder.rendering.ReconstructionView;
import kintsugi3d.builder.resources.ibr.IBRResourcesAnalytic;
import kintsugi3d.builder.state.impl.SimpleSettingsModel;
import kintsugi3d.gl.core.ColorFormat;
import kintsugi3d.gl.core.Drawable;
import kintsugi3d.gl.core.FramebufferObject;
import kintsugi3d.gl.core.ProgramObject;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.opengl.OpenGLContext;
import kintsugi3d.gl.opengl.OpenGLContextFactory;
import kintsugi3d.gl.vecmath.DoubleVector3;
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

    @BeforeEach
    void setUp() throws Exception
    {
        potatoViewSet = ViewSetReaderFromVSET.getInstance().readFromStream(getClass().getClassLoader().getResourceAsStream("test/Structured34View.vset"), null);
        potatoViewSetTonemapped = potatoViewSet.copy();

        // Using tonemapping from the Guan Yu dataset
        potatoViewSetTonemapped.setTonemapping(2.2f, new double [] { 0.031, 0.090, 0.198, 0.362, 0.591, 0.900 },
            new byte [] { 50, 105, (byte)140, (byte)167, (byte)176, (byte)185 });

        context = OpenGLContextFactory.getInstance().buildWindow("Kintsugi 3D Builder Tests", 1, 1).create().getContext();
        context.getState().enableDepthTest();

        Potato potato = new Potato(50, 0.75f, 0.1f, 250000);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        potato.writeToStream(new PrintStream(out));

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        potatoGeometry = VertexGeometry.createFromOBJStream(in);
    }

    @AfterEach
    void tearDown()
    {
        context.close();
    }

    @Test
    @DisplayName("Normalized linear reconstruction error")
    void normalizedLinear() throws IOException
    {
        BiFunction<SpecularFitProgramFactory<OpenGLContext>, IBRResourcesAnalytic<OpenGLContext>, ProgramObject<OpenGLContext>> programCreator =
            (programFactory, resources) ->
            {
                try
                {
                    return programFactory.getShaderProgramBuilder(resources,
                            new File("shaders/common/imgspace.vert"),
                            new File("shaders/test/syntheticWithLinearNoise.frag"))
                        .createProgram();
                }
                catch (FileNotFoundException e)
                {
                    throw new RuntimeException(e);
                }
            };

        BiConsumer<ColorAppearanceRMSE, Float> validation =
            (rmse, noiseScale) -> assertEquals(noiseScale / (float) Math.sqrt(12.0f), rmse.getNormalizedLinear(), 0.001);

        multiTest(potatoViewSet, programCreator, ImageReconstructionTest::createGroundTruthProgramGamma, validation);
        multiTest(potatoViewSetTonemapped, programCreator, ImageReconstructionTest::createGroundTruthProgramSRGB, validation);
    }

    @Test
    @DisplayName("Normalized sRGB reconstruction error")
    void normalizedSRGB() throws IOException
    {
        BiFunction<SpecularFitProgramFactory<OpenGLContext>, IBRResourcesAnalytic<OpenGLContext>, ProgramObject<OpenGLContext>> programCreator =
            (programFactory, resources) ->
            {
                try
                {
                    return programFactory.getShaderProgramBuilder(resources,
                            new File("shaders/common/imgspace.vert"),
                            new File("shaders/test/syntheticWithSRGBNoise.frag"))
                        .createProgram();
                }
                catch (FileNotFoundException e)
                {
                    throw new RuntimeException(e);
                }
            };

        BiConsumer<ColorAppearanceRMSE, Float> validation =
            (rmse, noiseScale) -> assertEquals(noiseScale / (float) Math.sqrt(12.0f), rmse.getNormalizedSRGB(), 0.001);

        multiTest(potatoViewSet, programCreator, ImageReconstructionTest::createGroundTruthProgramGamma, validation);
        multiTest(potatoViewSetTonemapped, programCreator, ImageReconstructionTest::createGroundTruthProgramSRGB, validation);
    }

    @Test
    @DisplayName("Tonemapped and lit reconstruction error")
    void tonemappedLit() throws IOException
    {
        BiFunction<SpecularFitProgramFactory<OpenGLContext>, IBRResourcesAnalytic<OpenGLContext>, ProgramObject<OpenGLContext>> programCreator =
            (programFactory, resources) ->
            {
                try
                {
                    return programFactory.getShaderProgramBuilder(resources,
                            new File("shaders/common/imgspace.vert"),
                            new File("shaders/test/syntheticWithTonemappedLitNoise.frag"))
                        .createProgram();
                }
                catch (FileNotFoundException e)
                {
                    throw new RuntimeException(e);
                }
            };

        BiConsumer<ColorAppearanceRMSE, Float> validation =
            (rmse, noiseScale) -> assertEquals(noiseScale / (float) Math.sqrt(12.0f), rmse.getEncodedGroundTruth(), 0.001);

        multiTest(potatoViewSet, programCreator, ImageReconstructionTest::createGroundTruthProgramGamma, validation);
        multiTest(potatoViewSetTonemapped, programCreator, ImageReconstructionTest::createGroundTruthProgramSRGB, validation);
    }

    static ProgramObject<OpenGLContext> createGroundTruthProgramGamma(SpecularFitProgramFactory<OpenGLContext> programFactory, IBRResourcesAnalytic<OpenGLContext> resources)
    {
        try
        {
            return programFactory.getShaderProgramBuilder(resources,
                    new File("shaders/common/imgspace.vert"),
                    new File("shaders/test/syntheticTonemapped.frag"))
                .define("SRGB_TONEMAPPING_ENABLED", 0)
                .createProgram();
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    static ProgramObject<OpenGLContext> createGroundTruthProgramSRGB(SpecularFitProgramFactory<OpenGLContext> programFactory, IBRResourcesAnalytic<OpenGLContext> resources)
    {
        try
        {
            return programFactory.getShaderProgramBuilder(resources,
                    new File("shaders/common/imgspace.vert"),
                    new File("shaders/test/syntheticTonemapped.frag"))
                .define("SRGB_TONEMAPPING_ENABLED", 1)
                .createProgram();
        }
        catch (FileNotFoundException e)
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
                    potatoViewSet,
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
}