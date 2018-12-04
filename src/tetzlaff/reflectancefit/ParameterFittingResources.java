package tetzlaff.reflectancefit;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.Program;
import tetzlaff.gl.core.Texture3D;

public interface ParameterFittingResources<ContextType extends Context<ContextType>>
{
    DiffuseFit<ContextType> createDiffuseFit(Framebuffer<ContextType> framebuffer, int subdiv);
    SpecularFit<ContextType> createSpecularFit(Framebuffer<ContextType> framebuffer, int subdiv);
    Texture3D<ContextType> getViewTextures();
    Texture3D<ContextType> getDepthTextures();
    Program<ContextType> getHoleFillProgram();

    String getMaterialFileName();
    String getMaterialName();
}
