package tetzlaff.ibr.export.general;

public enum LoopMode
{
    SINGLE_FRAME("Render once"),
    MULTIFRAME("Render a certain number of frames"),
    MULTIVIEW("Render for each view in the model's view set"),
    MULTIVIEW_RETARGET("Render for each view in another target view set");

    private final String description;

    LoopMode(String description)
    {
        this.description = description;
    }

    @Override
    public String toString()
    {
        return description;
    }
}
