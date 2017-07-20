package tetzlaff.ibr.gui2.other;//Created by alexk on 7/20/2017.

public interface Converter<I, J>{
    public I convertLeft(J from);
    public J convertRight(I from);
}
