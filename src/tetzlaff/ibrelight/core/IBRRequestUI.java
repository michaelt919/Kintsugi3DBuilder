package tetzlaff.ibrelight.core;

import java.util.function.Consumer;

public interface IBRRequestUI
{
    void prompt(Consumer<IBRRequest> requestHandler);
}
