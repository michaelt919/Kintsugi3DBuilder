package tetzlaff.ibr.javafx.models;//Created by alexk on 7/24/2017.

import java.util.HashMap;
import java.util.Map;

import tetzlaff.ibr.tools.ToolBindingModel;
import tetzlaff.ibr.tools.ToolType;
import tetzlaff.util.MouseMode;

public class JavaFXToolBindingModel implements ToolBindingModel
{
    private final Map<MouseMode, ToolType> tools = new HashMap<>(MouseMode.getMaxOrdinal());

    @Override
    public ToolType getTool(MouseMode mode)
    {
        return this.tools.get(mode);
    }

    @Override
    public void setTool(MouseMode mode, ToolType tool)
    {
        this.tools.put(mode, tool);
    }
}
