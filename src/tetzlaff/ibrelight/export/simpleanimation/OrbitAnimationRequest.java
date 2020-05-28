/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

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
