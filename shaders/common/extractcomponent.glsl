#ifndef EXTRACT_COMPONENT_GLSL
#define EXTRACT_COMPONENT_GLSL

#line 5 9902

float extractComponentByIndex(vec4 packedVector, int componentIndex)
{
    if (componentIndex == 0)
    {
        return packedVector[0];
    }
    else if (componentIndex == 1)
    {
        return packedVector[1];
    }
    else if (componentIndex == 2)
    {
        return packedVector[2];
    }
    else
    {
        return packedVector[3];
    }
}

int extractComponentByIndex(ivec4 packedVector, int componentIndex)
{
    if (componentIndex == 0)
    {
        return packedVector[0];
    }
    else if (componentIndex == 1)
    {
        return packedVector[1];
    }
    else if (componentIndex == 2)
    {
        return packedVector[2];
    }
    else
    {
        return packedVector[3];
    }
}

uint extractComponentByIndex(uvec4 packedVector, int componentIndex)
{
    if (componentIndex == 0)
    {
        return packedVector[0];
    }
    else if (componentIndex == 1)
    {
        return packedVector[1];
    }
    else if (componentIndex == 2)
    {
        return packedVector[2];
    }
    else
    {
        return packedVector[3];
    }
}

#endif // EXTRACT_COMPONENT_GLSL
