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

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.GlobalBootstrap;
import kintsugi3d.builder.core.metrics.ReadonlyColorAppearanceRMSE;
import kintsugi3d.builder.core.viewset.ViewSet;
import kintsugi3d.builder.fit.BasisAndTexturesOptimizationProcess;
import kintsugi3d.builder.fit.settings.BasisOptimizationSettings;
import kintsugi3d.builder.fit.settings.SpecularFitSettings;
import kintsugi3d.builder.io.LoadOptionsModel;
import kintsugi3d.builder.io.ViewSetDirectories;
import kintsugi3d.builder.io.ViewSetLoadOptions;
import kintsugi3d.builder.io.ViewSetReaderFromVSET;
import kintsugi3d.builder.io.metashape.MetashapeChunk;
import kintsugi3d.builder.io.metashape.MetashapeDocument;
import kintsugi3d.builder.io.metashape.MetashapeModel;
import kintsugi3d.builder.javafx.controllers.modals.RecentLogMessageAppender;
import kintsugi3d.builder.javafx.internal.ObservableLoadOptionsModel;
import kintsugi3d.builder.rendering.ImageBasedRenderableManager;
import kintsugi3d.builder.resources.project.GraphicsResourcesImageSpace;
import kintsugi3d.builder.resources.project.ReadonlyImageBasedGraphicsResources;
import kintsugi3d.builder.state.settings.DefaultSettings;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;
import kintsugi3d.builder.state.settings.SimpleGeneralSettingsModel;
import kintsugi3d.builder.util.logging.LogMessage;
import kintsugi3d.builder.util.logging.LogMessageListener;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.interactive.ProgressMonitor;
import kintsugi3d.gl.interactive.UserCancellationException;
import kintsugi3d.gl.opengl.OpenGLContext;
import kintsugi3d.gl.opengl.OpenGLContextFactory;
import kintsugi3d.gl.vecmath.Vector3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SmokeTest
{
    private static final File TEST_OUTPUT_DIR = new File("test-output");

    private ProgressMonitor progressMonitor;
    private ViewSet viewSet;
    private ViewSet tonemappedViewSet;
    private OpenGLContext context;
    private VertexGeometry potatoGeometry;
    private Consumer<Program<OpenGLContext>> setupColor;
    private Consumer<Program<OpenGLContext>> setupMetallic;
    private AtomicReference<Throwable> observerFailure;

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

    private class TestLogListener extends LogMessageListener
    {

        @Override
        public void newLogMessage(LogMessage logMessage)
        {
//            assertNotSame(Level.ERROR, logMessage.getLogLevel());
            if (logMessage.getLogLevel() == Level.ERROR)
            {
                observerFailure.set(new AssertionError("Error found in log message!"));
            }
        }
    }

    @BeforeEach
    void setup() throws IOException, URISyntaxException
    {
        // Commented out code comes from other test class and could probably be used to do a from scratch test in
        // addition to the current Metashape test.

        // Initialize testing objects
        progressMonitor = new ProgressMonitorImpl();
        observerFailure = new AtomicReference<>();

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
//        Potato potato = new Potato(50, 0.75f, 0.1f, 250000);
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        potato.writeToStream(new PrintStream(out, false, StandardCharsets.UTF_8));
//        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
//        potatoGeometry = VertexGeometry.createFromOBJStream(in);

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
        // Check whether the log contained any errors
        Throwable failure = observerFailure.get();
        if (failure != null)
        {
            fail(failure.getMessage());
        }
        context.close();
    }

    @Test
    @DisplayName("Rodin fit, from Metashape export")
    void testFit_rodinMetashape() throws Exception
    {
        LogMessageListener logListener = new TestLogListener();
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
            },
            "Rodin_metashape");
        int numLoadedCameras = viewSet.getViewCount();
        assertEquals(397, numLoadedCameras);
        int numEnabledCameras = viewSet.getEnabledViewCount();
        assertEquals(397, numEnabledCameras);
        int numDisabledCameras = viewSet.getDisabledViewCount();
        assertEquals(0, numDisabledCameras);
    }

    @Test
    @DisplayName("Rodin fit, from Psx import")
    void testFit_rodinPsx() throws Exception
    {
        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitPsx(
            "src/main/resources/test/Rodin/Mia_001239_Rodin_301.psx",
                   "Rodin/Processed dark 25",
            rmse ->
            {
                System.out.println("Encoded RMSE: " + rmse.getEncodedGroundTruth());
                System.out.println("Normalized sRGB RMSE: " + rmse.getNormalizedSRGB());
                System.out.println("Normalized linear RMSE: " + rmse.getNormalizedLinear());
            },
            "Rodin_psx");
        int numLoadedCameras = viewSet.getViewCount();
        assertEquals(90, numLoadedCameras);
        int numEnabledCameras = viewSet.getEnabledViewCount();
        assertEquals(90, numEnabledCameras);
        int numDisabledCameras = viewSet.getDisabledViewCount();
        assertEquals(0, numDisabledCameras);
    }

    @Test
    @DisplayName("Katrina Fuller fit, from Metashape export")
    void testFit_katrinafullerMetashape() throws Exception
    {
        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitMetashape(
            "KatrinaFuller/Mia_124131_KratinaFuller_168cameras.files.xml",
            "KatrinaFuller/Mia_124131_KratinaFuller_64k.obj",
            "KatrinaFuller/Downscaled25",
            rmse ->
            {
                System.out.println("Encoded RMSE: " + rmse.getEncodedGroundTruth());
                System.out.println("Normalized sRGB RMSE: " + rmse.getNormalizedSRGB());
                System.out.println("Normalized linear RMSE: " + rmse.getNormalizedLinear());
            },
            "KatrinaFuller_metashape");
        int numLoadedCameras = viewSet.getViewCount();
        assertEquals(168, numLoadedCameras);
        int numEnabledCameras = viewSet.getEnabledViewCount();
        assertEquals(168, numEnabledCameras);
        int numDisabledCameras = viewSet.getDisabledViewCount();
        assertEquals(0, numDisabledCameras);
    }

        @Test
    @DisplayName("Katrina Fuller fit, from Reality Capture export")
    void testFit_katrinafullerRealityCapture() throws Exception
    {
        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitMetashape(
            "KatrinaFuller/katrinafuller-realitycapture.csv",
            "KatrinaFuller/katrinafuller-realitycapture.obj",
            "KatrinaFuller/Downscaled25",
            rmse ->
            {
                System.out.println("Encoded RMSE: " + rmse.getEncodedGroundTruth());
                System.out.println("Normalized sRGB RMSE: " + rmse.getNormalizedSRGB());
                System.out.println("Normalized linear RMSE: " + rmse.getNormalizedLinear());
            },
            "KatrinaFuller_realitycapture");
        int numLoadedCameras = viewSet.getViewCount();
        assertEquals(158, numLoadedCameras);
        int numEnabledCameras = viewSet.getEnabledViewCount();
        assertEquals(158, numEnabledCameras);
        int numDisabledCameras = viewSet.getDisabledViewCount();
        assertEquals(0, numDisabledCameras);
    }

    private void testFitMetashape(String cameras, String geometry, String imageDirectory,
                                  Consumer<ReadonlyColorAppearanceRMSE> validation, String testName) throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        LoadOptionsModel imageLoadOptions = new ObservableLoadOptionsModel();
        imageLoadOptions.setColorImagesRequested(false); // don't generate/load preview images; not needed for this test
        // These are set since they otherwise are set in JavaFX related code
        TestingState state = new TestingState();
        GlobalBootstrap.initialize(state, imageLoadOptions);
        ImageBasedRenderableManager<OpenGLContext> mockIOHandler = new ImageBasedRenderableManager<>(context);
        mockIOHandler.setLoadedViewSet(viewSet); // Probably should find a better way to do this instead of using a new method for it
        Global.io().setLoadingHandler(mockIOHandler);

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
            viewSet = resources.getViewSet();
            testFit(resources, validation, testName);
        }
    }

    private void testFitPsx(String psxFile, String imageDirectory, Consumer<ReadonlyColorAppearanceRMSE> validation,
                            String testName) throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        LoadOptionsModel imageLoadOptions = new ObservableLoadOptionsModel();
        imageLoadOptions.setColorImagesRequested(false); // don't generate/load preview images; not needed for this test
        // These are set since they otherwise are set in JavaFX related code
        TestingState state = new TestingState();
        GlobalBootstrap.initialize(state, imageLoadOptions);
        ImageBasedRenderableManager<OpenGLContext> mockIOHandler = new ImageBasedRenderableManager<>(context);
        mockIOHandler.setLoadedViewSet(viewSet); // Probably should find a
        // better way to do this instead of using a new method for it
        Global.io().setLoadingHandler(mockIOHandler);

        MetashapeDocument doc = new MetashapeDocument(psxFile);
        MetashapeChunk chunk = doc.getSelectedChunk();
        MetashapeModel model = chunk.getSelectedModel();

        File imgDir = new File(classLoader.getResource("test/" + imageDirectory).toURI());
        model.getLoadPreferences().setFullResOverride(imgDir);

        try (GraphicsResourcesImageSpace<OpenGLContext> resources = GraphicsResourcesImageSpace.<OpenGLContext>getBuilderForContext(context)
            .setImageLoadOptions(imageLoadOptions)
            .setProgressMonitor(progressMonitor)
            .loadFromMetashapeModel(model)
            .create())
        {
            resources.calibrateLightIntensities();
            viewSet = resources.getViewSet();
            testFit(resources, validation, testName);
        }
    }

    private void testFit(ReadonlyImageBasedGraphicsResources<OpenGLContext> resources, Consumer<ReadonlyColorAppearanceRMSE> validation, String testName)
        throws IOException, UserCancellationException
    {
        File outputDirectory = new File(TEST_OUTPUT_DIR, testName);
        outputDirectory.mkdirs();

        GeneralSettingsModel settings = new SimpleGeneralSettingsModel();
        DefaultSettings.applyGlobalDefaults(settings);
        SpecularFitSettings params = new SpecularFitSettings(512, 512);
        params.getImageCacheSettings().setCacheParentDirectory(new File (outputDirectory, "cache"));

        // Perform the specular fit
        BasisAndTexturesOptimizationProcess specularFitProcess =
            new BasisAndTexturesOptimizationProcess(params, new BasisOptimizationSettings(), outputDirectory);
        specularFitProcess.optimizeFitWithCache(resources, progressMonitor);

        specularFitProcess.reconstructAll(resources,
            (view, rmse) -> validation.accept(rmse));
    }
}
