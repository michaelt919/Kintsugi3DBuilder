package tetzlaff.ibr.rendering2.tools2;//Created by alexk on 8/2/2017.

import javafx.beans.property.DoubleProperty;
import kautzTesting.SphereMath;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.gl.window.KeyCodes;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.MouseButtonState;
import tetzlaff.gl.window.Window;
import tetzlaff.mvc.models.*;

class LightDragTool extends AbstractTool {
    public LightDragTool(ControllableCameraModel cameraModel, ControllableEnvironmentMapModel environmentMapModel, ControllableLightModel lightModel) {
        super(cameraModel, environmentMapModel, lightModel);
    }


    @Override
    public void cursorMoved(Window<?> window, double xpos, double ypos) {
        if(window.getMouseButtonState(MB1).equals(MouseButtonState.Pressed)){
            float xF =(float) (xpos / window.getWindowSize().height);
            float yF =(float) (ypos / window.getWindowSize().height);

            System.out.println("Fire!");

            Vector4 out = new Vector4((xF-0.5f)*.5f, (0.5f-yF)*.5f, -1f, 0f);
            out = cameraModel.getOrbit().transpose().times(out);

            Vector3 start =cameraModel.getCenter().plus(cameraModel.getOrbit().transpose().times(new Vector4(0,0,cameraModel.getDistance(), 1f)).getXYZ());

            System.out.println("Shooting from " + start + " into " + out.getXYZ());

            ControllableSubLightModel light = lightModel.getLight(0);

            if(light.exists()){
                System.out.println("We have a light");
                float radius = light.getDistance();
                System.out.println("Distance: " + radius);

                Vector2 azANDinc = SphereMath.ShootSphere(start, out.getXYZ().normalized(), radius);

                System.out.println("Setting az and inc to " + azANDinc);
                light.setAzimuth(Math.toDegrees(azANDinc.x));
                light.setInclination(Math.toDegrees(azANDinc.y));


            }else {
                System.out.println("No light.");
            }

        }
        else if(window.getMouseButtonState(MB2).equals(MouseButtonState.Pressed)){
            float xF =(float) (xpos / window.getWindowSize().height);
            float yF =(float) (ypos / window.getWindowSize().height);

            System.out.println("Fire!");

            Vector4 out = new Vector4((xF-0.5f)*.5f, (0.5f-yF)*.5f, -1f, 0f);
            out = cameraModel.getOrbit().transpose().times(out);

            Vector3 start =cameraModel.getCenter().plus(cameraModel.getOrbit().transpose().times(new Vector4(0,0,cameraModel.getDistance(), 1f)).getXYZ());

            System.out.println("Shooting from " + start + " into " + out.getXYZ());

            ControllableSubLightModel light = lightModel.getLight(0);

            if(light.exists()){
                System.out.println("We have a light");
                float radius = light.getDistance();
                System.out.println("Distance: " + radius);

                Vector2 azANDinc = SphereMath.ShootSphereInner(start, out.getXYZ().normalized(), radius);

                System.out.println("Setting az and inc to " + azANDinc);
                light.setAzimuth(Math.toDegrees(azANDinc.x));
                light.setInclination(Math.toDegrees(azANDinc.y));


            }else {
                System.out.println("No light.");
            }

        }
    }

    @Override
    public void keyPressed(Window<?> window, int keycode, ModifierKeys mods) {
        if(keycode == KeyCodes.SPACE){
            System.out.println("Fire!");

            Vector4 out = new Vector4(0f, 0f, -1f, 0f);
            out = cameraModel.getOrbit().transpose().times(out);

            Vector3 start =cameraModel.getCenter().plus(cameraModel.getOrbit().transpose().times(new Vector4(0,0,cameraModel.getDistance(), 1f)).getXYZ());

            System.out.println("Shooting from " + start + " into " + out.getXYZ());

            ControllableSubLightModel light = lightModel.getLight(0);

            if(light.exists()){
                System.out.println("We have a light");
                float radius = light.getDistance();
                System.out.println("Distance: " + radius);

                Vector2 azANDinc = SphereMath.ShootSphere(start, out.getXYZ().normalized(), radius);

                System.out.println("Setting az and inc to " + azANDinc);
                light.setAzimuth(Math.toDegrees(azANDinc.x));
                light.setInclination(Math.toDegrees(azANDinc.y));


            }else {
                System.out.println("No light.");
            }

        }
    }
}
