/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.preferences.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import kintsugi3d.builder.state.GlobalSettingsModel;
import kintsugi3d.builder.state.ReadonlyGlobalSettingsModel;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;

import java.io.IOException;
import java.util.Iterator;

public class SettingsModelSerializer extends StdSerializer<GlobalSettingsModel>
{
    public SettingsModelSerializer()
    {
        this(null);
    }

    protected SettingsModelSerializer(Class<GlobalSettingsModel> t)
    {
        super(t);
    }

    @Override
    public void serialize(GlobalSettingsModel settingsModel, JsonGenerator jGen, SerializerProvider serializerProvider) throws IOException
    {
        jGen.writeStartObject();

        for (Iterator<ReadonlyGlobalSettingsModel.Setting> it = settingsModel.iterator(); it.hasNext(); )
        {
            ReadonlyGlobalSettingsModel.Setting setting = it.next();

            if (!setting.shouldSerialize())
            {
                continue;
            }

            if (shouldStandardSerialize(setting.getType()))
            {
                jGen.writeObjectField(setting.getName(), setting.getValue());
            }
            else
            {
                jGen.writeObjectFieldStart(setting.getName());

                jGen.writeObjectField("$TYPE", setting.getType());
                jGen.writeObjectField("$VALUE", setting.getValue());

                jGen.writeEndObject();
            }
        }

        jGen.writeEndObject();
    }

    private boolean shouldStandardSerialize(Class<?> type)
    {
        if (type.isAssignableFrom(Boolean.class) || type.isAssignableFrom(Number.class) || type.isAssignableFrom(String.class))
            return true;

        if (type.isAssignableFrom(Vector2.class) || type.isAssignableFrom(Vector3.class) || type.isAssignableFrom(Vector4.class))
            return true;

        return false;
    }
}
