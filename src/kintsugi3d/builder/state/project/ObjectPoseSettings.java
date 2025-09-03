package kintsugi3d.builder.state.project;

public interface ObjectPoseSettings
{
    double getCenterX();

    void setCenterX(double centerX);

    double getCenterY();

    void setCenterY(double centerY);

    double getCenterZ();

    void setCenterZ(double centerZ);

    double getRotateY();

    void setRotateY(double rotateY);

    double getRotateX();

    void setRotateX(double rotateX);

    double getRotateZ();

    void setRotateZ(double rotateZ);

    boolean isLocked();

    void setLocked(boolean locked);

    double getScale();

    void setScale(double scale);

    String getName();

    void setName(String name);
}
