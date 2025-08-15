package kintsugi3d.builder.javafx.experience;

import java.io.IOException;

public class ExportRender extends ExperienceBase
{
    private final String fxmlURLString;
    private final String shortName;

    public ExportRender(String fxmlURLString, String shortName)
    {
        this.fxmlURLString = fxmlURLString;
        this.shortName = shortName;
    }

    public String getFxmlURLString()
    {
        return fxmlURLString;
    }

    public String getShortName()
    {
        return this.shortName;
    }

    @Override
    public String getName()
    {
        return String.format("Export: %s", shortName);
    }

    @Override
    public void open() throws IOException
    {
        openModal(fxmlURLString);
    }
}
