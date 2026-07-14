/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state.cards;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProjectDataCard
{
    private final String internalName;
    private final UUID cardId;
    private final String title;
    private final String imagePath;
    private final Map<String, String> textFields;
    private final List<? extends Map<String, Runnable>> actionGroups;
    private boolean isDisabled = false; // TODO: consider getting from viewset

    public ProjectDataCard(String internalName, String title, String imagePath, Map<String, String> textFields, List<? extends Map<String, Runnable>> actionGroups)
    {
        this.internalName = internalName;
        this.cardId = UUID.randomUUID();
        this.title = title;
        this.imagePath = imagePath;
        this.textFields = Collections.unmodifiableMap(textFields);
        this.actionGroups = Collections.unmodifiableList(actionGroups);
    }

    public ProjectDataCard(String internalName, String title, String imagePath, Map<String, String> textFields, Map<String, Runnable> actions, boolean isDisabled)
    {
        this(internalName, title, imagePath, textFields, List.of(actions));
        this.isDisabled = isDisabled;
    }
    public ProjectDataCard(String internalName, String title, String imagePath, Map<String, String> textFields, Map<String, Runnable> actions)
    {
        this(internalName, title, imagePath, textFields, List.of(actions));
    }

    public ProjectDataCard(String internalName, String title, String imagePath, Map<String, String> textFields)
    {
        this(internalName, title, imagePath, textFields, List.of());
    }

    public ProjectDataCard(String internalName, String title, String imagePath)
    {
        this(internalName, title, imagePath, Map.of());
    }

    public String getInternalName()
    {
        return internalName;
    }

    public UUID getCardId()
    {
        return cardId;
    }

    public String getTitle()
    {
        if (isDisabled)
        {
            return String.format("%s - DISABLED", title);
        }
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

    public boolean isDisabled() { return isDisabled; }

    public void setIsDisabled(boolean isDisabled) { this.isDisabled = isDisabled; }
}
