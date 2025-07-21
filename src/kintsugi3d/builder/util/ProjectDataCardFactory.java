package kintsugi3d.builder.util;

import kintsugi3d.builder.resources.ProjectDataCard;

import java.io.File;
import java.util.LinkedHashMap;

public class ProjectDataCardFactory {

    public static ProjectDataCard createProjectDataCard(File file) {

        LinkedHashMap<String, String> map = new LinkedHashMap<>() {{
            put("File Name", file.getName()); put("Resolution", "320x200"); put("Size", String.valueOf(file.length()));
        }};
        return new ProjectDataCard(file.getName(), "image-path", map);
    }

    public static ProjectDataCard createProjectDataCard(LinkedHashMap<String,String> content) {
        return new ProjectDataCard(content.get("File Name"), "image-path", content);
    }

    public static ProjectDataCard createProjectDataCard(String title, String imagePath) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>() {{
            put("File Name", imagePath); put("Resolution", "320x200"); put("Size", "0");
        }};
        return new ProjectDataCard(title, imagePath, map);
    }

    public static ProjectDataCard createCameraCard(String title) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>() {{
            put("File Name", "path"); put("Resolution", "320x200"); put("Size", "0");
        }};
        ProjectDataCard card = new ProjectDataCard(title, "path", map);
        Runnable run = () -> {};
        card.addButton(0,"Replace", run);

        return card;
    }

    public static ProjectDataCard createCameraCard(String title, String imagePath, LinkedHashMap<String, String> textContent) {
        ProjectDataCard card = new ProjectDataCard(title, imagePath, textContent);
        Runnable run = () -> {};
        card.addButton(0, "Replace", run);
        card.addButton(1, "Refresh", run);
        card.addButton(1, "Disable", run);
        card.addButton(1, "Delete ", run);
        return card;
    }
}
