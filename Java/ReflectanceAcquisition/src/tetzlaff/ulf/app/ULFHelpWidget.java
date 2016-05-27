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
package tetzlaff.ulf.app;

import com.trolltech.qt.core.QUrl;
import com.trolltech.qt.gui.QTextBrowser;
import com.trolltech.qt.gui.QWidget;
import com.trolltech.qt.help.QHelpEngine;

public class ULFHelpWidget extends QTextBrowser {

	private QHelpEngine helpEngine;
	
	public ULFHelpWidget(QHelpEngine engine, QWidget parent)
	{
		super(parent);
		this.helpEngine = engine;
	}
	
	@Override
    public Object loadResource(int type, QUrl name)
    {
		// Intercept any resources that come from the help engine
    	if (name.scheme().equalsIgnoreCase("qthelp"))
    	{
            return helpEngine.fileData(name);
    	}
        else
        {
            return super.loadResource(type, name);
        }
    }

}
