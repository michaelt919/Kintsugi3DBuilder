package tetzlaff.ibrelight.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;

import tetzlaff.util.AbstractImage;

public interface LoadingHandler 
{
    void loadFromVSETFile(String id, File vsetFile, ReadonlyLoadOptionsModel loadOptions);
    void loadFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory,
        String primaryViewName, ReadonlyLoadOptionsModel loadOptions);

    Optional<AbstractImage> loadEnvironmentMap(File environmentMapFile) throws FileNotFoundException;
    void loadBackplate(File backplateFile) throws FileNotFoundException;

    void saveToVSETFile(File vsetFile) throws IOException;

    void unload();

    void setLoadingMonitor(LoadingMonitor loadingMonitor);

    DoubleUnaryOperator getLuminanceEncodingFunction();
    void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues);
    void applyLightCalibration();
}
