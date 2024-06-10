/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.preferences.serialization;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import kintsugi3d.builder.state.SettingsModel;
import kintsugi3d.builder.state.impl.SimpleSettingsModel;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;

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
        ObjectMapper mapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addDeserializer(Vector2.class, new VectorDeserializer.Vector2Deserializer());
        module.addDeserializer(Vector3.class, new VectorDeserializer.Vector3Deserializer());
        module.addDeserializer(Vector4.class, new VectorDeserializer.Vector4Deserializer());
        mapper.registerModule(module);


        JsonNode rootNode = jsonParser.getCodec().readTree(jsonParser);
        for (Iterator<Map.Entry<String, JsonNode>> it = rootNode.fields(); it.hasNext(); )
        {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode node = entry.getValue();

            switch (node.getNodeType())
            {
                case BOOLEAN:
                    outputModel.createBooleanSetting(entry.getKey(), node.asBoolean(), true);
                    break;
                case NUMBER:
                    outputModel.createNumericSetting(entry.getKey(), node.numberValue(), true);
                    break;
                case STRING:
                    outputModel.createObjectSetting(entry.getKey(), node.asText(), true);
                    break;
                case NULL:
                    outputModel.createObjectSetting(entry.getKey(), null, true);
                    break;
                case OBJECT:
                    //TODO: Deserialize objects... need to know the type to deserialize to. maybe serialize the class that created the data?

                    if (node.has("$TYPE"))
                    {
                        Class<?> objType = mapper.readValue(node.get("$TYPE").toString(), Class.class);
                        outputModel.createObjectSetting(entry.getKey(), mapper.readValue(node.get("$VALUE").toString(), objType), true);
                    }

                    // Explicitly deserialize Vector2, Vector3 and Vector4
                    if (node.has("x") && node.has("y") && node.has("z") && node.has("w"))
                    {
                        outputModel.createObjectSetting(entry.getKey(), mapper.readValue(node.toString(), Vector4.class), true);
                    }
                    else if (node.has("x") && node.has("y") && node.has("z"))
                    {
                        outputModel.createObjectSetting(entry.getKey(), mapper.readValue(node.toString(), Vector3.class), true);
                    }
                    else if (node.has("x") && node.has("y"))
                    {
                        outputModel.createObjectSetting(entry.getKey(), mapper.readValue(node.toString(), Vector2.class), true);
                    }
                    break;
                default:
                    break;
            }
        }

        return outputModel;
    }
}
