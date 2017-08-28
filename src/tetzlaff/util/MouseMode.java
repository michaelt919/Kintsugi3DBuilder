package tetzlaff.util;

import java.util.Objects;

import tetzlaff.gl.window.ModifierKeys;

public class MouseMode
{
    public static final int MAX_BUTTON_INDEX = 7;

    private final int buttonIndex;
    private final ModifierKeys modifierKeys;

    public MouseMode(int buttonIndex, ModifierKeys modifierKeys)
    {
        if (buttonIndex > MAX_BUTTON_INDEX)
        {
            throw new IllegalArgumentException("Button index too high.");
        }

        this.buttonIndex = buttonIndex;
        this.modifierKeys = modifierKeys;
    }

    public static int getMaxOrdinal()
    {
        return (MAX_BUTTON_INDEX << 4) | 0xF;
    }

    public static MouseMode fromOrdinal(int ordinal)
    {
        return new MouseMode(ordinal >>> 4,
            new ModifierKeys()
            {
                @Override
                public boolean getShiftModifier()
                {
                    return (ordinal & 1) != 0;
                }

                @Override
                public boolean getControlModifier()
                {
                    return (ordinal & 2) != 0;
                }

                @Override
                public boolean getAltModifier()
                {
                    return (ordinal & 4) != 0;
                }

                @Override
                public boolean getSuperModifier()
                {
                    return (ordinal & 8) != 0;
                }
            });
    }

    public int getOrdinal()
    {
        return  (modifierKeys.getShiftModifier()   ? 1 : 0) |
                (modifierKeys.getControlModifier() ? 2 : 0) |
                (modifierKeys.getAltModifier()     ? 4 : 0) |
                (modifierKeys.getSuperModifier()   ? 8 : 0) |
                (buttonIndex << 4);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof MouseMode)
        {
            MouseMode otherMapping = (MouseMode)obj;
            return otherMapping.buttonIndex == this.buttonIndex && Objects.equals(otherMapping.modifierKeys, this.modifierKeys);
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return this.getOrdinal();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(25);

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

        switch(buttonIndex)
        {
            case 0:
                builder.append("LMB");
                break;
            case 1:
                builder.append("RMB");
                break;
            case 2:
                builder.append("MMB");
                break;
            default:
                builder.append("MB");
                builder.append(buttonIndex);
                break;
        }

        return builder.toString();
    }
}
