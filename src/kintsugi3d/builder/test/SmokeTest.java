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

import kintsugi3d.app.RenderingBootstrap;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.GlobalBootstrap;
import kintsugi3d.builder.core.viewset.ViewSet;
import kintsugi3d.builder.fit.SpecularFitRequest;
import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.io.*;
import kintsugi3d.builder.io.gltf.MaterialExporterFactory;
import kintsugi3d.builder.io.gltf.kintsugi3dviewer.Kintsugi3DViewerExporterFactory;
import kintsugi3d.builder.io.metashape.MetashapeChunk;
import kintsugi3d.builder.io.metashape.MetashapeDocument;
import kintsugi3d.builder.io.metashape.MetashapeModel;
import kintsugi3d.builder.io.usdz.USDZMetallicExporterFactory;
import kintsugi3d.builder.io.usdz.USDZSpecularExporterFactory;
import kintsugi3d.fx.controllers.modals.RecentLogMessageAppender;
import kintsugi3d.fx.internal.ObservableLoadOptionsModel;
import kintsugi3d.builder.rendering.Rendering;
import kintsugi3d.builder.util.logging.LogMessage;
import kintsugi3d.builder.util.logging.LogMessageListener;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.interactive.InitializationException;
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
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    private TestingState testingState = new TestingState();
    private static AtomicBoolean initialized = new AtomicBoolean(false);
    private static AtomicBoolean loadFinished = new AtomicBoolean(false);
    private static AtomicBoolean closedFinished = new AtomicBoolean(false);
    private static AtomicBoolean exportFinished = new AtomicBoolean(false);
    private static AtomicInteger processCounter = new AtomicInteger(0);
    private static AtomicInteger saveCounter = new AtomicInteger(0);
    private static long timeout = TimeUnit.SECONDS.toNanos(300);
    private enum ReloadMode  {
        BOTH,
        FIRST_ONLY,
        SECOND_ONLY
    }

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

        // Create Testing State
        testingState = new TestingState();
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

    private static void resetGlobalState()
    {
        IOModel io = Global.io();

        if (io.getLoadedViewSet() != null)
        {

            closedFinished.set(false);
            io.closeProject();


            while (!closedFinished.get()) {
                Thread.onSpinWait();
            }
        }
    }

    private static void initializeRendering(TestingState state)
    {
        if (initialized.compareAndSet(false, true))
        {
            LoadOptionsModel imageLoadOptions = new ObservableLoadOptionsModel();
            imageLoadOptions.setColorImagesRequested(false); // don't generate/load preview images; not needed for this test
            // These are set since they otherwise are set in JavaFX related code

            GlobalBootstrap.initialize(state, imageLoadOptions);

            Thread renderThread = new Thread(() -> {
                try {
                    RenderingBootstrap.runProgram();
                }
                catch (InitializationException e) {
                    throw new RuntimeException(e);
                }
            }, "render-thread");

            renderThread.start();

            while (!Global.io().hasValidHandler()) {
                Thread.onSpinWait();
            }

            setupListeners();
        }
        else
        {
            resetGlobalState();
        }
    }

    static void setupListeners() {

        // Project Load listener
        Global.io().projectLoadedListeners().register(event ->
        {
            System.out.println("Project Loaded");
            loadFinished.set(true);
        });

        // Project closed listener
        Global.io().projectClosedListeners().register(event ->
        {
            System.out.println("Project Closed");
            closedFinished.set(true);
        });

        // Project processed listener
        Global.io().projectProcessedListeners().register(event ->
        {
            System.out.println("Project Processed");
            processCounter.incrementAndGet();
        });

        Global.io().projectSavedListeners().register(event ->
        {
            System.out.println("Project Saved");
            saveCounter.incrementAndGet();
        });
    }



    private static void waitFor(AtomicBoolean finished, TestingState testingState, String operation)
    {
        int startingErrorCount = testingState.getErrors().size();
        long start = System.nanoTime();
        while (!finished.get())
        {
            if (testingState.getErrors().size() > startingErrorCount) {
                throw new ExpectedFailureException("Error occurred during project " + operation);
            }

            if (System.nanoTime() - start > timeout)
            {
                throw new AssertionError(
                    "Timed out waiting for project" + operation);
            }

            Thread.onSpinWait();
        }
    }

    private static void waitForLoad(TestingState testingState)
    {
        waitFor(loadFinished, testingState, "load");
    }

    private static void waitForClose(TestingState testingState)
    {
        waitFor(closedFinished, testingState, "close");
    }

    private static void waitForExport(TestingState testingState)
    {
        waitFor(exportFinished, testingState, "export");
    }

    private static void waitForProcess(int startingCount, TestingState testingState)
    {
        int startingErrorCount = testingState.getErrors().size();
        long start = System.nanoTime();
        while (processCounter.get() == startingCount)
        {
            if (testingState.getErrors().size() > startingErrorCount) {
                throw new ExpectedFailureException("Error occurred during project process");
            }

            if (System.nanoTime() - start > timeout*2)
            {
                throw new AssertionError(
                    "Timed out waiting for project process");
            }

            Thread.onSpinWait();
        }
    }

    private static void waitForSave(int startingCount, TestingState testingState)
    {
        int startingErrorCount = testingState.getErrors().size();
        long start = System.nanoTime();
        while (saveCounter.get() == startingCount)
        {
            if (testingState.getErrors().size() > startingErrorCount) {
                throw new ExpectedFailureException("Error occurred during project save");
            }

            if (System.nanoTime() - start > timeout)
            {
                throw new AssertionError(
                    "Timed out waiting for project save");
            }

            Thread.onSpinWait();
        }
    }



    private static void loadedChecks()
    {
        assertNotNull(Global.io().getMainRenderable());
        assertNotNull(Global.io().getLoadedGeometry());
        assertNotNull(Global.io().getLoadedViewSetFile());
        assertNotNull(Global.io().getLoadOptionsModel());
        assertTrue(Global.io().hasLoadedRenderable());
    }

    private static void cameraChecks(int expected)
    {
        int numLoadedCameras = Global.io().getLoadedViewSet().getViewCount();
        assertEquals(expected, numLoadedCameras);
        int numEnabledCameras = Global.io().getLoadedViewSet().getEnabledViewCount();
        assertEquals(expected, numEnabledCameras);
        int numDisabledCameras = Global.io().getLoadedViewSet().getDisabledViewCount();
        assertEquals(0, numDisabledCameras);
    }

    private void logChecks()
    {
        // Check UI errors and Warnings
        int numErrors = testingState.getErrors().size();
        assertEquals(0, numErrors);
        int numWarnings = testingState.getWarnings().size();
        assertEquals(0, numWarnings);
    }
    @Test
    @DisplayName("Rodin fit, from Metashape export")
    void testFit_rodinMetashape() throws Exception
    {
        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);

        testFitMetashape(
            "Rodin/Mia_001239_Rodin_90cameras.xml",
            "Rodin/Mia_001239_Rodin_64k.obj",
            "Rodin/Processed dark 25",
            "Rodin_metashape", testingState);
        verifyProject(90);
    }

    private void verifyProject(int expected)
    {
        cameraChecks(expected);

        loadedChecks();

        logChecks();
    }

    @Test
    @DisplayName("Rodin fit, from Psx import")
    void testFit_rodinPsx() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File psxFile = new File(classLoader.getResource("test/Rodin/Mia_001239_Rodin_301.psx").toURI());

        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitPsx(
            psxFile,
                   "Rodin/Processed dark 25",
            "Rodin_psx", testingState);
        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Reload_Both Test")
    void testFit_rodinReload() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/Rodin/Rodin_psx.k3d").toURI());

        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitReload(
            projectFile,
            "Rodin_reloaded",
            "Rodin/Processed dark 25",
            testingState, ReloadMode.BOTH);

        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Wrong Directory")
    void testFit_rodin_wrongDirectory()
    {
        assertThrows(
            NullPointerException.class,
            () -> testFitMetashape(
                "Rodin/Mia_001239_Rodin_90cameras.xml",
                "Rodin/Mia_001239_Rodin_64k.obj",
                "Rodin/DoesNotExist",
                "broken",
                testingState));
    }

    @Test
    @DisplayName("Rodin fit, Wrong Camera File")
    void testFit_rodinMetashape_expectedFailure() throws Exception
    {
        assertThrows(
            ExpectedFailureException.class,
            () -> testFitMetashape(
                "Rodin/Rodincameras.xml",
                "Rodin/Mia_001239_Rodin_64k.obj",
                "Rodin/Processed dark 25",
                "broken",
                testingState));
    }

    @Test
    @DisplayName("Rodin fit, Reload_First Test")
    void testFit_rodinReloadFirst() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/Rodin/Rodin_psx.k3d").toURI());

        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitReload(
            projectFile,
            "Rodin_reloadedFirst",
            "Rodin/Processed dark 25",
            testingState, ReloadMode.FIRST_ONLY);

        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Reload_Second Test")
    void testFit_rodinReloadSecond() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/Rodin/Rodin_psx.k3d").toURI());

        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitReload(
            projectFile,
            "Rodin_reloadedSecond",
            "Rodin/Processed dark 25",
            testingState, ReloadMode.SECOND_ONLY);

        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Export Test")
    void testFit_rodinExport() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/Rodin/Rodin_psx.k3d").toURI());
        MaterialExporterFactory exporterFactory = Kintsugi3DViewerExporterFactory.getInstance();
        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitExport(
            projectFile,
            "Rodin_k3dViewerExport",
            "Rodin/Processed dark 25",
            testingState, exporterFactory);

        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Specular Export Test")
    void testFit_rodinSpecExport() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/Rodin/Rodin_psx.k3d").toURI());
        MaterialExporterFactory exporterFactory = USDZSpecularExporterFactory.getInstance();
        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitExport(
            projectFile,
            "Rodin_specularExport",
            "Rodin/Processed dark 25",
            testingState, exporterFactory);

        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Metallic Export Test")
    void testFit_rodinMetallicExport() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/Rodin/Rodin_psx.k3d").toURI());
        MaterialExporterFactory exporterFactory = USDZMetallicExporterFactory.getInstance();
        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitExport(
            projectFile,
            "Rodin_metallicExport",
            "Rodin/Processed dark 25",
            testingState, exporterFactory);

        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Reload_Both Test")
    void testFit_rodinReloadfromVset() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/Rodin/Rodin_psx.k3d.files/Rodin_psx.k3d.vset").toURI());

        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitReload(
            projectFile,
            "Rodin_reloaded_fromVset",
            "Rodin/Processed dark 25",
            testingState, ReloadMode.BOTH);

        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Light Calibration Test")
    void testFit_rodinLightCalibration() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/Rodin/Rodin_psx.k3d.files/Rodin_psx.k3d.vset").toURI());

        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitLightCalibration(
            projectFile,
            "Rodin_lightCalibration",
            "Rodin/Processed dark 25",
            testingState);

        verifyProject(90);

        assertEquals(Vector3.ZERO, Global.io().getLoadedViewSet().getLightPosition(0));
        assertEquals(new Vector3(29.082094f), Global.io().getLoadedViewSet().getLightIntensity(0));
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
            "KatrinaFuller_metashape", testingState);

        verifyProject(168);
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
            "KatrinaFuller_realitycapture", testingState);

        verifyProject(158);
    }

    @Test
    @DisplayName("Katrina Fuller fit, Reload_Both Test")
    void testFit_katrinafullerReload() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/KatrinaFuller/KatrinaFuller_realitycapture.k3d").toURI());

        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);

        testFitReload(
            projectFile,
            "KatrinaFuller_reloaded",
            "KatrinaFuller/Downscaled25",
            testingState, ReloadMode.BOTH);

        verifyProject(158);
    }

    @Test
    @DisplayName("Katrina Fuller fit, Reload_First Test")
    void testFit_katrinafullerReloadFirst() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/KatrinaFuller/KatrinaFuller_realitycapture.k3d").toURI());

        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);

        testFitReload(
            projectFile,
            "KatrinaFuller_reloadedFirst",
            "KatrinaFuller/Downscaled25",
            testingState, ReloadMode.FIRST_ONLY);

        verifyProject(158);
    }

    @Test
    @DisplayName("Katrina Fuller fit, Reload_Second Test")
    void testFit_katrinafullerReloadSecond() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/KatrinaFuller/KatrinaFuller_realitycapture.k3d").toURI());

        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);

        testFitReload(
            projectFile,
            "KatrinaFuller_reloaded",
            "KatrinaFuller/Downscaled25",
            testingState, ReloadMode.SECOND_ONLY);

        verifyProject(158);
    }

    @Test
    @DisplayName("Katrina Fuller fit, Export Test")
    void testFit_katrinaFullerExport() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/KatrinaFuller/KatrinaFuller_realityCapture.k3d").toURI());
        MaterialExporterFactory exporterFactory = USDZSpecularExporterFactory.getInstance();
        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitExport(
            projectFile,
            "KatrinaFuller_k3dViewerExport",
            "KatrinaFuller/Downscaled25",
            testingState, exporterFactory);

        verifyProject(158);
    }


    @Test
    @DisplayName("Katrina Fuller fit, Specular Export Test")
    void testFit_katrinaFullerSpecExport() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/KatrinaFuller/KatrinaFuller_realityCapture.k3d").toURI());
        MaterialExporterFactory exporterFactory = USDZSpecularExporterFactory.getInstance();
        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitExport(
            projectFile,
            "KatrinaFuller_specularExport",
            "KatrinaFuller/Downscaled25",
            testingState, exporterFactory);

        verifyProject(158);
    }

    @Test
    @DisplayName("Katrina Fuller fit, Metallic Export Test")
    void testFit_katrinaFullerMetallicExport() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/KatrinaFuller/KatrinaFuller_realityCapture.k3d").toURI());
        MaterialExporterFactory exporterFactory = USDZSpecularExporterFactory.getInstance();
        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitExport(
            projectFile,
            "KatrinaFuller_metallicExport",
            "KatrinaFuller/Downscaled25",
            testingState, exporterFactory);

        verifyProject(158);
    }

    @Test
    @DisplayName("Katrina Fuller fit, Reload_Vset Test")
    void testFit_katrinafullerReloadfromVset() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/KatrinaFuller/KatrinaFuller_realitycapture.k3d.files/KatrinaFuller_realitycapture.k3d.vset").toURI());

        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);

        testFitReload(
            projectFile,
            "KatrinaFuller_reloaded_fromVset",
            "KatrinaFuller/Downscaled25",
            testingState, ReloadMode.BOTH);

        verifyProject(158);
    }

    @Test
    @DisplayName("Katrina Fuller fit, Light Calibration Test")
    void testFit_katrinaFullerLightCalibration() throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File projectFile = new File(classLoader.getResource("test/KatrinaFuller/KatrinaFuller_realitycapture.k3d.files/KatrinaFuller_realitycapture.k3d.vset").toURI());

        LogMessageListener logListener = new TestLogListener();
        RecentLogMessageAppender.getInstance().addListener(logListener);
        testFitLightCalibration(
            projectFile,
            "KatrinaFuller_lightCalibration",
            "KatrinaFuller/Downscaled25",
            testingState);

        verifyProject(158);

        assertEquals(Vector3.ZERO, Global.io().getLoadedViewSet().getLightPosition(0));
        assertEquals(new Vector3(3242.3916f), Global.io().getLoadedViewSet().getLightIntensity(0));
    }


    private static void specularFit()
    {
        Global.io().getLoadedViewSet().getProjectSettings().set("textureSize", 512);
        SpecularFitRequest request = SpecularFitRequest.createBasisAndTexturesOptimizationRequest();
        Rendering.getRequestQueue().addGraphicsRequest(request);
    }

    private void runSpecularFit() throws InterruptedException
    {
        // Wait for any in-flight load processing event
        Thread.sleep(50);

        int countAfterLoad = processCounter.get();
        specularFit();

        waitForProcess(countAfterLoad, testingState);
        waitForSave(saveCounter.get(), testingState);
    }

    private void testFitMetashape(String cameras, String geometry, String imageDirectory,
                                  String testName, TestingState state) throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        initializeRendering(state);

        ViewSetLoadOptions viewSetLoadOptions = new ViewSetLoadOptions();
        viewSetLoadOptions.geometryFile = new File(classLoader.getResource("test/" + geometry).toURI());
        viewSetLoadOptions.mainDirectories.fullResImageDirectory = new File(classLoader.getResource("test/" + imageDirectory).toURI());
        viewSetLoadOptions.mainDirectories.fullResImagesNeedUndistort = true;

        loadFinished.set(false);
        File newFile = new File("test-output",String.format("%s.k3d", testName));

        Global.io().loadFromLooseFiles(newFile, testName, new File(classLoader.getResource("test/" + cameras).toURI()), viewSetLoadOptions);

        waitForLoad(testingState);

        runSpecularFit();
    }




    private void testFitPsx(File psxFile, String imageDirectory,
                            String testName, TestingState state) throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        initializeRendering(state);

        MetashapeDocument doc = new MetashapeDocument(psxFile.getAbsolutePath());
        MetashapeChunk chunk = doc.getSelectedChunk();
        MetashapeModel model = chunk.getSelectedModel();

        // setting the image directory
        File imgDir = new File(classLoader.getResource("test/" + imageDirectory).toURI());
        model.getLoadPreferences().setFullResOverride(imgDir);

        loadFinished.set(false);
        File newFile = new File("test-output",String.format("%s.k3d", testName));
        Global.io().loadFromMetashapeModel(newFile, model);
        waitForLoad(testingState);

        runSpecularFit();
    }

    private void testFitReload(File projectFile ,String testName, String imageDirectory, TestingState state, ReloadMode mode)
        throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        initializeRendering(state);

        // --- FIRST LOAD ---
        loadFinished.set(false);
        Global.io().loadExistingProject(projectFile);
        waitForLoad(testingState);

        File imgDir =  new File(classLoader.getResource("test/" + imageDirectory).toURI());
        Global.io().getLoadedViewSet().setFullResImageDirectory(imgDir);

        // --- FIRST PROCESS ---
        if (mode == ReloadMode.FIRST_ONLY
            || mode == ReloadMode.BOTH)
        {
            runSpecularFit();
        }

        // --- CLOSE PROJECT ---
        closedFinished.set(false);
        Global.io().closeProject();
        waitForClose(testingState);

        // --- SECOND LOAD ---
        loadFinished.set(false);
        Global.io().loadExistingProject(projectFile);
        waitForLoad(testingState);

        // --- SECOND PROCESS ---
        if (mode == ReloadMode.SECOND_ONLY
            || mode == ReloadMode.BOTH)
        {
            runSpecularFit();
        }
    }

    private void testFitExport(File projectFile, String testName, String imageDirectory, TestingState state, MaterialExporterFactory exporterFactory) throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        ExportSettings settings = new ExportSettings();
        settings.setExporterFactory(exporterFactory);
        settings.setShouldSaveTextures(true);
        initializeRendering(state);

        // --- LOAD ---
        loadFinished.set(false);
        Global.io().loadExistingProject(projectFile);
        waitForLoad(testingState);

        File imgDir =  new File(classLoader.getResource("test/" + imageDirectory).toURI());
        Global.io().getLoadedViewSet().setFullResImageDirectory(imgDir);

        // --- PROCESS ---
        runSpecularFit();

        File newFile = new File("test-output/export-output",String.format("%s.k3d",testName));
        ExportTexturesRequest request = new ExportTexturesRequest(newFile, settings, () ->
        {
            System.out.println("Project Exported");
            exportFinished.set(true);
        });
        Rendering.getRequestQueue().addGraphicsRequest(request);

        waitForExport(testingState);
    }

    private void testFitLightCalibration(File projectFile, String testName, String imageDirectory, TestingState state) throws  Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        initializeRendering(state);

        // --- LOAD ---
        loadFinished.set(false);
        Global.io().loadExistingProject(projectFile);
        waitForLoad(testingState);

        File imgDir =  new File(classLoader.getResource("test/" + imageDirectory).toURI());
        Global.io().getLoadedViewSet().setFullResImageDirectory(imgDir);

        // Run with default project lights
        runSpecularFit();

        ViewSet viewSet = Global.io().getLoadedViewSet();

        assertNotNull(viewSet);
        assertTrue(viewSet.getLightCount() > 0);

        System.out.println(
            viewSet.getLightCount());

        ArrayList<Vector3> origLightPositions = new ArrayList<>(1);
        ArrayList<Vector3> origLightIntensities = new ArrayList<>(1);

        // Save original light values
        for (int i = 0; i < viewSet.getLightCount(); i++)
        {
            System.out.printf(
                "Orig. Light %d position: %s%n",
                i,
                viewSet.getLightPosition(i));
            origLightPositions.add(viewSet.getLightPosition(i));

            System.out.printf(
                "Orig. Light %d intensity: %s%n",
                i,
                viewSet.getLightIntensity(i));
            origLightIntensities.add(viewSet.getLightIntensity(i));
        }

        boolean hadCustomEncoding =
            viewSet.hasCustomLuminanceEncoding();

        double[] origLinear = null;
        byte[] origEncoded = null;

        if(hadCustomEncoding)
        {
            origLinear =
                viewSet.getLinearLuminanceValues();

            origEncoded =
                viewSet.getEncodedLuminanceValues();

            double[] modLinear =
                origLinear.clone();

            for (int i = 0; i < modLinear.length; i++)
            {
                modLinear[i] *= 1.05;
            }

            viewSet.setLuminanceEncoding(modLinear, origEncoded);
        }
        else
        {
            viewSet.setLuminanceEncoding(
                new double[]
                    {
                        0.0,
                        0.25,
                        0.5,
                        0.75,
                        1.0
                    },
                new byte[]
                    {
                        0,
                        64,
                        (byte)128,
                        (byte)192,
                        (byte)255
                    });
        }

        // Modify original light values
        for(int i = 0; i < viewSet.getLightCount(); i++)
        {
            Vector3 modifiedPos =
                origLightPositions.get(i).times(1.1f);
            viewSet.setLightPosition(i, modifiedPos);

            Vector3 modifiedInt =
                origLightIntensities.get(i).times(1.2f);
            viewSet.setLightIntensity(i, modifiedInt);

            System.out.printf(
                "Mod. Light %d position: %s%n",
                i,
                viewSet.getLightPosition(i));

            System.out.printf(
                "Mod. Light %d intensity: %s%n",
                i,
                viewSet.getLightIntensity(i));
        }

        runSpecularFit();

        // Restore the original values
        for (int i = 0; i < viewSet.getLightCount(); i++)
        {
            viewSet.setLightPosition(i, origLightPositions.get(i));
            viewSet.setLightIntensity(i, origLightIntensities.get(i));

            if (hadCustomEncoding)
            {
                viewSet.setLuminanceEncoding(
                    origLinear,
                    origEncoded);
            }
            else {
                viewSet.clearLuminanceEncoding();
            }
        }

        int startingCount = saveCounter.get();
        Global.io().saveProject();

       Thread.sleep(50);

        waitForSave(startingCount, state);
    }

    private static class ExpectedFailureException extends RuntimeException
    {
        public ExpectedFailureException(String message)
        {
            super(message);
        }
    }
}
