package tetzlaff.ibr.rendering2;//Created by alexk on 7/24/2017.

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
