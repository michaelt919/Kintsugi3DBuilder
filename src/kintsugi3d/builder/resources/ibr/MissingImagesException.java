package kintsugi3d.builder.resources.ibr;

import java.io.File;

public class MissingImagesException extends RuntimeException {
    private final int numMissingImgs;
    private final File imgDirectory;


    public MissingImagesException(String message) {
        super(message);
        this.numMissingImgs = -1;
        imgDirectory = null;
    }

    public MissingImagesException(String message, int numMissingImgs) {
        super(message);
        this.numMissingImgs = numMissingImgs;
        imgDirectory = null;
    }

    public MissingImagesException(String message, int numMissingImgs, File imgDirectory) {
        super(message);
        this.numMissingImgs = numMissingImgs;
        this.imgDirectory = imgDirectory;
    }

    public int getNumMissingImgs(){return numMissingImgs;}
    public File getImgDirectory(){return imgDirectory;}
}
