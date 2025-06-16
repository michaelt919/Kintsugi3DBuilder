package kintsugi3d.builder.resources;

import java.util.List;
import java.util.UUID;

public class ProjectDataCard {
    private String cardId;
    private String headerName;
    private String fileName;
    private String resolution;
    private String fileSize;
    private String description;
    private List<String> labels;
    private String imagePath;

    public ProjectDataCard(String headerName, String fileName, String resolution, String fileSize, String description, List<String> labels, String imagePath) {
        this.headerName = headerName;
        this.fileName = fileName;
        this.resolution = resolution;
        this.fileSize = fileSize;
        this.description = description;
        this.labels = labels;
        this.imagePath = imagePath;
        this.cardId = UUID.randomUUID().toString();
    }

    public String getCardId() { return cardId; }
    public String getHeaderName() { return headerName; }
    public String getFileName() { return fileName; }
    public String getResolution() { return resolution; }
    public String getFileSize() { return fileSize; }
    public String getDescription() { return description; }
    public List<String> getLabels() { return labels; }
    public String getImagePath() { return imagePath; }

}
