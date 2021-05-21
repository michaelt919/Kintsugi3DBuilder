/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

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
