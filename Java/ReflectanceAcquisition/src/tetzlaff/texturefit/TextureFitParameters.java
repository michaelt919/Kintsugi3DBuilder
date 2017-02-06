package tetzlaff.texturefit;

public class TextureFitParameters 
{
	// Sampling parameters
	private float gamma = 2.2f;
	private boolean cameraVisibilityTestEnabled = true;
	private float cameraVisibilityTestBias = 0.0025f;
	
	private int imageWidth = 1024;
	private int imageHeight = 1024;
	private int textureSize = 2048;
	private int textureSubdivision = 8;
	private boolean imageRescalingEnabled = false;
	private boolean imagePreprojectionUseEnabled = false;
	private boolean imagePreprojectionGenerationEnabled = false;
	
	private boolean areLightSourcesInfinite;
	
	// Diffuse fitting parameters
	private float diffuseDelta = 0.1f;
	private int diffuseIterations = 16;
	private float diffuseComputedNormalWeight = 0.0f;
	private float diffuseInputNormalWeight = Float.MAX_VALUE;

	public TextureFitParameters() 
	{
	}

	public float getGamma() {
		return this.gamma;
	}

	public void setGamma(float gamma) {
		this.gamma = gamma;
	}

	public boolean isCameraVisibilityTestEnabled() {
		return this.cameraVisibilityTestEnabled;
	}

	public void setCameraVisibilityTestEnabled(boolean cameraVisibilityTestEnabled) {
		this.cameraVisibilityTestEnabled = cameraVisibilityTestEnabled;
	}

	public float getCameraVisibilityTestBias() {
		return this.cameraVisibilityTestBias;
	}

	public void setCameraVisibilityTestBias(float cameraVisibilityTestBias) {
		this.cameraVisibilityTestBias = cameraVisibilityTestBias;
	}

	public int getImageWidth() {
		return this.imageWidth;
	}

	public void setImageWidth(int imageWidth) {
		this.imageWidth = imageWidth;
	}

	public int getImageHeight() {
		return this.imageHeight;
	}

	public void setImageHeight(int imageHeight) {
		this.imageHeight = imageHeight;
	}

	public int getTextureSize() {
		return this.textureSize;
	}

	public void setTextureSize(int textureSize) {
		this.textureSize = textureSize;
	}

	public int getTextureSubdivision() {
		return this.textureSubdivision;
	}

	public void setTextureSubdivision(int textureSubdivision) {
		this.textureSubdivision = textureSubdivision;
	}

	public boolean isImageRescalingEnabled() {
		return this.imageRescalingEnabled;
	}

	public void setImageRescalingEnabled(boolean imageRescalingEnabled) {
		this.imageRescalingEnabled = imageRescalingEnabled;
	}

	public boolean isImagePreprojectionUseEnabled() {
		return this.imagePreprojectionUseEnabled;
	}

	public void setImagePreprojectionUseEnabled(boolean imagePreprojectionUseEnabled) {
		this.imagePreprojectionUseEnabled = imagePreprojectionUseEnabled;
	}

	public boolean isImagePreprojectionGenerationEnabled() {
		return this.imagePreprojectionGenerationEnabled;
	}

	public void setImagePreprojectionGenerationEnabled(
			boolean imagePreprojectionGenerationEnabled) {
		this.imagePreprojectionGenerationEnabled = imagePreprojectionGenerationEnabled;
	}

	public float getDiffuseDelta() {
		return this.diffuseDelta;
	}

	public void setDiffuseDelta(float diffuseDelta) {
		this.diffuseDelta = diffuseDelta;
	}

	public int getDiffuseIterations() {
		return this.diffuseIterations;
	}

	public void setDiffuseIterations(int diffuseIterations) {
		this.diffuseIterations = diffuseIterations;
	}

	public float getDiffuseComputedNormalWeight() {
		return this.diffuseComputedNormalWeight;
	}

	public void setDiffuseComputedNormalWeight(float diffuseComputedNormalWeight) {
		this.diffuseComputedNormalWeight = diffuseComputedNormalWeight;
	}

	public float getDiffuseInputNormalWeight() {
		return this.diffuseInputNormalWeight;
	}

	public void setDiffuseInputNormalWeight(float diffuseInputNormalWeight) {
		this.diffuseInputNormalWeight = diffuseInputNormalWeight;
	}

	public boolean areLightSourcesInfinite() {
		return this.areLightSourcesInfinite;
	}

}
