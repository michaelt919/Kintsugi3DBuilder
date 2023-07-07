package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.scene.image.Image;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tetzlaff.gl.util.UnzipHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MetashapeObjectChunk {
    //contains a metashape object and a specific chunk
    private MetashapeObject metashapeObject;
    private String chunkZipPath;
    private String chunkName;
    private int chunkID;

    private Document chunkXML;
    private Document frameZip;

    public MetashapeObjectChunk(){
        metashapeObject = new MetashapeObject();
        chunkName = "";
        chunkID = -1;//TODO: GOOD NULL CHUNK ID?
        chunkXML = null;
        frameZip = null;
    }

    public MetashapeObjectChunk(MetashapeObject metashapeObject, String selectedChunkZip) {
        this.metashapeObject = metashapeObject;
        this.chunkZipPath = selectedChunkZip;

        //set chunk xml
        try {
            this.chunkXML = UnzipHelper.unzipToDocument(this.chunkZipPath);
        } catch (IOException e) {
            this.chunkXML = null;
            throw new RuntimeException(e);
        }

        //set chunk name
        Node chunk = this.chunkXML.getElementsByTagName("chunk").item(0);
        Element chunkElement = (Element) chunk;
        this.chunkName = chunkElement.getAttribute("label");

        //set chunkID
        this.chunkID = getChunkIdFromZipPath();

        //unzip frame.zip
        String psxFilePath = metashapeObject.getPsxFilePath();
        String psxPathBase = psxFilePath.substring(0, psxFilePath.length() - 4);//remove ".psx" from path

        String frameZipPath = psxPathBase + ".files\\" + chunkID + "\\0\\frame.zip";
        try {
            this.frameZip = UnzipHelper.unzipToDocument(frameZipPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String getChunkName() {
        return this.chunkName;
    }

    private int getChunkIdFromZipPath() {
        //example chunk zip path ----> ...GuanYu_with_ground_truth.files\0\chunk.zip
        //want to extract the 0 in this example because that number denotes the chunkID
        File file = new File(chunkZipPath);

        //parent file would give "...GuanYu_with_ground_truth.files\0" in this case
        File parentFile = new File(file.getParent());

        try{
            return Integer.parseInt(parentFile.getName());
        }
        catch (NumberFormatException nfe){
            nfe.printStackTrace();
            return -1;
        }
    }

    public Document getChunkXML() {
        return this.chunkXML;
    }

    public String getPsxFilePath() {
        return this.metashapeObject.getPsxFilePath();
    }

    public int getChunkID() {
        return this.chunkID;
    }

    public Map<String, String> getChunkZipPathPairs() {
        return metashapeObject.getChunkZipPathPairs();
    }

    public MetashapeObject getMetashapeObject() {
        return this.metashapeObject;
    }

    public ArrayList<Image> getThumbnailImageList() {
        //unzip thumbnail folder
        String psxFilePath = this.metashapeObject.getPsxFilePath();
        String psxPathBase = psxFilePath.substring(0, psxFilePath.length() - 4);//remove ".psx" from path

        String thumbnailPath = psxPathBase + ".files\\" + chunkID + "\\0\\thumbnails\\thumbnails.zip";
        return UnzipHelper.unzipImages(thumbnailPath);
    }

    public List<Element> getThumbnailCameras() {
        NodeList cams = this.chunkXML.getElementsByTagName("camera");
        ArrayList<Element> cameras = new ArrayList<>();
        for (int i = 0; i < cams.getLength(); ++i) {
            Node camera = cams.item(i);
            if (camera.getNodeType() == Node.ELEMENT_NODE) {
                cameras.add((Element)camera);
            }
        }
        return cameras;
    }

    public Document getFrameZip() {
        return this.frameZip;
    }

    public Element matchImageToCam(String imageName) {
        //takes in an image name, outputs the camera in frame.zip which took that image

        NodeList cameras = frameZip.getElementsByTagName("frame").
                item(0).getChildNodes().
                item(1).getChildNodes();

        for (int i = 0; i < cameras.getLength(); ++i) {
            //cameras also holds text fields associated with the cameras, so filter them out
            if (cameras.item(i).getNodeName().equals("camera")) {
                Element camera = (Element) cameras.item(i);

                Node photoNode = camera.getElementsByTagName("photo").item(0);
                Element photoElement = (Element) photoNode;

                //path in photo element contains "../../.." before the name of the image,
                // so we cannot test for an exact match
                //using regex to see if the image names are the same regardless of their paths
                //ex. "folder/anotherFolder/asdfghjk/imageName.png" matches with "imageName.png"
                if (photoElement.getAttribute("path").matches(".*" + imageName + ".*")) {
                    return camera;
                }
            }
        }

        return null;//no matching camera found
    }

    public File getImgFileFromCam(Element selectedItemCam) {
        Element photo = (Element) selectedItemCam.getElementsByTagName("photo").item(0);
        String path = photo.getAttribute("path");
        //example path: "../../../160518_mia337_114828_a_ding/160517_mia337_2013_9_7a_ding_nearFocus_R1_C4_0_30.jpg"

        //need to replace ../../../ with the parent of the .psx file
        File psxFile = new File(this.metashapeObject.getPsxFilePath());
        String parentPath = psxFile.getParentFile().getAbsolutePath();
        path = parentPath + "\\" + path.substring(9, path.length());

        //String path now holds the full path to the selected thumbnail's full-res image
        return new File(path);
    }
}
