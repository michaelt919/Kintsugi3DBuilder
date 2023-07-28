package kintsugi3d.builder.state;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;

public interface ProjectModel
{
    File openProjectFile(File projectFile) throws IOException, ParserConfigurationException, SAXException;
    void saveProjectFile(File projectFile, File vsetFile) throws IOException, ParserConfigurationException, TransformerException;
}
