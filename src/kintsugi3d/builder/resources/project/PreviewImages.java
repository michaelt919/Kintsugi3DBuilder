package kintsugi3d.builder.resources.project;

import kintsugi3d.builder.core.DistortionProjection;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.util.ImageUndistorter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;

class PreviewImages
{
    private static final Logger LOG = LoggerFactory.getLogger(PreviewImages.class);

    private static final int MAX_THUMBNAIL_SIZE = 450;

    private final ViewSet viewSet;
    private final int viewIndex;
    private final ProgressMonitor progressMonitor;
    private final AtomicInteger finishedCount;
    private final AtomicInteger failedCount;

    private final int projectionIndex;

    private boolean missingPreview;
    private boolean missingThumbnail;

    private ImageHelper fullResImage;
    private BufferedImage undistortedPreviewImage;

    PreviewImages(int viewIndex, ViewSet viewSet, ProgressMonitor progressMonitor,
        AtomicInteger finishedCount, AtomicInteger failedCount)
    {
        this.viewSet = viewSet;
        this.viewIndex = viewIndex;
        this.progressMonitor = progressMonitor;
        this.finishedCount = finishedCount;
        this.failedCount = failedCount;

        projectionIndex = viewSet.getCameraProjectionIndex(viewIndex);

        File previewImageFile = viewSet.tryFindPreviewImageFile(viewIndex);
        File thumbnailImageFile = viewSet.tryFindThumbnailImageFile(viewIndex);

        // If the preview image doesn't exist but the preview width / height are zero (i.e. scaling off)
        // and undistortion isn't necessary, we don't need it.
        missingPreview = (previewImageFile == null || !previewImageFile.exists())
            && ((viewSet.getPreviewWidth() > 0 && viewSet.getPreviewHeight() > 0) || needsUndistortion());
        missingThumbnail = thumbnailImageFile == null || !thumbnailImageFile.exists();

        // If neither are missing, mark finished
        if (!missingPreview && !missingThumbnail)
        {
            logExists(previewImageFile);
            logExists(thumbnailImageFile);
            markCreated();
            progressMonitor.setProgress(finishedCount.get() + failedCount.get(),
                MessageFormat.format("Completed: {0} ({1}/{2})", viewSet.getImageFileName(viewIndex),
                    finishedCount.get() + failedCount.get(), viewSet.getCameraPoseCount()));

            fullResImage = null; // skip for faster loading
        }
        else
        {
            try
            {
                fullResImage = viewSet.loadFullResMaskedImage(viewIndex);
            }
            catch (RuntimeException | IOException ex)
            {
                LOG.error(ex.getMessage(), ex);
                failedCount.getAndAdd(1);
            }
        }
    }

    boolean fullResImageExists()
    {
        return fullResImage != null;
    }

    boolean hasMissingFiles()
    {
        return missingPreview || missingThumbnail;
    }

    boolean needsUndistortion()
    {
        return viewSet.getCameraProjection(projectionIndex) instanceof DistortionProjection;
    }

    <ContextType extends Context<ContextType>> void tryGenerateUndistortedPreviewImage(ContextType context)
    {
        try
        {
            LOG.info("Undistorting preview image {}", viewIndex);

            DistortionProjection distortion = (DistortionProjection) viewSet.getCameraProjection(projectionIndex);

            if (viewSet.getPreviewWidth() > 0 && viewSet.getPreviewHeight() > 0)
            {
                distortion = distortion.scaledTo(viewSet.getPreviewWidth(), viewSet.getPreviewHeight());
            }
            // If no preview width / height is specified, just use whatever was originally in the distortion model

            try (ImageUndistorter<?> undistort = new ImageUndistorter<>(context))
            {
                undistortedPreviewImage = undistort.undistort(fullResImage.getBufferedImage(), distortion);
            }
        }
        catch (IOException | RuntimeException ex)
        {
            // Failure to undistort
            LOG.error(ex.getMessage(), ex);
        }
    }


    /**
     * Uses graphics context to undistort if necessary, and resizes and saves as necessary.
     */
    <ContextType extends Context<ContextType>> void tryCreateMissingFiles(ContextType context)
    {
        if (needsUndistortion() && missingPreview && undistortedPreviewImage == null)
        {
            tryGenerateUndistortedPreviewImage(context);
        }

        tryCreateMissingFiles();
    }

    /**
     * Resizes and saves as necessary, but cannot undistort without graphics context
     */
    void tryCreateMissingFiles()
    {
        try
        {
            if (needsUndistortion())
            {
                if (missingPreview && undistortedPreviewImage == null)
                {
                    // Can't undistort without graphics context
                    markFailed();
                }
                else
                {
                    if (missingPreview)
                    {
                        ImageIO.write(undistortedPreviewImage, "PNG", viewSet.getPreviewImageFile(viewIndex));
                        logFinished(viewSet.getPreviewImageFile(viewIndex));
                    }

                    if (missingThumbnail)
                    {
                        Resolution thumbnailResolution = getThumbnailResolution();

                        // Thumbnail doesn't need undistortion.
                        fullResImage.saveAtResolution("PNG", viewSet.getThumbnailImageFile(viewIndex),
                            thumbnailResolution.width, thumbnailResolution.height);
                        logFinished(viewSet.getThumbnailImageFile(viewIndex));
                    }

                    markCreated();
                }
            }
            else
            {
                tryCreateMissingFilesNoUndistortion();
            }
        }
        catch (RuntimeException | IOException ex)
        {
            LOG.error(ex.getMessage(), ex);
            markFailed();
        }
    }

    private static class Resolution
    {
        public final int width;
        public final int height;

        public Resolution(int width, int height)
        {
            this.width = width;
            this.height = height;
        }
    }

    private Resolution getThumbnailResolution()
    {
        int fullWidth = fullResImage.getBufferedImage().getWidth();
        int fullHeight = fullResImage.getBufferedImage().getHeight();

        int thumbnailWidth, thumbnailHeight;
        if (fullWidth > fullHeight)
        {
            // Landscape: thumbnail size limited by width
            thumbnailWidth = MAX_THUMBNAIL_SIZE;
            thumbnailHeight = Math.round((float)MAX_THUMBNAIL_SIZE * (float)fullHeight / (float)fullWidth);
        }
        else
        {
            // Portrait: thumbnail size limited by height
            thumbnailHeight = MAX_THUMBNAIL_SIZE;
            thumbnailWidth = Math.round((float)MAX_THUMBNAIL_SIZE * (float)fullWidth / (float)fullHeight);
        }
        return new Resolution(thumbnailWidth, thumbnailHeight);
    }

    void tryCreateMissingFilesNoUndistortion()
    {
        try
        {
            // Fallback to simply resizing without undistorting
            // Use PNG to ensure losslessness
            if (missingPreview)
            {
                fullResImage.saveAtResolution("PNG", viewSet.getPreviewImageFile(viewIndex, "png"),
                    viewSet.getPreviewWidth(), viewSet.getPreviewHeight());
                logFinished(viewSet.getPreviewImageFile(viewIndex));
            }

            if (missingThumbnail)
            {
                Resolution thumbnailResolution = getThumbnailResolution();
                fullResImage.saveAtResolution("PNG", viewSet.getThumbnailImageFile(viewIndex, "png"),
                    thumbnailResolution.width, thumbnailResolution.height);
                logFinished(viewSet.getThumbnailImageFile(viewIndex));
            }

            markCreated();
        }
        catch (RuntimeException | IOException ex)
        {
            LOG.error(ex.getMessage(), ex);
            markFailed();
        }
    }

    private static void logExists(File file)
    {
        LOG.info("Skipping {} : Already exists", file);
    }

    private static void logFinished(File fileFinished)
    {
        LOG.info("Finished {}", fileFinished);
    }

    private void markCreated()
    {
        finishedCount.getAndAdd(1); // mark finished even if trivial

        if (hasMissingFiles())
        {
            progressMonitor.setProgress(finishedCount.get() + failedCount.get(),
                MessageFormat.format("Completed: {0} ({1}/{2})", viewSet.getImageFileName(viewIndex),
                    finishedCount.get() + failedCount.get(), viewSet.getCameraPoseCount()));

            missingPreview = false;
            missingThumbnail = false;
        }
    }

    private void markFailed()
    {
        failedCount.getAndAdd(1);
    }
}
