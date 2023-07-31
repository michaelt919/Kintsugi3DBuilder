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

package kintsugi3d.builder.rendering.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.core.StandardRenderingMode;
import kintsugi3d.builder.resources.IBRResourcesImageSpace;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.StandardShader;
import kintsugi3d.builder.util.KNNViewWeightGenerator;
import kintsugi3d.util.ShadingParameterMode;

import java.io.FileNotFoundException;
import java.util.AbstractList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class IBRSubject<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(IBRSubject.class);
    private final ContextType context;
    private final SceneModel sceneModel;
    private final SceneViewportModel<ContextType> sceneViewportModel;
    private final IBRResourcesImageSpace<ContextType> resources;

    private StandardShader<ContextType> standardShader;
    private Drawable<ContextType> drawable;

    private UniformBuffer<ContextType> weightBuffer;

    private StandardRenderingMode lastCompiledRenderingMode = StandardRenderingMode.IMAGE_BASED;

    public IBRSubject(IBRResourcesImageSpace<ContextType> resources, LightingResources<ContextType> lightingResources,
                      SceneModel sceneModel, SceneViewportModel<ContextType> sceneViewportModel)
    {
        this.resources = resources;
        this.context = resources.getContext();
        this.sceneModel = sceneModel;
        this.sceneViewportModel = sceneViewportModel;
        this.sceneViewportModel.addSceneObjectType("IBRObject");
        this.standardShader = new StandardShader<>(resources, lightingResources, sceneModel);
    }

    public Program<ContextType> getProgram()
    {
        return standardShader.getProgram();
    }

    @Override
    public void initialize() throws FileNotFoundException
    {
        standardShader.initialize(this.sceneModel.getSettingsModel() == null ?
            StandardRenderingMode.IMAGE_BASED : this.sceneModel.getSettingsModel().get("renderingMode", StandardRenderingMode.class));
        refreshDrawable();
    }

    @Override
    public void update() throws FileNotFoundException
    {
        Map<String, Optional<Object>> defineMap = standardShader.getPreprocessorDefines();

        // Reloads shaders only if compiled settings have changed.
        if (getExpectedRenderingMode() != lastCompiledRenderingMode ||
            defineMap.entrySet().stream().anyMatch(
                defineEntry -> !Objects.equals(drawable.program().getDefine(defineEntry.getKey()), defineEntry.getValue())))
        {
            log.info("Updating compiled render settings.");
            reloadShaders();
        }
    }

    @Override
    public void reloadShaders() throws FileNotFoundException
    {
        StandardRenderingMode renderingMode = getExpectedRenderingMode();

        // Force reload shaders
        standardShader.reload(renderingMode);
        refreshDrawable();

        this.lastCompiledRenderingMode = renderingMode;
    }

    private void refreshDrawable()
    {
        this.drawable = resources.createDrawable(standardShader.getProgram());
    }

    private StandardRenderingMode getExpectedRenderingMode()
    {
        return this.sceneModel.getSettingsModel() == null ?  StandardRenderingMode.IMAGE_BASED
            : this.sceneModel.getSettingsModel().get("renderingMode", StandardRenderingMode.class);
    }

    private void setupModelView(Program<ContextType> p, Matrix4 modelView)
    {
        p.setUniform("model_view", modelView);
        p.setUniform("viewPos", modelView.quickInverse(0.01f).getColumn(3).getXYZ());

        if (!this.sceneModel.getSettingsModel().getBoolean("relightingEnabled") && !sceneModel.getSettingsModel().getBoolean("lightCalibrationMode")
                && this.sceneModel.getSettingsModel().get("weightMode", ShadingParameterMode.class) == ShadingParameterMode.UNIFORM)
        {
            if (weightBuffer == null)
            {
                weightBuffer = context.createUniformBuffer();
            }
            weightBuffer.setData(this.generateViewWeights(modelView)); // TODO modelView might not be the right matrix?
            p.setUniformBuffer("ViewWeights", weightBuffer);
        }
    }

    private ReadonlyNativeVectorBuffer generateViewWeights(Matrix4 targetView)
    {
        float[] viewWeights = //new PowerViewWeightGenerator(settings.getWeightExponent())
            new KNNViewWeightGenerator(4)
                .generateWeights(resources,
                    new AbstractList<Integer>()
                    {
                        @Override
                        public Integer get(int index)
                        {
                            return index;
                        }

                        @Override
                        public int size()
                        {
                            return resources.getViewSet().getCameraPoseCount();
                        }
                    },
                    targetView);

        return NativeVectorBufferFactory.getInstance().createFromFloatArray(1, viewWeights.length, viewWeights);
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        context.getState().disableBackFaceCulling();

        // After the ground plane, use a gray color for anything without a texture map.
        drawable.program().setUniform("defaultDiffuseColor", new Vector3(0.125f));

        standardShader.setup();
        drawable.program().setUniform("objectID", sceneViewportModel.lookupSceneObjectID("IBRObject"));

        Matrix4 modelView = sceneModel.getModelViewMatrix(cameraViewport.getView());

        setupModelView(drawable.program(), modelView);

        drawable.program().setUniform("projection", cameraViewport.getViewportProjection());
        drawable.program().setUniform("fullProjection", cameraViewport.getFullProjection());

        drawable.draw(framebuffer, cameraViewport.getX(), cameraViewport.getY(), cameraViewport.getWidth(), cameraViewport.getHeight());

        context.getState().enableBackFaceCulling();
    }

    @Override
    public void close()
    {
        if (standardShader != null)
        {
            standardShader.close();
            standardShader = null;
        }

        if (weightBuffer != null)
        {
            weightBuffer.close();
            weightBuffer = null;
        }
    }
}
