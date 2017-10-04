package tetzlaff.ibrelight.tools;

import tetzlaff.models.EnvironmentModel;

public class EnvironmentBrightnessTool implements KeyPressTool
{
    private static final float SMALL_FACTOR = 1.125f;
    private static final float LARGE_FACTOR = 2.0f;

    public enum Type
    {
        UP_LARGE(LARGE_FACTOR),
        DOWN_LARGE(1 / LARGE_FACTOR),
        UP_SMALL(SMALL_FACTOR),
        DOWN_SMALL(1 / SMALL_FACTOR);

        private final float factor;

        Type(float factor)
        {
            this.factor = factor;
        }
    }

    private static class Builder extends ToolBuilderBase<EnvironmentBrightnessTool>
    {
        private final float factor;

        Builder(Type type)
        {
            this.factor = type.factor;
        }

        @Override
        public EnvironmentBrightnessTool build()
        {
            return new EnvironmentBrightnessTool(factor, getEnvironmentModel());
        }
    }

    static ToolBuilder<EnvironmentBrightnessTool> getBuilder(Type type)
    {
        return new Builder(type);
    }

    private final float factor;
    private final EnvironmentModel environmentModel;

    public EnvironmentBrightnessTool(float factor, EnvironmentModel environmentModel)
    {
        this.factor = factor;
        this.environmentModel = environmentModel;
    }

    @Override
    public void keyPressed()
    {
        environmentModel.setEnvironmentIntensity(environmentModel.getEnvironmentIntensity() * this.factor);
    }
}
