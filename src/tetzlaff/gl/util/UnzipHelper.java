package tetzlaff.gl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipHelper {
    public UnzipHelper() {
    }

    public void unzip() throws IOException {
        //TODO: BOTH NEED TO BE SELECTABLE BY USER
        String zipFileName = "C:\\Users\\DenneyLuke\\OneDrive - University of Wisconsin-Stout\\Summer 2023\\unzipping.zip";
        String destDirectory = "C:\\Users\\DenneyLuke\\OneDrive - University of Wisconsin-Stout\\Summer 2023\\unzipDestination";

        File destDirectoryFolder = new File(destDirectory);
        if (!destDirectoryFolder.exists()) {
            destDirectoryFolder.mkdir();
        }
        byte[] buffer = new byte[1024];
        ZipInputStream zis= new ZipInputStream(new FileInputStream(zipFileName));
        ZipEntry zipEntry = zis.getNextEntry();
        while(zipEntry !=null) {
            String filePath = destDirectory + File.separator + zipEntry.getName();
            System.out.println("Unzipping "+filePath);
            if(!zipEntry.isDirectory()) {
                FileOutputStream fos = new FileOutputStream(filePath);
                int len;
                while ((len = zis.read(buffer)) >0){
                    fos.write(buffer,0,len);
                }
                fos.close();
            }
            else {
                File dir = new File(filePath);
                dir.mkdir();
            }
            zis.closeEntry();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        System.out.println("Unzipping complete");
    }
}
