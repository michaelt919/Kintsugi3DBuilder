package tetzlaff.util;

import java.util.Objects;

import tetzlaff.gl.window.Key;
import tetzlaff.gl.window.ModifierKeys;

public class KeyPress
{
    private final Key key;
    private final ModifierKeys modifierKeys;

    public KeyPress(Key key, ModifierKeys modifierKeys)
    {
        this.key = key;
        this.modifierKeys = modifierKeys;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof KeyPress)
        {
            KeyPress otherMapping = (KeyPress)obj;
            return otherMapping.key == this.key && Objects.equals(otherMapping.modifierKeys, this.modifierKeys);
        }
        else
        {
            return false;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(40);

        if (modifierKeys.getShiftModifier())
        {
            builder.append("SHIFT-");
        }

        if (modifierKeys.getSuperModifier())
        {
            builder.append("SUPER-");
        }

        if (modifierKeys.getControlModifier())
        {
            builder.append("CTRL-");
        }

        if (modifierKeys.getAltModifier())
        {
            builder.append("ALT-");
        }

        builder.append(key);

        return builder.toString();
    }

    @Override
    public int hashCode()
    {
        int result = key.hashCode();
        result = 31 * result + modifierKeys.hashCode();
        return result;
    }
}
