/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.gl.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.bytesource.ByteSourceInputStream;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipHelper {
    private static final Logger log = LoggerFactory.getLogger(UnzipHelper.class);
    static final String[] validExtensions = {"*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.png", "*.bmp", "*.wbmp"};

    private UnzipHelper() {
    }

    public static String unzipToString(String zipFileName) throws IOException {
        //unzip the zip file (which should be a .psx) and return the contents as a string
        //intended to only unzip one file
        //Note: if this function unzips a file with multiple text files, it will simply concatenate them

        ZipInputStream zis= new ZipInputStream(new FileInputStream(zipFileName));
        try{
            byte[] buffer = new byte[1024];
            StringBuilder s = new StringBuilder();
            int read = 0;
            while ((zis.getNextEntry())!= null) {
                while ((read = zis.read(buffer, 0, 1024)) >= 0) {
                    s.append(new String(buffer, 0, read));
                }
            }
            return s.toString();
        }
        catch(Exception e){
            log.error("Error unzipping file:", e);
        }
        finally{
            zis.closeEntry();
            zis.close();
        }
        return "";
    }

    public static Document convertStringToDocument(String xmlStr) {
        //Taken from https://www.digitalocean.com/community/tutorials/java-convert-string-to-xml-document-and-xml-document-to-string
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xmlStr)));
        } catch (Exception e) {
            log.error("Error converting document:", e);
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
            log.error("Error converting document:", e);
        }

        return null;
    }

    public static List<Image> unzipImages(String zipFilePath){
        ArrayList<Image> images = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (isValidImageType(entryName)) {
                    Image imageData = readImageData(zipInputStream, entryName);
                    images.add(imageData);
                }
                zipInputStream.closeEntry();
            }

        } catch (IOException | ImageReadException e) {
            log.error("Error unzipping images:", e);
        }

        log.info("Total images extracted: " + images.size());
        return images;
    }

    private static Image readImageData(InputStream inputStream, String fileName) throws IOException, ImageReadException {
        //convert unzipped image if it is a .tif or .tiff file
        if (fileName.toLowerCase().matches(".*\\.tiff?")) {//convert image if it is a .tif or .tiff
            TiffImageParser tiffImageParser = new TiffImageParser();
            ByteSourceInputStream byteSourceInputStream = new ByteSourceInputStream(inputStream, fileName);
            BufferedImage bufferedImage = tiffImageParser.getBufferedImage(byteSourceInputStream, null);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        }

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

    public static Document unzipToDocument(String zipPath) throws IOException {
        return UnzipHelper.convertStringToDocument(UnzipHelper.unzipToString(zipPath));
    }
}
