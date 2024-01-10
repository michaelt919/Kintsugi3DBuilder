/*
 *  Copyright (c) Michael Tetzlaff 2024
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package test;

import javafx.beans.property.SimpleFloatProperty;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.fit.ReconstructionShaders;
import kintsugi3d.builder.fit.SpecularFitProgramFactory;
import kintsugi3d.builder.fit.settings.SpecularBasisSettings;
import kintsugi3d.builder.io.ViewSetReaderFromVSET;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.builder.rendering.ImageReconstruction;
import kintsugi3d.builder.rendering.ReconstructionView;
import kintsugi3d.builder.resources.ibr.IBRResourcesAnalytic;
import kintsugi3d.builder.state.impl.SimpleSettingsModel;
import kintsugi3d.gl.core.ColorFormat;
import kintsugi3d.gl.core.Drawable;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.core.ProgramObject;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.glfw.CanvasWindow;
import kintsugi3d.gl.opengl.OpenGLContext;
import kintsugi3d.gl.opengl.OpenGLContextFactory;
import kintsugi3d.util.Potato;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;

class ImageReconstructionTest
{
    @Test
    @DisplayName("Ground truth reconstruction error")
    void groundTruth() throws IOException
    {
        ViewSet viewSet;

        try
        {
            viewSet = ViewSetReaderFromVSET.getInstance().readFromStream(
                    getClass().getClassLoader().getResourceAsStream("test/Structured34View.vset"), null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        OpenGLContext context =
            OpenGLContextFactory.getInstance().buildWindow("Kintsugi 3D Builder Tests", 1, 1).create().getContext();

        Potato potato = new Potato(50, 0.75f, 0.1f, 250000);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        potato.writeToStream(new PrintStream(out));

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        VertexGeometry geometry = VertexGeometry.createFromOBJStream(in);

        SimpleSettingsModel ibrSettings = new SimpleSettingsModel();
        ibrSettings.createBooleanSetting("shadowsEnabled", false);
        ibrSettings.createBooleanSetting("occlusionEnabled", false);

        SpecularBasisSettings specularBasisSettings = new SpecularBasisSettings();
        specularBasisSettings.setBasisCount(1);

        SpecularFitProgramFactory<OpenGLContext> programFactory = new SpecularFitProgramFactory<>(
            ibrSettings, specularBasisSettings);

        try(IBRResourcesAnalytic<OpenGLContext> resources = new IBRResourcesAnalytic<>(context, viewSet, geometry);
            ImageReconstruction<OpenGLContext> reconstruction = new ImageReconstruction<>(
                viewSet,
                context.buildFramebufferObject(256, 256)
                        .addColorAttachment(ColorFormat.RGBA32F)
                        .addDepthAttachment(),
                context.buildFramebufferObject(256, 256)
                        .addColorAttachment(ColorFormat.RGBA32F)
                        .addDepthAttachment(),
                ReconstructionShaders.getIncidentRadianceProgramBuilder(resources, programFactory),
                resources);
            ProgramObject<OpenGLContext> analyticWithNoise = programFactory.getShaderProgramBuilder(resources,
                    new File("shaders/common/imgspace.vert"),
                    new File("shaders/test/renderWithNoise.frag"))
                    .createProgram())
        {
            Drawable<OpenGLContext> drawable = resources.createDrawable(analyticWithNoise);
            for (ReconstructionView<OpenGLContext> view : reconstruction)
            {
                view.reconstruct(drawable);
            }
        }
    }
}