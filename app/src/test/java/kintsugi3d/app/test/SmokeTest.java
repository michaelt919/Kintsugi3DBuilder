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
import kintsugi3d.app.test.TestingProjectModel.ErrorMessage;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.GlobalBootstrap;
import kintsugi3d.builder.core.viewset.ViewSet;
import kintsugi3d.builder.fit.SpecularFitRequest;
import kintsugi3d.builder.fit.settings.ExportSettings;
import kintsugi3d.builder.io.ExportTexturesRequest;
import kintsugi3d.builder.io.LoadOptionsModel;
import kintsugi3d.builder.io.SimpleLoadOptionsModel;
import kintsugi3d.builder.io.ViewSetLoadOptions;
import kintsugi3d.builder.io.gltf.MaterialExporterFactory;
import kintsugi3d.builder.io.gltf.kintsugi3dviewer.Kintsugi3DViewerExporterFactory;
import kintsugi3d.builder.io.metashape.MetashapeChunk;
import kintsugi3d.builder.io.metashape.MetashapeDocument;
import kintsugi3d.builder.io.metashape.MetashapeModel;
import kintsugi3d.builder.io.usdz.USDZMetallicExporterFactory;
import kintsugi3d.builder.io.usdz.USDZSpecularExporterFactory;
import kintsugi3d.builder.preferences.GlobalUserPreferencesManager;
import kintsugi3d.builder.rendering.Rendering;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;
import kintsugi3d.builder.util.ApplicationFolders;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

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

    private static final int TEXTURE_RESOLUTION = 256;
    private static final int TEXTURE_SAMPLED_SIZE = 128;
    private static final int TEXTURE_SUBDIV_SIZE = 128;

    // Can be disabled i.e. for MacOS testing where rendering has to be on first thread.
    private static boolean renderThreadRequired = true;

    private static final AtomicReference<Throwable> LOG_ERROR = new AtomicReference<>();
    private static final TestingState TESTING_STATE = new TestingState();

    private static final AtomicBoolean LOAD_FINISHED = new AtomicBoolean(false);
    private static final AtomicBoolean EXPORT_FINISHED = new AtomicBoolean(false);
    private static final AtomicInteger PROCESSED_COUNTER = new AtomicInteger(0);
    private static final AtomicInteger SAVE_COUNTER = new AtomicInteger(0);
    private static final long TIMEOUT = TimeUnit.SECONDS.toNanos(600);
    
    private static final ClassLoader CLASS_LOADER = SmokeTest.class.getClassLoader();

    private enum ReloadMode
    {
        BOTH,
        FIRST_ONLY,
        SECOND_ONLY
    }

    private static class TestLogListener implements LogMessageListener
    {
        @Override
        public void newLogMessage(LogMessage logMessage)
        {
            // Only record errors, and only note the first error in a run.
            if (logMessage.getLogLevel() == Level.ERROR && LOG_ERROR.get() == null)
            {
                LOG_ERROR.set(new AssertionError("Error found in log message!", logMessage.getThrown()));
            }
        }
    }

    public static boolean isRenderThreadRequired()
    {
        return renderThreadRequired;
    }

    public static void setRenderThreadRequired(boolean renderThreadRequired)
    {
        SmokeTest.renderThreadRequired = renderThreadRequired;
    }


    @BeforeAll
    static void initialize()
    {
        LOG.debug("JUnit tests started.");

        // If the rendering thread is already started (i.e. MacOS requires it to be first thead),
        // then we don't want to start another one.
        if (renderThreadRequired)
        {
            preRenderThreadInitialize();
            RenderingBootstrap.startRenderThread(null, false);
        }

        LOG.debug("Waiting for rendering thread.");

        while (!Global.io().hasValidHandler())
        {
            Thread.onSpinWait();
        }

        LOG.debug("Rendering thread is ready.");

        setupListeners();

        LOG.debug("Listeners registered.");
    }

    public static void preRenderThreadInitialize()
    {
        // Same startup logging as Kintsugi3DBuilder.java
        LOG.debug("Logger initialized");
        GlobalUserPreferencesManager.getInstance().logStartupExceptions();
        ApplicationFolders.logApplicationDirectories();

        // Set system properties, etc. that need to be set before anything touches LWJGL
        // (which could happen indirectly i.e. by loading Kintsugi's matrix math classes)
        Configuration.DEBUG.set(true);
        Configuration.DEBUG_STREAM.set(System.out);

        // Attach log listener
        LogMessageListener logListener = new TestLogListener();
        TestingLogMessageAppender.getInstance().addListener(logListener);

        LoadOptionsModel imageLoadOptions = new SimpleLoadOptionsModel();
        imageLoadOptions.setColorImagesRequested(false); // Don't generate/load preview images; not needed for this test
        // These are set since they otherwise are set in JavaFX related code

        GlobalBootstrap.initialize(TESTING_STATE, imageLoadOptions);
    }

    @BeforeEach
    void setup()
    {
        LOG.debug("Entering test setup.");

        // Clear errors and warnings for next test.
        TESTING_STATE.clearErrorsAndWarnings();
        LOG_ERROR.set(null);

        // Make sure there is no project open.
        if (Global.io().getLoadedViewSet() != null)
        {
            closeAndWait();
        }

        // Cache clear doesn't consistently work (tested on MacOS)
//        LOG.debug("Clearing cache...");
//        Global.state().getCacheModel().requestClearCache();
//
//        LOG.debug("Waiting for cache to clear...");
//        waitFor(() -> Global.state().getCacheModel().getCacheSizeBytes() == 0, "cache clear");
//        LOG.debug("Cache clear complete.");

        LOG.debug("Finished test setup.");
    }

    @AfterEach
    void tearDown()
    {
        LOG.debug("Cleaning up after test...");

        // Make sure the project is closed at the end of each test.
        closeAndWait();

        LOG.debug("Finished cleaning up after test.");
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
        Global.io().projectClosedListeners().register(event -> LOG.info("Project Closed"));

        // Project processed listener
        Global.io().projectProcessedListeners().register(event ->
        {
            LOG.info("Project Processed");
            PROCESSED_COUNTER.incrementAndGet();
        });

        Global.io().projectSavedListeners().register(event ->
        {
            LOG.info("Project Saved");
            SAVE_COUNTER.incrementAndGet();
        });
    }

    private static void waitFor(BooleanSupplier finished, String operation)
    {
        List<ErrorMessage> errorsLive = TESTING_STATE.getErrors();
        int startingErrorCount = errorsLive.size();
        long start = System.nanoTime();
        while (!finished.getAsBoolean())
        {
            int currentErrorCount = errorsLive.size();
            if (currentErrorCount > startingErrorCount)
            {
                ErrorMessage firstError = errorsLive.get(startingErrorCount);
                throw new ReportedErrorException(
                    String.format("Error occurred during project %s: %s", operation, firstError.message),
                    firstError.throwable);
            }

            if (System.nanoTime() - start > TIMEOUT)
            {
                fail(String.format("Timed out waiting for project %s", operation));
            }

            Thread.onSpinWait();
        }
    }

    private static void waitFor(AtomicBoolean finished, String operation)
    {
        waitFor(finished::get, operation);
    }

    private static void waitForLoad()
    {
        waitFor(LOAD_FINISHED, "load");

        // The "load finished" signal is fired while it's still finishing up some stuff on the GPU
        // so we don't want to push forward until after the frame has finished.
        waitForNextFrame();
    }

    private static void waitForExport()
    {
        waitFor(EXPORT_FINISHED, "export");
    }

    private static void waitForNextFrame()
    {
        AtomicBoolean waiting = new AtomicBoolean(true);
        Rendering.runLater(() -> waiting.set(false));

        while (waiting.get())
        {
            Thread.onSpinWait();
        }
    }

    private static void closeAndWait()
    {
        LOG.debug("Closing project...");

        AtomicBoolean open = new AtomicBoolean(true);
        Global.io().closeProject(() -> open.set(false));

        LOG.debug("Waiting for project to close...");

        while (open.get())
        {
            Thread.onSpinWait();
        }

        LOG.debug("Project is now closed.");
    }

    private static void runSpecularFit()
    {
        // Initialize to Integer.MAX_VALUE to prevent early termination
        // while waiting for the initial counts to be determined.
        AtomicInteger originalProcessedCount = new AtomicInteger(Integer.MAX_VALUE);
        AtomicInteger originalSaveCount = new AtomicInteger(Integer.MAX_VALUE);

        // Wait for the graphics thread to finish up the current task before capturing the processed count.
        // (It is possible for processed to be set on load, which creates a race condition otherwise).
        Rendering.runLater(() ->
        {
            originalProcessedCount.set(PROCESSED_COUNTER.get());
            originalSaveCount.set(SAVE_COUNTER.get());

            GeneralSettingsModel projectSettings = Global.io().getLoadedViewSet().getProjectSettings();
            projectSettings.set("textureSize", TEXTURE_RESOLUTION);
            projectSettings.set("textureSampledSize", TEXTURE_SAMPLED_SIZE);
            projectSettings.set("textureSubdivSize", TEXTURE_SUBDIV_SIZE);
            SpecularFitRequest request = SpecularFitRequest.createBasisAndTexturesOptimizationRequest();
            Rendering.getRequestQueue().addGraphicsRequest(request);
        });

        LOG.debug("Waiting for processing to complete...");

        waitFor(() -> PROCESSED_COUNTER.get() > originalProcessedCount.get()
                && SAVE_COUNTER.get() > originalSaveCount.get(),
            "processing");

        LOG.debug("Waiting for next frame...");

        // Make sure the process has completely finished before proceeding.
        waitForNextFrame();

        LOG.debug("Done waiting.");
    }

    private static void saveProject() throws IOException
    {
        int originalSaveCount = SAVE_COUNTER.get();
        Global.io().saveProject();
        waitFor(() -> SAVE_COUNTER.get() > originalSaveCount, "save");
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

    private static void assertNoErrors()
    {
        // Check for user-facing (UI) errors and warnings
        int numErrors = TESTING_STATE.getErrors().size();
        assertEquals(0, numErrors);
        int numWarnings = TESTING_STATE.getWarnings().size();
        assertEquals(0, numWarnings);

        // Check whether the log contained any errors
        Throwable failure = LOG_ERROR.get();
        if (failure != null)
        {
            // Reset the error
            LOG_ERROR.set(null);
            fail(failure.getMessage(), failure);
        }
    }

    private static void verifyProject(int expected)
    {
        assertProjectLoaded();
        assertCameras(expected);
        assertNoErrors();
    }

    @Test
    @DisplayName("Rodin fit, from Metashape export")
    void testFit_rodinMetashape() throws URISyntaxException
    {
        testFitLooseFiles(
            "Rodin/Mia_001239_Rodin_90cameras.xml",
            "Rodin/Mia_001239_Rodin_64k.obj",
            "Rodin/Processed dark 25",
            "rodinMetashape");
        verifyProject(90);
    }

    @Test
    @DisplayName("Rodin fit, from Psx import")
    void testFit_rodinPsx() throws URISyntaxException
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
    void testFit_rodinReload() throws URISyntaxException
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
            ReportedErrorException.class,
            () -> testFitLooseFiles(
                "Rodin/Rodincameras_bad.xml",
                "Rodin/Mia_001239_Rodin_64k.obj",
                "Rodin/Processed dark 25",
                "rodinMetashape_expectedFailure"
            ));
    }

    @Test
    @DisplayName("Rodin fit, Reload_First Test")
    void testFit_rodinReloadFirst() throws URISyntaxException
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
    void testFit_rodinReloadSecond() throws URISyntaxException
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
    void testFit_rodinExport() throws URISyntaxException
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
    void testFit_rodinSpecExport() throws URISyntaxException
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
    void testFit_rodinMetallicExport() throws URISyntaxException
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
    void testFit_rodinReloadfromVset() throws URISyntaxException
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
    void testFit_rodinLightCalibration() throws URISyntaxException, IOException
    {
        File projectFile = new File(Objects.requireNonNull(CLASS_LOADER.getResource(
            "Rodin/Rodin_psx.k3d.files/Rodin_psx.k3d.vset")).toURI());

        testFitLightCalibration(
            projectFile,
            "Rodin/Processed dark 25"
        );

        assertEquals(Vector3.ZERO, Global.io().getLoadedViewSet().getLightPosition(0));
        assertEquals(new Vector3(29.082094f), Global.io().getLoadedViewSet().getLightIntensity(0));

        verifyProject(90);
    }

    @Test
    @DisplayName("Katrina Fuller fit, from Metashape export")
    void testFit_katrinaFullerMetashape() throws URISyntaxException
    {
        testFitLooseFiles(
            "KatrinaFuller/Mia_124131_KratinaFuller_168cameras.files.xml",
            "KatrinaFuller/Mia_124131_KratinaFuller_64k.obj",
            "KatrinaFuller/Downscaled25",
            "katrinaFullerMetashape");

        verifyProject(168);

        LOG.debug("Test passed all assertions.");
    }

    @Test
    @DisplayName("Katrina Fuller fit, from Reality Capture export")
    void testFit_katrinaFullerRealityCapture() throws URISyntaxException
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
    void testFit_katrinaFullerReload() throws URISyntaxException
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
    void testFit_katrinaFullerReloadFirst() throws URISyntaxException
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
    void testFit_katrinaFullerReloadSecond() throws URISyntaxException
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
    void testFit_katrinaFullerExport() throws URISyntaxException
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
    void testFit_katrinaFullerSpecExport() throws URISyntaxException
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
    void testFit_katrinaFullerMetallicExport() throws URISyntaxException
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
    void testFit_katrinaFullerReloadfromVset() throws URISyntaxException
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
    void testFit_katrinaFullerLightCalibration() throws URISyntaxException, IOException
    {
        File projectFile = new File(Objects.requireNonNull(CLASS_LOADER.getResource(
            "KatrinaFuller/KatrinaFuller_realitycapture.k3d.files/KatrinaFuller_realitycapture.k3d.vset")).toURI());

        testFitLightCalibration(
            projectFile,
            "KatrinaFuller/Downscaled25"
        );

        assertEquals(Vector3.ZERO, Global.io().getLoadedViewSet().getLightPosition(0));
        assertEquals(new Vector3(3242.3916f), Global.io().getLoadedViewSet().getLightIntensity(0));

        verifyProject(158);
    }

    private static void testFitLooseFiles(String cameras, String geometry, String imageDirectory, String testName)
        throws URISyntaxException
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
        waitForLoad();

        runSpecularFit();
    }
    
    private static void testFitPsx(File psxFile, String imageDirectory, String testName)
        throws URISyntaxException
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
        waitForLoad();

        runSpecularFit();
    }

    private static void testFitReload(File projectFile, String imageDirectory, ReloadMode mode)
        throws URISyntaxException
    {
        // --- FIRST LOAD ---
        LOAD_FINISHED.set(false);
        Global.io().loadExistingProject(projectFile);
        waitForLoad();

        File imgDir = new File(Objects.requireNonNull(CLASS_LOADER.getResource(imageDirectory)).toURI());
        Global.io().getLoadedViewSet().setFullResImageDirectory(imgDir);

        // --- FIRST PROCESS ---
        if (mode == ReloadMode.FIRST_ONLY || mode == ReloadMode.BOTH)
        {
            runSpecularFit();
        }

        // --- CLOSE PROJECT ---
        closeAndWait();

        // --- SECOND LOAD ---
        LOAD_FINISHED.set(false);
        Global.io().loadExistingProject(projectFile);
        waitForLoad();

        // --- SECOND PROCESS ---
        if (mode == ReloadMode.SECOND_ONLY || mode == ReloadMode.BOTH)
        {
            runSpecularFit();
        }
    }

    private static void testFitExport(
        File projectFile, String testName, String imageDirectory, MaterialExporterFactory exporterFactory)
        throws URISyntaxException
    {
        ExportSettings settings = new ExportSettings();
        settings.setExporterFactory(exporterFactory);
        settings.setShouldSaveTextures(true);

        // --- LOAD ---
        LOAD_FINISHED.set(false);
        Global.io().loadExistingProject(projectFile);
        waitForLoad();

        File imgDir =  new File(Objects.requireNonNull(CLASS_LOADER.getResource(imageDirectory)).toURI());
        Global.io().getLoadedViewSet().setFullResImageDirectory(imgDir);

        // --- PROCESS ---
        runSpecularFit();

        EXPORT_FINISHED.set(false);
        File newFile = new File("test-output/export-output",String.format("%s.k3d",testName));
        Rendering.getRequestQueue().addGraphicsRequest(new ExportTexturesRequest(newFile, settings, () ->
        {
            LOG.info("Project Exported");
            EXPORT_FINISHED.set(true);
        }));

        waitForExport();
    }

    private static void testFitLightCalibration(File projectFile, String imageDirectory)
        throws URISyntaxException, IOException
    {
        // --- LOAD ---
        LOAD_FINISHED.set(false);
        Global.io().loadExistingProject(projectFile);
        waitForLoad();

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

        saveProject();
    }

    private static final class ReportedErrorException extends RuntimeException
    {
        private static final long serialVersionUID = 3966816332863385595L;

        private ReportedErrorException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}
