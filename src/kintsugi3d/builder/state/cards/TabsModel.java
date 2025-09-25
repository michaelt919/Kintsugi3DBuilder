package kintsugi3d.builder.state.cards;

public interface TabsModel
{
    void addTab(String tabName, ProjectDataCardFactory cardFactory);
    void clearTabs();
    CardsModel getTab(String label);
}
