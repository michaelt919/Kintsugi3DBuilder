package kintsugi3d.builder.javafx.experience;

import javafx.beans.binding.BooleanExpression;
import javafx.stage.Window;
import kintsugi3d.builder.javafx.JavaFXState;

public final class ExperienceManager
{
    // Modal window manager objects
    private final CreateProject createProject = new CreateProject();
    private final ObjectOrientation objectOrientation = new ObjectOrientation();
    private final LightCalibration lightCalibration = new LightCalibration();
    private final MaskOptions maskOptions = new MaskOptions();
    private final ToneCalibration toneCalibration = new ToneCalibration();
    private final SpecularFit specularFit = new SpecularFit();
    private final ExportModel exportModel = new ExportModel();
    private final Log log = new Log();
    private final SystemSettings systemSettings = new SystemSettings();
    private final About about = new About();

    private final ExportRenderManager exportRenderManager = new ExportRenderManager();

    private BooleanExpression anyModalOpen;

    private static final ExperienceManager INSTANCE = new ExperienceManager();

    private ExperienceManager()
    {
    }

    public static ExperienceManager getInstance()
    {
        return INSTANCE;
    }

    public void initialize(Window parentWindow, JavaFXState state)
    {
        createProject.initialize(parentWindow, state);
        objectOrientation.initialize(parentWindow, state);
        lightCalibration.initialize(parentWindow, state);
        maskOptions.initialize(parentWindow, state);
        toneCalibration.initialize(parentWindow, state);
        specularFit.initialize(parentWindow, state);
        exportModel.initialize(parentWindow, state);
        log.initialize(parentWindow, state);
        systemSettings.initialize(parentWindow, state);
        about.initialize(parentWindow, state);
        exportRenderManager.initialize(parentWindow, state);

        anyModalOpen = createProject.getModal().getOpenProperty()
            .or(objectOrientation.getModal().getOpenProperty())
            .or(lightCalibration.getModal().getOpenProperty())
            .or(maskOptions.getModal().getOpenProperty())
            .or(toneCalibration.getModal().getOpenProperty())
            .or(specularFit.getModal().getOpenProperty())
            .or(exportModel.getModal().getOpenProperty())
            .or(log.getModal().getOpenProperty())
            .or(systemSettings.getModal().getOpenProperty())
            .or(about.getModal().getOpenProperty())
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

    public CreateProject getCreateProject()
    {
        return createProject;
    }

    public ObjectOrientation getObjectOrientation()
    {
        return objectOrientation;
    }

    public LightCalibration getLightCalibration()
    {
        return lightCalibration;
    }

    public MaskOptions getMaskOptions()
    {
        return maskOptions;
    }

    public ToneCalibration getToneCalibration()
    {
        return toneCalibration;
    }

    public SpecularFit getSpecularFit()
    {
        return specularFit;
    }

    public ExportModel getExportModel()
    {
        return exportModel;
    }

    public Log getLog()
    {
        return log;
    }

    public SystemSettings getSystemSettings()
    {
        return systemSettings;
    }

    public About getAbout()
    {
        return about;
    }

    public ExportRenderManager getExportRenderManager()
    {
        return exportRenderManager;
    }
}
