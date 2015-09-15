package tetzlaff.ulf;

public interface ULFLoadingMonitor 
{
	void startLoading(double maximum);
	void setProgress(double progress);
	void loadingComplete();
}
