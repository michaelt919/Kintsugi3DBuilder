package kintsugi3d.builder.javafx.controllers.paged;

public interface NonEntryPage<
        PrevPageType extends Page<?>,
        ControllerType extends PageController<?>>
    extends Page<ControllerType>
{
    void setPrevPage(PrevPageType page);
}
