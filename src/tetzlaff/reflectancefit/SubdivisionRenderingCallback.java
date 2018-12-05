package tetzlaff.reflectancefit;

/**
 * Provides a callback that allows for logging, etc. every time a subdivision block completes.
 */
@FunctionalInterface
interface SubdivisionRenderingCallback
{
    /**
     * The callback to run after a particular subdivision block has been completed.
     * @param row The row index of the subdivision block.
     * @param col The column index of the subdivision block.
     */
    void execute(int row, int col);
}
