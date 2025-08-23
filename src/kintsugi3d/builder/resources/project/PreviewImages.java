package kintsugi3d.builder.resources.project;

import kintsugi3d.builder.core.DistortionProjection;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.gl.core.Context;
import kintsugi3d.util.ImageHelper;
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

    private final ViewSet viewSet;
    private final int viewIndex;
    private final ProgressMonitor progressMonitor;
    private final AtomicInteger finishedCount;
    private final AtomicInteger failedCount;

    private final int projectionIndex;

    private boolean missingPreview;
    private boolean missingThumbnail;

    private BufferedImage fullResImage;
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
                fullResImage = loadFullResMaskedImage(viewSet, viewIndex);
            }
            catch (RuntimeException | IOException ex)
            {
                LOG.error(ex.getMessage(), ex);
                failedCount.getAndAdd(1);
            }
        }
    }

    private static BufferedImage loadFullResMaskedImage( ViewSet viewSet, int i) throws IOException
    {
        ImageHelper source = new ImageHelper(viewSet.findFullResImageFile(i));

        File maskFile = viewSet.getMask(i);
        if (maskFile != null && maskFile.exists())
        {
            source.setAlphaMask(maskFile);
        }

        return source.getBufferedImage();
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
                undistortedPreviewImage = undistort.undistort(fullResImage, distortion);
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
                        // Thumbnail doesn't need undistortion.
                        ImageHelper helper = new ImageHelper(fullResImage);
                        helper.saveAtResolution(viewSet.getThumbnailImageFile(viewIndex),
                            viewSet.getThumbnailWidth(), viewSet.getThumbnailHeight());
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

    void tryCreateMissingFilesNoUndistortion()
    {
        try
        {
            // Fallback to simply resizing without undistorting
            ImageHelper helper = new ImageHelper(fullResImage);

            if (missingPreview)
            {
                helper.saveAtResolution(viewSet.getPreviewImageFile(viewIndex),
                    viewSet.getPreviewWidth(), viewSet.getPreviewHeight());
                logFinished(viewSet.getPreviewImageFile(viewIndex));
            }

            if (missingThumbnail)
            {
                helper.saveAtResolution(viewSet.getThumbnailImageFile(viewIndex),
                    viewSet.getThumbnailWidth(), viewSet.getThumbnailHeight());
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
        if (hasMissingFiles())
        {
            finishedCount.getAndAdd(1);

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
