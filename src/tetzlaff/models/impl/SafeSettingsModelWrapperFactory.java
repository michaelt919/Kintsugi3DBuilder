/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

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
