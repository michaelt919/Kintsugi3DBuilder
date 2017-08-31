package tetzlaff.ibr.tools;

import tetzlaff.models.EnvironmentMapModel;

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
            return new EnvironmentBrightnessTool(factor, getEnvironmentMapModel());
        }
    }

    static ToolBuilder<EnvironmentBrightnessTool> getBuilder(Type type)
    {
        return new Builder(type);
    }

    private final float factor;
    private final EnvironmentMapModel environmentMapModel;

    public EnvironmentBrightnessTool(float factor, EnvironmentMapModel environmentMapModel)
    {
        this.factor = factor;
        this.environmentMapModel = environmentMapModel;
    }

    @Override
    public void keyPressed()
    {
        environmentMapModel.setEnvironmentIntensity(environmentMapModel.getEnvironmentIntensity() * this.factor);
    }
}
