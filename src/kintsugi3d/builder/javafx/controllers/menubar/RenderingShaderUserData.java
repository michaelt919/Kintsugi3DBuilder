package kintsugi3d.builder.javafx.controllers.menubar;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RenderingShaderUserData
{
    private final String shaderName;

    private final Map<String, Optional<Object>> shaderDefines;

    public RenderingShaderUserData(String shaderName)
    {
        this.shaderName = shaderName;
        this.shaderDefines = new HashMap<>(0);
    }

    public RenderingShaderUserData(String shaderName, Map<String, Optional<Object>> shaderDefines)
    {
        this.shaderName = shaderName;
        this.shaderDefines = shaderDefines;
    }

    public String getShaderName()
    {
        return shaderName;
    }

    public Map<String, Optional<Object>> getShaderDefines()
    {
        return shaderDefines;
    }
}
