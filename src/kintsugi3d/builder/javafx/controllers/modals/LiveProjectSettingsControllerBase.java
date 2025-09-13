package kintsugi3d.builder.javafx.controllers.modals;

public abstract class LiveProjectSettingsControllerBase extends ProjectSettingsControllerBase
{
    private final LiveProjectSettingsManager projectSettingsManager;

    protected LiveProjectSettingsControllerBase(LiveProjectSettingsManager projectSettingsManager)
    {
        super(projectSettingsManager);
        this.projectSettingsManager = projectSettingsManager;
    }

    protected LiveProjectSettingsControllerBase()
    {
        this(new LiveProjectSettingsManager());
    }

    @Override
    public boolean cancel()
    {
        if (super.cancel())
        {
            projectSettingsManager.cancel();
            return true;
        }
        else
        {
            return false;
        }
    }
}
