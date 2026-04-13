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

package kintsugi3d.builder.javafx.multithread;

import javafx.application.Platform;
import kintsugi3d.builder.state.cards.CardsModel;
import kintsugi3d.builder.state.cards.ProjectDataCard;

import java.util.List;

public class SynchronizedCardsModel implements CardsModel
{
    private final CardsModel base;

    public SynchronizedCardsModel(CardsModel base)
    {
        this.base = base;
    }

    @Override
    public List<ProjectDataCard> getCardList()
    {
        return base.getCardList();
    }

    @Override
    public void setCardList(List<ProjectDataCard> cards)
    {
        Platform.runLater(() -> base.setCardList(cards));
    }

    @Override
    public void deleteCard(ProjectDataCard card)
    {
        Platform.runLater(() -> base.deleteCard(card));
    }

    @Override
    public void confirm(String title, String header, String message, Runnable onConfirm)
    {
        Platform.runLater(() -> base.confirm(title, header, message, onConfirm));
    }
}
