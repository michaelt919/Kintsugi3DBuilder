package kintsugi3d.builder.javafx.multithread;

import javafx.application.Platform;
import kintsugi3d.builder.javafx.util.MultithreadValue;
import kintsugi3d.builder.state.ProjectModel;
import kintsugi3d.gl.vecmath.Vector3;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;

/**
 * Wraps project model for thread safety when accessed from the graphics thread
 * (Platform.runLater needed since setters typically are bound to JavaFX properties)
 */
public class ProjectModelWrapper implements ProjectModel
{
    private final ProjectModel baseModel;

    private final MultithreadValue<Boolean> projectOpen;
    private final MultithreadValue<String> projectName;
    private final MultithreadValue<Boolean> projectLoaded;
    private final MultithreadValue<Boolean> projectProcessed;
    private final MultithreadValue<Integer> processedTextureResolution;
    private final MultithreadValue<Vector3> modelSize;
    private final MultithreadValue<File> colorCheckerFile;

    public ProjectModelWrapper(ProjectModel baseModel)
    {
        this.baseModel = baseModel;
        this.projectOpen = MultithreadValue.createFromFunctions(baseModel::isProjectOpen, baseModel::setProjectOpen);
        this.projectName = MultithreadValue.createFromFunctions(baseModel::getProjectName, baseModel::setProjectName);
        this.projectLoaded = MultithreadValue.createFromFunctions(baseModel::isProjectLoaded, baseModel::setProjectLoaded);
        this.projectProcessed = MultithreadValue.createFromFunctions(baseModel::isProjectProcessed, baseModel::setProjectProcessed);
        this.processedTextureResolution = MultithreadValue.createFromFunctions(baseModel::getProcessedTextureResolution, baseModel::setProcessedTextureResolution);
        this.modelSize = MultithreadValue.createFromFunctions(baseModel::getModelSize, baseModel::setModelSize);
        this.colorCheckerFile = MultithreadValue.createFromFunctions(baseModel::getColorCheckerFile, baseModel::setColorCheckerFile);
    }

    @Override
    public File openProjectFile(File projectFile) throws IOException, ParserConfigurationException, SAXException
    {
        return baseModel.openProjectFile(projectFile);
    }

    @Override
    public void saveProjectFile(File projectFile, File vsetFile) throws IOException, ParserConfigurationException, TransformerException
    {
        baseModel.saveProjectFile(projectFile, vsetFile);
    }

    @Override
    public File getColorCheckerFile()
    {
        return colorCheckerFile.getValue();
    }

    @Override
    public void setColorCheckerFile(File colorCheckerFile)
    {
        this.colorCheckerFile.setValue(colorCheckerFile);
    }

    @Override
    public boolean isProjectOpen()
    {
        return projectOpen.getValue();
    }

    @Override
    public void setProjectOpen(boolean projectOpen)
    {
        this.projectOpen.setValue(projectOpen);
    }

    @Override
    public String getProjectName()
    {
        return projectName.getValue();
    }

    @Override
    public void setProjectName(String projectName)
    {
        this.projectName.setValue(projectName);
    }

    @Override
    public boolean isProjectLoaded()
    {
        return projectLoaded.getValue();
    }

    @Override
    public void setProjectLoaded(boolean projectLoaded)
    {
        this.projectLoaded.setValue(projectLoaded);
    }

    @Override
    public boolean isProjectProcessed()
    {
        return projectProcessed.getValue();
    }

    @Override
    public void setProjectProcessed(boolean projectProcessed)
    {
        this.projectProcessed.setValue(projectProcessed);
    }

    @Override
    public int getProcessedTextureResolution()
    {
        return processedTextureResolution.getValue();
    }

    @Override
    public void setProcessedTextureResolution(int processedTextureResolution)
    {
        this.processedTextureResolution.setValue(processedTextureResolution);
    }

    @Override
    public Vector3 getModelSize()
    {
        return modelSize.getValue();
    }

    @Override
    public void setModelSize(Vector3 modelSize)
    {
        this.modelSize.setValue(modelSize);
    }

    @Override
    public void notifyProcessingComplete()
    {
        Platform.runLater(baseModel::notifyProcessingComplete);
    }
}
