package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.util.MouseMode;

public interface ToolBindingModel
{
    ToolType getTool(MouseMode mode);
    void setTool(MouseMode mode, ToolType tool);
}
