/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package tetzlaff.lightfield;

/**
 * A monitor that fires callbacks at key points during the loading of a light field.
 * @author Michael Tetzlaff
 *
 */
public interface LFLoadingMonitor 
{
	/**
	 * A callback fired when the light field starts to load.
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
