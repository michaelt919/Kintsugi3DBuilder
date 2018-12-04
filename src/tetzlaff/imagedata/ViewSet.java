package tetzlaff.imagedata;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;

import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface ViewSet
{
    NativeVectorBuffer getCameraPoseData();
    NativeVectorBuffer getCameraProjectionData();
    NativeVectorBuffer getCameraProjectionIndexData();
    NativeVectorBuffer getLightPositionData();
    NativeVectorBuffer getLightIntensityData();
    NativeVectorBuffer getLightIndexData();
    ViewSet createPermutation(Iterable<Integer> permutationIndices);
    void writeVSETFileToStream(OutputStream outputStream);
    void writeVSETFileToStream(OutputStream outputStream, Path parentDirectory);
    Matrix4 getCameraPose(int poseIndex);
    Matrix4 getCameraPoseInverse(int poseIndex);
    File getRootDirectory();
    void setRootDirectory(File rootDirectory);
    String getGeometryFileName();
    void setGeometryFileName(String fileName);
    File getGeometryFile();
    File getImageFilePath();
    String getRelativeImagePathName();
    void setRelativeImagePathName(String relativeImagePath);
    String getImageFileName(int poseIndex);
    File getImageFile(int poseIndex);
    int getPrimaryViewIndex();
    void setPrimaryView(int poseIndex);
    void setPrimaryView(String viewName);
    Projection getCameraProjection(int projectionIndex);
    int getCameraProjectionIndex(int poseIndex);
    Vector3 getLightPosition(int lightIndex);
    Vector3 getLightIntensity(int lightIndex);
    void setLightPosition(int lightIndex, Vector3 lightPosition);
    void setLightIntensity(int lightIndex, Vector3 lightIntensity);
    int getLightIndex(int poseIndex);
    int getCameraPoseCount();
    int getCameraProjectionCount();
    int getLightCount();
    float getRecommendedNearPlane();
    float getRecommendedFarPlane();
    float getGamma();
    boolean hasCustomLuminanceEncoding();
    SampledLuminanceEncoding getLuminanceEncoding();
    void setTonemapping(float gamma, double[] linearLuminanceValues, byte[] encodedLuminanceValues);
    boolean areLightSourcesInfinite();
    void setInfiniteLightSources(boolean infiniteLightSources);
}
