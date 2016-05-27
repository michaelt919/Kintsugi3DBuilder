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
package tetzlaff.interactive;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import tetzlaff.helpers.ErrorMessage;
import tetzlaff.helpers.ExceptionTranslator;
import tetzlaff.helpers.MessageBox;

/**
 * An interactive application consisting of pollable components that only fire under certain circumstances (i.e. events)
 * as well as a refreshable component that is updated as often as possible.
 * @author Michael Tetzlaff
 *
 */
public class InteractiveApplication
{
	private List<EventPollable> pollable;
	private Refreshable refreshable;
	private ExceptionTranslator exceptionTranslator;
	private static MessageBox userMessageBox = null;
	
	/**
	 * Creates a new interactive application.
	 * @param pollable The primary pollable component of the application.
	 * @param refreshable The refreshable component of the application.
	 */
	public InteractiveApplication(EventPollable pollable, Refreshable refreshable) 
	{
		this.pollable = new ArrayList<EventPollable>();
		this.pollable.add(pollable);
		this.refreshable = refreshable;
	}
	
	/**
	 * Sets the abstract message box in which error messages are to be placed.
	 * @param newMessageBox The message box.
	 */
	public static void setMessageBox(MessageBox newMessageBox)
	{
		userMessageBox = newMessageBox;
	}
	
	/**
	 * Gets the abstract message box in which error messages are to be placed.
	 * @return The message box.
	 */
	public static MessageBox getMessageBox()
	{
		return userMessageBox;
	}
	
	/**
	 * Sets a translator to be used to translate any exceptions that are thrown during execution into a more user-friendly error message.
	 * @param translator The translator to use.
	 */
	public void setExceptionTranslator(ExceptionTranslator translator)
	{
		this.exceptionTranslator = translator;
	}

	/**
	 * Adds an additional pollable component to the application.
	 * @param pollable The new pollable component to add.
	 */
	public void addPollable(EventPollable pollable)
	{
		this.pollable.add(pollable);		
	}
	
	/**
	 * Requests that the application save a description of its current state to a particular file for debugging purposes.
	 * This request may be either handled by writing to the specified file in the requested format, or may be ignored.
	 * @param fileFormat The desired format of the debug file to be written.
	 * @param file The debug file to write to.
	 */
	public void requestDebugDump(String fileFormat, File file)
	{
		refreshable.requestDebugDump(fileFormat, file);
	}
	
	/**
	 * Runs the application.  This will block the active thread until the application terminates.
	 */
	public void run()
	{
		int pollingTime = 0;
		int refreshTime = 0;
		
		this.refreshable.initialize();

		Date startTimestamp = new Date();
		Date timestampA = startTimestamp;
		
		System.out.println("Main loop started.");
		boolean shouldTerminate = false;		
		while (!shouldTerminate)
		{
			this.refreshable.refresh();
			if(this.refreshable.hasError())
			{
				Exception error = refreshable.getError();
				error.printStackTrace();

				if(userMessageBox != null && exceptionTranslator != null)
				{
					ErrorMessage errorMessage = exceptionTranslator.translate(error);
					userMessageBox.warning(errorMessage.title, errorMessage.message);
				}
			}
			
			Date timestampB = new Date();
			refreshTime += timestampB.getTime() - timestampA.getTime();
			for (EventPollable poller : pollable)
			{
				poller.pollEvents();
				shouldTerminate = shouldTerminate || poller.shouldTerminate();
			}
			timestampA = new Date();
			pollingTime += timestampA.getTime() - timestampB.getTime();
		}
		
		System.out.println("Main loop terminated.");
		System.out.println("Total time elapsed: " + (timestampA.getTime() - startTimestamp.getTime()) + " milliseconds");
		System.out.println("Time spent polling for events: " + pollingTime + " milliseconds");
		System.out.println("Time spent on refreshes: " + refreshTime + " milliseconds");

		this.refreshable.terminate();
	}
	
	/**
	 * Runs several applications simultaneously within the same thread.
	 * This will block the active thread until all applications terminate.
	 * @param apps
	 */
	public static void runSimultaneous(Iterable<InteractiveApplication> apps)
	{
		Collection<InteractiveApplication> activeApps = new ArrayList<InteractiveApplication>();
		for (InteractiveApplication app : apps)
		{
			app.refreshable.initialize();
			activeApps.add(app);
		}
		
		while(!activeApps.isEmpty())
		{
			Collection<InteractiveApplication> appsToTerminate = new ArrayList<InteractiveApplication>();
			for (InteractiveApplication app : activeApps)
			{
				boolean shouldTerminate = false;
				for (EventPollable poller : app.pollable)
				{
					shouldTerminate = shouldTerminate || poller.shouldTerminate();
				}
				
				if (shouldTerminate)
				{
					appsToTerminate.add(app);
				}
			}
			for (InteractiveApplication app : appsToTerminate)
			{
				app.refreshable.terminate();
			}
			activeApps.removeAll(appsToTerminate);
			
			for (InteractiveApplication app : activeApps)
			{
				app.refreshable.refresh();
			}

			for (InteractiveApplication app : activeApps)
			{
				for (EventPollable poller : app.pollable)
				{
					poller.pollEvents();
				}
			}
		}
	}
}
