package tetzlaff.ibr.ulf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.ibr.IBRDrawable;
import tetzlaff.ibr.IBRLoadOptions;
import tetzlaff.ibr.IBRLoadingMonitor;
import tetzlaff.ibr.ViewSet;

public class ULFMorphRenderer<ContextType extends Context<ContextType>> implements IBRDrawable<ContextType>
{
	private ContextType context;
	private Program<ContextType> program;
    private File lfmFile;
    private IBRLoadOptions loadOptions;
    private CameraController cameraController;
    private String id;

    private IBRLoadingMonitor callback;
	private List<ULFRenderer<ContextType>> stages;
	private int currentStage;

	public ULFMorphRenderer(ContextType context, Program<ContextType> program, File lfmFile, IBRLoadOptions loadOptions, CameraController cameraController) throws FileNotFoundException 
	{
		this.context = context;
		this.program = program;
		this.lfmFile = lfmFile;
		this.loadOptions = loadOptions;
		this.cameraController = cameraController;
		
		this.id = lfmFile.getParentFile().getName();
		
		this.stages = new ArrayList<ULFRenderer<ContextType>>();
		this.currentStage = 0;
	}
	
	public int getCurrentStage()
	{
		return this.currentStage;
	}
	
	public void setCurrentStage(int newStage)
	{
		this.currentStage = newStage;
	}
	
	public int getStageCount()
	{
		return this.stages.size();
	}

	@Override
	public void initialize() 
	{
		try 
		{
			Scanner scanner = new Scanner(lfmFile);
			File directory = lfmFile.getParentFile();
			while (scanner.hasNextLine())
			{
				String vsetFileName = scanner.nextLine();
				stages.add(new ULFRenderer<ContextType>(context, program, null, new File(directory, vsetFileName), null, loadOptions, cameraController));
			}
			scanner.close();
			
			int stagesLoaded = 0;
			for(ULFRenderer<ContextType> stage : stages)
			{
				System.out.println(stage.getVSETFile());
				callback.setProgress((double)stagesLoaded / (double)stages.size());
				stage.initialize();
				stagesLoaded++;
			}
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		
		callback.loadingComplete();
	}

	@Override
	public void update() 
	{
		for(ULFRenderer<ContextType> stage : stages)
		{
			stage.update();
		}
	}

	@Override
	public void draw() 
	{
		stages.get(currentStage).draw();
	}

	@Override
	public void cleanup() 
	{
		for(ULFRenderer<ContextType> stage : stages)
		{
			stage.cleanup();
		}
	}

	@Override
	public void setOnLoadCallback(IBRLoadingMonitor callback) 
	{
		this.callback = callback;
	}
	
	public UnstructuredLightField<ContextType> getLightField()
	{
		return stages.get(currentStage).getLightField();
	}
	
	@Override
	public ViewSet<ContextType> getActiveViewSet()
	{
		return this.getLightField().viewSet;
	}
	
	@Override
	public VertexMesh getActiveProxy()
	{
		return this.getLightField().proxy;
	}

	@Override
	public float getGamma() 
	{
		return this.getLightField().settings.getGamma();
	}

	@Override
	public float getWeightExponent() 
	{
		return this.getLightField().settings.getWeightExponent();
	}

	@Override
	public boolean isOcclusionEnabled() 
	{
		return this.getLightField().settings.isOcclusionEnabled();
	}

	@Override
	public float getOcclusionBias() 
	{
		return this.getLightField().settings.getOcclusionBias();
	}

	@Override
	public void setGamma(float gamma)
	{
		for (ULFRenderer<ContextType> stage : stages)
		{
			stage.getLightField().settings.setGamma(gamma);
		}
	}

	@Override
	public void setWeightExponent(float weightExponent) 
	{
		for (ULFRenderer<ContextType> stage : stages)
		{
			stage.getLightField().settings.setWeightExponent(weightExponent);
		}
	}

	@Override
	public void setOcclusionEnabled(boolean occlusionEnabled) 
	{
		for (ULFRenderer<ContextType> stage : stages)
		{
			stage.getLightField().settings.setOcclusionEnabled(occlusionEnabled);
		}
	}

	@Override
	public void setOcclusionBias(float occlusionBias) 
	{
		for (ULFRenderer<ContextType> stage : stages)
		{
			stage.getLightField().settings.setOcclusionBias(occlusionBias);
		}
	}
	
	@Override
	public String toString()
	{
		return this.id;
	}

	@Override
	public void requestResample(int width, int height, File targetVSETFile, File exportPath) throws IOException 
	{
		this.stages.get(this.currentStage).requestResample(width, height, targetVSETFile, exportPath);
	}

	@Override
	public void requestFidelity(File exportPath) throws IOException 
	{
		this.stages.get(this.currentStage).requestFidelity(exportPath);
	}

	@Override
	public void setHalfResolution(boolean halfResEnabled)
	{	
		this.stages.get(this.currentStage).setHalfResolution(halfResEnabled);
	}

	@Override
	public boolean getHalfResolution()
	{	
		return this.stages.get(this.currentStage).getHalfResolution();
	}
	
	@Override
	public boolean getMultisampling()
	{
		return this.stages.get(this.currentStage).getMultisampling();	
	}


	@Override
	public void setMultisampling(boolean multisamplingEnabled)
	{
		this.stages.get(this.currentStage).setMultisampling(multisamplingEnabled);	
	}

	@Override
	public void setProgram(Program<ContextType> program) 
	{
		for(ULFRenderer<ContextType> stage : stages)
		{
			stage.setProgram(program);
		}
	}

	@Override
	public void setIndexProgram(Program<ContextType> program) 
	{
		for(ULFRenderer<ContextType> stage : stages)
		{
			stage.setIndexProgram(program);
		}
	}

	@Override
	public boolean isViewIndexCacheEnabled() 
	{
		return this.stages.get(this.currentStage).isViewIndexCacheEnabled();
	}

	@Override
	public void setViewIndexCacheEnabled(boolean viewIndexCacheEnabled) 
	{
		this.stages.get(this.currentStage).setViewIndexCacheEnabled(viewIndexCacheEnabled);
	}

	@Override
	public void requestBTF(int width, int height, File exportPath)
			throws IOException 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void reloadHelperShaders() 
	{
		for(ULFRenderer<ContextType> stage : stages)
		{
			stage.reloadHelperShaders();
		}
	}
	
	@Override
	public Texture2D<ContextType> getEnvironmentTexture()
	{
		return this.stages.get(this.currentStage).getEnvironmentTexture();
	}
	
	@Override
	public void setEnvironment(File environmentFile) throws IOException 
	{
		this.stages.get(this.currentStage).setEnvironment(environmentFile);
	}

	@Override
	public boolean isIBREnabled() 
	{
		return this.stages.get(this.currentStage).isIBREnabled();
	}

	@Override
	public boolean isRelightingEnabled() 
	{
		return this.stages.get(this.currentStage).isRelightingEnabled();
	}

	@Override
	public boolean isPBRGeometricAttenuationEnabled() 
	{
		return this.stages.get(this.currentStage).isPBRGeometricAttenuationEnabled();
	}

	@Override
	public boolean isFresnelEnabled() 
	{
		return this.stages.get(this.currentStage).isFresnelEnabled();
	}

	@Override
	public void setIBREnabled(boolean ibrEnabled) 
	{
		for (ULFRenderer<ContextType> stage : stages)
		{
			stage.getLightField().settings.setIBREnabled(ibrEnabled);
		}
	}

	@Override
	public void setRelightingEnabled(boolean relightingEnabled) 
	{
		for (ULFRenderer<ContextType> stage : stages)
		{
			stage.getLightField().settings.setRelightingEnabled(relightingEnabled);
		}
	}

	@Override
	public void setPBRGeometricAttenuationEnabled(boolean pbrGeometricAttenuationEnabled) 
	{
		for (ULFRenderer<ContextType> stage : stages)
		{
			stage.getLightField().settings.setPBRGeometricAttenuationEnabled(pbrGeometricAttenuationEnabled);
		}
	}

	@Override
	public void setFresnelEnabled(boolean fresnelEnabled) 
	{
		for (ULFRenderer<ContextType> stage : stages)
		{
			stage.getLightField().settings.setFresnelEnabled(fresnelEnabled);
		}
	}

	@Override
	public boolean areTexturesEnabled() 
	{
		return this.stages.get(this.currentStage).areTexturesEnabled();
	}

	@Override
	public void setTexturesEnabled(boolean texturesEnabled) 
	{
		for (ULFRenderer<ContextType> stage : stages)
		{
			stage.getLightField().settings.setTexturesEnabled(texturesEnabled);
		}
	}
}
