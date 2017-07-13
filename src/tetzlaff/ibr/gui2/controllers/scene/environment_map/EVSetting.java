package tetzlaff.ibr.gui2.controllers.scene.environment_map;

import javafx.beans.property.*;
import org.jdom2.Element;
import tetzlaff.misc.XML_Writable;
import javafx.scene.paint.Color;

public class EVSetting implements XML_Writable{

    private final BooleanProperty evUseImage = new SimpleBooleanProperty();
    private final BooleanProperty evUseColor = new SimpleBooleanProperty();
    private final BooleanProperty bpUseImage = new SimpleBooleanProperty();
    private final BooleanProperty bpUseColor = new SimpleBooleanProperty();
    private final BooleanProperty imagePathsRelative = new SimpleBooleanProperty();
    private final StringProperty evImagePath = new SimpleStringProperty();
    private final StringProperty bpImagePath = new SimpleStringProperty();
    private final DoubleProperty evColorIntensity = new SimpleDoubleProperty();
    private final DoubleProperty evRotation = new SimpleDoubleProperty();
    private final Property<Color> evColor = new SimpleObjectProperty<>();
    private final Property<Color> bpColor = new SimpleObjectProperty<>();
    private final StringProperty name = new SimpleStringProperty();
    private final BooleanProperty locked = new SimpleBooleanProperty();
// BooleanProperty evUseImage
// BooleanProperty evUseColor
// BooleanProperty bpUseImage
// BooleanProperty bpUseColor
// BooleanProperty imagePathsRelative
// StringProperty evImagePath
// StringProperty bpImagePath
// DoubleProperty evColorIntensity
// DoubleProperty evRotation
// Property<Color> evColor
// Property<Color> bpColor
// StringProperty name

    public EVSetting(Boolean evUseImage,Boolean evUseColor,Boolean bpUseImage,Boolean bpUseColor,Boolean imagePathsRelative,String evImagePath,String bpImagePath,Double evColorIntensity,Double evRotation,Color evColor,Color bpColor,String name, Boolean locked) {
        this.evUseImage.setValue(evUseImage);
        this.evUseColor.setValue(evUseColor);
        this.bpUseImage.setValue(bpUseImage);
        this.bpUseColor.setValue(bpUseColor);
        this.imagePathsRelative.setValue(imagePathsRelative);
        this.evImagePath.setValue(evImagePath);
        this.bpImagePath.setValue(bpImagePath);
        this.evColorIntensity.setValue(evColorIntensity);
        this.evRotation.setValue(evRotation);
        this.evColor.setValue(evColor);
        this.bpColor.setValue(bpColor);
        this.name.setValue(name);
        this.locked.setValue(locked);
    }

    @Override
    public Element toJDOM2Element(){
        return new Element("EVSetting")
                .setAttribute("evUseImage", evUseImage.getValue().toString())
                .setAttribute("evUseColor", evUseColor.getValue().toString())
                .setAttribute("bpUseImage", bpUseImage.getValue().toString())
                .setAttribute("bpUseColor", bpUseColor.getValue().toString())
                .setAttribute("imagePathsRelative", imagePathsRelative.getValue().toString())
                .setAttribute("evImagePath", evImagePath.getValue())
                .setAttribute("bpImagePath", bpImagePath.getValue())
                .setAttribute("evColorIntensity", evColorIntensity.getValue().toString())
                .setAttribute("evRotation", evRotation.getValue().toString())
                .setAttribute("evColor", evColor.getValue().toString())
                .setAttribute("bpColor", bpColor.getValue().toString())
                .setAttribute("name", name.getValue())
                .setAttribute("locked", locked.getValue().toString());
    }


    public static EVSetting fromJDOM2Element(Element element){
        return new EVSetting(
                Boolean.valueOf(element.getAttributeValue("evUseImage")),
                Boolean.valueOf(element.getAttributeValue("evUseColor")),
                Boolean.valueOf(element.getAttributeValue("bpUseImage")),
                Boolean.valueOf(element.getAttributeValue("bpUseColor")),
                Boolean.valueOf(element.getAttributeValue("imagePathsRelative")),
                String.valueOf(element.getAttributeValue("evImagePath")),
                String.valueOf(element.getAttributeValue("bpImagePath")),
                Double.valueOf(element.getAttributeValue("evColorIntensity")),
                Double.valueOf(element.getAttributeValue("evRotation")),
                Color.valueOf(element.getAttributeValue("evColor")),
                Color.valueOf(element.getAttributeValue("bpColor")),
                String.valueOf(element.getAttributeValue("name")),
                Boolean.valueOf(element.getAttributeValue("locked"))
        );
    }

    @Override
    public String toString(){
        String out = name.getValue();
        if(locked.getValue()) out = "(L) " + out;
        return out;
    }


    public EVSetting duplicate(){
        return new EVSetting(
                evUseImage.getValue(),
                evUseColor.getValue(),
                bpUseImage.getValue(),
                bpUseColor.getValue(),
                imagePathsRelative.getValue(),
                evImagePath.getValue(),
                bpImagePath.getValue(),
                evColorIntensity.getValue(),
                evRotation.getValue(),
                evColor.getValue(),
                bpColor.getValue(),
                (name.getValue() + " copy"),
                locked.getValue()
        );

    }

    public boolean isEvUseImage() {
        return evUseImage.get();
    }

    public BooleanProperty evUseImageProperty() {
        return evUseImage;
    }

    public void setEvUseImage(boolean evUseImage) {
        this.evUseImage.set(evUseImage);
    }

    public boolean isEvUseColor() {
        return evUseColor.get();
    }

    public BooleanProperty evUseColorProperty() {
        return evUseColor;
    }

    public void setEvUseColor(boolean evUseColor) {
        this.evUseColor.set(evUseColor);
    }

    public boolean isBpUseImage() {
        return bpUseImage.get();
    }

    public BooleanProperty bpUseImageProperty() {
        return bpUseImage;
    }

    public void setBpUseImage(boolean bpUseImage) {
        this.bpUseImage.set(bpUseImage);
    }

    public boolean isBpUseColor() {
        return bpUseColor.get();
    }

    public BooleanProperty bpUseColorProperty() {
        return bpUseColor;
    }

    public void setBpUseColor(boolean bpUseColor) {
        this.bpUseColor.set(bpUseColor);
    }

    public boolean isImagePathsRelative() {
        return imagePathsRelative.get();
    }

    public BooleanProperty imagePathsRelativeProperty() {
        return imagePathsRelative;
    }

    public void setImagePathsRelative(boolean imagePathsRelative) {
        this.imagePathsRelative.set(imagePathsRelative);
    }

    public String getEvImagePath() {
        return evImagePath.get();
    }

    public StringProperty evImagePathProperty() {
        return evImagePath;
    }

    public void setEvImagePath(String evImagePath) {
        this.evImagePath.set(evImagePath);
    }

    public String getBpImagePath() {
        return bpImagePath.get();
    }

    public StringProperty bpImagePathProperty() {
        return bpImagePath;
    }

    public void setBpImagePath(String bpImagePath) {
        this.bpImagePath.set(bpImagePath);
    }

    public double getEvColorIntensity() {
        return evColorIntensity.get();
    }

    public DoubleProperty evColorIntensityProperty() {
        return evColorIntensity;
    }

    public void setEvColorIntensity(double evColorIntensity) {
        this.evColorIntensity.set(evColorIntensity);
    }

    public double getEvRotation() {
        return evRotation.get();
    }

    public DoubleProperty evRotationProperty() {
        return evRotation;
    }

    public void setEvRotation(double evRotation) {
        this.evRotation.set(evRotation);
    }

    public Color getEvColor() {
        return evColor.getValue();
    }

    public Property<Color> evColorProperty() {
        return evColor;
    }

    public void setEvColor(Color evColor) {
        this.evColor.setValue(evColor);
    }

    public Color getBpColor() {
        return bpColor.getValue();
    }

    public Property<Color> bpColorProperty() {
        return bpColor;
    }

    public void setBpColor(Color bpColor) {
        this.bpColor.setValue(bpColor);
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

    public boolean isLocked() {
        return locked.get();
    }

    public BooleanProperty lockedProperty() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked.set(locked);
    }
}
