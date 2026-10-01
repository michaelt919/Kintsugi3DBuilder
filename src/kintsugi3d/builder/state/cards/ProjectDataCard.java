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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class ProjectDataCard
{
    private final String internalName;
    private final String fullResImageFilePath;
    private final UUID cardId;
    private final String title;
    private final String thumbnailPath;
    private final Map<String, String> textFields;
    private final Collection<? extends Map<String, Runnable>> actionGroups;
    private boolean isDisabled;

    public ProjectDataCard(
        String internalName, String title, String fullResImageFilePath, String thumbnailPath,
        Map<String, String> textFields, Collection<? extends Map<String, Runnable>> actionGroups, boolean isDisabled)
    {
        this.internalName = internalName;
        this.cardId = UUID.randomUUID();
        this.title = title;
        this.fullResImageFilePath = fullResImageFilePath;
        this.thumbnailPath = thumbnailPath;
        this.textFields = Collections.unmodifiableMap(textFields);
        this.actionGroups = Collections.unmodifiableCollection(actionGroups);
        this.isDisabled = isDisabled;
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

    public String getThumbnailPath()
    {
        return thumbnailPath;
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

    public Collection<? extends Map<String, Runnable>> getActions()
    {
        return actionGroups;
    }

    public boolean isDisabled() { return isDisabled; }

    public void setIsDisabled(boolean isDisabled) { this.isDisabled = isDisabled; }

    public String getFullResImageFilePath()
    {
        return fullResImageFilePath;
    }
}
