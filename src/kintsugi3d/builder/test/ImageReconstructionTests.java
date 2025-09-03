/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.test;

import kintsugi3d.builder.core.*;
import kintsugi3d.builder.core.metrics.ColorAppearanceRMSE;
import kintsugi3d.builder.fit.ReconstructionShaders;
import kintsugi3d.builder.fit.SpecularFitOptimizable;
import kintsugi3d.builder.fit.SpecularFitProcess;
import kintsugi3d.builder.fit.SpecularFitProgramFactory;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.builder.fit.settings.SpecularFitRequestParams;
import kintsugi3d.builder.io.ViewSetDirectories;
import kintsugi3d.builder.io.ViewSetLoadOptions;
import kintsugi3d.builder.io.ViewSetReaderFromVSET;
import kintsugi3d.builder.javafx.internal.ObservableLoadOptionsModel;
import kintsugi3d.builder.rendering.ImageReconstruction;
import kintsugi3d.builder.rendering.ReconstructionView;
import kintsugi3d.builder.resources.project.GraphicsResources;
import kintsugi3d.builder.resources.project.GraphicsResourcesAnalytic;
import kintsugi3d.builder.resources.project.GraphicsResourcesCacheable;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace;
import kintsugi3d.builder.resources.project.specular.SpecularMaterialResources;
import kintsugi3d.builder.state.DefaultSettings;
import kintsugi3d.builder.state.GeneralSettingsModel;
import kintsugi3d.builder.state.SimpleGeneralSettingsModel;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.opengl.OpenGLContext;
import kintsugi3d.gl.opengl.OpenGLContextFactory;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;
import kintsugi3d.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageReconstructionTests
{
    private static final File TEST_OUTPUT_DIR = new File("test-output");
    private static final boolean SAVE_TEST_IMAGES = true;
    private static final boolean SAVE_GROUND_TRUTH_SYNTHETIC_IMAGES = false;

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
        public void setProcessName(String processName)
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
            System.out.println(MessageFormat.format("[{0}%] {1}", progress / maxProgress * 100, message));
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

        @Override
        public boolean isConflictingProcess()
        {
            return false;
        }
    }

    @BeforeEach
    void setUp() throws Exception
    {
        progressMonitor = new ProgressMonitorImpl();
        ViewSetDirectories directories = new ViewSetDirectories();
        directories.fullResImagesNeedUndistort = true;
        potatoViewSet = ViewSetReaderFromVSET.getInstance().readFromStream(getClass().getClassLoader().getResourceAsStream("test/Structured34View.vset"), directories).finish();
        potatoViewSet.getProjectSettings().set("occlusionEnabled", false);

        potatoViewSetTonemapped = potatoViewSet.copy();

        // Using tonemapping from the Guan Yu dataset
        potatoViewSetTonemapped.setTonemapping(new double [] { 0.031, 0.090, 0.198, 0.362, 0.591, 0.900 },
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

        if (SAVE_GROUND_TRUTH_SYNTHETIC_IMAGES)
        {
            saveGroundTruthSyntheticImages(potatoViewSet, (factory, resources) -> createGroundTruthProgram(factory, resources, p->{}),
                "groundTruthSynthetic_grayscale");
            saveGroundTruthSyntheticImages(potatoViewSet, (factory, resources) -> createGroundTruthProgram(factory, resources, setupColor),
                "groundTruthSynthetic_color");
            saveGroundTruthSyntheticImages(potatoViewSet, (factory, resources) -> createGroundTruthProgram(factory, resources, setupMetallic),
                "groundTruthSynthetic_metallic");
            saveGroundTruthSyntheticImages(potatoViewSetTonemapped, (factory, resources) -> createGroundTruthProgram(factory, resources, p->{}),
                "groundTruthSynthetic_grayscaleTonemapped");
            saveGroundTruthSyntheticImages(potatoViewSetTonemapped, (factory, resources) -> createGroundTruthProgram(factory, resources, setupColor),
                "groundTruthSynthetic_colorTonemapped");
            saveGroundTruthSyntheticImages(potatoViewSetTonemapped, (factory, resources) -> createGroundTruthProgram(factory, resources, setupMetallic),
                "groundTruthSynthetic_metallicTonemapped");
        }
    }

    void saveGroundTruthSyntheticImages(
            ViewSet viewSet,
            BiFunction<SpecularFitProgramFactory<OpenGLContext>, GraphicsResourcesAnalytic<OpenGLContext>, ProgramObject<OpenGLContext>> groundTruthProgramCreator,
            String groundTruthName)
        throws IOException
    {
        SimpleGeneralSettingsModel globalSettings = new SimpleGeneralSettingsModel();
        DefaultSettings.applyGlobalDefaults(globalSettings);

        SpecularBasisSettings specularBasisSettings = new SpecularBasisSettings();
        specularBasisSettings.setBasisCount(1);

        SpecularFitProgramFactory<OpenGLContext> programFactory = new SpecularFitProgramFactory<>(specularBasisSettings);

        try (GraphicsResourcesAnalytic<OpenGLContext> resources = new GraphicsResourcesAnalytic<>(context, viewSet, potatoGeometry);
            ProgramObject<OpenGLContext> groundTruthProgram = groundTruthProgramCreator.apply(programFactory, resources);
            Drawable<OpenGLContext> groundTruthDrawable = resources.createDrawable(groundTruthProgram))
        {
            groundTruthProgram.setUniform("noiseScale", 0.0f);
            resources.setupShaderProgram(groundTruthProgram);

            try (FramebufferObject<OpenGLContext> groundTruthFBO = context.buildFramebufferObject(256, 256)
                .addColorAttachment(ColorFormat.RGBA32F)
                .addDepthAttachment()
                .createFramebufferObject())
            {
                File outputDirectory = new File(TEST_OUTPUT_DIR, groundTruthName);
                outputDirectory.mkdirs();

                for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
                {
                    renderGroundTruth(viewSet, i, groundTruthDrawable, groundTruthFBO);
                    groundTruthFBO.getTextureReaderForColorAttachment(0).saveToFile("PNG",
                        new File(outputDirectory, MessageFormat.format("{0,number,0000}.png", i)));
                }
            }
        }
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

    private BiFunction<SpecularFitProgramFactory<OpenGLContext>, GraphicsResourcesAnalytic<OpenGLContext>, ProgramObject<OpenGLContext>>
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
            (factory, resources) -> createGroundTruthProgram(factory, resources, p->{}), validationLinear, "normalizedLinear_grayscale");
    }

    @Test
    @DisplayName("Normalized linear reconstruction error, grayscale tonemapped synthetic data")
    void normalizedLinear_grayscaleTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithLinearNoise", p->{}),
            (factory, resources) -> createGroundTruthProgram(factory, resources, p->{}), validationLinear, "normalizedLinear_grayscaleTonemapped");
    }

    @Test
    @DisplayName("Normalized linear reconstruction error, color synthetic data")
    void normalizedLinear_color() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithLinearColorNoise", setupColor),
            (factory, resources) -> createGroundTruthProgram(factory, resources, setupColor), validationLinear, "normalizedLinear_color");
    }

    @Test
    @DisplayName("Normalized linear reconstruction error, color tonemapped synthetic data")
    void normalizedLinear_colorTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithLinearColorNoise", setupColor),
            (factory, resources) -> createGroundTruthProgram(factory, resources, setupColor), validationLinear, "normalizedLinear_colorTonemapped");
    }

    @Test
    @DisplayName("Normalized linear reconstruction error, metallic synthetic data")
    void normalizedLinear_metallic() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithLinearColorNoise", setupMetallic),
            (factory, resources) -> createGroundTruthProgram(factory, resources, setupMetallic), validationLinear, "normalizedLinear_metallic");
    }

    @Test
    @DisplayName("Normalized linear reconstruction error, metallic tonemapped synthetic data")
    void normalizedLinear_metallicTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithLinearColorNoise", setupMetallic),
            (factory, resources) -> createGroundTruthProgram(factory, resources, setupMetallic), validationLinear, "normalizedLinear_metallicTonemapped");
    }

    @Test
    @DisplayName("Normalized sRGB reconstruction error, grayscale synthetic data")
    void normalizedSRGB_grayscale() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithSRGBNoise", p->{}),
            (factory, resources) -> createGroundTruthProgram(factory, resources, p->{}), validationSRGB, "normalizedSRGB_grayscale");
    }

    @Test
    @DisplayName("Normalized sRGB reconstruction error, grayscale tonemapped synthetic data")
    void normalizedSRGB_grayscaleTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithSRGBNoise", p->{}),
            (factory, resources) -> createGroundTruthProgram(factory, resources, p->{}), validationSRGB, "normalizedSRGB_grayscaleTonemapped");
    }

    @Test
    @DisplayName("Normalized sRGB reconstruction error, color synthetic data")
    void normalizedSRGB_color() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithSRGBColorNoise", setupColor),
            (factory, resources) -> createGroundTruthProgram(factory, resources, setupColor), validationSRGB, "normalizedSRGB_color");
    }

    @Test
    @DisplayName("Normalized sRGB reconstruction error, color tonemapped synthetic data")
    void normalizedSRGB_colorTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithSRGBColorNoise", setupColor),
            (factory, resources) -> createGroundTruthProgram(factory, resources, setupColor), validationSRGB, "normalizedSRGB_colorTonemapped");
    }

    @Test
    @DisplayName("Normalized sRGB reconstruction error, metallic synthetic data")
    void normalizedSRGB_metallic() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithSRGBColorNoise", setupMetallic),
            (factory, resources) -> createGroundTruthProgram(factory, resources, setupMetallic), validationSRGB, "normalizedSRGB_metallic");
    }

    @Test
    @DisplayName("Normalized sRGB reconstruction error, metallic tonemapped synthetic data")
    void normalizedSRGB_metallicTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithSRGBColorNoise", setupMetallic),
            (factory, resources) -> createGroundTruthProgram(factory, resources, setupMetallic), validationSRGB, "normalizedSRGB_metallicTonemapped");
    }

    @Test
    @DisplayName("Tonemapped and lit reconstruction error, grayscale synthetic data")
    void tonemappedLit_grayscale() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithTonemappedLitNoise", p->{}),
            (factory, resources) -> createGroundTruthProgram(factory, resources, p->{}), validationEncoded, "tonemappedLit_grayscale");
    }

    @Test
    @DisplayName("Tonemapped and lit reconstruction error, grayscale tonemapped synthetic data")
    void tonemappedLit_grayscaleTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithTonemappedLitNoise", p->{}),
            (factory, resources) -> createGroundTruthProgram(factory, resources, p->{}), validationEncoded, "tonemappedLit_grayscaleTonemapped");
    }

    @Test
    @DisplayName("Tonemapped and lit reconstruction error, color synthetic data")
    void tonemappedLit_color() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithTonemappedLitColorNoise", setupColor),
            (factory, resources) -> createGroundTruthProgram(factory, resources, setupColor), validationEncoded, "tonemappedLit_color");
    }

    @Test
    @DisplayName("Tonemapped and lit reconstruction error, color tonemapped synthetic data")
    void tonemappedLit_colorTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithTonemappedLitColorNoise", setupColor),
            (factory, resources) -> createGroundTruthProgram(factory, resources, setupColor), validationEncoded, "tonemappedLit_colorTonemapped");
    }

    @Test
    @DisplayName("Tonemapped and lit reconstruction error, metallic synthetic data")
    void tonemappedLit_metallic() throws IOException
    {
        multiTest(potatoViewSet, getProgramCreator("syntheticWithTonemappedLitColorNoise", setupMetallic),
            (factory, resources) -> createGroundTruthProgram(factory, resources, setupMetallic), validationEncoded, "tonemappedLit_metallic");
    }

    @Test
    @DisplayName("Tonemapped and lit reconstruction error, metallic tonemapped synthetic data")
    void tonemappedLit_metallicTonemapped() throws IOException
    {
        multiTest(potatoViewSetTonemapped, getProgramCreator("syntheticWithTonemappedLitColorNoise", setupMetallic),
            (factory, resources) -> createGroundTruthProgram(factory, resources, setupMetallic), validationEncoded, "tonemappedLit_metallicTonemapped");
    }

    @Test
    @DisplayName("Grayscale smooth synthetic data fit")
    void testFit_grayscaleSmooth()
    {
        testFitSynthetic(potatoViewSet,
            builder -> builder
                .define("NORMAL_MAP_SCALE", 0.0)
                .define("DEFAULT_DIFFUSE_COLOR", "vec3(0.5, 0.5, 0.5)")
                .define("DEFAULT_SPECULAR_COLOR", "vec3(0.04, 0.04, 0.04)")
                .define("DEFAULT_SPECULAR_ROUGHNESS", "vec3(0.25, 0.25, 0.25)"),
            fit ->
            {
                ColorList specularReflectivityList = fit.getSpecularReflectivityMap().getColorTextureReader().readColorListRGBA();
                ColorList roughnessList = fit.getSpecularRoughnessMap().getColorTextureReader().readColorListRGBA();
                ColorList diffuseList = fit.getDiffuseMap().getColorTextureReader().readColorListRGBA();
                ColorList normalList = fit.getNormalMap().getColorTextureReader().readColorListRGBA();

                float expectedReflectivity = (float)SRGB.fromLinear(0.04);
                float expectedRoughness = 0.25f;
                float expectedDiffuseTone = (float)SRGB.fromLinear(0.5);

                for (Vector4 reflectivity : specularReflectivityList)
                {
                    assertEquals(expectedReflectivity, reflectivity.x, 0.001f);
                    assertEquals(expectedReflectivity, reflectivity.y, 0.001f);
                    assertEquals(expectedReflectivity, reflectivity.z, 0.001f);
                    assertEquals(1.0f, reflectivity.w);
                }

                for (Vector4 roughness : roughnessList)
                {
                    assertEquals(expectedRoughness, roughness.x, 0.001f);
                    assertEquals(expectedRoughness, roughness.y, 0.001f);
                    assertEquals(expectedRoughness, roughness.z, 0.001f);
                    assertEquals(1.0f, roughness.w);
                }

                for (Vector4 diffuseColor : diffuseList)
                {
                    assertEquals(expectedDiffuseTone, diffuseColor.x, 0.001f);
                    assertEquals(expectedDiffuseTone, diffuseColor.y, 0.001f);
                    assertEquals(expectedDiffuseTone, diffuseColor.z, 0.001f);
                    assertEquals(1.0f, diffuseColor.w);
                }

                for (Vector4 packedNormal : normalList)
                {
                    assertEquals(0.5f, packedNormal.x, 0.001f);
                    assertEquals(0.5f, packedNormal.y, 0.001f);
                    assertEquals(1.0f, packedNormal.z, 0.001f);
                    assertEquals(1.0f, packedNormal.w);
                }
            },
            rmse ->
            {
                System.out.println("Encoded RMSE: " + rmse.getEncodedGroundTruth());
                System.out.println("Normalized sRGB RMSE: " + rmse.getNormalizedSRGB());
                System.out.println("Normalized linear RMSE: " + rmse.getNormalizedLinear());
                assertTrue (rmse.getEncodedGroundTruth() < 0.1);
                assertTrue (rmse.getNormalizedSRGB() < 0.1);
                assertTrue (rmse.getNormalizedLinear() < 0.1);
            },
            "testFit_grayscaleSmooth");
    }

    @Test
    @DisplayName("Color smooth synthetic data fit")
    void testFit_colorSmooth()
    {
        testFitSynthetic(potatoViewSet,
            builder -> builder
                .define("DEFAULT_DIFFUSE_COLOR", "vec3(1.0, 0.8, 0.2)")
                .define("DEFAULT_SPECULAR_COLOR", "vec3(0.04, 0.04, 0.04)")
                .define("DEFAULT_SPECULAR_ROUGHNESS", "vec3(0.25, 0.25, 0.25)")
                .define("NORMAL_MAP_SCALE", 0.0),
            fit ->
            {
                ColorList specularReflectivityList = fit.getSpecularReflectivityMap().getColorTextureReader().readColorListRGBA();
                ColorList roughnessList = fit.getSpecularRoughnessMap().getColorTextureReader().readColorListRGBA();
                ColorList diffuseList = fit.getDiffuseMap().getColorTextureReader().readColorListRGBA();
                ColorList normalList = fit.getNormalMap().getColorTextureReader().readColorListRGBA();

                float expectedReflectivity = (float)SRGB.fromLinear(0.04);
                float expectedRoughness = 0.25f;
                Vector3 expectedDiffuseColor = SRGB.fromLinear(new Vector3(1.0f, 0.8f, 0.2f));

                for (Vector4 reflectivity : specularReflectivityList)
                {
                    assertEquals(expectedReflectivity, reflectivity.x, 0.001f);
                    assertEquals(expectedReflectivity, reflectivity.y, 0.001f);
                    assertEquals(expectedReflectivity, reflectivity.z, 0.001f);
                    assertEquals(1.0f, reflectivity.w);
                }

                for (Vector4 roughness : roughnessList)
                {
                    assertEquals(expectedRoughness, roughness.x, 0.001f);
                    assertEquals(expectedRoughness, roughness.y, 0.001f);
                    assertEquals(expectedRoughness, roughness.z, 0.001f);
                    assertEquals(1.0f, roughness.w);
                }

                for (Vector4 diffuseColor : diffuseList)
                {
                    assertEquals(expectedDiffuseColor.x, diffuseColor.x, 0.001f);
                    assertEquals(expectedDiffuseColor.y, diffuseColor.y, 0.001f);
                    assertEquals(expectedDiffuseColor.z, diffuseColor.z, 0.001f);
                    assertEquals(1.0f, diffuseColor.w);
                }

                for (Vector4 packedNormal : normalList)
                {
                    assertEquals(0.5f, packedNormal.x, 0.001f);
                    assertEquals(0.5f, packedNormal.y, 0.001f);
                    assertEquals(1.0f, packedNormal.z, 0.001f);
                    assertEquals(1.0f, packedNormal.w);
                }
            },
            rmse ->
            {
                System.out.println("Encoded RMSE: " + rmse.getEncodedGroundTruth());
                System.out.println("Normalized sRGB RMSE: " + rmse.getNormalizedSRGB());
                System.out.println("Normalized linear RMSE: " + rmse.getNormalizedLinear());
                assertTrue (rmse.getEncodedGroundTruth() < 0.1);
                assertTrue (rmse.getNormalizedSRGB() < 0.1);
                assertTrue (rmse.getNormalizedLinear() < 0.1);
            },
            "testFit_colorSmooth");
    }

    @Test
    @DisplayName("Metallic smooth synthetic data fit")
    void testFit_metallicSmooth()
    {
        testFitSynthetic(potatoViewSet,
            builder -> builder
                .define("DEFAULT_DIFFUSE_COLOR", "vec3(0.0, 0.0, 0.0)")
                .define("DEFAULT_SPECULAR_COLOR", "vec3(1.0, 0.8, 0.2)")
                .define("DEFAULT_SPECULAR_ROUGHNESS", "vec3(0.25, 0.25, 0.25)")
                .define("NORMAL_MAP_SCALE", 0.0),
            fit ->
            {
                ColorList specularReflectivityList = fit.getSpecularReflectivityMap().getColorTextureReader().readColorListRGBA();
                ColorList roughnessList = fit.getSpecularRoughnessMap().getColorTextureReader().readColorListRGBA();
                ColorList diffuseList = fit.getDiffuseMap().getColorTextureReader().readColorListRGBA();
                ColorList normalList = fit.getNormalMap().getColorTextureReader().readColorListRGBA();

                Vector3 expectedReflectivity = SRGB.fromLinear(new Vector3(1.0f, 0.8f, 0.2f));
                float expectedDiffuse = 0.0f;
                float expectedRoughness = 0.25f;

                for (Vector4 reflectivity : specularReflectivityList)
                {
                    assertEquals(expectedReflectivity.x, reflectivity.x, 0.001f);
                    assertEquals(expectedReflectivity.y, reflectivity.y, 0.001f);
                    assertEquals(expectedReflectivity.z, reflectivity.z, 0.001f);
                    assertEquals(1.0f, reflectivity.w);
                }

                for (Vector4 roughness : roughnessList)
                {
                    assertEquals(expectedRoughness, roughness.x, 0.001f);
                    assertEquals(expectedRoughness, roughness.y, 0.001f);
                    assertEquals(expectedRoughness, roughness.z, 0.001f);
                    assertEquals(1.0f, roughness.w);
                }

                for (Vector4 diffuseColor : diffuseList)
                {
                    assertEquals(expectedDiffuse, diffuseColor.x, 0.001f);
                    assertEquals(expectedDiffuse, diffuseColor.y, 0.001f);
                    assertEquals(expectedDiffuse, diffuseColor.z, 0.001f);
                    assertEquals(1.0f, diffuseColor.w);
                }

                for (Vector4 packedNormal : normalList)
                {
                    assertEquals(0.5f, packedNormal.x, 0.001f);
                    assertEquals(0.5f, packedNormal.y, 0.001f);
                    assertEquals(1.0f, packedNormal.z, 0.001f);
                    assertEquals(1.0f, packedNormal.w);
                }
            },
            rmse ->
            {
                System.out.println("Encoded RMSE: " + rmse.getEncodedGroundTruth());
                System.out.println("Normalized sRGB RMSE: " + rmse.getNormalizedSRGB());
                System.out.println("Normalized linear RMSE: " + rmse.getNormalizedLinear());
                assertTrue (rmse.getEncodedGroundTruth() < 0.1);
                assertTrue (rmse.getNormalizedSRGB() < 0.1);
                assertTrue (rmse.getNormalizedLinear() < 0.1);
            },
            "testFit_metallicSmooth");
    }

    @Test
    @DisplayName("Grayscale bumpy synthetic data fit")
    void testFit_grayscaleBumpy()
    {
        testFitSynthetic(potatoViewSet,
            builder -> builder
                .define("NORMAL_MAP_SCALE", 1.0)
                .define("DEFAULT_DIFFUSE_COLOR", "vec3(0.5, 0.5, 0.5)")
                .define("DEFAULT_SPECULAR_COLOR", "vec3(0.04, 0.04, 0.04)")
                .define("DEFAULT_SPECULAR_ROUGHNESS", "vec3(0.25, 0.25, 0.25)"),
            fit ->
            {
                ColorList specularReflectivityList = fit.getSpecularReflectivityMap().getColorTextureReader().readColorListRGBA();
                ColorList roughnessList = fit.getSpecularRoughnessMap().getColorTextureReader().readColorListRGBA();
                ColorList diffuseList = fit.getDiffuseMap().getColorTextureReader().readColorListRGBA();
                ColorList normalList = fit.getNormalMap().getColorTextureReader().readColorListRGBA();

                float expectedReflectivity = (float)SRGB.fromLinear(0.04);
                float expectedRoughness = 0.25f;
                float expectedDiffuseTone = (float)SRGB.fromLinear(0.5);

                for (Vector4 reflectivity : specularReflectivityList)
                {
                    assertEquals(expectedReflectivity, reflectivity.x, 0.001f);
                    assertEquals(expectedReflectivity, reflectivity.y, 0.001f);
                    assertEquals(expectedReflectivity, reflectivity.z, 0.001f);
                    assertEquals(1.0f, reflectivity.w);
                }

                for (Vector4 roughness : roughnessList)
                {
                    assertEquals(expectedRoughness, roughness.x, 0.001f);
                    assertEquals(expectedRoughness, roughness.y, 0.001f);
                    assertEquals(expectedRoughness, roughness.z, 0.001f);
                    assertEquals(1.0f, roughness.w);
                }

                for (Vector4 diffuseColor : diffuseList)
                {
                    assertEquals(expectedDiffuseTone, diffuseColor.x, 0.001f);
                    assertEquals(expectedDiffuseTone, diffuseColor.y, 0.001f);
                    assertEquals(expectedDiffuseTone, diffuseColor.z, 0.001f);
                    assertEquals(1.0f, diffuseColor.w);
                }
            },
            rmse ->
            {
                System.out.println("Encoded RMSE: " + rmse.getEncodedGroundTruth());
                System.out.println("Normalized sRGB RMSE: " + rmse.getNormalizedSRGB());
                System.out.println("Normalized linear RMSE: " + rmse.getNormalizedLinear());
                assertTrue (rmse.getEncodedGroundTruth() < 0.1);
                assertTrue (rmse.getNormalizedSRGB() < 0.1);
                assertTrue (rmse.getNormalizedLinear() < 0.1);
            },
            "testFit_grayscaleBumpy");
    }

    @Test
    @DisplayName("Color bumpy synthetic data fit")
    void testFit_colorBumpy()
    {
        testFitSynthetic(potatoViewSet,
            builder -> builder
                .define("DEFAULT_DIFFUSE_COLOR", "vec3(1.0, 0.8, 0.2)")
                .define("DEFAULT_SPECULAR_COLOR", "vec3(0.04, 0.04, 0.04)")
                .define("DEFAULT_SPECULAR_ROUGHNESS", "vec3(0.25, 0.25, 0.25)")
                .define("NORMAL_MAP_SCALE", 1.0),
            fit ->
            {
                ColorList specularReflectivityList = fit.getSpecularReflectivityMap().getColorTextureReader().readColorListRGBA();
                ColorList roughnessList = fit.getSpecularRoughnessMap().getColorTextureReader().readColorListRGBA();
                ColorList diffuseList = fit.getDiffuseMap().getColorTextureReader().readColorListRGBA();
                ColorList normalList = fit.getNormalMap().getColorTextureReader().readColorListRGBA();

                float expectedReflectivity = (float)SRGB.fromLinear(0.04);
                float expectedRoughness = 0.25f;
                Vector3 expectedDiffuseColor = SRGB.fromLinear(new Vector3(1.0f, 0.8f, 0.2f));

                for (Vector4 reflectivity : specularReflectivityList)
                {
                    assertEquals(expectedReflectivity, reflectivity.x, 0.001f);
                    assertEquals(expectedReflectivity, reflectivity.y, 0.001f);
                    assertEquals(expectedReflectivity, reflectivity.z, 0.001f);
                    assertEquals(1.0f, reflectivity.w);
                }

                for (Vector4 roughness : roughnessList)
                {
                    assertEquals(expectedRoughness, roughness.x, 0.001f);
                    assertEquals(expectedRoughness, roughness.y, 0.001f);
                    assertEquals(expectedRoughness, roughness.z, 0.001f);
                    assertEquals(1.0f, roughness.w);
                }

                for (Vector4 diffuseColor : diffuseList)
                {
                    assertEquals(expectedDiffuseColor.x, diffuseColor.x, 0.001f);
                    assertEquals(expectedDiffuseColor.y, diffuseColor.y, 0.001f);
                    assertEquals(expectedDiffuseColor.z, diffuseColor.z, 0.001f);
                    assertEquals(1.0f, diffuseColor.w);
                }
            },
            rmse ->
            {
                System.out.println("Encoded RMSE: " + rmse.getEncodedGroundTruth());
                System.out.println("Normalized sRGB RMSE: " + rmse.getNormalizedSRGB());
                System.out.println("Normalized linear RMSE: " + rmse.getNormalizedLinear());
                assertTrue (rmse.getEncodedGroundTruth() < 0.1);
                assertTrue (rmse.getNormalizedSRGB() < 0.1);
                assertTrue (rmse.getNormalizedLinear() < 0.1);
            },
            "testFit_colorBumpy");
    }

    @Test
    @DisplayName("Metallic bumpy synthetic data fit")
    void testFit_metallicBumpy()
    {
        testFitSynthetic(potatoViewSet,
            builder -> builder
                .define("DEFAULT_DIFFUSE_COLOR", "vec3(0.0, 0.0, 0.0)")
                .define("DEFAULT_SPECULAR_COLOR", "vec3(1.0, 0.8, 0.2)")
                .define("DEFAULT_SPECULAR_ROUGHNESS", "vec3(0.25, 0.25, 0.25)")
                .define("NORMAL_MAP_SCALE", 1.0),
            fit ->
            {
                ColorList specularReflectivityList = fit.getSpecularReflectivityMap().getColorTextureReader().readColorListRGBA();
                ColorList roughnessList = fit.getSpecularRoughnessMap().getColorTextureReader().readColorListRGBA();
                ColorList diffuseList = fit.getDiffuseMap().getColorTextureReader().readColorListRGBA();
                ColorList normalList = fit.getNormalMap().getColorTextureReader().readColorListRGBA();

                Vector3 expectedReflectivity = SRGB.fromLinear(new Vector3(1.0f, 0.8f, 0.2f));
                float expectedDiffuse = 0.0f;
                float expectedRoughness = 0.25f;

                for (Vector4 reflectivity : specularReflectivityList)
                {
                    assertEquals(expectedReflectivity.x, reflectivity.x, 0.001f);
                    assertEquals(expectedReflectivity.y, reflectivity.y, 0.001f);
                    assertEquals(expectedReflectivity.z, reflectivity.z, 0.001f);
                    assertEquals(1.0f, reflectivity.w);
                }

                for (Vector4 roughness : roughnessList)
                {
                    assertEquals(expectedRoughness, roughness.x, 0.001f);
                    assertEquals(expectedRoughness, roughness.y, 0.001f);
                    assertEquals(expectedRoughness, roughness.z, 0.001f);
                    assertEquals(1.0f, roughness.w);
                }

                for (Vector4 diffuseColor : diffuseList)
                {
                    assertEquals(expectedDiffuse, diffuseColor.x, 0.001f);
                    assertEquals(expectedDiffuse, diffuseColor.y, 0.001f);
                    assertEquals(expectedDiffuse, diffuseColor.z, 0.001f);
                    assertEquals(1.0f, diffuseColor.w);
                }
            },
            rmse ->
            {
                System.out.println("Encoded RMSE: " + rmse.getEncodedGroundTruth());
                System.out.println("Normalized sRGB RMSE: " + rmse.getNormalizedSRGB());
                System.out.println("Normalized linear RMSE: " + rmse.getNormalizedLinear());
                assertTrue (rmse.getEncodedGroundTruth() < 0.1);
                assertTrue (rmse.getNormalizedSRGB() < 0.1);
                assertTrue (rmse.getNormalizedLinear() < 0.1);
            },
            "testFit_metallicBumpy");
    }

    @Test
    @DisplayName("Rodin fit, from Metashape export")
    void testFit_rodinMetashape() throws Exception
    {
        testFitMetashape(
            "Rodin/Mia_001239_Rodin_399cameras.xml",
            "Rodin/Mia_001239_Rodin_200kAverage.obj",
            "Rodin/Processed dark 25",
            rmse ->
            {
                System.out.println("Encoded RMSE: " + rmse.getEncodedGroundTruth());
                System.out.println("Normalized sRGB RMSE: " + rmse.getNormalizedSRGB());
                System.out.println("Normalized linear RMSE: " + rmse.getNormalizedLinear());
                assertTrue (rmse.getEncodedGroundTruth() < 0.1);
                assertTrue (rmse.getNormalizedSRGB() < 0.1);
                assertTrue (rmse.getNormalizedLinear() < 0.1);
            },
            "Rodin_metashape");
    }

    static ProgramObject<OpenGLContext> createGroundTruthProgram(
        SpecularFitProgramFactory<OpenGLContext> programFactory, GraphicsResourcesAnalytic<OpenGLContext> resources, Consumer<Program<OpenGLContext>> setupShader)
    {
        try
        {
            ProgramObject<OpenGLContext> program = programFactory.getShaderProgramBuilder(resources,
                    new File("shaders/common/imgspace.vert"),
                    new File("shaders/test/syntheticTonemapped.frag"))
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
        BiFunction<SpecularFitProgramFactory<OpenGLContext>, GraphicsResourcesAnalytic<OpenGLContext>, ProgramObject<OpenGLContext>> testProgramCreator,
        BiFunction<SpecularFitProgramFactory<OpenGLContext>, GraphicsResourcesAnalytic<OpenGLContext>, ProgramObject<OpenGLContext>> groundTruthProgramCreator,
        BiConsumer<ColorAppearanceRMSE, Float> validationByNoiseScale,
        String testName)  throws IOException
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
                rmse -> validationByNoiseScale.accept(rmse, noiseScale),
                MessageFormat.format("{0} ({1,number,0.00})", testName, noiseScale));
        }
    }

    private void testSynthetic(
        ViewSet viewSet,
        BiFunction<SpecularFitProgramFactory<OpenGLContext>, GraphicsResourcesAnalytic<OpenGLContext>, ProgramObject<OpenGLContext>> testProgramCreator,
        BiFunction<SpecularFitProgramFactory<OpenGLContext>, GraphicsResourcesAnalytic<OpenGLContext>, ProgramObject<OpenGLContext>> groundTruthProgramCreator,
        Consumer<ColorAppearanceRMSE> validation,
        String testName) throws IOException
    {
        SimpleGeneralSettingsModel globalSettings = new SimpleGeneralSettingsModel();
        DefaultSettings.applyGlobalDefaults(globalSettings);

        SpecularBasisSettings specularBasisSettings = new SpecularBasisSettings();
        specularBasisSettings.setBasisCount(1);

        SpecularFitProgramFactory<OpenGLContext> programFactory = new SpecularFitProgramFactory<>(specularBasisSettings);

        try (GraphicsResourcesAnalytic<OpenGLContext> resources = new GraphicsResourcesAnalytic<>(context, viewSet, potatoGeometry);
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
                    viewIndex -> renderGroundTruth(viewSet, viewIndex, groundTruthDrawable, groundTruthFBO));
                ProgramObject<OpenGLContext> syntheticWithNoise = testProgramCreator.apply(programFactory, resources);
                Drawable<OpenGLContext> drawable = resources.createDrawable(syntheticWithNoise))
            {
                resources.setupShaderProgram(syntheticWithNoise);

                File outputDirectory = new File(TEST_OUTPUT_DIR, testName);
                outputDirectory.mkdirs();

                for (ReconstructionView<OpenGLContext> view : reconstruction)
                {
                    // Pass light intensity for noise generation methods that depend on it.
                    syntheticWithNoise.setUniform("reconstructionLightIntensity", viewSet.getLightIntensity(viewSet.getLightIndex(view.getIndex())));

                    ColorAppearanceRMSE rmse = view.reconstruct(drawable);

                    if (SAVE_TEST_IMAGES)
                    {
                        view.getReconstructionFramebuffer().getTextureReaderForColorAttachment(0)
                            .saveToFile("PNG", new File(outputDirectory,
                                    MessageFormat.format("{0,number,0000}.png", view.getIndex())),
                                // Luminance encoding expects [0, 1] range, but encodes in [0, 255] range.
                                // Tonemapper parameter taken by saveToFile assumes both are [0, 255]
                                (color, index) -> viewSet.getLuminanceEncoding().encode(
                                    color.asDoubleFloatingPoint().dividedBy(255.0).times(view.getIncidentRadiance(index).asVector4(1.0))).rounded());
                    }
                    validation.accept(rmse);
                }
            }
        }
    }

    private static ColorArrayImage renderGroundTruth(ReadonlyViewSet viewSet, int viewIndex,
        Drawable<OpenGLContext> groundTruthDrawable, Framebuffer<OpenGLContext> groundTruthFBO)
    {
        groundTruthDrawable.program().setUniform("model_view", viewSet.getCameraPose(viewIndex));
        groundTruthDrawable.program().setUniform("projection",
            viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex)).getProjectionMatrix(
                viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
        groundTruthDrawable.program().setUniform("reconstructionCameraPos",
            viewSet.getCameraPoseInverse(viewIndex).getColumn(3).getXYZ());
        groundTruthDrawable.program().setUniform("reconstructionLightPos",
            viewSet.getCameraPoseInverse(viewIndex).times(viewSet.getLightPosition(viewSet.getLightIndex(viewIndex)).asPosition()).getXYZ());
        groundTruthDrawable.program().setUniform("reconstructionLightIntensity",
            viewSet.getLightIntensity(viewSet.getLightIndex(viewIndex)));

        groundTruthFBO.clearColorBuffer(0, 0, 0, 0, 0);
        groundTruthFBO.clearDepthBuffer();
        groundTruthDrawable.draw(groundTruthFBO);

        float[] groundTruth = groundTruthFBO.getTextureReaderForColorAttachment(0).readFloatingPointRGBA();
        return new ColorArrayImage(groundTruth, 256, 256);
    }

    private void testFitSynthetic(ViewSet viewSet, Function<ProgramBuilder<OpenGLContext>, ProgramBuilder<OpenGLContext>> injectDefines,
        Consumer<SpecularMaterialResources<?>> fitValidation, Consumer<ColorAppearanceRMSE> rmseValidation, String testName)
    {
        try (GraphicsResources<OpenGLContext> resources = new GraphicsResourcesAnalytic<>(context, viewSet, potatoGeometry)
        {
            @Override
            public ProgramBuilder<OpenGLContext> getShaderProgramBuilder()
            {
                return injectDefines.apply(super.getShaderProgramBuilder());
            }
        })
        {
            // TODO not yet tested
            File outputDirectory = new File(TEST_OUTPUT_DIR, testName);
            outputDirectory.mkdirs();

            GeneralSettingsModel settings = new SimpleGeneralSettingsModel();
            DefaultSettings.applyGlobalDefaults(settings);
            SpecularFitRequestParams params = new SpecularFitRequestParams(512, 512);
            params.setOutputDirectory(outputDirectory);

            // Perform the specular fit
            SpecularFitProcess specularFitProcess = new SpecularFitProcess(params);
            try (SpecularFitOptimizable<OpenGLContext> specularFit = specularFitProcess.optimizeFit(resources, progressMonitor))
            {
                fitValidation.accept(specularFit);

                specularFitProcess.reconstructAll(resources,
                    (view, rmse) ->
                    {
                        if (SAVE_TEST_IMAGES)
                        {
                            try
                            {
                                view.getReconstructionFramebuffer().getTextureReaderForColorAttachment(0).saveToFile("PNG",
                                    new File(outputDirectory, ImageFinder.getInstance().getImageFileNameWithFormat(
                                        resources.getViewSet().getImageFileName(view.getIndex()), "png")),
                                    // Luminance encoding expects [0, 1] range, but encodes in [0, 255] range.
                                    // Tonemapper parameter taken by saveToFile assumes both are [0, 255]
                                    (color, index) -> resources.getViewSet().getLuminanceEncoding().encode(
                                        color.asDoubleFloatingPoint().dividedBy(255.0).times(view.getIncidentRadiance(index).asVector4(1.0))).rounded());
                            }
                            catch (IOException e)
                            {
                                throw new RuntimeException(e);
                            }
                        }

                        rmseValidation.accept(rmse);
                    });
            }
        }
        catch (UserCancellationException | IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void testFitMetashape(String cameras, String geometry, String imageDirectory,
        Consumer<ColorAppearanceRMSE> validation, String testName) throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        LoadOptionsModel imageLoadOptions = new ObservableLoadOptionsModel();
        imageLoadOptions.setColorImagesRequested(false); // don't generate/load preview images; not needed for this test

        ViewSetLoadOptions viewSetLoadOptions = new ViewSetLoadOptions();
        viewSetLoadOptions.geometryFile = new File(classLoader.getResource("test/" + geometry).toURI());
        viewSetLoadOptions.mainDirectories.fullResImageDirectory = new File(classLoader.getResource("test/" + imageDirectory).toURI());
        viewSetLoadOptions.mainDirectories.fullResImagesNeedUndistort = true;

        try (GraphicsResourcesImageSpace<OpenGLContext> resources = GraphicsResourcesImageSpace.getBuilderForContext(context)
                .setImageLoadOptions(imageLoadOptions)
                .setProgressMonitor(progressMonitor)
                .loadLooseFiles(new File(classLoader.getResource("test/" + cameras).toURI()), viewSetLoadOptions)
                .create())
        {
            resources.calibrateLightIntensities(false);
            testFit(resources, validation, testName);
        }
    }

    private void testFitVSET(File viewSetFile, Consumer<ColorAppearanceRMSE> validation, String testName) throws Exception
    {
        LoadOptionsModel loadOptions = new ObservableLoadOptionsModel();
        loadOptions.setColorImagesRequested(false); // don't generate/load preview images; not needed for this test
        try (GraphicsResourcesImageSpace<OpenGLContext> resources = GraphicsResourcesImageSpace.getBuilderForContext(context)
            .setImageLoadOptions(loadOptions)
            .setProgressMonitor(progressMonitor)
            .loadVSETFile(viewSetFile, viewSetFile.getParentFile())
            .create())
        {
            resources.calibrateLightIntensities(false);
            testFit(resources, validation, testName);
        }
        catch (UserCancellationException | IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void testFit(GraphicsResourcesCacheable<OpenGLContext> resources, Consumer<ColorAppearanceRMSE> validation, String testName)
        throws IOException, UserCancellationException
    {
        // TODO not yet tested
        File outputDirectory = new File(TEST_OUTPUT_DIR, testName);
        outputDirectory.mkdirs();

        GeneralSettingsModel settings = new SimpleGeneralSettingsModel();
        DefaultSettings.applyGlobalDefaults(settings);
        SpecularFitRequestParams params = new SpecularFitRequestParams(512, 512);
        params.setOutputDirectory(outputDirectory);
        params.getImageCacheSettings().setCacheParentDirectory(new File (outputDirectory, "cache"));

        // Perform the specular fit
        SpecularFitProcess specularFitProcess = new SpecularFitProcess(params);
        specularFitProcess.optimizeFitWithCache(resources, progressMonitor);

        specularFitProcess.reconstructAll(resources,
            (view, rmse) ->
            {
                if (SAVE_TEST_IMAGES)
                {
                    try
                    {
                        view.getReconstructionFramebuffer().getTextureReaderForColorAttachment(0).saveToFile("PNG",
                            new File(outputDirectory, ImageFinder.getInstance().getImageFileNameWithFormat(
                                resources.getViewSet().getImageFileName(view.getIndex()), "png")),
                            // Luminance encoding expects [0, 1] range, but encodes in [0, 255] range.
                            // Tonemapper parameter taken by saveToFile assumes both are [0, 255]
                            (color, index) -> resources.getViewSet().getLuminanceEncoding().encode(
                                color.asDoubleFloatingPoint().dividedBy(255.0).times(view.getIncidentRadiance(index).asVector4(1.0))).rounded());
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }

                validation.accept(rmse);
            });
    }
}