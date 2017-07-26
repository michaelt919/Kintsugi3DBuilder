package tetzlaff.ibr.rendering2.tools;

import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.ibr.rendering.ImageBasedRendererList;
import tetzlaff.ibr.rendering2.Passer;
import tetzlaff.ibr.rendering2.Trigger;

public class ToolModel2 {

    private Passer<ImageBasedRendererList<OpenGLContext>> modelPasser;

    public ImageBasedRendererList<OpenGLContext> getModel(){
        if(modelPasser != null) return modelPasser.getObject();
        else return null;
    }

    public void setModelPasser(Passer<ImageBasedRendererList<OpenGLContext>> modelPasser) {
        this.modelPasser = modelPasser;
    }

    private Tool tool = Tool.LOOK;

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }
}
