package tetzlaff.gl.window;

public interface ModifierKeys
{
    ModifierKeys NONE = new ModifierKeysBase()
    {
        @Override
        public boolean getShiftModifier()
        {
            return false;
        }

        @Override
        public boolean getControlModifier()
        {
            return false;
        }

        @Override
        public boolean getAltModifier()
        {
            return false;
        }

        @Override
        public boolean getSuperModifier()
        {
            return false;
        }
    };

    boolean getShiftModifier();
    boolean getControlModifier();
    boolean getAltModifier();
    boolean getSuperModifier();
}
