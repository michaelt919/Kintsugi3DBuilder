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

import java.util.AbstractList;
import java.util.Collections;
import java.util.Map;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.builder.rendering.StandardShaderComponent;
import kintsugi3d.builder.resources.LightingResources;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.builder.util.KNNViewWeightGenerator;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.ShadingParameterMode;
public class IBRSubject<ContextType extends Context<ContextType>> extends StandardShaderComponent<ContextType>
{
    private UniformBuffer<ContextType> weightBuffer;

    public IBRSubject(IBRResourcesImageSpace<ContextType> resources, SceneViewportModel sceneViewportModel,
        SceneModel sceneModel, LightingResources<ContextType> lightingResources)
    {
        super (resources, sceneViewportModel, "IBRObject", sceneModel, lightingResources);
    }

    @Override
    protected Map<String, VertexBuffer<ContextType>> createVertexBuffers(ContextType context)
    {
        // Vertex buffers come from the IBRResources and don't need to be created.
        return Collections.emptyMap();
    }

    @Override
    protected Drawable<ContextType> createDrawable(Program<ContextType> program)
    {
        // Use IBRResources to create a drawable with all the available vertex buffers.
        return resources.createDrawable(program);
    }

    public Program<ContextType> getProgram()
    {
        return getDrawable().program();
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
    protected void setupModelView(Program<ContextType> p, Matrix4 modelView)
    {
        super.setupModelView(p, modelView);

        if (!this.sceneModel.getSettingsModel().getBoolean("relightingEnabled")
            && !isLightCalibrationMode()
            && this.sceneModel.getSettingsModel().get("weightMode", ShadingParameterMode.class) == ShadingParameterMode.UNIFORM)
        {
            if (weightBuffer == null)
            {
                weightBuffer = resource(getContext().createUniformBuffer());
            }

            weightBuffer.setData(this.generateViewWeights(modelView)); // TODO modelView might not be the right matrix?
            p.setUniformBuffer("ViewWeights", weightBuffer);
        }
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        getContext().getState().disableBackFaceCulling();

        // After the ground plane, use a gray color for anything without a texture map.
        getDrawable().program().setUniform("defaultDiffuseColor", new Vector3(0.125f));

        setupShader(cameraViewport);
        getDrawable().draw(cameraViewport.ofFramebuffer(framebuffer));

        getContext().getState().enableBackFaceCulling();
    }
}
