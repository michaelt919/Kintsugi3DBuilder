/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.tools;

import tetzlaff.models.*;

interface ToolBuilder<ToolType>
{
    ToolBuilder<ToolType> setToolBindingModel(ToolBindingModel toolBindingModel);
    ToolBuilder<ToolType> setCameraModel(ExtendedCameraModel cameraModel);
    ToolBuilder<ToolType> setEnvironmentMapModel(EnvironmentModel environmentModel);
    ToolBuilder<ToolType> setLightingModel(ExtendedLightingModel lightingModel);
    ToolBuilder<ToolType> setObjectModel(ExtendedObjectModel lightingModel);
    ToolBuilder<ToolType> setSceneViewportModel(SceneViewportModel sceneViewportModel);
    ToolBuilder<ToolType> setSettingsModel(SettingsModel settingsModel);
    ToolType build();
}
