package kintsugi3d.builder.core;

import kintsugi3d.builder.javafx.MultithreadState;

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
