package kintsugi3d.builder.javafx.controllers.paged;

import java.util.Map;

public interface SelectionPage<T> extends Page<T, T>, Iterable<Map.Entry<String, Page<? super T, ?>>>
{
    Page<? super T, ?> getChoicePage(String choiceLabel);
    void addChoice(String choiceLabel, Page<? super T, ?> page);
    void selectChoice(String choiceLabel);
}
