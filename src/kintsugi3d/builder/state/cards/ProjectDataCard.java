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

package kintsugi3d.builder.state.cards;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProjectDataCard
{
    private final UUID cardId;
    private final String title;
    private final String imagePath;
    private final Map<String, String> textFields;
    private final List<? extends Map<String, Runnable>> actionGroups;

    public ProjectDataCard(String title, String imagePath, Map<String, String> textFields, List<? extends Map<String, Runnable>> actionGroups)
    {
        this.cardId = UUID.randomUUID();
        this.title = title;
        this.imagePath = imagePath;
        this.textFields = textFields;
        this.actionGroups = actionGroups;
    }

    public ProjectDataCard(String title, String imagePath, Map<String, String> textFields, Map<String, Runnable> actions)
    {
        this(title, imagePath, textFields, List.of(actions));
    }

    public ProjectDataCard(String title, String imagePath, Map<String, String> textFields)
    {
        this(title, imagePath, textFields, List.of());
    }

    public ProjectDataCard(String title, String imagePath)
    {
        this(title, imagePath, Map.of());
    }

    public UUID getCardId()
    {
        return cardId;
    }

    public String getTitle()
    {
        return title;
    }

    public String getImagePath()
    {
        return imagePath;
    }

    public String getValue(String key)
    {
        if (!textFields.containsKey(key))
        {
            throw new IllegalArgumentException("Key does not exist.");
        }
        return textFields.get(key);
    }

    public Map<String, String> getTextContent()
    {
        return textFields;
    }

    public List<? extends Map<String, Runnable>> getActions()
    {
        return actionGroups;
    }
}
