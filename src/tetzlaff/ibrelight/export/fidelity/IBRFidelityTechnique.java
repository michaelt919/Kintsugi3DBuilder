package tetzlaff.ibrelight.export.fidelity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.ibrelight.util.PowerViewWeightGenerator;
import tetzlaff.ibrelight.util.ViewWeightGenerator;
import tetzlaff.models.ReadonlySettingsModel;
import tetzlaff.util.ShadingParameterMode;

public class IBRFidelityTechnique<ContextType extends Context<ContextType>> implements FidelityEvaluationTechnique<ContextType>
{
    private IBRResources<ContextType> resources;
    private Drawable<ContextType> drawable;
    private FramebufferObject<ContextType> framebuffer;
    private ReadonlySettingsModel settings;
    
    private List<Integer> activeViewIndexList;

    private Program<ContextType> fidelityProgram;
    private NativeVectorBuffer viewIndexData;

    private ViewWeightGenerator viewWeightGenerator;

    @Override
    public boolean isGuaranteedInterpolating()
    {
        return true;
    }

    @Override
    public boolean isGuaranteedMonotonic()
    {
        return false;
    }

    @Override
    public double evaluateBaselineError(int targetViewIndex, File debugFile)
    {
        return 0.0;
    }

    @Override
    public void initialize(IBRResources<ContextType> resources, ReadonlySettingsModel settings, int size) throws IOException
    {
        this.resources = resources;
        this.settings = settings;

        this.viewWeightGenerator = new PowerViewWeightGenerator(settings.getFloat("weightExponent"));

        framebuffer = resources.context.buildFramebufferObject(size, size)
            .addColorAttachment(ColorFormat.RG32F)
            .createFramebufferObject();

        resources.context.getState().disableBackFaceCulling();

        viewIndexData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.INT, 1, resources.viewSet.getCameraPoseCount());
    }

    @Override
    public void setMask(File maskFile) throws IOException
    {
        if (maskFile != null)
        {
            throw new UnsupportedOperationException("Masks are not currently supported.");
        }
    }

    @Override
    public void updateActiveViewIndexList(List<Integer> activeViewIndexList)
    {
        this.activeViewIndexList = new ArrayList<>(activeViewIndexList);

        for (int i = 0; i < activeViewIndexList.size(); i++)
        {
            viewIndexData.set(i, 0, activeViewIndexList.get(i));
        }

        try
        {
            if (fidelityProgram != null)
            {
                fidelityProgram.close();
                fidelityProgram = null;
            }

            fidelityProgram = resources.getIBRShaderProgramBuilder()
                .define("VIEW_COUNT", activeViewIndexList.size())
                .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
                .addShader(ShaderType.FRAGMENT, new File("shaders/relight/fidelity.frag"))
                .createProgram();

            drawable = resources.context.createDrawable(fidelityProgram);
            drawable.addVertexBuffer("position", resources.positionBuffer);
            drawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
            drawable.addVertexBuffer("normal", resources.normalBuffer);
            drawable.addVertexBuffer("tangent", resources.tangentBuffer);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public double evaluateError(int targetViewIndex, File debugFile)
    {
        resources.setupShaderProgram(drawable.program(), false);

        //NativeVectorBuffer viewWeightBuffer = null;
        UniformBuffer<ContextType> weightBuffer = null;

        if (this.settings.get("weightMode", ShadingParameterMode.class) == ShadingParameterMode.UNIFORM)
        {
            drawable.program().setUniform("perPixelWeightsEnabled", false);
            float[] viewWeights = viewWeightGenerator.generateWeights(resources, activeViewIndexList, resources.viewSet.getCameraPose(targetViewIndex));

            weightBuffer = resources.context.createUniformBuffer().setData(
                /*viewWeightBuffer = */NativeVectorBufferFactory.getInstance().createFromFloatArray(1, viewWeights.length, viewWeights));
            drawable.program().setUniformBuffer("ViewWeights", weightBuffer);
            drawable.program().setUniform("occlusionEnabled", false);
        }
        else
        {
            drawable.program().setUniform("perPixelWeightsEnabled", true);

            drawable.program().setUniform("weightExponent", this.settings.getFloat("weightExponent"));
            drawable.program().setUniform("occlusionEnabled", resources.depthTextures != null && this.settings.getBoolean("occlusionEnabled"));
            drawable.program().setUniform("occlusionBias", this.settings.getFloat("occlusionBias"));
        }

        drawable.program().setUniform("model_view", resources.viewSet.getCameraPose(targetViewIndex));
        drawable.program().setUniform("viewPos", resources.viewSet.getCameraPose(targetViewIndex).quickInverse(0.01f).getColumn(3).getXYZ());
        drawable.program().setUniform("projection",
                resources.viewSet.getCameraProjection(resources.viewSet.getCameraProjectionIndex(targetViewIndex))
                .getProjectionMatrix(resources.viewSet.getRecommendedNearPlane(), resources.viewSet.getRecommendedFarPlane()));

        drawable.program().setUniform("targetViewIndex", targetViewIndex);

        try (UniformBuffer<ContextType> viewIndexBuffer = resources.context.createUniformBuffer().setData(viewIndexData))
        {
            drawable.program().setUniformBuffer("ViewIndices", viewIndexBuffer);

            framebuffer.clearColorBuffer(0, -1.0f, -1.0f, -1.0f, -1.0f);
            framebuffer.clearDepthBuffer();

            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
        }

        try
        {
            if (debugFile != null)
            {
                framebuffer.saveColorBufferToFile(0, "PNG", debugFile);
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

//        // Alternate error calculation method that should give the same result in theory
//        MatrixSystem system = getMatrixSystem(targetViewIndex, new AbstractList<Integer>()
//        {
//            @Override
//            public Integer get(int index)
//            {
//                return viewIndexData.get(index, 0).intValue();
//            }
//
//            @Override
//            public int size()
//            {
//                return activeViewCount;
//            }
//        },
//        encodedVector -> new Vector3(
//                encodedVector.x / unitReflectanceEncoding,
//                encodedVector.y / unitReflectanceEncoding,
//                encodedVector.z / unitReflectanceEncoding));

//        SimpleMatrix weightVector = new SimpleMatrix(activeViewCount, 1);
//        for (int i = 0; i < activeViewCount; i++)
//        {
//            int viewIndex = viewIndexData.get(i, 0).intValue();
//            weightVector.set(i, viewWeightBuffer.get(viewIndex, 0).doubleValue());
//        }
//
//        SimpleMatrix recon = system.mA.mult(weightVector);
//        SimpleMatrix error = recon.minus(system.b);
//        double matrixError = error.normF() / Math.sqrt(system.b.numRows() / 3);
        
        // Primary error calculation method
        double sumSqError = 0.0;
        //double sumWeights = 0.0;
        double sumMask = 0.0;

        float[] fidelityArray = framebuffer.readFloatingPointColorBufferRGBA(0);
        for (int k = 0; 4 * k + 3 < fidelityArray.length; k++)
        {
            if (fidelityArray[4 * k + 1] >= 0.0f)
            {
                sumSqError += fidelityArray[4 * k];
                //sumWeights += fidelityArray[4 * k + 1];
                sumMask += 1.0;
            }
        }

        if (weightBuffer != null)
        {
            weightBuffer.close();
        }

        return Math.sqrt(sumSqError / sumMask);
    }

    @Override
    public void close()
    {
        if (fidelityProgram != null)
        {
            fidelityProgram.close();
        }

        framebuffer.close();
    }
}
