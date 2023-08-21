/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.rendering;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;
import kintsugi3d.util.ShadingParameterMode;

public class StandardShader<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private final IBRResourcesImageSpace<ContextType> resources;
    private final LightingResources<ContextType> lightingResources;
    private final SceneModel sceneModel;

    private ProgramObject<ContextType> program;

    // Set default shader to be the untextured IBR shader
    private File fragmentShaderFile = new File(new File("shaders", "rendermodes"), "ibrUntextured.frag");

    public StandardShader(IBRResourcesImageSpace<ContextType> resources, LightingResources<ContextType> lightingResources,
                          SceneModel sceneModel)
    {
        this.resources = resources;
        this.lightingResources = lightingResources;
        this.sceneModel = sceneModel;
    }

    public Program<ContextType> getProgram()
    {
        return program;
    }

    public void initialize() throws FileNotFoundException
    {
        if (this.program == null)
        {
            this.program = loadMainProgram(getPreprocessorDefines());
        }
    }

    public File getFragmentShaderFile()
    {
        return fragmentShaderFile;
    }

    public void setFragmentShaderFile(File shaderFile)
    {
        this.fragmentShaderFile = shaderFile;
    }

    public void reload() throws FileNotFoundException
    {
        ProgramObject<ContextType> newProgram = loadMainProgram(getPreprocessorDefines());

        if (this.program != null)
        {
            this.program.close();
        }

        this.program = newProgram;
    }

    private ProgramBuilder<ContextType> getProgramBuilder(Map<String, Optional<Object>> defineMap)
    {
        ProgramBuilder<ContextType> programBuilder = resources.getShaderProgramBuilder();

        for (Map.Entry<String, Optional<Object>> defineEntry : defineMap.entrySet())
        {
            if (defineEntry.getValue().isPresent())
            {
                programBuilder.define(defineEntry.getKey(), defineEntry.getValue().get());
            }
        }

        return programBuilder;
    }

    private ProgramObject<ContextType> loadMainProgram(Map<String, Optional<Object>> defineMap) throws FileNotFoundException
    {
        return this.getProgramBuilder(defineMap)
            .define("SPOTLIGHTS_ENABLED", true)
            .addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
            .addShader(ShaderType.FRAGMENT, this.fragmentShaderFile)
            .createProgram();
    }

    public Map<String, Optional<Object>> getPreprocessorDefines()
    {
        Map<String, Optional<Object>> defineMap = new HashMap<>(256);

        // Initialize to defaults
        defineMap.put("PHYSICALLY_BASED_MASKING_SHADOWING", Optional.empty());
        defineMap.put("FRESNEL_EFFECT_ENABLED", Optional.empty());
        defineMap.put("SHADOWS_ENABLED", Optional.empty());
        defineMap.put("BUEHLER_ALGORITHM", Optional.empty());
        defineMap.put("SORTING_SAMPLE_COUNT", Optional.empty());
        defineMap.put("RELIGHTING_ENABLED", Optional.empty());
        defineMap.put("VISIBILITY_TEST_ENABLED", Optional.empty());
        defineMap.put("SHADOW_TEST_ENABLED", Optional.empty());
        defineMap.put("PRECOMPUTED_VIEW_WEIGHTS_ENABLED", Optional.empty());
        defineMap.put("USE_VIEW_INDICES", Optional.empty());

        defineMap.put("VIEW_COUNT", Optional.empty());
        defineMap.put("VIRTUAL_LIGHT_COUNT", Optional.empty());
        defineMap.put("ENVIRONMENT_ILLUMINATION_ENABLED", Optional.empty());

        defineMap.put("LUMINANCE_MAP_ENABLED", Optional.of(this.resources.getViewSet().hasCustomLuminanceEncoding()));
        defineMap.put("INVERSE_LUMINANCE_MAP_ENABLED", Optional.of(false/*this.resources.viewSet.hasCustomLuminanceEncoding()*/));

        defineMap.put("RAY_DEPTH_GRADIENT", Optional.of(0.1 * sceneModel.getScale()));
        defineMap.put("RAY_POSITION_JITTER", Optional.of(0.01 * sceneModel.getScale()));

        if (this.sceneModel.getSettingsModel() != null)
        {
            defineMap.put("PHYSICALLY_BASED_MASKING_SHADOWING",
                Optional.of(this.sceneModel.getSettingsModel().getBoolean("pbrGeometricAttenuationEnabled")));
            defineMap.put("FRESNEL_EFFECT_ENABLED", Optional.of(this.sceneModel.getSettingsModel().getBoolean("fresnelEnabled")));
            defineMap.put("SHADOWS_ENABLED", Optional.of(this.sceneModel.getSettingsModel().getBoolean("shadowsEnabled")));

            defineMap.put("BUEHLER_ALGORITHM", Optional.of(this.sceneModel.getSettingsModel().getBoolean("buehlerAlgorithm")));
            defineMap.put("SORTING_SAMPLE_COUNT", Optional.of(this.sceneModel.getSettingsModel().getInt("buehlerViewCount")));
            defineMap.put("RELIGHTING_ENABLED", Optional.of(this.sceneModel.getSettingsModel().getBoolean("relightingEnabled")
                && lightingResources != null && this.sceneModel.getLightingModel() != null));

            boolean occlusionEnabled = this.sceneModel.getSettingsModel().getBoolean("occlusionEnabled")
                && (this.sceneModel.getSettingsModel().getBoolean("relightingEnabled")
                || sceneModel.getSettingsModel().getBoolean("lightCalibrationMode")
                || this.sceneModel.getSettingsModel().get("weightMode", ShadingParameterMode.class) != ShadingParameterMode.UNIFORM);

            defineMap.put("VISIBILITY_TEST_ENABLED", Optional.of(occlusionEnabled && this.resources.depthTextures != null));
            defineMap.put("SHADOW_TEST_ENABLED", Optional.of(occlusionEnabled && this.resources.shadowTextures != null
                && lightingResources != null));

            defineMap.put("PRECOMPUTED_VIEW_WEIGHTS_ENABLED",
                Optional.of(!this.sceneModel.getSettingsModel().getBoolean("relightingEnabled")
                        && !sceneModel.getSettingsModel().getBoolean("lightCalibrationMode")
                    && this.sceneModel.getSettingsModel().get("weightMode", ShadingParameterMode.class) == ShadingParameterMode.UNIFORM));

            if (sceneModel.getSettingsModel().getBoolean("lightCalibrationMode"))
            {
                defineMap.put("USE_VIEW_INDICES", Optional.of(true));
                defineMap.put("VIEW_COUNT", Optional.of(1));
            }

            if (this.sceneModel.getLightingModel() != null && this.sceneModel.getSettingsModel().getBoolean("relightingEnabled")
                && lightingResources != null)
            {
                defineMap.put("VIRTUAL_LIGHT_COUNT", Optional.of(sceneModel.getLightingModel().getLightCount()));
                defineMap.put("ENVIRONMENT_ILLUMINATION_ENABLED", Optional.of(!Objects.equals(sceneModel.getLightingModel().getAmbientLightColor(), Vector3.ZERO)));
                defineMap.put("ENVIRONMENT_TEXTURE_ENABLED", Optional.of(lightingResources.getEnvironmentMap() != null && sceneModel.getLightingModel().isEnvironmentMappingEnabled()));
            }
        }

        return defineMap;
    }

    public void setup()
    {
        setup(sceneModel.getUnscaledMatrix(sceneModel.getObjectModel().getTransformationMatrix()).times(sceneModel.getBaseModelMatrix()));
    }

    public void setup(Matrix4 model)
    {
        setupUnlit(model);

        for (int lightIndex = 0; lightIndex < sceneModel.getLightingModel().getLightCount(); lightIndex++)
        {
            Matrix4 lightViewMatrix = sceneModel.getLightViewMatrix(lightIndex);

            Vector3 controllerLightIntensity = sceneModel.getLightingModel().getLightPrototype(lightIndex).getColor();

            // Light intensity depends on distance to centroid using the subject's model matrix (regardless of what we're drawing right now)
            float lightDistance = sceneModel.getLightModelViewMatrix(lightIndex).times(sceneModel.getCentroid().asPosition()).getXYZ().length();

            float lightScale = resources.getViewSet().areLightSourcesInfinite() ? 1.0f :
                resources.getViewSet().getCameraPose(resources.getViewSet().getPrimaryViewIndex())
                    .times(Objects.requireNonNull(resources.getGeometry()).getCentroid().asPosition())
                    .getXYZ().length();
            program.setUniform("lightIntensityVirtual[" + lightIndex + ']',
                controllerLightIntensity.times(lightDistance * lightDistance * resources.getViewSet().getLightIntensity(0).y / (lightScale * lightScale)));


            setupLight(program, lightIndex, lightViewMatrix.times(model));
        }
    }

    private void setupLight(Program<ContextType> program, int lightIndex, Matrix4 lightMatrix)
    {
        Matrix4 lightMatrixInverse = lightMatrix.quickInverse(0.001f);

        Vector3 lightPos = lightMatrixInverse.times(Vector4.ORIGIN).getXYZ();

        program.setUniform("lightPosVirtual[" + lightIndex + ']', lightPos);

        if (lightingResources != null)
        {
            program.setUniform("lightMatrixVirtual[" + lightIndex + ']', lightingResources.getLightProjection(lightIndex).times(lightMatrix));
        }
        program.setUniform("lightOrientationVirtual[" + lightIndex + ']',
                lightMatrixInverse.times(new Vector4(0.0f, 0.0f, -1.0f, 0.0f)).getXYZ().normalized());
        program.setUniform("lightSpotSizeVirtual[" + lightIndex + ']',
                (float)Math.sin(sceneModel.getLightingModel().getLightPrototype(lightIndex).getSpotSize()));
        program.setUniform("lightSpotTaperVirtual[" + lightIndex + ']', sceneModel.getLightingModel().getLightPrototype(lightIndex).getSpotTaper());
    }

    public void setupUnlit(Matrix4 model)
    {
        this.resources.setupShaderProgram(program);

        program.setUniform("weightExponent", this.sceneModel.getSettingsModel().getFloat("weightExponent"));
        program.setUniform("isotropyFactor", this.sceneModel.getSettingsModel().getFloat("isotropyFactor"));
        program.setUniform("occlusionBias", this.sceneModel.getSettingsModel().getFloat("occlusionBias"));

        float gamma = this.sceneModel.getSettingsModel().getFloat("gamma");
        program.setUniform("renderGamma", gamma);

        if (lightingResources != null)
        {
            program.setTexture("shadowMaps", lightingResources.getShadowMaps());
        }

        if (lightingResources == null || lightingResources.getEnvironmentMap() == null
            || !sceneModel.getLightingModel().isEnvironmentMappingEnabled())
        {
            program.setTexture("environmentMap", resources.getContext().getTextureFactory().getNullTexture(SamplerType.FLOAT_CUBE_MAP));
        }
        else
        {
            program.setUniform("useEnvironmentMap", true);
            program.setTexture("environmentMap", lightingResources.getEnvironmentMap());
            program.setUniform("environmentMipMapLevel",
                Math.max(0, Math.min(lightingResources.getEnvironmentMap().getMipmapLevelCount() - 1,
                    this.sceneModel.getLightingModel().getEnvironmentMapFilteringBias()
                        + (float)(0.5 *
                        Math.log(6 * (double)lightingResources.getEnvironmentMap().getFaceSize() * (double)lightingResources.getEnvironmentMap().getFaceSize()
                            / (double) resources.getViewSet().getCameraPoseCount() )
                        / Math.log(2.0)))));
            program.setUniform("diffuseEnvironmentMipMapLevel", lightingResources.getEnvironmentMap().getMipmapLevelCount() - 1);

            Matrix4 envMapMatrix = sceneModel.getEnvironmentMapMatrix(model);
            program.setUniform("envMapMatrix", envMapMatrix);

            program.setTexture("screenSpaceDepthBuffer", lightingResources.getScreenSpaceDepthTexture());
        }

        program.setUniform("ambientColor",
            sceneModel.getLightingModel().getAmbientLightColor().applyOperator(x -> Math.pow(x, gamma)));
    }

    @Override
    public void close()
    {
        if (program != null)
        {
            program.close();
            program = null;
        }
    }
}
