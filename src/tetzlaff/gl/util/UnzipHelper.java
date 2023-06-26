package tetzlaff.gl.util;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipHelper {
    private UnzipHelper() {
    }

    public static String unzipToString(String zipFileName) throws IOException {
        //unzip the zip file (which should be a .psx) and return the contents as a string
        //intended to only unzip one file
        //string will be converted to XML outside of this function
        //TODO: NEED TO ADD SAFETY FOR WHEN IT OPENS A ZIP WITH MULTIPLE FILES?
        //TODO: SONARLINT UPSET BECAUSE THE ZIPINPUTSTREAM MAY NOT BE CLOSED (if an exception occurs)
        ZipInputStream zis= new ZipInputStream(new FileInputStream(zipFileName));
        byte[] buffer = new byte[1024];
        StringBuilder s = new StringBuilder();
        int read = 0;
        while ((zis.getNextEntry())!= null) {
            while ((read = zis.read(buffer, 0, 1024)) >= 0) {
                s.append(new String(buffer, 0, read));
            }
        }
        zis.closeEntry();
        zis.close();
        return s.toString();
    }

    public static Document convertStringToDocument(String xmlStr) {
        //Taken from https://www.digitalocean.com/community/tutorials/java-convert-string-to-xml-document-and-xml-document-to-string
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
