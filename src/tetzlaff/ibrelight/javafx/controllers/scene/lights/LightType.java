package tetzlaff.ibrelight.javafx.controllers.scene.lights;//Created by alexk on 7/16/2017.

import javafx.util.StringConverter;

public enum LightType
{
    PointLight,
    SpotLight,
    AreaLight,
    DirectionalLight;

    public static final StringConverter<LightType> converter = new StringConverter<LightType>()
    {
        @Override
        public String toString(LightType object)
        {
            if (object == null)
            {
                return "Null";
            }
            switch (object)
            {
                case PointLight:
                    return "Point Light";
                case SpotLight:
                    return "Spot Light";
                case AreaLight:
                    return "Area Light";
                case DirectionalLight:
                    return "Directional Light";
                default:
                    return null;
            }
        }

        @Override
        public LightType fromString(String string)
        {
            if (string == null || "Null".equals(string))
            {
                return null;
            }
            switch (string)
            {
                case "Point Light":
                    return PointLight;
                case "Spot Light":
                    return SpotLight;
                case "Area Light":
                    return AreaLight;
                case "Directional Light":
                    return DirectionalLight;
                default:
                    return null;
            }
        }
    };
}
