package kintsugi3d.builder.state;

import java.util.*;

public class ProjectDataCard
{
    private final UUID cardId;
    private final String title;
    private final String imagePath;
    private final LinkedHashMap<String, String> textFields;
    private final List<LinkedHashMap<String, Runnable>> actionGroups;

    public ProjectDataCard(String title, String imagePath, Map<String, String> textFields)
    {
        this.cardId = UUID.randomUUID();
        this.title = title;
        this.imagePath = imagePath;
        this.textFields = new LinkedHashMap<>(textFields);
        this.actionGroups = new ArrayList<>()
        {{
            add(new LinkedHashMap<>());
            add(new LinkedHashMap<>());
        }};
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

    public LinkedHashMap<String, String> getTextContent()
    {
        return textFields;
    }

    public void addButton(int buttonGroup, String label, Runnable runnable)
    {
        this.actionGroups.get(buttonGroup).put(label, runnable);
    }

    public List<LinkedHashMap<String, Runnable>> getActions()
    {
        return actionGroups;
    }
}
