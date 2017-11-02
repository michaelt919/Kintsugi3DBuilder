package tetzlaff.ibrelight.export.simpleanimation;

import java.io.File;

import tetzlaff.gl.vecmath.Matrix4;

public final class OrbitAnimationRequest extends SimpleAnimationRequestBase
{
    protected static class BuilderImplementation extends BuilderBase<OrbitAnimationRequest>
    {
        @Override
        public OrbitAnimationRequest create()
        {
            return new OrbitAnimationRequest(getWidth(), getHeight(), getFrameCount(), getExportPath());
        }
    }

    private OrbitAnimationRequest(int width, int height, int frameCount, File exportPath)
    {
        super(width, height, frameCount, exportPath);
    }

    @Override
    protected Matrix4 getRelativeViewMatrix(int frame, Matrix4 baseRelativeViewMatrix)
    {
        return baseRelativeViewMatrix.times(Matrix4.rotateY(frame * 2 * Math.PI / this.getFrameCount()));
    }
}
