package kintsugi3d.builder.resources;

import java.util.*;

public class ProjectDataCard {
    private UUID cardId;
    private String title;
    private String imagePath;
    private LinkedHashMap<String,String> textFields;
    private List<LinkedHashMap<String, Runnable>> actionGroups;

    public ProjectDataCard(String title, String imagePath, Map<String, String> textFields) {
        this.cardId = UUID.randomUUID();
        this.title = title;
        this.imagePath = imagePath;
        this.textFields = new LinkedHashMap<>(textFields);
        this.actionGroups = new ArrayList<>() {{
            add(new LinkedHashMap<>());
            add(new LinkedHashMap<>());
        }};
    }

    public UUID getCardId() { return cardId; }
    public String getTitle() { return title; }
    public String getImagePath() { return imagePath; }
    public String getValue(String key) {
        if (!textFields.containsKey(key)) {
            throw new IllegalArgumentException("Key does not exist.");
        }
            return textFields.get(key);
    }
    public LinkedHashMap<String, String> getTextContent() { return textFields; }
    public void addButton(int buttonGroup, String label, Runnable runnable) {
        this.actionGroups.get(buttonGroup).put(label,runnable);
    }

    public List<LinkedHashMap<String,Runnable>> getActions() {
        return actionGroups;
    }
}
