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
package tetzlaff.lightfield.viewer;

import com.trolltech.qt.core.Qt.WindowModality;
import com.trolltech.qt.gui.QDialog;
import com.trolltech.qt.gui.QPixmap;
import com.trolltech.qt.gui.QWidget;

public class LFAboutQDialog extends QDialog {

	Ui_AboutDialog gui;
	
	public LFAboutQDialog(QWidget parent) {
		super(parent);
		
		gui = new Ui_AboutDialog();
		gui.setupUi(this);
		
		gui.iconLabel.setPixmap(new QPixmap("classpath:images#/icons/icon.png"));
		
		this.setWindowModality(WindowModality.ApplicationModal);
		this.setModal(true);
	}
}
