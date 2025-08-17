package kintsugi3d.builder.core;

import kintsugi3d.builder.javafx.core.MultithreadState;

public final class Global
{
    private Global()
    {
    }

    public static MultithreadState state()
    {
        return MultithreadState.getInstance();
    }
}
