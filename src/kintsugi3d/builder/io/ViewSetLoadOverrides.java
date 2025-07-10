package kintsugi3d.builder.io;

import java.io.File;
import java.util.UUID;

public class ViewSetLoadOverrides {
    public File projectRoot;

    public File geometryFile;
    public File fullResImageDirectory;
    public File supportingFilesDirectory;
    public File masksDirectory;

    public String primaryViewName;
    public double primaryViewRotation;

    public boolean needsUndistort;
    public UUID uuid;
}