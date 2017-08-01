package tetzlaff.ibr.rendering2;//Created by alexk on 7/24/2017.

import tetzlaff.ibr.gui2.controllers.menu_bar.IBRSettings3;
import tetzlaff.ibr.gui2.controllers.menu_bar.LoadSettings;
import tetzlaff.ibr.rendering2.to_sort.IBRLoadOptions2;
import tetzlaff.ibr.rendering2.to_sort.IBRSettings2;
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

    private final LoadSettings loadSettings = new LoadSettings();
    private final IBRSettings3 ibrSettings3 = new IBRSettings3();

    public LoadSettings getLoadSettings() {
        return loadSettings;
    }

    public IBRSettings3 getIbrSettings3() {
        return ibrSettings3;
    }

    @Override
    protected IBRSettings2 getSettings() {
        return ibrSettings3;
    }

    @Override
    protected IBRLoadOptions2 getLoadOptions() {
        return loadSettings;
    }
}
