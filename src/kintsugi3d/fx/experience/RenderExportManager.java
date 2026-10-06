/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.fx.experience;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.stage.Window;
import kintsugi3d.fx.core.JavaFXState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class RenderExportManager
{
    private static final Logger LOG = LoggerFactory.getLogger(RenderExportManager.class);

    private static final File RENDER_EXPORT_CLASS_DEFINITION_FILE = new File("render-exports.txt");

    private final List<RenderExport> renderExportList = new ArrayList<>(4);

    private BooleanExpression anyModalOpen;

    public boolean isAnyModalOpen()
    {
        return anyModalOpen.get();
    }

    public BooleanExpression getAnyModalOpenProperty()
    {
        return anyModalOpen;
    }

    public List<RenderExport> getList()
    {
        return renderExportList;
    }

    public void initialize(Window parentWindow, JavaFXState state)
    {
        if (RENDER_EXPORT_CLASS_DEFINITION_FILE.exists())
        {
            try (Scanner scanner = new Scanner(RENDER_EXPORT_CLASS_DEFINITION_FILE, StandardCharsets.UTF_8))
            {
                scanner.useLocale(Locale.ROOT);

                while (scanner.hasNext())
                {
                    String fxmlURL = scanner.next();

                    if (scanner.hasNextLine())
                    {
                        String menuName = scanner.nextLine().trim();
                        RenderExport renderExport = new RenderExport(fxmlURL, menuName);
                        renderExport.initialize(parentWindow, state);
                        renderExportList.add(renderExport);
                    }
                }
            }
            catch (IOException e)
            {
                LOG.error("Failed to find export classes file:", e);
            }
        }

        anyModalOpen = renderExportList.stream()
            .map(experience -> experience.getModal().getOpenProperty())
            .reduce(BooleanExpression::or)
            .orElse(new SimpleBooleanProperty(false));
    }
}
