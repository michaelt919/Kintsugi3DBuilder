/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
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
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;

import java.io.IOException;

public class VectorDeserializer
{

    public static class Vector2Deserializer extends JsonDeserializer<Vector2>
    {
        @Override
        public Vector2 deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException
        {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            return new Vector2(node.get("x").floatValue(), node.get("y").floatValue());
        }
    }

    public static class Vector3Deserializer extends JsonDeserializer<Vector3>
    {
        @Override
        public Vector3 deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException
        {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            return new Vector3(node.get("x").floatValue(), node.get("y").floatValue(), node.get("z").floatValue());
        }
    }

    public static class Vector4Deserializer extends JsonDeserializer<Vector4>
    {
        @Override
        public Vector4 deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException
        {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            return new Vector4(node.get("x").floatValue(), node.get("y").floatValue(), node.get("z").floatValue(), node.get("w").floatValue());
        }
    }

}
