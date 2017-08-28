package tetzlaff.gl.window;

public abstract class ModifierKeysBase implements ModifierKeys
{
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ModifierKeys)
        {
            ModifierKeys modifierKeys = (ModifierKeys) obj;
            return (modifierKeys.getShiftModifier()     || !this.getShiftModifier())
                && (modifierKeys.getControlModifier()   || !this.getControlModifier())
                && (modifierKeys.getAltModifier()       || !this.getAltModifier())
                && (modifierKeys.getSuperModifier()     || !this.getSuperModifier());
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return  (this.getShiftModifier()   ? 1 : 0) |
                (this.getControlModifier() ? 2 : 0) |
                (this.getAltModifier()     ? 4 : 0) |
                (this.getSuperModifier()   ? 8 : 0);
    }
}
