package kintsugi3d.builder.resources.project;

import kintsugi3d.builder.core.DefaultProgressMonitor;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.UserCancellationException;
import kintsugi3d.builder.core.ViewSet;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PreviewImageGenerator
{
    private final ViewSet viewSet;
    private final ProgressMonitor progressMonitor;

    private final AtomicInteger finishedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    AtomicReference<UserCancellationException> cancelled = new AtomicReference<>(null);

    static PreviewImageGenerator start(ViewSet viewSet)
    {
        return start(viewSet, new DefaultProgressMonitor());
    }

    static PreviewImageGenerator start(ViewSet viewSet, ProgressMonitor progressMonitor)
    {
        if (Objects.equals(viewSet.getRelativePreviewImagePathName(), viewSet.getRelativeFullResImagePathName()))
        {
            throw new IllegalStateException("Preview directory is the same as the full res directory; generating preview images would overwrite full resolution images.");
        }

        viewSet.getPreviewImageDirectory().mkdirs(); // Create preview directory
        new File(viewSet.getSupportingFilesDirectory(), "thumbnails").mkdirs(); // Create thumbnail directory

        progressMonitor.setMaxProgress(viewSet.getCameraPoseCount());

        return new PreviewImageGenerator(viewSet, progressMonitor);
    }

    private PreviewImageGenerator(ViewSet viewSet, ProgressMonitor progressMonitor)
    {
        this.viewSet = viewSet;
        this.progressMonitor = progressMonitor;
    }

    PreviewImages forView(int viewIndex)
    {
        return new PreviewImages(viewIndex, viewSet, progressMonitor, finishedCount, failedCount);
    }

    int getViewCount()
    {
        return viewSet.getCameraPoseCount();
    }

    void allowUserCancellation() throws UserCancellationException
    {
        try
        {
            progressMonitor.allowUserCancellation();
        }
        catch (UserCancellationException e)
        {
            cancelled.set(e); // forward exception to another thread
            throw e;
        }
    }

    void waitAndFinish() throws IOException, UserCancellationException
    {
        // Wait for all threads to finish
        while (cancelled.get() == null && finishedCount.get() + failedCount.get() < viewSet.getCameraPoseCount())
        {
            Thread.onSpinWait();
        }

        if (cancelled.get() != null)
        {
            throw cancelled.get();
        }
        else if (failedCount.get() > 0)
        {
            throwUndistortFailed(progressMonitor, failedCount.get());
        }
        else
        {
            // Generating preview images is now complete.
            // Go back to indeterminate progress until it starts to actually load for rendering
            progressMonitor.setMaxProgress(0.0);
        }
    }

    private static void throwUndistortFailed(ProgressMonitor progressMonitor, int failedCount) throws IOException
    {
        IOException e = new IOException("Failed to undistort " + failedCount + " images");
        progressMonitor.warn(e);

        // Generating preview images partially failed, but we'll try to load the rest of the project.
        // Go back to indeterminate progress until it starts to actually load for rendering
        progressMonitor.setMaxProgress(0.0);

        throw e;
    }
}
