package tetzlaff.ulf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector4;

/**
 * A renderer for a sequence of related unstructured light fields.
 * This renderer acts as a aggregate ULFDrawable for each stage of the morph, with draw requests directed towards the currently active stage of the morph.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the context that will be used for rendering.
 */
public class ULFMorphRenderer<ContextType extends Context<ContextType>> implements ULFDrawable<ContextType>
{
	private ContextType context;
	private Program<ContextType> program;
    private File lfmFile;
    private ULFLoadOptions loadOptions;
    private Trackball trackball;
    private String id;

    private ULFLoadingMonitor callback;
	private List<ULFRenderer<ContextType>> stages;
	private int currentStage;

	/**
	 * Creates a new unstructured light field morph renderer.
	 * @param contextThe GL context in which to perform the rendering.
     * @param program The program to use for rendering.
	 * @param lfmFile The file defining the stages of the light field morph to be loaded.
	 * @param loadOptions The options to use when loading the light field.
     * @param trackball The trackball controlling the movement of the virtual camera.
	 * @throws FileNotFoundException Thrown due to a File I/O error occurring.
	 */
	public ULFMorphRenderer(ContextType context, Program<ContextType> program, File lfmFile, ULFLoadOptions loadOptions, Trackball trackball) throws FileNotFoundException 
	{
		this.context = context;
		this.program = program;
		this.lfmFile = lfmFile;
		this.loadOptions = loadOptions;
		this.trackball = trackball;
		
		this.id = lfmFile.getParentFile().getName();
		
		this.stages = new ArrayList<ULFRenderer<ContextType>>();
		this.currentStage = 0;
	}
	
	/**
	 * Gets the currently active stage of the light field morph.
	 * @return The currently active stage of the morph.
	 */
	public int getCurrentStage()
	{
		return this.currentStage;
	}
	
	/**
	 * Sets the currently active stage of the light field morph.
	 * @param newStage The currently active stage of the morph.
	 */
	public void setCurrentStage(int newStage)
	{
		this.currentStage = newStage;
	}
	
	/**
	 * Gets the number of stages in the morph.
	 * @return The number of stages in the morph.
	 */
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
				stages.add(new ULFRenderer<ContextType>(context, program, new File(directory, vsetFileName), loadOptions, trackball));
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
	public boolean draw() 
	{
		return stages.get(currentStage).draw();
	}

	@Override
	public void saveToFile(String fileFormat, File file)
	{
		stages.get(currentStage).saveToFile(fileFormat, file);
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
	public void setOnLoadCallback(ULFLoadingMonitor callback) 
	{
		this.callback = callback;
	}
	
	public UnstructuredLightField<ContextType> getLightField()
	{
		return stages.get(currentStage).getLightField();
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
	public void setHalfResolution(boolean halfResEnabled)
	{	
		this.stages.get(this.currentStage).setHalfResolution(halfResEnabled);
	}

	@Override
	public void setVisualizeCameras(boolean camerasEnabled)
	{	
		this.stages.get(this.currentStage).setVisualizeCameras(camerasEnabled);
	}

	@Override
	public boolean getHalfResolution()
	{	
		return this.stages.get(this.currentStage).getHalfResolution();
	}

	@Override
	public void setMultisampling(boolean multisamplingEnabled)
	{
		context.makeContextCurrent();
		if(multisamplingEnabled)
		{
			context.enableMultisampling();
		}
		else
		{
			context.disableMultisampling();			
		}		
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
	public void setBackgroundColor(Vector4 RGBA) {
		for (ULFRenderer<ContextType> stage : stages)
		{
			stage.setBackgroundColor(RGBA);
		}
	}

	@Override
	public Vector4 getBackgroundColor() {
		return this.stages.get(this.currentStage).getBackgroundColor();
	}

	@Override
	public boolean isKNeighborsEnabled() {
		return this.stages.get(this.currentStage).isKNeighborsEnabled();
	}

	@Override
	public void setKNeighborsEnabled(boolean kNeighborsEnabled) {
		for (ULFRenderer<ContextType> stage : stages)
		{
			stage.setKNeighborsEnabled(kNeighborsEnabled);
		}
	}

	@Override
	public int getKNeighborCount() {
		return this.stages.get(this.currentStage).getKNeighborCount();
	}

	@Override
	public void setKNeighborCount(int kNeighborCount) {
		for (ULFRenderer<ContextType> stage : stages)
		{
			stage.setKNeighborCount(kNeighborCount);
		}
	}
}
