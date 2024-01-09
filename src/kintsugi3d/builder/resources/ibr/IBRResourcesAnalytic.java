package kintsugi3d.builder.resources.ibr;

import kintsugi3d.builder.core.ColorAppearanceMode;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Drawable;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.geometry.GeometryMode;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.material.TextureLoadOptions;

public class IBRResourcesAnalytic<ContextType extends Context<ContextType>> extends IBRResourcesBase<ContextType>
{
    public IBRResourcesAnalytic(ContextType context, ViewSet viewSet, VertexGeometry geometry)
    {
        super(new IBRSharedResources<>(context, viewSet, geometry, new TextureLoadOptions()), true);
    }

    @Override
    public ProgramBuilder<ContextType> getShaderProgramBuilder()
    {
        return getSharedResources().getShaderProgramBuilder()
                .define("GEOMETRY_MODE", GeometryMode.PROJECT_3D_TO_2D) // should default to this, but just in case
                .define("GEOMETRY_TEXTURES_ENABLED", false) // should default to this, but just in case
                .define("COLOR_APPEARANCE_MODE", ColorAppearanceMode.ANALYTIC)
                .define("CAMERA_PROJECTION_COUNT", getViewSet().getCameraProjectionCount());
    }

    @Override
    public void setupShaderProgram(Program<ContextType> program)
    {
        getSharedResources().setupShaderProgram(program);
    }

    @Override
    public Drawable<ContextType> createDrawable(Program<ContextType> program)
    {
        return getGeometryResources().createDrawable(program);
    }
}
