package kintsugi3d.builder.resources;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ProjectDataCard {
    private String cardId;
    private String headerName;
    private LinkedHashMap<String,String> textFields;
    private String imagePath;

    public ProjectDataCard(String headerName, String imagePath, Map<String, String> textFields) {
        this.headerName = headerName;
        this.imagePath = imagePath;
        this.textFields = new LinkedHashMap<>(textFields);
        this.cardId = UUID.randomUUID().toString();
    }

    public String getCardId() { return cardId; }
    public String getHeaderName() { return headerName; }
    public String getValue(String key) {
        if (!textFields.containsKey(key)) {
            throw new IllegalArgumentException("Key does not exist.");
        }
            return textFields.get(key);
    }
    public LinkedHashMap<String, String> getTextContent() { return textFields; }
    public String getImagePath() { return imagePath; }

}
