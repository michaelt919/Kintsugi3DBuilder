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

package kintsugi3d.app.test;

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
import kintsugi3d.builder.preferences.GlobalUserPreferencesManager;
import kintsugi3d.builder.rendering.Rendering;
import kintsugi3d.builder.util.ApplicationFolders;
import kintsugi3d.builder.util.OperatingSystem;
import kintsugi3d.builder.util.logging.LogMessage;
import kintsugi3d.builder.util.logging.LogMessageListener;
import kintsugi3d.gl.vecmath.Vector3;
import org.junit.jupiter.api.*;
import org.lwjgl.system.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class SmokeTest
{
    static
    {
        // Dynamically set the log directory based on the OS before instantiating a logger
        ApplicationFolders.makeLogDirectorySystemProperty();
    }

    private static final Logger LOG = LoggerFactory.getLogger(SmokeTest.class);
    private static final File TEST_OUTPUT_DIR = new File("test-output");

    private AtomicReference<Throwable> observerFailure;
    private TestingState testingState = new TestingState();

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static final AtomicBoolean LOAD_FINISHED = new AtomicBoolean(false);
    private static final AtomicBoolean CLOSED_FINISHED = new AtomicBoolean(false);
    private static final AtomicBoolean EXPORT_FINISHED = new AtomicBoolean(false);
    private static final AtomicInteger PROCESS_COUNTER = new AtomicInteger(0);
    private static final AtomicInteger SAVE_COUNTER = new AtomicInteger(0);
    private static final long TIMEOUT = TimeUnit.SECONDS.toNanos(300);
    
    private static final ClassLoader CLASS_LOADER = SmokeTest.class.getClassLoader();

    private enum ReloadMode
    {
        BOTH,
        FIRST_ONLY,
        SECOND_ONLY
    }

    private class TestLogListener implements LogMessageListener
    {
        @Override
        public void newLogMessage(LogMessage logMessage)
        {
            if (logMessage.getLogLevel() == Level.ERROR)
            {
                observerFailure.set(new AssertionError("Error found in log message!"));
            }
        }
    }

    @BeforeAll
    static void initialize()
    {
        // Same startup logging as Kintsugi3DBuilder.java
        LOG.debug("Logger initialized");
        GlobalUserPreferencesManager.getInstance().logStartupExceptions();
        ApplicationFolders.logApplicationDirectories();

        // Set system properties, etc. that need to be set before anything touches LWJGL
        // (which could happen indirectly i.e. by loading Kintsugi's matrix math classes)
        RenderingBootstrap.preInitialize();
    }

    @BeforeEach
    void setup()
    {
        // Initialize testing objects
        observerFailure = new AtomicReference<>();

        // Create testing State
        testingState = new TestingState();

        // Attach log listener
        LogMessageListener logListener = new TestLogListener();
        TestingLogMessageAppender.getInstance().addListener(logListener);

        initializeRendering();
    }

    @AfterEach
    void tearDown()
    {
        // Check for user-facing (UI) errors and warnings
        int numErrors = testingState.getErrors().size();
        assertEquals(0, numErrors);
        int numWarnings = testingState.getWarnings().size();
        assertEquals(0, numWarnings);

        // Check whether the log contained any errors
        Throwable failure = observerFailure.get();
        if (failure != null)
        {
            fail(failure.getMessage());
        }
    }

    private static void resetGlobalState()
    {
        IOModel io = Global.io();

        if (io.getLoadedViewSet() != null)
        {

            CLOSED_FINISHED.set(false);
            io.closeProject();


            while (!CLOSED_FINISHED.get()) {
                Thread.onSpinWait();
            }
        }
    }

    private void initializeRendering()
    {
        if (INITIALIZED.compareAndSet(false, true))
        {
            if (OperatingSystem.getCurrentOS() == OperatingSystem.MACOS)
            {
                // Mac OS requires glfw_async to inject GLFW calls into the "first thread".
                Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
            }

            LoadOptionsModel imageLoadOptions = new SimpleLoadOptionsModel();
            imageLoadOptions.setColorImagesRequested(false); // don't generate/load preview images; not needed for this test
            // These are set since they otherwise are set in JavaFX related code

            GlobalBootstrap.initialize(testingState, imageLoadOptions);
            RenderingBootstrap.startRenderThread(null, false);

            while (!Global.io().hasValidHandler())
            {
                Thread.onSpinWait();
            }

            setupListeners();
        }
        else
        {
            resetGlobalState();
        }
    }

    private static void setupListeners()
    {
        // Project Load listener
        Global.io().projectLoadedListeners().register(event ->
        {
            LOG.info("Project Loaded");
            LOAD_FINISHED.set(true);
        });

        // Project closed listener
        Global.io().projectClosedListeners().register(event ->
        {
            LOG.info("Project Closed");
            CLOSED_FINISHED.set(true);
        });

        // Project processed listener
        Global.io().projectProcessedListeners().register(event ->
        {
            LOG.info("Project Processed");
            PROCESS_COUNTER.incrementAndGet();
        });

        Global.io().projectSavedListeners().register(event ->
        {
            LOG.info("Project Saved");
            SAVE_COUNTER.incrementAndGet();
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

            if (System.nanoTime() - start > TIMEOUT)
            {
                throw new AssertionError(
                    "Timed out waiting for project" + operation);
            }

            Thread.onSpinWait();
        }
    }

    private static void waitForLoad(TestingState testingState)
    {
        waitFor(LOAD_FINISHED, testingState, "load");
    }

    private static void waitForClose(TestingState testingState)
    {
        waitFor(CLOSED_FINISHED, testingState, "close");
    }

    private static void waitForExport(TestingState testingState)
    {
        waitFor(EXPORT_FINISHED, testingState, "export");
    }

    private static void waitForProcess(int startingCount, TestingState testingState)
    {
        int startingErrorCount = testingState.getErrors().size();
        long start = System.nanoTime();
        while (PROCESS_COUNTER.get() == startingCount)
        {
            if (testingState.getErrors().size() > startingErrorCount) {
                throw new ExpectedFailureException("Error occurred during project process");
            }

            if (System.nanoTime() - start > TIMEOUT *2)
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
        while (SAVE_COUNTER.get() == startingCount)
        {
            if (testingState.getErrors().size() > startingErrorCount) {
                throw new ExpectedFailureException("Error occurred during project save");
            }

            if (System.nanoTime() - start > TIMEOUT)
            {
                throw new AssertionError(
                    "Timed out waiting for project save");
            }

            Thread.onSpinWait();
        }
    }

    private static void assertProjectLoaded()
    {
        assertNotNull(Global.io().getMainRenderable());
        assertNotNull(Global.io().getLoadedGeometry());
        assertNotNull(Global.io().getViewSetFile());
        assertNotNull(Global.io().getLoadOptionsModel());
        assertTrue(Global.io().hasLoadedRenderable());
    }

    private static void assertCameras(int expected)
    {
        int numLoadedCameras = Global.io().getLoadedViewSet().getViewCount();
        assertEquals(expected, numLoadedCameras);
        int numEnabledCameras = Global.io().getLoadedViewSet().getEnabledViewCount();
        assertEquals(expected, numEnabledCameras);
        int numDisabledCameras = Global.io().getLoadedViewSet().getDisabledViewCount();
        assertEquals(0, numDisabledCameras);
    }

    @Test
    @DisplayName("Rodin fit, from Metashape export")
    void testFit_rodinMetashape() throws URISyntaxException, InterruptedException
    {
        testFitLooseFiles(
            "Rodin/Mia_001239_Rodin_90cameras.xml",
            "Rodin/Mia_001239_Rodin_64k.obj",
            "Rodin/Processed dark 25",
            "rodinMetashape");
        verifyProject(90);
    }

    private static void verifyProject(int expected)
    {
        assertProjectLoaded();
        assertCameras(expected);
    }

    @Test
    @DisplayName("Rodin fit, from Psx import")
    void testFit_rodinPsx() throws URISyntaxException, InterruptedException
    {
        File psxFile = new File(
            Objects.requireNonNull(CLASS_LOADER.getResource("Rodin/Mia_001239_Rodin_301.psx")).toURI());

        testFitPsx(
            psxFile,
            "Rodin/Processed dark 25",
            "rodinPsx");
        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Reload_Both Test")
    void testFit_rodinReload() throws URISyntaxException, InterruptedException
    {
        File projectFile = new File(
            Objects.requireNonNull(CLASS_LOADER.getResource("Rodin/Rodin_psx.k3d")).toURI());

        testFitReload(
            projectFile,
            "Rodin/Processed dark 25",
            ReloadMode.BOTH);

        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Wrong Directory")
    void testFit_rodin_wrongDirectory()
    {
        assertThrows(
            NullPointerException.class,
            () -> testFitLooseFiles(
                "Rodin/Mia_001239_Rodin_90cameras.xml",
                "Rodin/Mia_001239_Rodin_64k.obj",
                "Rodin/DoesNotExist",
                "rodin_wrongDirectory"
            ));
    }

    @Test
    @DisplayName("Rodin fit, Wrong Camera File")
    void testFit_rodinMetashape_expectedFailure()
    {
        assertThrows(
            ExpectedFailureException.class,
            () -> testFitLooseFiles(
                "Rodin/Rodincameras_bad.xml",
                "Rodin/Mia_001239_Rodin_64k.obj",
                "Rodin/Processed dark 25",
                "rodinMetashape_expectedFailure"
            ));
    }

    @Test
    @DisplayName("Rodin fit, Reload_First Test")
    void testFit_rodinReloadFirst() throws URISyntaxException, InterruptedException
    {
        File projectFile = new File(
            Objects.requireNonNull(CLASS_LOADER.getResource("Rodin/Rodin_psx.k3d")).toURI());

        testFitReload(
            projectFile,
            "Rodin/Processed dark 25",
            ReloadMode.FIRST_ONLY);

        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Reload_Second Test")
    void testFit_rodinReloadSecond() throws URISyntaxException, InterruptedException
    {
        File projectFile = new File(
            Objects.requireNonNull(CLASS_LOADER.getResource("Rodin/Rodin_psx.k3d")).toURI());

        testFitReload(
            projectFile,
            "Rodin/Processed dark 25",
            ReloadMode.SECOND_ONLY);

        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Export Test")
    void testFit_rodinExport() throws URISyntaxException, InterruptedException
    {
        File projectFile = new File(
            Objects.requireNonNull(CLASS_LOADER.getResource("Rodin/Rodin_psx.k3d")).toURI());
        MaterialExporterFactory exporterFactory = Kintsugi3DViewerExporterFactory.getInstance();
        testFitExport(
            projectFile,
            "rodinExport",
            "Rodin/Processed dark 25",
            exporterFactory);

        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Specular Export Test")
    void testFit_rodinSpecExport() throws URISyntaxException, InterruptedException
    {
        File projectFile = new File(
            Objects.requireNonNull(CLASS_LOADER.getResource("Rodin/Rodin_psx.k3d")).toURI());
        MaterialExporterFactory exporterFactory = USDZSpecularExporterFactory.getInstance();
        testFitExport(
            projectFile,
            "rodinSpecExport",
            "Rodin/Processed dark 25",
            exporterFactory);

        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Metallic Export Test")
    void testFit_rodinMetallicExport() throws URISyntaxException, InterruptedException
    {
        File projectFile = new File(
            Objects.requireNonNull(CLASS_LOADER.getResource("Rodin/Rodin_psx.k3d")).toURI());
        MaterialExporterFactory exporterFactory = USDZMetallicExporterFactory.getInstance();
        testFitExport(
            projectFile,
            "rodinMetallicExport",
            "Rodin/Processed dark 25",
            exporterFactory);

        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Reload_Both Test")
    void testFit_rodinReloadfromVset() throws URISyntaxException, InterruptedException
    {
        File projectFile = new File(Objects.requireNonNull(CLASS_LOADER.getResource(
            "Rodin/Rodin_psx.k3d.files/Rodin_psx.k3d.vset")).toURI());

        testFitReload(
            projectFile,
            "Rodin/Processed dark 25",
            ReloadMode.BOTH);

        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, Light Calibration Test")
    void testFit_rodinLightCalibration() throws URISyntaxException, InterruptedException, IOException
    {
        File projectFile = new File(Objects.requireNonNull(CLASS_LOADER.getResource(
            "Rodin/Rodin_psx.k3d.files/Rodin_psx.k3d.vset")).toURI());

        testFitLightCalibration(
            projectFile,
            "Rodin/Processed dark 25"
        );

        verifyProject(90);

        assertEquals(Vector3.ZERO, Global.io().getLoadedViewSet().getLightPosition(0));
        assertEquals(new Vector3(29.082094f), Global.io().getLoadedViewSet().getLightIntensity(0));
    }

    @Test
    @DisplayName("Katrina Fuller fit, from Metashape export")
    void testFit_katrinaFullerMetashape() throws URISyntaxException, InterruptedException
    {
        testFitLooseFiles(
            "KatrinaFuller/Mia_124131_KratinaFuller_168cameras.files.xml",
            "KatrinaFuller/Mia_124131_KratinaFuller_64k.obj",
            "KatrinaFuller/Downscaled25",
            "katrinaFullerMetashape");

        verifyProject(168);
    }


    @Test
    @DisplayName("Katrina Fuller fit, from Reality Capture export")
    void testFit_katrinaFullerRealityCapture() throws URISyntaxException, InterruptedException
    {
        testFitLooseFiles(
            "KatrinaFuller/katrinafuller-realitycapture.csv",
            "KatrinaFuller/katrinafuller-realitycapture.obj",
            "KatrinaFuller/Downscaled25",
            "katrinaFullerRealityCapture");

        verifyProject(158);
    }

    @Test
    @DisplayName("Katrina Fuller fit, Reload_Both Test")
    void testFit_katrinaFullerReload() throws URISyntaxException, InterruptedException
    {
        File projectFile = new File(Objects.requireNonNull(CLASS_LOADER.getResource(
            "KatrinaFuller/KatrinaFuller_realitycapture.k3d")).toURI());

        testFitReload(
            projectFile,
            "KatrinaFuller/Downscaled25",
            ReloadMode.BOTH);

        verifyProject(158);
    }

    @Test
    @DisplayName("Katrina Fuller fit, Reload_First Test")
    void testFit_katrinaFullerReloadFirst() throws URISyntaxException, InterruptedException
    {
        File projectFile = new File(Objects.requireNonNull(CLASS_LOADER.getResource(
            "KatrinaFuller/KatrinaFuller_realitycapture.k3d")).toURI());

        testFitReload(
            projectFile,
            "KatrinaFuller/Downscaled25",
            ReloadMode.FIRST_ONLY);

        verifyProject(158);
    }

    @Test
    @DisplayName("Katrina Fuller fit, Reload_Second Test")
    void testFit_katrinaFullerReloadSecond() throws URISyntaxException, InterruptedException
    {
        File projectFile = new File(Objects.requireNonNull(CLASS_LOADER.getResource(
            "KatrinaFuller/KatrinaFuller_realitycapture.k3d")).toURI());

        testFitReload(
            projectFile,
            "KatrinaFuller/Downscaled25",
            ReloadMode.SECOND_ONLY);

        verifyProject(158);
    }

    @Test
    @DisplayName("Katrina Fuller fit, Export Test")
    void testFit_katrinaFullerExport() throws URISyntaxException, InterruptedException
    {
        File projectFile = new File(Objects.requireNonNull(CLASS_LOADER.getResource(
            "KatrinaFuller/KatrinaFuller_realitycapture.k3d")).toURI());
        MaterialExporterFactory exporterFactory = USDZSpecularExporterFactory.getInstance();
        testFitExport(
            projectFile,
            "katrinaFullerExport",
            "KatrinaFuller/Downscaled25",
            exporterFactory);

        verifyProject(158);
    }


    @Test
    @DisplayName("Katrina Fuller fit, Specular Export Test")
    void testFit_katrinaFullerSpecExport() throws URISyntaxException, InterruptedException
    {
        File projectFile = new File(Objects.requireNonNull(CLASS_LOADER.getResource(
            "KatrinaFuller/KatrinaFuller_realitycapture.k3d")).toURI());
        MaterialExporterFactory exporterFactory = USDZSpecularExporterFactory.getInstance();
        testFitExport(
            projectFile,
            "katrinaFullerSpecExport",
            "KatrinaFuller/Downscaled25",
            exporterFactory);

        verifyProject(158);
    }

    @Test
    @DisplayName("Katrina Fuller fit, Metallic Export Test")
    void testFit_katrinaFullerMetallicExport() throws URISyntaxException, InterruptedException
    {
        File projectFile = new File(Objects.requireNonNull(CLASS_LOADER.getResource(
            "KatrinaFuller/KatrinaFuller_realitycapture.k3d")).toURI());
        MaterialExporterFactory exporterFactory = USDZSpecularExporterFactory.getInstance();
        testFitExport(
            projectFile,
            "katrinaFullerMetallicExport",
            "KatrinaFuller/Downscaled25",
            exporterFactory);

        verifyProject(158);
    }

    @Test
    @DisplayName("Katrina Fuller fit, Reload_Vset Test")
    void testFit_katrinaFullerReloadfromVset() throws URISyntaxException, InterruptedException
    {
        File projectFile = new File(Objects.requireNonNull(CLASS_LOADER.getResource(
            "KatrinaFuller/KatrinaFuller_realitycapture.k3d.files/KatrinaFuller_realitycapture.k3d.vset")).toURI());

        testFitReload(
            projectFile,
            "KatrinaFuller/Downscaled25",
            ReloadMode.BOTH);

        verifyProject(158);
    }

    @Test
    @DisplayName("Katrina Fuller fit, Light Calibration Test")
    void testFit_katrinaFullerLightCalibration() throws URISyntaxException, InterruptedException, IOException
    {
        File projectFile = new File(Objects.requireNonNull(CLASS_LOADER.getResource(
            "KatrinaFuller/KatrinaFuller_realitycapture.k3d.files/KatrinaFuller_realitycapture.k3d.vset")).toURI());

        testFitLightCalibration(
            projectFile,
            "KatrinaFuller/Downscaled25"
        );

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
        Thread.sleep(5000);

        int countAfterLoad = PROCESS_COUNTER.get();
        specularFit();

        waitForProcess(countAfterLoad, testingState);
        waitForSave(SAVE_COUNTER.get(), testingState);
    }

    private void testFitLooseFiles(String cameras, String geometry, String imageDirectory, String testName)
        throws URISyntaxException, InterruptedException
    {
        ViewSetLoadOptions viewSetLoadOptions = new ViewSetLoadOptions();
        viewSetLoadOptions.geometryFile = new File(Objects.requireNonNull(CLASS_LOADER.getResource(geometry)).toURI());
        viewSetLoadOptions.mainDirectories.fullResImageDirectory =
            new File(Objects.requireNonNull(CLASS_LOADER.getResource(imageDirectory)).toURI());
        viewSetLoadOptions.mainDirectories.fullResImagesNeedUndistort = true;

        LOAD_FINISHED.set(false);
        File newFile = new File(TEST_OUTPUT_DIR,String.format("%s.k3d", testName));

        Global.io().loadFromLooseFiles(newFile, testName,
            new File(Objects.requireNonNull(CLASS_LOADER.getResource(cameras)).toURI()), viewSetLoadOptions);
        waitForLoad(testingState);

        runSpecularFit();
    }
    
    private void testFitPsx(File psxFile, String imageDirectory, String testName)
        throws URISyntaxException, InterruptedException
    {
        MetashapeDocument doc = new MetashapeDocument(psxFile.getAbsolutePath());
        MetashapeChunk chunk = doc.getSelectedChunk();
        MetashapeModel model = chunk.getSelectedModel();

        // setting the image directory
        File imgDir = new File(Objects.requireNonNull(CLASS_LOADER.getResource(imageDirectory)).toURI());
        model.getLoadPreferences().setFullResOverride(imgDir);

        LOAD_FINISHED.set(false);
        File newFile = new File(TEST_OUTPUT_DIR,String.format("%s.k3d", testName));
        Global.io().loadFromMetashapeModel(newFile, model);
        waitForLoad(testingState);

        runSpecularFit();
    }

    private void testFitReload(File projectFile, String imageDirectory, ReloadMode mode)
        throws URISyntaxException, InterruptedException
    {
        // --- FIRST LOAD ---
        LOAD_FINISHED.set(false);
        Global.io().loadExistingProject(projectFile);
        waitForLoad(testingState);

        File imgDir = new File(Objects.requireNonNull(CLASS_LOADER.getResource(imageDirectory)).toURI());
        Global.io().getLoadedViewSet().setFullResImageDirectory(imgDir);

        // --- FIRST PROCESS ---
        if (mode == ReloadMode.FIRST_ONLY || mode == ReloadMode.BOTH)
        {
            runSpecularFit();
        }

        // --- CLOSE PROJECT ---
        CLOSED_FINISHED.set(false);
        Global.io().closeProject();
        waitForClose(testingState);

        // --- SECOND LOAD ---
        LOAD_FINISHED.set(false);
        Global.io().loadExistingProject(projectFile);
        waitForLoad(testingState);

        // --- SECOND PROCESS ---
        if (mode == ReloadMode.SECOND_ONLY || mode == ReloadMode.BOTH)
        {
            runSpecularFit();
        }
    }

    private void testFitExport(
        File projectFile, String testName, String imageDirectory, MaterialExporterFactory exporterFactory)
        throws URISyntaxException, InterruptedException
    {
        ExportSettings settings = new ExportSettings();
        settings.setExporterFactory(exporterFactory);
        settings.setShouldSaveTextures(true);

        // --- LOAD ---
        LOAD_FINISHED.set(false);
        Global.io().loadExistingProject(projectFile);
        waitForLoad(testingState);

        File imgDir =  new File(Objects.requireNonNull(CLASS_LOADER.getResource(imageDirectory)).toURI());
        Global.io().getLoadedViewSet().setFullResImageDirectory(imgDir);

        // --- PROCESS ---
        runSpecularFit();

        File newFile = new File("test-output/export-output",String.format("%s.k3d",testName));
        Rendering.getRequestQueue().addGraphicsRequest(new ExportTexturesRequest(newFile, settings, () ->
        {
            LOG.info("Project Exported");
            EXPORT_FINISHED.set(true);
        }));

        waitForExport(testingState);
    }

    private void testFitLightCalibration(File projectFile, String imageDirectory)
        throws URISyntaxException, InterruptedException, IOException
    {
        // --- LOAD ---
        LOAD_FINISHED.set(false);
        Global.io().loadExistingProject(projectFile);
        waitForLoad(testingState);

        File imgDir =  new File(Objects.requireNonNull(CLASS_LOADER.getResource(imageDirectory)).toURI());
        Global.io().getLoadedViewSet().setFullResImageDirectory(imgDir);

        // Run with default project lights
        runSpecularFit();

        ViewSet viewSet = Global.io().getLoadedViewSet();

        assertNotNull(viewSet);
        assertTrue(viewSet.getLightCount() > 0);

        LOG.info("Light count: {}", viewSet.getLightCount());

        ArrayList<Vector3> origLightPositions = new ArrayList<>(1);
        ArrayList<Vector3> origLightIntensities = new ArrayList<>(1);

        // Save original light values
        for (int i = 0; i < viewSet.getLightCount(); i++)
        {
            LOG.info(
                "Orig. Light {} position: {}",
                i,
                viewSet.getLightPosition(i));
            origLightPositions.add(viewSet.getLightPosition(i));

            LOG.info(
                "Orig. Light {} intensity: {}",
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

            LOG.info(
                "Mod. Light {} position: {}",
                i,
                viewSet.getLightPosition(i));

            LOG.info(
                "Mod. Light {} intensity: {}",
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

        int startingCount = SAVE_COUNTER.get();
        Global.io().saveProject();

        Thread.sleep(5000);

        waitForSave(startingCount, testingState);
    }

    private static final class ExpectedFailureException extends RuntimeException
    {
        private static final long serialVersionUID = 3966816332863385595L;

        private ExpectedFailureException(String message)
        {
            super(message);
        }
    }
}
