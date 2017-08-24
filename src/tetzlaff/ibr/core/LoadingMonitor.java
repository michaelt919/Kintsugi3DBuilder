package tetzlaff.ibr.core;

/**
 * A monitor that fires callbacks at key points during the loading of an unstructured light field.
 * @author Michael Tetzlaff
 *
 */
public interface LoadingMonitor 
{
    /**
     * A callback fired when the unstructured light field starts to load.
     * This will always be fired before setProgress() or loadingComplete().
     */
    void startLoading();

    /**
     * A callback fired when the size of the total workload is determined.
     * setProgress() will never indicate a progress value higher than the maximum,
     * and when the progress value reaches the maximum, loadingComplete() should be fired shortly afterward.
     * @param maximum The maximum progress value.
     */
    void setMaximum(double maximum);

    /**
     * A callback fired when a progress checkpoint is reached.
     * This will always be fired between startLoading() and loadingComplete().
     * @param progress The amount of progress that has occurred.
     */
    void setProgress(double progress);

    /**
     * A callback fired when loading is complete.
     * This will always be fired after startLoading() and setProgress() will never be fired after this without startLoading() being fired once again.
     */
    void loadingComplete();
}
