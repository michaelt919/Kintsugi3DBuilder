package tetzlaff.ibr.tools;//Created by alexk on 7/24/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.MouseButtonState;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.mvc.models.ExtendedCameraModel;
import tetzlaff.mvc.models.SceneViewportModel;
import tetzlaff.mvc.models.impl.EnvironmentMapModelBase;
import tetzlaff.mvc.models.impl.LightingModelBase;

class OrbitTool extends AbstractTool {

    private final double orbitSensitivity = 1.0 * Math.PI; //todo: get from gui somehow
    private double orbitSensitivityAdjusted = 1.0;

    OrbitTool(ExtendedCameraModel cameraModel, EnvironmentMapModelBase environmentMapModel, LightingModelBase lightingModel, SceneViewportModel sceneViewportModel) {
        super(cameraModel, environmentMapModel, lightingModel, sceneViewportModel);
    }

    private Matrix4 oldOrbitMatrix;

    @Override
    public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) {
        super.mouseButtonPressed(window, buttonIndex, mods);
        if(buttonIndex == MB1) {
            oldOrbitMatrix = cameraModel.getOrbit();
            WindowSize windowSize = window.getWindowSize();
            orbitSensitivityAdjusted = orbitSensitivity / Math.min(windowSize.width, windowSize.height);
        }
    }

    @Override
    public void cursorMoved(Window<?> window, double xpos, double ypos) {

        if(window.getMouseButtonState(MB1) == MouseButtonState.Pressed &&
                !Double.isNaN(mouseStartX_MB1) && !Double.isNaN(mouseStartY_MB1) &&
                (ypos != mouseStartY_MB1 || xpos != mouseStartX_MB1)){


                Vector3 rotationVector =
                        new Vector3(
                                (float) (ypos - mouseStartY_MB1),
                                (float) (xpos - mouseStartX_MB1),
                                0.0f
                        );

//                Vector3 changePolerVector = OrbitPolarConverter.self.convertRight(Matrix4.rotateAxis(rotationVector.normalized(), rotationVector.length() * orbitSensitivityAdjusted));
//
//                System.out.println(changePolerVector);
//
//                cameraModel.setAzimuth(cameraModel.getAzimuth() + changePolerVector.x);
//
//                cameraModel.setInclination(cameraModel.getInclination() + changePolerVector.y);
//
//                cameraModel.setTwist(cameraModel.getTwist() + changePolerVector.z);

            cameraModel.setOrbit(
                    Matrix4.rotateAxis(rotationVector.normalized(),
                            rotationVector.length() * orbitSensitivityAdjusted
                            ).times(oldOrbitMatrix)
            );

        }

    }
}
