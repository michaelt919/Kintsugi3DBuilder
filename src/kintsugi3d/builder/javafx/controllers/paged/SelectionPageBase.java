/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

import java.util.*;

public abstract class SelectionPageBase<T, ControllerType extends SelectionPageController<T>>
    extends PageBase<T, T, ControllerType> implements SelectionPage<T>
{
    private String prompt = "Select an option:";
    private final HashMap<String, Page<? super T, ?>> choicePages = new LinkedHashMap<>(4);

    SelectionPageBase(FXMLLoader loader)
    {
        super(loader);
    }

    @Override
    public String getPrompt()
    {
        return prompt;
    }

    @Override
    public void setPrompt(String prompt)
    {
        this.prompt = prompt;
    }

    @Override
    public void initController()
    {
        this.getController().setPage(this);
        this.getController().initPage();
    }

    @Override
    public Page<? super T, ?> getChoicePage(String choiceLabel)
    {
        return choicePages.get(choiceLabel);
    }

    @Override
    public void addChoice(String choiceLabel, Page<? super T, ?> page)
    {
        choicePages.put(choiceLabel, page);

        if (page.getPrevPage() == null)
        {
            // set default link back (useful for fallback pages which weren't accessed via next button)
            page.setPrevPage(this);
        }
    }

    @Override
    public void selectChoice(String choiceLabel)
    {
        setNextPage(getChoicePage(choiceLabel));
    }

    @Override
    public Set<Map.Entry<String, Page<? super T, ?>>> getChoices()
    {
        return Collections.unmodifiableSet(choicePages.entrySet());
    }
}
