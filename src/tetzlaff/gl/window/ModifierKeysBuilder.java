package tetzlaff.gl.window;

public class ModifierKeysBuilder
{
    private boolean shiftModifier;
    private boolean controlModifier;
    private boolean altModifier;
    private boolean superModifier;

    public static ModifierKeysBuilder begin()
    {
        return new ModifierKeysBuilder();
    }

    public ModifierKeysBuilder shift()
    {
        this.shiftModifier = true;
        return this;
    }

    public ModifierKeysBuilder control()
    {
        this.controlModifier = true;
        return this;
    }

    public ModifierKeysBuilder alt()
    {
        this.altModifier = true;
        return this;
    }

    public ModifierKeysBuilder superKey()
    {
        this.superModifier = true;
        return this;
    }

    public ModifierKeys end()
    {
        return new ModifierKeysImplementation(shiftModifier, controlModifier, altModifier, superModifier);
    }

    private static final class ModifierKeysImplementation extends ModifierKeysBase
    {
        private final boolean shiftModifier;
        private final boolean controlModifier;
        private final boolean altModifier;
        private final boolean superModifier;

        private ModifierKeysImplementation(boolean shiftModifier, boolean controlModifier, boolean altModifier, boolean superModifier)
        {
            this.shiftModifier = shiftModifier;
            this.controlModifier = controlModifier;
            this.altModifier = altModifier;
            this.superModifier = superModifier;
        }

        @Override
        public boolean getShiftModifier()
        {
            return shiftModifier;
        }

        @Override
        public boolean getControlModifier()
        {
            return controlModifier;
        }

        @Override
        public boolean getAltModifier()
        {
            return altModifier;
        }

        @Override
        public boolean getSuperModifier()
        {
            return superModifier;
        }
    }
}
