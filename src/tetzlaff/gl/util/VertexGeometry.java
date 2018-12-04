package tetzlaff.gl.util;

import java.io.File;

import tetzlaff.gl.material.Material;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;

public interface VertexGeometry
{
    boolean hasTexCoords();
    boolean hasNormals();

    NativeVectorBuffer getVertices();
    NativeVectorBuffer getTexCoords();
    NativeVectorBuffer getNormals();
    NativeVectorBuffer getTangents();

    String getMaterialFileName();
    Material getMaterial();
    File getFilename();
    void setFilename(File filename);
}
