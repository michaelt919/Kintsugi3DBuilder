package tetzlaff.ibrelight.tools;//Created by alexk on 7/24/2017.

import tetzlaff.util.KeyPress;
import tetzlaff.util.MouseMode;

public interface ToolBindingModel
{
    DragToolType getDragTool(MouseMode mode);
    void setDragTool(MouseMode mode, DragToolType tool);

    KeyPressToolType getKeyPressTool(KeyPress keyPress);
    void setKeyPressTool(KeyPress keyPress, KeyPressToolType tool);
}
