/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import kintsugi3d.builder.core.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.bytesource.ByteSourceInputStream;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public final class UnzipHelper
{
    private static final Logger log = LoggerFactory.getLogger(UnzipHelper.class);
    static final String[] VALID_EXTENSIONS = {"*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.png", "*.bmp", "*.wbmp"};

    private UnzipHelper()
    {
    }

    private static String unzipToString(File zipFile) throws IOException
    {
        //unzip the zip file, find the .xml portion, and return it as a document
        log.info("Unzipping {}", zipFile);
        try (ZipFile file = new ZipFile(zipFile))
        {
            Enumeration<? extends ZipEntry> entries = file.entries();
            while (entries.hasMoreElements())
            {
                ZipEntry entry = entries.nextElement();
                // Check if entry is a directory
                if (entry.getName().endsWith(".xml"))
                {
                    try (InputStream inputStream = file.getInputStream(entry))
                    {
                        return fileStreamToString(inputStream);
                    }
                }
            }
        }
        return "";
    }

    /**
     * Unzips the first XML file it finds in a ZIP file
     * @param zipFile
     * @return
     * @throws IOException
     */
    public static Document unzipToDocument(File zipFile) throws IOException
    {
        return convertStringToDocument(unzipToString(zipFile));
    }

    /**
     * Unzips a specific file from a ZIP file
     * @param zipFile
     * @param targetFileName
     * @return
     * @throws IOException
     */
    public static String unzipToString(File zipFile, String targetFileName) throws IOException
    {
        try (ZipFile file = new ZipFile(zipFile))
        {
            Enumeration<? extends ZipEntry> entries = file.entries();
            while (entries.hasMoreElements())
            {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equals(targetFileName))
                {
                    // Found the desired file inside the zip
                    try (InputStream inputStream = file.getInputStream(entry))
                    {
                        return fileStreamToString(inputStream);
                    }
                }
            }
        }

        return "";
    }

    /**
     * Unzips a specific XML file from a ZIP file
     * @param zipFile
     * @param targetFileName
     * @return
     * @throws IOException
     */
    public static Document unzipToDocument(File zipFile, String targetFileName) throws IOException
    {
        return convertStringToDocument(unzipToString(zipFile, targetFileName));
    }

    //this function unzips a specific file within a zip directory
    private static String fileStreamToString(InputStream stream) throws IOException
    {
        byte[] buffer = new byte[1024];
        StringBuilder s = new StringBuilder();
        int read = 0;
        while ((read = stream.read(buffer, 0, 1024)) >= 0)
        {
            s.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
        }
        return s.toString();
    }

    public static Document convertStringToDocument(String xmlStr)
    {
        //Taken from https://www.digitalocean.com/community/tutorials/java-convert-string-to-xml-document-and-xml-document-to-string
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try
        {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xmlStr)));
        }
        catch (RuntimeException | IOException | ParserConfigurationException | SAXException e)
        {
            log.error("Error converting document:", e);
        }
        return null;
    }

    public static String convertDocumentToString(Document doc)
    {
        //Taken from https://www.digitalocean.com/community/tutorials/java-convert-string-to-xml-document-and-xml-document-to-string
        TransformerFactory tf = TransformerFactory.newInstance();
        try
        {
            Transformer transformer = tf.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.getBuffer().toString();
        }
        catch (TransformerException e)
        {
            log.error("Error converting document:", e);
        }

        return null;
    }

    public static List<Image> unzipImages(File zipFile)
    {
        ArrayList<Image> images = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile)))
        {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null)
            {
                String entryName = entry.getName();
                if (isValidImageType(entryName))
                {
                    Image imageData = readImageData(zipInputStream, entryName);
                    images.add(imageData);
                }
                zipInputStream.closeEntry();
            }

        }
        catch (IOException | ImageReadException e)
        {
            log.error("Error unzipping images:", e);
        }

        log.info("Total images extracted: {}", images.size());
        return images;
    }

    private static Image readImageData(InputStream inputStream, String fileName) throws IOException, ImageReadException
    {
        //convert unzipped image if it is a .tif or .tiff file
        if (fileName.toLowerCase().matches(".*\\.tiff?"))
        {//convert image if it is a .tif or .tiff
            TiffImageParser tiffImageParser = new TiffImageParser();
            ByteSourceInputStream byteSourceInputStream = new ByteSourceInputStream(inputStream, fileName);
            BufferedImage bufferedImage = tiffImageParser.getBufferedImage(byteSourceInputStream, null);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1)
        {
            outputStream.write(buffer, 0, bytesRead);
        }
        ByteArrayInputStream imageInputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return new Image(imageInputStream);
    }

    private static boolean isValidImageType(String path)
    {
        for (String extension : VALID_EXTENSIONS)
        {
            if (path.matches('.' + extension))
            {
                return true;
            }
        }
        return false;
    }

    public static Map<Integer, Image> unzipImagesToMap(File imgsDir)
    {
        //  <thumbnail camera_id="0" path="c0.png"/>

        Map<Integer, Image> imagesMap = new HashMap<>();

        //need this intermediary because Image objects don't store their source path when unzipped this way
        Map<String, Image> tempMap = new HashMap<>();

        Document docXml = null;
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(imgsDir)))
        {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null)
            {
                String entryName = entry.getName();
                if (entryName.endsWith(".xml"))
                {
                    try (ZipFile zFile = new ZipFile(imgsDir);
                         InputStream zis = zFile.getInputStream(entry))
                    {
                        docXml = convertStringToDocument(fileStreamToString(zis));
                    }
                }
                else
                {
                    //this is an image, add it to the temp map
                    tempMap.put(entryName, readImageData(zipInputStream, entryName));
                }
                zipInputStream.closeEntry();
            }

        }
        catch (IOException | ImageReadException e)
        {
            log.error("Error unzipping images:", e);
        }

        if (docXml == null)
        {
            return imagesMap;
        }

        NodeList list = docXml.getElementsByTagName("thumbnail");

        for (int i = 0; i < list.getLength(); ++i)
        {
            Element elem = (Element) list.item(i);

            String camId = elem.getAttribute("camera_id");
            int cameraId = i;
            if (!camId.isBlank())
            {
                cameraId = Integer.parseInt(camId);
            }

            String path = elem.getAttribute("path");
            Image img = tempMap.get(path);

            imagesMap.put(cameraId, img);
        }

        log.info("Total images extracted: {}", imagesMap.size());
        return imagesMap;
    }

    //   Adapted from https://www.baeldung.com/java-compress-and-uncompress
    public static void unzipToDirectory(File zippedDir, File destinationDir, ProgressMonitor monitor) throws IOException
    {
        byte[] buffer = new byte[1024];

        ZipFile zf = new ZipFile(zippedDir);
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zippedDir));
        ZipEntry zipEntry = zis.getNextEntry();

        int numEntries = zf.size();
        if (monitor != null)
        {
            monitor.setMaxProgress(numEntries);
        }
        int idx = 0;
        while (zipEntry != null)
        {
            //ignore directories for now
            if (!zipEntry.isDirectory())
            {
                if (monitor != null)
                {
                    monitor.setProgress(idx, zipEntry.getName());
                }
                FileOutputStream fos = new FileOutputStream(new File(destinationDir, zipEntry.getName()));
                int len;
                while ((len = zis.read(buffer)) > 0)
                {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
            idx++;
        }
        zis.closeEntry();
        zis.close();
    }
}
