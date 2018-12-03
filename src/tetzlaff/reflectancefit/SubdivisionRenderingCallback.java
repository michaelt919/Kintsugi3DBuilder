package tetzlaff.reflectancefit;

import java.io.IOException;

@FunctionalInterface
interface SubdivisionRenderingCallback
{
    void execute(int row, int col) throws IOException;
}
