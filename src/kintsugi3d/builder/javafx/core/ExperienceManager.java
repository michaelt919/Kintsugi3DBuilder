/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.core;

import javafx.beans.binding.BooleanExpression;
import javafx.stage.Window;
import kintsugi3d.builder.javafx.experience.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ExperienceManager
{
    private final Map<String, Experience> experiences = new HashMap<>(16);
    private final ExportRenderManager exportRenderManager = new ExportRenderManager();

    private BooleanExpression anyModalOpen;

    private static final ExperienceManager INSTANCE = new ExperienceManager();

    private ExperienceManager()
    {
        experiences.put("CreateProject", new CreateProject());
        experiences.put("ObjectOrientation", new ObjectOrientation());
        experiences.put("LightCalibration", new LightCalibration());
        experiences.put("MaskOptions", new MaskOptions());
        experiences.put("ToneCalibration", new ToneCalibration());
        experiences.put("SpecularBasisFit", new SpecularBasisFit());
        experiences.put("SpecularTextureFit", new SpecularTextureFit());
        experiences.put("ExportModel", new ExportModel());
        experiences.put("Log", new Log());
        experiences.put("SystemSettings", new SystemSettings());
        experiences.put("About", new About());
        experiences.put("ReplaceModel", new ReplaceModel());
    }

    public static ExperienceManager getInstance()
    {
        return INSTANCE;
    }

    public void initialize(Window parentWindow, JavaFXState state)
    {
        for (Experience experience : experiences.values())
        {
            experience.initialize(parentWindow, state);
        }

        exportRenderManager.initialize(parentWindow, state);

        anyModalOpen = experiences.values().stream()
            .map(experience -> experience.getModal().getOpenProperty())
            .reduce(BooleanExpression::or).orElseThrow()
            .or(exportRenderManager.getAnyModalOpenProperty());
    }

    boolean isAnyModalOpen()
    {
        return anyModalOpen.get();
    }

    public BooleanExpression getAnyModalOpenProperty()
    {
        return anyModalOpen;
    }

    public Experience getExperience(String name)
    {
        return experiences.get(name);
    }

    public <ExperienceType extends Experience> ExperienceType getExperience(String name, Class<ExperienceType> experienceClass)
    {
        Experience experience = experiences.get(name);
        if (Objects.equals(experience.getClass(), experienceClass))
        {
            //noinspection unchecked
            return (ExperienceType)experience;
        }
        else
        {
            return null;
        }
    }

    public ExportRenderManager getExportRenderManager()
    {
        return exportRenderManager;
    }
}
