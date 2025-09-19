package kintsugi3d.builder.core;

import kintsugi3d.builder.javafx.core.MultithreadState;

public final class Global
{
    private Global()
    {
    }

    public static Kintsugi3DBuilderState state()
    {
        return MultithreadState.getInstance();
    }
}
