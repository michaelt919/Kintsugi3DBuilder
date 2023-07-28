/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state;//Created by alexk on 7/21/2017.

import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

public interface ExtendedObjectModel extends ObjectModel, ReadonlyExtendedObjectModel
{
    void setOrbit(Matrix4 orbit);
    void setCenter(Vector3 center);
    void setRotationZ(float rotationZ);
    void setRotationY(float rotationY);
    void setRotationX(float rotationX);
}
