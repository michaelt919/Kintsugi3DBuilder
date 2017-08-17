package tetzlaff.ibr.javafx.models;//Created by alexk on 7/24/2017.

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import tetzlaff.ibr.tools.ToolSelectionModel;
import tetzlaff.ibr.tools.ToolType;

public class JavaFXToolSelectionModel implements ToolSelectionModel
{
    private final ObjectProperty<ToolType> tool = new SimpleObjectProperty<>(ToolType.ORBIT);

    @Override
    public void setTool(ToolType tool)
    {
        this.tool.setValue(tool);
    }

    @Override
    public ToolType getTool()
    {
        return tool.getValue();
    }

    public ObjectProperty<ToolType> toolProperty()
    {
        return tool;
    }
}
