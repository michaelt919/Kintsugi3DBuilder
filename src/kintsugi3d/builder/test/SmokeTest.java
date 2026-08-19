/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.test;

import kintsugi3d.builder.app.logging.LogMessage;
import kintsugi3d.builder.app.logging.LogMessageListener;
import kintsugi3d.builder.app.logging.RecentLogMessageAppender;
import kintsugi3d.builder.core.*;
import kintsugi3d.builder.core.metrics.ColorAppearanceRMSE;
import kintsugi3d.builder.fit.SpecularFitProcess;
import kintsugi3d.builder.fit.settings.SpecularFitSettings;
import kintsugi3d.builder.io.ViewSetDirectories;
import kintsugi3d.builder.io.ViewSetLoadOptions;
import kintsugi3d.builder.io.ViewSetReaderFromVSET;
import kintsugi3d.builder.javafx.internal.ObservableLoadOptionsModel;
import kintsugi3d.builder.rendering.ProjectInstanceManager;
import kintsugi3d.builder.resources.project.GraphicsResourcesCacheable;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace;
import kintsugi3d.builder.state.settings.DefaultSettings;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;
import kintsugi3d.builder.state.settings.SimpleGeneralSettingsModel;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.opengl.OpenGLContext;
import kintsugi3d.gl.opengl.OpenGLContextFactory;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.Potato;
import org.junit.jupiter.api.*;
import org.slf4j.event.Level;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class SmokeTest
{
    private static final File TEST_OUTPUT_DIR = new File("test-output");

    private ProgressMonitor progressMonitor;
    private ViewSet viewSet;
    private ViewSet tonemappedViewSet;
    private Context context;
    private VertexGeometry potatoGeometry;
//    private BiConsumer<ColorAppearanceRMSE, Float> validationLinear;
//    private BiConsumer<ColorAppearanceRMSE, Float> validationSRGB;
//    private BiConsumer<ColorAppearanceRMSE, Float> validationEncoded;
    private Consumer<Program<OpenGLContext>> setupColor;
    private Consumer<Program<OpenGLContext>> setupMetallic;

    private static class ProgressMonitorImpl implements ProgressMonitor
    {
        private double maxProgress;
        private int stageCount;
        private int stage;

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
            this.stage = 0;
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
            this.stage = stage;
//            System.out.println(MessageFormat.format("[{0}/{1}] {2}", stage, stageCount, message));
        }

        @Override
        public void advanceStage(String message)
        {
            setStage(stage + 1, message);
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
    void setup() throws IOException, URISyntaxException
    {
        progressMonitor = new ProgressMonitorImpl();

        // Create directories
        ViewSetDirectories directories = new ViewSetDirectories();
        directories.fullResImagesNeedUndistort = true;

        // Create ViewSet
        viewSet = ViewSetReaderFromVSET.getInstance().readFromFile(new File(Objects.requireNonNull(
            SmokeTest.class.getClassLoader().getResource("test/Structured34View.vset")).toURI())).finish();
        viewSet.getProjectSettings().set("occlusionEnabled", false);

        // Create tonemapped ViewSet (Using tonemap from the Guan Yu dataset)
        tonemappedViewSet = viewSet.copy();
        tonemappedViewSet.setLuminanceEncoding(new double [] { 0.031, 0.090, 0.198, 0.362, 0.591, 0.900 },
            new byte [] { 50, 105, (byte)140, (byte)167, (byte)176, (byte)185 });

        // Create Context
        context = OpenGLContextFactory.getInstance().buildWindow("Kintsugi 3D Builder Tests", 1, 1).create().getContext();
        context.getState().enableDepthTest();

        // Create geometry
        Potato potato = new Potato(50, 0.75f, 0.1f, 250000);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        potato.writeToStream(new PrintStream(out, false, StandardCharsets.UTF_8));
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        potatoGeometry = VertexGeometry.createFromOBJStream(in);

        // Validations
//        validationLinear = (rmse, noiseScale) -> assertEquals(noiseScale / (float) Math.sqrt(12.0f), rmse.getNormalizedLinear(), 0.001);
//        validationSRGB = (rmse, noiseScale) -> assertEquals(noiseScale / (float) Math.sqrt(12.0f), rmse.getNormalizedSRGB(), 0.001);
//        validationEncoded = (rmse, noiseScale) -> assertEquals(noiseScale / (float) Math.sqrt(12.0f), rmse.getEncodedGroundTruth(), 0.005);

        // Create colors
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

    @Test
    @DisplayName("Rodin fit, from Metashape export")
    void testFit_rodinMetashape() throws Exception
    {
        LogMessageListener logListener = new LogMessageListener()
        {
            @Override
            public void newLogMessage(LogMessage logMessage)
            {
                assertNotSame(Level.ERROR, logMessage.getLogLevel());
            }
        };
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitMetashape(
            "Rodin/Mia_001239_Rodin_399cameras.xml",
            "Rodin/Mia_001239_Rodin_200kAverage.obj",
            "Rodin/Processed dark 25",
            rmse ->
            {
                System.out.println("Encoded RMSE: " + rmse.getEncodedGroundTruth());
                System.out.println("Normalized sRGB RMSE: " + rmse.getNormalizedSRGB());
                System.out.println("Normalized linear RMSE: " + rmse.getNormalizedLinear());
//                assertTrue (rmse.getEncodedGroundTruth() < 0.1);
//                assertTrue (rmse.getNormalizedSRGB() < 0.1);
//                assertTrue (rmse.getNormalizedLinear() < 0.1);
            },
            "Rodin_metashape");
    }

    private void testFitMetashape(String cameras, String geometry, String imageDirectory,
                                  Consumer<ColorAppearanceRMSE> validation, String testName) throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        LoadOptionsModel imageLoadOptions = new ObservableLoadOptionsModel();
        imageLoadOptions.setColorImagesRequested(false); // don't generate/load preview images; not needed for this test
        // These are set since they otherwise are set in JavaFX related code
        IOModel.getInstance().setImageLoadOptionsModel(imageLoadOptions);
        ProjectInstanceManager mockIOHandler = new ProjectInstanceManager<>(context);
        mockIOHandler.setTestingViewSet(viewSet);
        IOModel.getInstance().setLoadingHandler(mockIOHandler);

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
            resources.calibrateLightIntensities();
            testFit(resources, validation, testName);
        }
    }

    private void testFit(GraphicsResourcesCacheable<OpenGLContext> resources, Consumer<ColorAppearanceRMSE> validation, String testName)
        throws IOException, UserCancellationException
    {
        File outputDirectory = new File(TEST_OUTPUT_DIR, testName);
        outputDirectory.mkdirs();

        GeneralSettingsModel settings = new SimpleGeneralSettingsModel();
        DefaultSettings.applyGlobalDefaults(settings);
        SpecularFitSettings params = new SpecularFitSettings(512, 512);
        params.setOutputDirectory(outputDirectory);
        params.getImageCacheSettings().setCacheParentDirectory(new File (outputDirectory, "cache"));

        // Perform the specular fit
        SpecularFitProcess specularFitProcess = new SpecularFitProcess(params);
        specularFitProcess.optimizeFitWithCache(resources, progressMonitor);

        specularFitProcess.reconstructAll(resources,
            (view, rmse) -> validation.accept(rmse));
    }
}
