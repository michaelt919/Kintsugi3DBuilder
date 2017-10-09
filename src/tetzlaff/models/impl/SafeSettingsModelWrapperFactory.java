package tetzlaff.models.impl;

import tetzlaff.models.ReadonlySettingsModel;
import tetzlaff.models.SafeReadonlySettingsModel;

public class SafeSettingsModelWrapperFactory
{
    private static final SafeSettingsModelWrapperFactory INSTANCE = new SafeSettingsModelWrapperFactory();

    private SafeSettingsModelWrapperFactory()
    {
    }

    public static SafeSettingsModelWrapperFactory getInstance()
    {
        return INSTANCE;
    }

    private static class Implementation implements SafeReadonlySettingsModel
    {
        private final ReadonlySettingsModel base;

        Implementation(ReadonlySettingsModel base)
        {
            this.base = base;
        }

        @Override
        public <T> T get(String name, Class<T> settingType)
        {
            if (base.existsForGet(name, settingType))
            {
                return base.get(name, settingType);
            }
            else
            {
                System.err.println("Could not find setting \"" + name + "\"; using default value instead.");
                return (T) DefaultSettingsModel.getDefault(settingType);
            }
        }
    }

    public SafeReadonlySettingsModel wrapUnsafeModel(ReadonlySettingsModel base)
    {
        return new Implementation(base);
    }
}
