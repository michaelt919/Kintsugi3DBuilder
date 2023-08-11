/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.preferences.serialization;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import kintsugi3d.builder.state.SettingsModel;
import kintsugi3d.builder.state.impl.SimpleSettingsModel;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class SettingsModelDeserializer extends StdDeserializer<SettingsModel>
{
    public SettingsModelDeserializer()
    {
        this(null);
    }

    protected SettingsModelDeserializer(Class<?> vc)
    {
        super(vc);
    }

    @Override
    public SettingsModel deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException
    {
        SimpleSettingsModel outputModel = new SimpleSettingsModel();

        JsonNode rootNode = jsonParser.getCodec().readTree(jsonParser);
        for (Iterator<Map.Entry<String, JsonNode>> it = rootNode.fields(); it.hasNext(); )
        {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode node = entry.getValue();

            JsonNodeType type = node.getNodeType();
            if (type == JsonNodeType.BOOLEAN)
            {
                outputModel.createBooleanSetting(entry.getKey(), entry.getValue().asBoolean(), true);
            }
            else if (type == JsonNodeType.NUMBER)
            {
                outputModel.createNumericSetting(entry.getKey(), entry.getValue().numberValue(), true);
            }
            else
            {
                //TODO: Deserialize objects... need to know the type to deserialize to. maybe serialize the class that created the data?
            }
        }

        return outputModel;
    }
}
