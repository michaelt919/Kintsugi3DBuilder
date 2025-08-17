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
        experiences.put("SpecularFit", new SpecularFit());
        experiences.put("ExportModel", new ExportModel());
        experiences.put("Log", new Log());
        experiences.put("SystemSettings", new SystemSettings());
        experiences.put("About", new About());
    }

    static ExperienceManager getInstance()
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
