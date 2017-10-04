package tetzlaff.ibrelight.tools;//Created by alexk on 7/24/2017.

import java.util.HashMap;
import java.util.Map;

import tetzlaff.util.KeyPress;
import tetzlaff.util.MouseMode;

public class ToolBindingModelImpl implements ToolBindingModel
{
    private final Map<MouseMode, DragToolType> dragTools = new HashMap<>(MouseMode.getMaxOrdinal());
    private final Map<KeyPress, KeyPressToolType> keyPressTools = new HashMap<>(KeyPressToolType.values().length);

    @Override
    public DragToolType getDragTool(MouseMode mode)
    {
        return this.dragTools.get(mode);
    }

    @Override
    public void setDragTool(MouseMode mode, DragToolType tool)
    {
        this.dragTools.put(mode, tool);
    }

    @Override
    public KeyPressToolType getKeyPressTool(KeyPress keyPress)
    {
        return this.keyPressTools.get(keyPress);
    }

    @Override
    public void setKeyPressTool(KeyPress keyPress, KeyPressToolType tool)
    {
        this.keyPressTools.put(keyPress, tool);
    }
}
