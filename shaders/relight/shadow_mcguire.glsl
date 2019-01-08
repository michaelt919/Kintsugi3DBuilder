#ifndef SHADOW_MCGUIRE_GLSL
#define SHADOW_MCGUIRE_GLSL

// Source code adapted from:
// McGuire, M., & Mara, M. (2014).
// Efficient GPU screen-space ray tracing.
// Journal of Computer Graphics Techniques (JCGT), 3(4), 73-85.

#line 7 3008

#ifndef Z_BUFFER_IS_HYPERBOLIC
#define Z_BUFFER_IS_HYPERBOLIC 1
#endif

bool traceScreenSpaceRay(vec3 csOrig, vec3 csDir, mat4x4 proj, sampler2D csZBuffer, float nearPlaneZ,
    float stride, float jitter, const float maxSteps, float maxDistance,
    out vec2 hitPixel, out vec3 csHitPoint)
{
    // Clip to the near plane
    float rayLength = ((csOrig.z + csDir.z * maxDistance) > nearPlaneZ) ?
        (nearPlaneZ - csOrig.z) / csDir.z : maxDistance;
    vec3 csEndPoint = csOrig + csDir * rayLength;

    // Project into screen space
    vec4 H0 = proj * vec4(csOrig, 1.0), H1 = proj * vec4(csEndPoint, 1.0);
    float k0 = 1.0 / H0.w, k1 = 1.0 / H1.w;
    vec3 Q0 = csOrig * k0, Q1 = csEndPoint * k1;

    // Screen-space endpoints
    vec2 P0 = H0.xy * k0, P1 = H1.xy * k1;

    // [ Optionally clip here using listing 4 ]

    P1 += vec2((dot(P0 - P1, P0 - P1) < 0.0001) ? 0.01 : 0.0);
    vec2 delta = P1 - P0;

    bool permute = false;
    if (abs(delta.x) < abs(delta.y))
    {
        permute = true;
        delta = delta.yx; P0 = P0.yx; P1 = P1.yx;
    }

    float stepDir = sign(delta.x), invdx = stepDir / delta.x;

    // Track the derivatives of Q and k.
    vec3 dQ = (Q1 - Q0) * invdx;
    float dk = (k1 - k0) * invdx;
    vec2 dP = vec2(stepDir, delta.y * invdx);

    dP *= stride; dQ *= stride; dk *= stride;
    P0 += dP * jitter; Q0 += dQ * jitter; k0 += dk * jitter;
    float prevZMaxEstimate = csOrig.z;

    // Slide P from P0 to P1, (now-homogeneous) Q from Q0 to Q1, k from k0 to k1
    vec3 Q = Q0; float k = k0, stepCount = 0.0, end = P1.x * stepDir;

    bool valid = false;

    for (vec2 P = P0;
        ((P.x * stepDir) <= end) && (stepCount < maxSteps);
        P += dP, Q.z += dQ.z, k += dk, stepCount += 1.0)
    {
        // Project back from homogeneous to camera space
        hitPixel = permute ? P.yx : P;

        // The depth range that the ray covers within this loop iteration.
        // Assume that the ray is moving in increasing z and swap if backwards.
        float rayZMin = prevZMaxEstimate;

        // Compute the value at 1/2 pixel into the future
        float rayZMax = (dQ.z * 0.5 + Q.z) / (dk * 0.5 + k);
        prevZMaxEstimate = rayZMax;
        if (rayZMin > rayZMax)
        {
            float tmp = rayZMin;
            rayZMin = rayZMax;
            rayZMax = tmp;
        }

        // Camera-space z of the background at each layer (there can be up to 4)
        float sceneZMax = texture(csZBuffer, hitPixel * 0.5 + 0.5)[0];
#if Z_BUFFER_IS_HYPERBOLIC
        sceneZMax = -proj[3][2] / (sceneZMax * 2 - 1 + proj[2][2]);
#endif
//        float sceneZMin = sceneZMax - zThickness;

        if ((/*(rayZMax >= sceneZMin) && */ (rayZMin <= sceneZMax - 0.01)) || (sceneZMax == 0))
        {
            valid = true;
            break;
        }
    } // for each pixel on ray

    // Advance Q based on the number of steps
    Q.xy += dQ.xy * stepCount; csHitPoint = Q * (1.0 / k);

    return valid && abs(hitPixel.x) <= 1 && abs(hitPixel.y) <= 1;
}

#endif
