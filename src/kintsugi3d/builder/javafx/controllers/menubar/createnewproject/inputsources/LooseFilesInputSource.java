/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources;

import javafx.stage.FileChooser;
import kintsugi3d.builder.io.ViewSetReader;

import java.util.ArrayList;
import java.util.List;

public class LooseFilesInputSource implements InputSource{
    @Override
    public List<FileChooser.ExtensionFilter> getExtensionFilters() {
        List<FileChooser.ExtensionFilter> list = new ArrayList<>();
        list.add(new FileChooser.ExtensionFilter("Agisoft Metashape XML file", "*.xml"));
        list.add(new FileChooser.ExtensionFilter("Reality Capture CSV file", "*.csv"));
        return list;
    }

    @Override
    public ViewSetReader getCameraFileReader() {
        return null;
    }
}
