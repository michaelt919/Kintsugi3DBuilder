package tetzlaff.gl.opengl;

import tetzlaff.gl.core.AlphaBlendingFunction;
import tetzlaff.gl.core.AlphaBlendingFunction.Weight;
import tetzlaff.gl.core.ContextState;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

public class OpenGLContextState implements ContextState
{
    OpenGLContext context;

    OpenGLContextState(OpenGLContext context)
    {
        this.context = context;
    }

    @Override
    public void enableDepthTest()
    {
        glEnable(GL_DEPTH_TEST);
        OpenGLContext.errorCheck();
    }

    @Override
    public void disableDepthTest()
    {
        glDisable(GL_DEPTH_TEST);
        OpenGLContext.errorCheck();
    }

    @Override
    public void enableDepthWrite()
    {
        glDepthMask(true);
        OpenGLContext.errorCheck();
    }

    @Override
    public void disableDepthWrite()
    {
        glDepthMask(false);
        OpenGLContext.errorCheck();
    }

    @Override
    public void enableMultisampling()
    {
        glEnable(GL_MULTISAMPLE);
        OpenGLContext.errorCheck();
    }

    @Override
    public void disableMultisampling()
    {
        glDisable(GL_MULTISAMPLE);
        OpenGLContext.errorCheck();
    }

    @Override
    public void enableBackFaceCulling()
    {
        glEnable(GL_CULL_FACE);
        OpenGLContext.errorCheck();
    }

    @Override
    public void disableBackFaceCulling()
    {
        glDisable(GL_CULL_FACE);
        OpenGLContext.errorCheck();
    }

    private int blendFuncEnumToInt(Weight func)
    {
        switch(func)
        {
        case DST_ALPHA: return GL_DST_ALPHA;
        case DST_COLOR: return GL_DST_COLOR;
        case ONE: return GL_ONE;
        case ONE_MINUS_DST_ALPHA: return GL_ONE_MINUS_DST_ALPHA;
        case ONE_MINUS_DST_COLOR: return GL_ONE_MINUS_DST_COLOR;
        case ONE_MINUS_SRC_ALPHA: return GL_ONE_MINUS_SRC_ALPHA;
        case ONE_MINUS_SRC_COLOR: return GL_ONE_MINUS_SRC_COLOR;
        case SRC_ALPHA: return GL_ONE_MINUS_SRC_ALPHA;
        case SRC_COLOR: return GL_ONE_MINUS_SRC_COLOR;
        case ZERO: return GL_ZERO;
        default: throw new IllegalArgumentException();
        }
    }

    @Override
    public void setAlphaBlendingFunction(AlphaBlendingFunction func)
    {
        glEnable(GL_BLEND);
        OpenGLContext.errorCheck();
        glBlendFunc(blendFuncEnumToInt(func.sourceWeightFunction), blendFuncEnumToInt(func.destinationWeightFunction));
        OpenGLContext.errorCheck();
    }

    @Override
    public void disableAlphaBlending()
    {
        glDisable(GL_BLEND);
        OpenGLContext.errorCheck();
    }

    private int getInteger(int queryId)
    {
        int queryResult = glGetInteger(queryId);
        OpenGLContext.errorCheck();
        return queryResult;
    }

    @Override
    public int getMaxCombinedVertexUniformComponents()
    {
        return getInteger(GL_MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS);
    }

    @Override
    public int getMaxCombinedFragmentUniformComponents()
    {
        return getInteger(GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS);
    }

    @Override
    public int getMaxUniformBlockSize()
    {
        return getInteger(GL_MAX_UNIFORM_BLOCK_SIZE);
    }

    @Override
    public int getMaxVertexUniformComponents()
    {
        return getInteger(GL_MAX_VERTEX_UNIFORM_COMPONENTS);
    }

    @Override
    public int getMaxFragmentUniformComponents()
    {
        return getInteger(GL_MAX_FRAGMENT_UNIFORM_COMPONENTS);
    }

    @Override
    public int getMaxArrayTextureLayers()
    {
        return getInteger(GL_MAX_ARRAY_TEXTURE_LAYERS);
    }

    @Override
    public int getMaxCombinedTextureImageUnits()
    {
        return getInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
    }

    @Override
    public int getMaxCombinedUniformBlocks()
    {
        return getInteger(GL_MAX_COMBINED_UNIFORM_BLOCKS);
    }
}
