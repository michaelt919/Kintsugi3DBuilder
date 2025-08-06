/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.util;

import javafx.fxml.FXMLLoader;
import javafx.stage.Window;
import kintsugi3d.builder.javafx.controllers.paged.Confirmable;
import kintsugi3d.builder.javafx.controllers.paged.Page;
import kintsugi3d.builder.javafx.controllers.paged.PageControllerBase;
import kintsugi3d.builder.javafx.controllers.paged.PageFrameController;
import kintsugi3d.util.Flag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.BiFunction;

public class PageWindow
{
    private static final Logger log = LoggerFactory.getLogger(PageWindow.class);

    private final Flag windowOpen = new Flag(false);

    public boolean isOpen()
    {
        return windowOpen.get();
    }

    public void open(Window parentWindow, String title,
        String firstPageFXMLPath, BiFunction<String, FXMLLoader, ? extends Page<?>> firstPageConstructor,
        Runnable initCallback, Runnable confirmCallback)
    {
        if (windowOpen.get())
        {
            return;
        }

        File fxmlFilesDirectory = new File("fxml-index.txt");

        if (!fxmlFilesDirectory.exists()){
            log.error("Failed to open fxml files directory for \"{}\" process.", title);
            return;
        }

        try (Scanner scanner = new Scanner(fxmlFilesDirectory, StandardCharsets.UTF_8))
        {
            scanner.useLocale(Locale.US);

            String hostFXMLPath = "fxml/PageFrame.fxml";
            PageFrameController frameController =
                WindowUtilities.makeWindow(parentWindow, title, windowOpen, hostFXMLPath);

            frameController.setPageFactory(loader ->
            {
                try
                {
                    loader.load();

                    PageControllerBase<?> controller = loader.getController();

                    if (controller instanceof Confirmable && ((Confirmable) controller).canConfirm())
                    {
                        controller.setConfirmCallback(confirmCallback);
                    }
                }
                catch (IOException e)
                {
                    log.error("Could not find fxml files for \"{}\" process.", title, e);
                }

                return loader;
            });

            Page<?> firstPage = frameController.createPage(firstPageFXMLPath, firstPageConstructor);
            frameController.setCurrentPage(firstPage);
            frameController.init();

            if (initCallback != null)
            {
                initCallback.run();
            }
        }
        catch (IOException e)
        {
            log.error("Could not find fxml files for \"{}\" process.", title, e);
        }
    }
}
