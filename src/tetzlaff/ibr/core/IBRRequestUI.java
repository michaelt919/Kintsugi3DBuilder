package tetzlaff.ibr.core;

import java.util.function.Consumer;

public interface IBRRequestUI
{
    void prompt(Consumer<IBRRequest> requestHandler);
}
