package tetzlaff.ulf;

public interface ULFLoadingMonitor 
{
	void startLoading();
	void setMaximum(double maximum);
	void setProgress(double progress);
	void loadingComplete();
}
