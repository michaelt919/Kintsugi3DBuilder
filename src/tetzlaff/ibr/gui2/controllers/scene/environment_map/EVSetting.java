package tetzlaff.ibr.gui2.controllers.scene.environment_map;

import java.io.File;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

import org.jdom2.Element;

import tetzlaff.ibr.util.StaticHouse;
import tetzlaff.misc.XML_Writable;

public class EVSetting implements XML_Writable{

    private final BooleanProperty evUseImage = new SimpleBooleanProperty();
    private final BooleanProperty evUseColor = new SimpleBooleanProperty();
    private final BooleanProperty bpUseImage = new SimpleBooleanProperty();
    private final BooleanProperty bpUseColor = new SimpleBooleanProperty();
    private final BooleanProperty imagePathsRelative = new SimpleBooleanProperty();

    private final Property<File> evImageFile = new SimpleObjectProperty<>();
    private final Property<File> bpImageFile = new SimpleObjectProperty<>();

    private final DoubleProperty evColorIntensity = StaticHouse.bound(0, Double.MAX_VALUE, new SimpleDoubleProperty());
    private final DoubleProperty evRotation = StaticHouse.wrap(-180, 180, new SimpleDoubleProperty());
    private final Property<Color> evColor = new SimpleObjectProperty<>();
    private final Property<Color> bpColor = new SimpleObjectProperty<>();
    private final StringProperty name = new SimpleStringProperty();
    private final BooleanProperty locked = new SimpleBooleanProperty();

    public EVSetting(Boolean evUseImage,Boolean evUseColor,Boolean bpUseImage,Boolean bpUseColor,Boolean imagePathsRelative,File evImageFile, File bpImageFile, Double evColorIntensity,Double evRotation,Color evColor,Color bpColor,String name, Boolean locked) {
        this.evUseImage.setValue(evUseImage);
        this.evUseColor.setValue(evUseColor);
        this.bpUseImage.setValue(bpUseImage);
        this.bpUseColor.setValue(bpUseColor);
        this.imagePathsRelative.setValue(imagePathsRelative);
        this.evImageFile.setValue(evImageFile);
        this.bpImageFile.setValue(bpImageFile);
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
                .setAttribute("evImageFile", evImageFile.getValue().getPath())
                .setAttribute("bpImageFile", bpImageFile.getValue().getPath())

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

                new File(element.getAttributeValue("evImageFile")),
                new File(element.getAttributeValue("bpImageFile")),

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
                evImageFile.getValue(),
                bpImageFile.getValue(),
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

    public File getEvImageFile() {
        return evImageFile.getValue();
    }

    public Property<File> evImageFileProperty() {
        return evImageFile;
    }

    public void setEvImageFile(File evImageFile) {
        this.evImageFile.setValue(evImageFile);
    }

    public File getBpImageFile() {
        return bpImageFile.getValue();
    }

    public Property<File> bpImageFileProperty() {
        return bpImageFile;
    }

    public void setBpImageFile(File bpImageFile) {
        this.bpImageFile.setValue(bpImageFile);
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
