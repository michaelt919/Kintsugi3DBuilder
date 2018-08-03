package tetzlaff.gl.core;

public interface ContextState
{
    void enableDepthTest();
    void disableDepthTest();

    void enableDepthWrite();
    void disableDepthWrite();

    void enableMultisampling();
    void disableMultisampling();

    void enableBackFaceCulling();
    void disableBackFaceCulling();

    void setAlphaBlendingFunction(AlphaBlendingFunction func);
    void disableAlphaBlending();

    int getMaxCombinedVertexUniformComponents();
    int getMaxCombinedFragmentUniformComponents();
    int getMaxUniformBlockSize();
    int getMaxVertexUniformComponents();
    int getMaxFragmentUniformComponents();
    int getMaxArrayTextureLayers();
    int getMaxCombinedTextureImageUnits();
    int getMaxCombinedUniformBlocks();
}
