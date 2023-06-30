package tetzlaff.gl.util;

import javafx.scene.image.Image;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipHelper {
    static final String[] validExtensions = {"*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.png", "*.bmp", "*.wbmp"};

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
            return builder.parse(new InputSource(new StringReader(xmlStr)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String convertDocumentToString(Document doc) {
        //Taken from https://www.digitalocean.com/community/tutorials/java-convert-string-to-xml-document-and-xml-document-to-string
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ArrayList<Image> unzipImages(String zipFilePath){
        ArrayList<Image> images = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (isValidImageType(entryName)) {
                    Image imageData = readImageData(zipInputStream);
                    images.add(imageData);
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Total images extracted: " + images.size());
        return images;
    }

    private static Image readImageData(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        ByteArrayInputStream imageInputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return new Image(imageInputStream);
    }

    private static boolean isValidImageType(String path) {
        for (String extension : validExtensions) {
            if (path.matches("." + extension)) {
                return true;
            }
        }
        return false;
    }
}
