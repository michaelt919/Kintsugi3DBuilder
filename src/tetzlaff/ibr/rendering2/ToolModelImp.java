package tetzlaff.ibr.rendering2;//Created by alexk on 7/24/2017.

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.stage.WindowEvent;
import tetzlaff.ibr.ControllableToolModel;
import tetzlaff.ibr.rendering2.tools2.ToolBox;


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

    private ObjectProperty<ToolBox.ToolType> tool = new SimpleObjectProperty<>(ToolBox.ToolType.ORBIT);

    public void setTool(ToolBox.ToolType tool) {
        this.tool.setValue(tool);
    }
    public ToolBox.ToolType getTool() {
        return tool.getValue();
    }
    public ObjectProperty<ToolBox.ToolType> toolProperty() {
        return tool;
    }
}
