package kintsugi3d.builder.javafx.experience;

import javafx.stage.Window;
import kintsugi3d.builder.javafx.JavaFXState;

public class ExperienceManager
{
    // Modal window manager objects
    private final CreateProject createProject = new CreateProject();
    private final ObjectOrientation objectOrientation = new ObjectOrientation();
    private final LightCalibration lightCalibration = new LightCalibration();
    private final MaskOptions maskOptions = new MaskOptions();
    private final ToneCalibration toneCalibration = new ToneCalibration();
    private final Log log = new Log();

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
        log.initialize(parentWindow, state);
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

    public Log getLog()
    {
        return log;
    }
}
