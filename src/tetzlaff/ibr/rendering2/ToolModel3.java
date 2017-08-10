package tetzlaff.ibr.rendering2;//Created by alexk on 7/24/2017.

import tetzlaff.ibr.gui2.controllers.menu_bar.IBRSettingsUIImpl;
import tetzlaff.ibr.gui2.controllers.menu_bar.LoadSettings;
import tetzlaff.ibr.rendering2.to_sort.IBRLoadOptions2;
import tetzlaff.ibr.rendering2.to_sort.IBRSettingsModel;
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
    private final IBRSettingsUIImpl ibrSettingsUIImpl = new IBRSettingsUIImpl();

    public LoadSettings getLoadSettings() {
        return loadSettings;
    }

    public IBRSettingsUIImpl getIbrSettingsUIImpl() {
        return ibrSettingsUIImpl;
    }

    @Override
    protected IBRSettingsModel getSettings() {
        return ibrSettingsUIImpl;
    }

    @Override
    protected IBRLoadOptions2 getLoadOptions() {
        return loadSettings;
    }
}
