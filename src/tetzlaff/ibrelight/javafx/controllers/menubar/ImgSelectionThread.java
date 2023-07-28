package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.w3c.dom.Element;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImgSelectionThread extends ChunkViewerController implements Runnable{

    //want to pass a function (loadFullResImg) into this thread
    // then run that function in run()
    private final String imageName;
    private volatile boolean stopRequested = false;
    private volatile boolean isRunning = false;

    public ImgSelectionThread(String imageName, ChunkViewerController chunkViewerController) {
        this.imageName = imageName;

        this.chunkViewerImgView = chunkViewerController.chunkViewerImgView;
        this.imgViewLabel = chunkViewerController.imgViewLabel;
        this.metashapeObjectChunk = chunkViewerController.metashapeObjectChunk;
        this.textFlow = chunkViewerController.textFlow;
    }

    @Override
    public void run() {
        isRunning = true;
        loadFullResImg(imageName);
        isRunning = false;
    }

    public boolean isActive(){return isRunning;}
    public void stopThread(){
        stopRequested = true;
    }

    private void loadFullResImg(String imageName) {
        Image image = null;
        try {
            //goal of this try block is to find the camera which is associated with the image name in selectedItem
            //then take that camera's image and put it into the imageview to show to the user
            Element selectedItemCam = metashapeObjectChunk.matchImageToCam(imageName);

            if (selectedItemCam != null) {
                File imgFile = metashapeObjectChunk.getImgFileFromCam(selectedItemCam);

                //set imageview to selected image
                if (imgFile.exists()) {
                    //convert image if it is a .tif or .tiff
                    if (imgFile.getAbsolutePath().toLowerCase().matches(".*\\.tiff?")) {
                        if(stopRequested){return;}
                        BufferedImage bufferedImage = ImageIO.read(imgFile);
                        if(stopRequested){return;}
                        image = SwingFXUtils.toFXImage(bufferedImage, null);
                        if(stopRequested){return;}
                    } else {
                        image = new Image(imgFile.toURI().toString());
                    }
                }
                else{//camera not found in xml document
                    imgViewLabel.setText(imgViewLabel.getText() +
                            " (full res image not found)");
                }
            }
        } catch (IllegalArgumentException e) {//could not find image
            imgViewLabel.setText(imgViewLabel.getText() + " (full res image not found)");
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Image finalImage = image;//copy here so a final version of image can be passed to lambda expression
        if (finalImage != null) {
            Platform.runLater(() -> {
                //TODO: WITH LARGER IMAGES, FORMATTING IS BROKEN
                chunkViewerImgView.setImage(finalImage);
                updateImageText(imageName);
            });
        }
    }
}
