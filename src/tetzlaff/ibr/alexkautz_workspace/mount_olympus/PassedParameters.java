package tetzlaff.ibr.alexkautz_workspace.mount_olympus;

import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.ibr.util.IBRRequestQueue;

public class PassedParameters {
    //local passedParameters

    private String name;

    private String filepath;

    private IBRRequestQueue<OpenGLContext> requestQueue;

    public IBRRequestQueue<OpenGLContext> getRequestQueue() {
        return requestQueue;
    }

    public void setRequestQueue(IBRRequestQueue<OpenGLContext> requestQueue) {
        this.requestQueue = requestQueue;
    }

    public static PassedParameters getPassedParameters() {
        return passedParameters;
    }

    public static void setPassedParameters(PassedParameters passedParameters) {
        PassedParameters.passedParameters = passedParameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    private PassedParameters(String filepath, String name) {
        this.filepath = filepath;
        this.name = name;
    }

    //static bridge

    private static PassedParameters passedParameters;

    public static void init(String filepath, String name){
        passedParameters = new PassedParameters(filepath, name);
    }

    public static PassedParameters get() {
        return passedParameters;
    }
}
