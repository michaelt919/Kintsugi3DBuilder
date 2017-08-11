package tetzlaff.ibr.rendering2;//Created by alexk on 7/24/2017.

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.stage.WindowEvent;
import tetzlaff.ibr.gui2.controllers.menu_bar.IBRSettingsUIImpl;
import tetzlaff.ibr.gui2.controllers.menu_bar.LoadSettings;
import tetzlaff.ibr.rendering2.tools2.ToolBox;
import tetzlaff.mvc.models.ControllableToolModel;


public class ToolModelImp extends ControllableToolModel {

    private EventHandler<WindowEvent> closeEvent;
    public void setCloseEvent(EventHandler<WindowEvent> closeEvent) {
        this.closeEvent = closeEvent;
    }
    @Override
    protected void requestGUIClose() {
        if (closeEvent != null) {
            closeEvent.handle(null);
        }
    }

    private ObjectProperty<ToolBox.TOOL> tool = new SimpleObjectProperty<>(ToolBox.TOOL.ORBIT);

    public void setTool(ToolBox.TOOL tool) {
        this.tool.setValue(tool);
    }
    public ToolBox.TOOL getTool() {
        return tool.getValue();
    }
    public ObjectProperty<ToolBox.TOOL> toolProperty() {
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
    protected IBRLoadOptions getLoadOptions() {
        return loadSettings;
    }
}
