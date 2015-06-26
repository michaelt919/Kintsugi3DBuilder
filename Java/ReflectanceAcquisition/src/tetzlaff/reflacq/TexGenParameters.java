package tetzlaff.reflacq;

public class TexGenParameters 
{
	// Sampling parameters
	private float gamma = 2.2f;
	private boolean cameraVisibilityTestEnabled = true;
	private float cameraVisibilityTestBias = 0.0025f;
	
	private int imageWidth = 1024;
	private int imageHeight = 1024;
	private int textureSize = 1024;
	private int textureSubdivision = 1;
	private boolean imageRescalingEnabled = true;
	private boolean imagePreprojectionUseEnabled = false;
	private boolean imagePreprojectionGenerationEnabled = false;
	
	// Diffuse fitting parameters
	private float diffuseDelta = 0.1f;
	private int diffuseIterations = 4;
	private float diffuseComputedNormalWeight = 1.0f;
	private float diffuseInputNormalWeight = Float.MAX_VALUE;
	
	// Specular fitting parameters
	private boolean specularRoughnessComputationEnabled = false;
	private boolean specularNormalComputationEnabled = false;
	private boolean trueBlinnPhongSpecularEnabled = true;
	
	private float specularSubtractDiffuseAmount = 0.98f;
	private float specularInfluenceScale = 0.35f;
	private float specularDeterminantThreshold = 0.002f;
	private float specularComputedNormalWeight = 0.0f;
	private float specularInputNormalComputedRoughnessWeight = 0.0f;
	private float specularInputNormalDefaultRoughnessWeight = 1.0f;
	private float defaultSpecularRoughness = 0.1f;
	private float specularRoughnessCap = 0.5f;

	public TexGenParameters() 
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

	public boolean isSpecularRoughnessComputationEnabled() {
		return this.specularRoughnessComputationEnabled;
	}

	public void setSpecularRoughnessComputationEnabled(
			boolean specularRoughnessComputationEnabled) {
		this.specularRoughnessComputationEnabled = specularRoughnessComputationEnabled;
	}

	public boolean isSpecularNormalComputationEnabled() {
		return this.specularNormalComputationEnabled;
	}

	public void setSpecularNormalComputationEnabled(
			boolean specularNormalComputationEnabled) {
		this.specularNormalComputationEnabled = specularNormalComputationEnabled;
	}

	public boolean isTrueBlinnPhongSpecularEnabled() {
		return this.trueBlinnPhongSpecularEnabled;
	}

	public void setTrueBlinnPhongSpecularEnabled(
			boolean trueBlinnPhongSpecularEnabled) {
		this.trueBlinnPhongSpecularEnabled = trueBlinnPhongSpecularEnabled;
	}

	public float getSpecularSubtractDiffuseAmount() {
		return this.specularSubtractDiffuseAmount;
	}

	public void setSpecularSubtractDiffuseAmount(float specularSubtractDiffuseAmount) {
		this.specularSubtractDiffuseAmount = specularSubtractDiffuseAmount;
	}

	public float getSpecularInfluenceScale() {
		return this.specularInfluenceScale;
	}

	public void setSpecularInfluenceScale(float specularInfluenceScale) {
		this.specularInfluenceScale = specularInfluenceScale;
	}

	public float getSpecularDeterminantThreshold() {
		return this.specularDeterminantThreshold;
	}

	public void setSpecularDeterminantThreshold(float specularDeterminantThreshold) {
		this.specularDeterminantThreshold = specularDeterminantThreshold;
	}

	public float getSpecularComputedNormalWeight() {
		return this.specularComputedNormalWeight;
	}

	public void setSpecularComputedNormalWeight(float specularComputedNormalWeight) {
		this.specularComputedNormalWeight = specularComputedNormalWeight;
	}

	public float getSpecularInputNormalComputedRoughnessWeight() {
		return this.specularInputNormalComputedRoughnessWeight;
	}

	public void setSpecularInputNormalComputedRoughnessWeight(
			float specularInputNormalComputedRoughnessWeight) {
		this.specularInputNormalComputedRoughnessWeight = specularInputNormalComputedRoughnessWeight;
	}

	public float getSpecularInputNormalDefaultRoughnessWeight() {
		return this.specularInputNormalDefaultRoughnessWeight;
	}

	public void setSpecularInputNormalDefaultRoughnessWeight(
			float specularInputNormalDefaultRoughnessWeight) {
		this.specularInputNormalDefaultRoughnessWeight = specularInputNormalDefaultRoughnessWeight;
	}

	public float getDefaultSpecularRoughness() {
		return this.defaultSpecularRoughness;
	}

	public void setDefaultSpecularRoughness(float defaultSpecularRoughness) {
		this.defaultSpecularRoughness = defaultSpecularRoughness;
	}

	public float getSpecularRoughnessCap() {
		return this.specularRoughnessCap;
	}

	public void setSpecularRoughnessCap(float specularRoughnessCap) {
		this.specularRoughnessCap = specularRoughnessCap;
	}

}
