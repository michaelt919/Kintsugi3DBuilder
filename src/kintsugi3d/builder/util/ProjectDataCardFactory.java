package kintsugi3d.builder.util;

import kintsugi3d.builder.resources.ProjectDataCard;

import java.io.File;
import java.util.LinkedHashMap;

public class ProjectDataCardFactory {

    public static ProjectDataCard createProjectDataCard(File file) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>() {{
            put("File Name", file.getName()); put("Resolution", "320x200"); put("Size", String.valueOf(file.length())); put("Purpose", "This is a description pertaining to the this card."); put("Labels", "");
        }};
        return new ProjectDataCard(file.getName(), "image-path", map);
    }
}
