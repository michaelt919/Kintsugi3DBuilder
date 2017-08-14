package tetzlaff.ibr.javafx.models;//Created by alexk on 7/24/2017.

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import tetzlaff.ibr.tools.ToolBox;
import tetzlaff.ibr.tools.ToolSelectionModel;

public class JavaFXToolSelectionModel implements ToolSelectionModel 
{
    private ObjectProperty<ToolBox.ToolType> tool = new SimpleObjectProperty<>(ToolBox.ToolType.ORBIT);

    public void setTool(ToolBox.ToolType tool) 
    {
        this.tool.setValue(tool);
    }
    public ToolBox.ToolType getTool() 
    {
        return tool.getValue();
    }
    public ObjectProperty<ToolBox.ToolType> toolProperty() 
    {
        return tool;
    }
}
