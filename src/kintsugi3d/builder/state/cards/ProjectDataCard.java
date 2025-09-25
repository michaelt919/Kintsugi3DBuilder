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
