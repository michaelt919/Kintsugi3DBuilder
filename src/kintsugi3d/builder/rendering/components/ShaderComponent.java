/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.rendering.components;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.builder.rendering.SceneViewportModel;
import kintsugi3d.gl.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ShaderComponent<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private static final Logger LOG = LoggerFactory.getLogger(ShaderComponent.class);

    private final ContextType context;
    private Map<String, VertexBuffer<ContextType>> vertexBuffers;
    private final Collection<Resource> otherResources = new ArrayList<>(8);
    private ProgramObject<ContextType> program;
    private Drawable<ContextType> drawable;

    private final SceneViewportModel sceneViewportModel;
    private final String sceneObjectTag;

    /**
     * Creates a shader component using object ID managed by scene viewport model and scene object tag for framebuffer writes
     * @param context
     * @param sceneViewportModel
     * @param sceneObjectTag
     */
    protected ShaderComponent(ContextType context, SceneViewportModel sceneViewportModel, String sceneObjectTag)
    {
        this.context = context;
        this.sceneViewportModel = sceneViewportModel;
        this.sceneObjectTag = sceneObjectTag;

        if (this.sceneViewportModel != null && this.sceneObjectTag != null)
        {
            this.sceneViewportModel.addSceneObjectType(sceneObjectTag);
        }
    }

    /**
     * Creates a shader component using object ID 0 for framebuffer writes
     * @param context
     */
    protected ShaderComponent(ContextType context)
    {
        this(context, null, null);
    }

    @Override
    public void initialize()
    {
        this.vertexBuffers = createVertexBuffers(context);
        reloadShaders();
    }

    @Override
    public void reloadShaders()
    {
        try
        {
            ProgramObject<ContextType> newProgram = createProgram(context);

            if (this.program != null)
            {
                this.program.close();
            }

            this.program = newProgram;

            if (this.sceneViewportModel != null && this.sceneObjectTag != null)
            {
                this.program.setUniform("objectID", sceneViewportModel.lookupSceneObjectID(sceneObjectTag));
            }
            else
            {
                this.program.setUniform("objectID", 0);
            }

            if (this.drawable != null)
            {
                this.drawable.close();
            }

            this.drawable = createDrawable(this.program);

            for (var entry : vertexBuffers.entrySet())
            {
                this.drawable.addVertexBuffer(entry.getKey(), entry.getValue());
            }
        }
        catch (IOException | RuntimeException e)
        {
            LOG.error("Failed to load shader.", e);
        }
    }

    @Override
    public void close()
    {
        if (program != null)
        {
            program.close();
            program = null;
        }

        if (drawable != null)
        {
            drawable.close();
            drawable = null;
        }

        for (VertexBuffer<ContextType> vertexBuffer : vertexBuffers.values())
        {
            vertexBuffer.close();
        }

        for (Resource resource : otherResources)
        {
            resource.close();
        }
    }

    public ContextType getContext()
    {
        return context;
    }

    public Drawable<ContextType> getDrawable()
    {
        return drawable;
    }

    public Program<ContextType> getProgram()
    {
        return drawable.program();
    }

    /**
     * Override to provide shader program.
     * Shader program is automatically managed and does not need to manually be added as a resource.
     * @return
     */
    protected abstract ProgramObject<ContextType> createProgram(ContextType context) throws IOException;

    /**
     * Override to provide vertex buffers.
     * Vertex buffers are automatically managed and do not need to manually be added as resources.
     * @return
     */
    protected abstract Map<String, VertexBuffer<ContextType>> createVertexBuffers(ContextType context);
    /**
     * Override to customize drawable creation.
     */
    protected Drawable<ContextType> createDrawable(Program<ContextType> program)
    {
        return context.createDrawable(program);
    }

    protected <ResourceType extends Resource> ResourceType resource(ResourceType resource)
    {
        this.otherResources.add(resource);
        return resource;
    }
}
