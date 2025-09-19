package kintsugi3d.builder.javafx.controllers.paged;

import java.util.Map;
import java.util.Set;

public interface SelectionPage<T> extends Page<T, T>
{
    String getPrompt();
    void setPrompt(String prompt);

    Set<Map.Entry<String, Page<? super T, ?>>> getChoices();
    Page<? super T, ?> getChoicePage(String choiceLabel);
    void addChoice(String choiceLabel, Page<? super T, ?> page);

    void selectChoice(String choiceLabel);
}
