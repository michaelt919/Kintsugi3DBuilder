package kintsugi3d.builder.io;

import java.io.File;
import java.util.UUID;

public class ViewSetLoadOptions
{
    public ViewSetDirectories mainDirectories = new ViewSetDirectories();
    public File geometryFile;
    public File masksDirectory;
    public String orientationViewName;
    public double orientationViewRotation;
    public UUID uuid;
}