package tetzlaff.ibr.rendering2;//Created by alexk on 7/24/2017.

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.ibr.rendering.ImageBasedRendererList;
import tetzlaff.ibr.rendering2.tools2.ToolBox;
import tetzlaff.mvc.models.ControllableToolModel;

public class ToolModel3 extends ControllableToolModel {
    private ToolBox.TOOL tool = ToolBox.TOOL.ORBIT;
    public void setTool(ToolBox.TOOL tool) {
        this.tool = tool;
    }
    public ToolBox.TOOL getTool() {
        return tool;
    }
}
