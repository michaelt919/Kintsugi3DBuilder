package kintsugi3d.builder.resources.ibr;

public class MissingImagesException extends RuntimeException {
    private final int numMissingImgs;

    public MissingImagesException(String message) {
        super(message);
        this.numMissingImgs = -1;
    }

    public MissingImagesException(String message, int numMissingImgs) {
        super(message);
        this.numMissingImgs = numMissingImgs;
    }

    public int getNumMissingImgs(){return numMissingImgs;}
}
