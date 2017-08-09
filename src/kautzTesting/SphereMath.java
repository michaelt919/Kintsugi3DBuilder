package kautzTesting;//Created by alexk on 8/2/2017.

import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;

public class SphereMath {

    /***
     * find the closest point on a line to the origin.
     * The equation of the line is (x,y,z)=startingPoint+lambda*direction;
     * @param startingPoint
     * @param direction
     * @return lambda
     */
    private static float minimizeLineCenterDistance(Vector3 startingPoint, Vector3 direction){
        return -(startingPoint.dot(direction) / direction.dot(direction));
    }

    /***
     * finds point of line intersection with sphere centered at Origin;
     * @param lineStartPoint
     * @param lineDirection
     * @param radius
     * @return the point
     */
    private static Vector3 pointOnSphere(Vector3 lineStartPoint, Vector3 lineDirection, float radius){
        float lambda = minimizeLineCenterDistance(lineStartPoint, lineDirection);
        Vector3 closeistPoint = lineStartPoint.plus(lineDirection.times(lambda));
        float distanceToCenter = closeistPoint.length();
        float remainingSquard = radius*radius - distanceToCenter*distanceToCenter;

        if(remainingSquard <= 0f){
            System.out.println("Missed sphere");
            return Vector3.ZERO;
        }else {
            float remaing = (float) Math.sqrt(remainingSquard);
            return lineStartPoint.plus(lineDirection.times(lambda+remaing));
        }
    }/***
     * finds point of line intersection with sphere centered at Origin;
     * @param lineStartPoint
     * @param lineDirection
     * @param radius
     * @return the point
     */
    private static Vector3 pointOnSphereInner(Vector3 lineStartPoint, Vector3 lineDirection, float radius){
        float lambda = minimizeLineCenterDistance(lineStartPoint, lineDirection);
        Vector3 closeistPoint = lineStartPoint.plus(lineDirection.times(lambda));
        float distanceToCenter = closeistPoint.length();
        float remainingSquard = radius*radius - distanceToCenter*distanceToCenter;

        if(remainingSquard <= 0f){
            System.out.println("Missed sphere");
            return Vector3.ZERO;
        }else {
            float remaing = (float) Math.sqrt(remainingSquard);
            return lineStartPoint.plus(lineDirection.times(lambda-remaing));
        }
    }

    /***
     * given a point on a sphere centerd at Origin and its radius, returns poler cordinits
     * @param point
     * @param distance
     * @return (azmith, Inc)
     */
    private static Vector2 quickAzimithInc(Vector3 point, float distance){
        point = point.dividedBy(distance);
        float inc =  (float) Math.asin(point.y);

        double cosInc = Math.cos(inc);
        double sinAz = point.x / cosInc;
        double cosAz = point.z / cosInc;

        float az = - (float) Math.atan2(sinAz, cosAz);

        System.out.println("From " + point);
        return new Vector2(az, inc);
    }

    /***
     * returns intersection of sphere around orign and line;
     * @param lineStart
     * @param lineDirection
     * @param radius
     * @return (azmith, inc)
     */
    public static Vector2 ShootSphere(Vector3 lineStart, Vector3 lineDirection, float radius){
        lineDirection = lineDirection.normalized();
        Vector3 pointOnSphere = pointOnSphere(lineStart, lineDirection, radius);
        if(pointOnSphere.length() == 0f) return Vector2.ZERO;
        return quickAzimithInc(pointOnSphere, radius);
    }

    /***
     * returns intersection of sphere around orign and line;
     * @param lineStart
     * @param lineDirection
     * @param radius
     * @return (azmith, inc)
     */
    public static Vector2 ShootSphereInner(Vector3 lineStart, Vector3 lineDirection, float radius){
        lineDirection = lineDirection.normalized();
        Vector3 pointOnSphere = pointOnSphereInner(lineStart, lineDirection, radius);
        if(pointOnSphere.length() == 0f) return Vector2.ZERO;
        return quickAzimithInc(pointOnSphere, radius);
    }

}
