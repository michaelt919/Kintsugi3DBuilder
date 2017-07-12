package tetzlaff.ibr.gui2.controllers.scene.camera;

import javafx.beans.property.*;

public class CameraSetting {

    //   Azimuth

    //   Inclination

    //   Distance

    //   Twist

    private StringProperty name = new SimpleStringProperty();
    private DoubleProperty azimuth = new SimpleDoubleProperty();
    private DoubleProperty inclination = new SimpleDoubleProperty();
    private DoubleProperty distance = new SimpleDoubleProperty();
    private DoubleProperty twist = new SimpleDoubleProperty();

    public CameraSetting(String name) {
        setName(name);
        setAzimuth(0.0);
        setInclination(90.0);
        setDistance(1.0);
        setTwist(0.0);
    }

    private CameraSetting(String name, double azimuth, double inclination, double distance, double twist){
        setName(name);
        setAzimuth(azimuth);
        setInclination(inclination);
        setDistance(distance);
        setTwist(twist);
    }

    public CameraSetting duplicate(){
        return new CameraSetting(
                (this.getName() + " copy"),
                this.getAzimuth(),
                this.getInclination(),
                this.getDistance(),
                this.getTwist()
        );
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public double getAzimuth() {
        return azimuth.get();
    }

    public DoubleProperty azimuthProperty() {
        return azimuth;
    }

    public void setAzimuth(double azimuth) {
        this.azimuth.set(azimuth);
    }

    public double getInclination() {
        return inclination.get();
    }

    public DoubleProperty inclinationProperty() {
        return inclination;
    }

    public void setInclination(double inclination) {
        this.inclination.set(inclination);
    }

    public double getDistance() {
        return distance.get();
    }

    public DoubleProperty distanceProperty() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance.set(distance);
    }

    public double getTwist() {
        return twist.get();
    }

    public DoubleProperty twistProperty() {
        return twist;
    }

    public void setTwist(double twist) {
        this.twist.set(twist);
    }


    @Override
    public String toString() {
        return this.getName();
    }
}
