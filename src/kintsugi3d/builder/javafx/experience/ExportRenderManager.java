package kintsugi3d.builder.javafx.experience;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.stage.Window;
import kintsugi3d.builder.javafx.core.JavaFXState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class ExportRenderManager
{
    private static final Logger LOG = LoggerFactory.getLogger(ExportRenderManager.class);

    private static final File EXPORT_CLASS_DEFINITION_FILE = new File("export-classes.txt");

    private final List<ExportRender> exportRenderList = new ArrayList<>(4);

    private BooleanExpression anyModalOpen;

    public boolean isAnyModalOpen()
    {
        return anyModalOpen.get();
    }

    public BooleanExpression getAnyModalOpenProperty()
    {
        return anyModalOpen;
    }

    public List<ExportRender> getList()
    {
        return exportRenderList;
    }

    public void initialize(Window parentWindow, JavaFXState state)
    {
        if (EXPORT_CLASS_DEFINITION_FILE.exists())
        {
            try (Scanner scanner = new Scanner(EXPORT_CLASS_DEFINITION_FILE, StandardCharsets.UTF_8))
            {
                scanner.useLocale(Locale.ROOT);

                while (scanner.hasNext())
                {
                    String fxmlURL = scanner.next();

                    if (scanner.hasNextLine())
                    {
                        String menuName = scanner.nextLine().trim();
                        ExportRender exportRender = new ExportRender(fxmlURL, menuName);
                        exportRender.initialize(parentWindow, state);
                        exportRenderList.add(exportRender);
                    }
                }
            }
            catch (IOException e)
            {
                LOG.error("Failed to find export classes file:", e);
            }
        }

        anyModalOpen = exportRenderList.stream()
            .map(experience -> experience.getModal().getOpenProperty())
            .reduce(BooleanExpression::or)
            .orElse(new SimpleBooleanProperty(false));
    }
}
