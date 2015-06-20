package tetzlaff.reflacq;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class TexGenUserInterface
{
	private JFrame frame;
	private File selectedVsetFile;
	
	private JLabel fileLabel;
	private JButton executeButton;
	
	private JSpinner gammaSpinner;
	private JCheckBox cameraVisCheckBox;
	private JSpinner cameraVisBiasSpinner;
	
	private JSpinner textureSizeSpinner;
	private JSpinner textureSubdivSpinner;
	private JCheckBox imagePreprojUseCheckBox;
	private JCheckBox imagePreprojGenCheckBox;
	
	private JSpinner diffuseDeltaSpinner;
	private JSpinner diffuseIterationsSpinner;
	private JSpinner diffuseCompNormalSpinner;
	private JCheckBox diffuseCompNormalInfCheckBox;
	private JSpinner diffuseInputNormalSpinner;
	private JCheckBox diffuseInputNormalInfCheckBox;
	
	private JCheckBox specRoughCheckBox;
	private JCheckBox specNormalCheckBox;
	private JCheckBox blinnPhongCheckBox;
	
	private JSpinner specSubDiffuseSpinner;
	private JSpinner specInfluenceSpinner;
	private JSpinner specDetThreshSpinner;
	private JSpinner specCompNormalSpinner;
	private JSpinner specCompRoughSpinner;
	private JSpinner specDefaultRoughSpinner;
	private JSpinner defaultRoughnessSpinner;
	private JSpinner roughnessCapSpinner;
	
	private JLabel addUILabel(String name)
	{
		JLabel label = new JLabel(name);
		label.setBorder(new EmptyBorder(25, 10, 5, 10));
		frame.add(label);
		return label;
	}
	
	
	private JSpinner addUIValueField(String name, double defaultValue, double minValue, double maxValue, double stepSize)
	{
		Box box = new Box(BoxLayout.X_AXIS);
		JLabel label = new JLabel(name + ":");
		label.setPreferredSize(new Dimension(256, 16));
		box.add(label);
		SpinnerNumberModel model = new SpinnerNumberModel(defaultValue, minValue, maxValue, stepSize);
		JSpinner spinner = new JSpinner(model);
		box.add(spinner);
		box.setBorder(new EmptyBorder(5, 10, 5, 10));
		frame.add(box);
		return spinner;
	}
	
	private JCheckBox addUICheckBoxField(String name, boolean checked)
	{
		JCheckBox checkBox = new JCheckBox(name, checked);
		checkBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		frame.add(checkBox);
		return checkBox;
	}
	
	public TexGenUserInterface() 
	{
		this.frame = new JFrame("Texture Generation Program");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocation(10, 10);
		frame.setSize(256, 256);
		frame.setResizable(false);
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		
		TexGenParameters defaults = new TexGenParameters();

		fileLabel = addUILabel("No file selected.");
		Box loadBox = new Box(BoxLayout.X_AXIS);
		JPanel loadWrapper = new JPanel();
		JButton loadButton = new JButton("Select View Set File...");
		loadWrapper.add(loadButton);
		loadWrapper.setBorder(new EmptyBorder(0, 10, 0, 10));
		loadBox.add(loadWrapper);
		frame.add(loadBox);
		

		addUILabel("Sampling Parameters");
		gammaSpinner = addUIValueField("Gamma", defaults.getGamma(), 1.0f, 99.0f, 0.1f);
		cameraVisCheckBox = addUICheckBoxField("Enable Camera Visibility Test", defaults.isCameraVisibilityTestEnabled());
		cameraVisBiasSpinner = addUIValueField("Camera Visibility Test Bias", defaults.getCameraVisibilityTestBias(), 0.0f, 1.0f, 0.0001f);
		textureSizeSpinner = addUIValueField("Texture Size", defaults.getTextureSize(), 1.0f, 8192.0f, 1.0f);
		textureSubdivSpinner = addUIValueField("Texture Subdivision", defaults.getTextureSubdivision(), 1.0f, 8192.0f, 1.0f);
		imagePreprojGenCheckBox = addUICheckBoxField("Generate Pre-projected images", defaults.isImagePreprojectionGenerationEnabled());
		imagePreprojUseCheckBox = addUICheckBoxField("Use Pre-projected images", defaults.isImagePreprojectionUseEnabled());
		
		addUILabel("Diffuse Fitting Parameters");
		diffuseDeltaSpinner = addUIValueField("Delta", defaults.getDiffuseDelta(), 0.0f, 1.0f, 0.01f);
		diffuseIterationsSpinner = addUIValueField("Iterations", defaults.getDiffuseIterations(), 0.0f, 8.0f, 1.0f);
		diffuseCompNormalSpinner = addUIValueField("Computed Normal Weight", Math.min(9999.0f, defaults.getDiffuseComputedNormalWeight()), 0.0f, 9999.0f, 0.1f);
		diffuseCompNormalInfCheckBox = addUICheckBoxField("(infinite)", defaults.getDiffuseComputedNormalWeight() >= Float.MAX_VALUE);
		diffuseInputNormalSpinner = addUIValueField("Input Normal Weight", Math.min(9999.0f, defaults.getDiffuseInputNormalWeight()),  0.0f, 9999.0f, 0.1f);
		diffuseInputNormalInfCheckBox = addUICheckBoxField("(infinite)", defaults.getDiffuseInputNormalWeight() >= Float.MAX_VALUE);
		
		addUILabel("Specular Fitting Parameters");
		specRoughCheckBox = addUICheckBoxField("Compute Roughness", defaults.isSpecularRoughnessComputationEnabled());
		specNormalCheckBox = addUICheckBoxField("Compute Separate Normal", defaults.isSpecularNormalComputationEnabled());
		blinnPhongCheckBox = addUICheckBoxField("Use \"True\" Blinn-Phong Model", defaults.isTrueBlinnPhongSpecularEnabled());
		specSubDiffuseSpinner = addUIValueField("Diffuse Subtraction Amount", defaults.getSpecularSubtractDiffuseAmount(), 0.0f, 1.0f, 0.01f);
		specInfluenceSpinner = addUIValueField("Influence Scale", defaults.getSpecularInfluenceScale(), 0.0f, 1.0f, 0.01f);
		specDetThreshSpinner = addUIValueField("Determinant Threshold", defaults.getSpecularDeterminantThreshold(), 0.0f, 1.0f, 0.0001f);
		specCompNormalSpinner = addUIValueField("Computed Normal Weight", defaults.getSpecularComputedNormalWeight(), 0.0f, 1.0f, 0.01f);
		specCompRoughSpinner = addUIValueField("Input Normal, Computed Roughness Weight", defaults.getSpecularInputNormalComputedRoughnessWeight(), 0.0f, 1.0f, 0.01f);
		specDefaultRoughSpinner = addUIValueField("Input Normal, Default Roughness Weight", defaults.getSpecularInputNormalDefaultRoughnessWeight(), 0.0f, 1.0f, 0.01f);
		defaultRoughnessSpinner = addUIValueField("Default Specular Roughness", defaults.getDefaultSpecularRoughness(), 0.0f, 10.0f, 0.01f);
		roughnessCapSpinner = addUIValueField("Specular Roughness Cap", defaults.getSpecularRoughnessCap(), 0.0f, 10.0f, 0.01f);
		
		JSpinner.NumberEditor cameraVisBiasNumberEditor = new JSpinner.NumberEditor(cameraVisBiasSpinner, "0.0000");
		cameraVisBiasSpinner.setEditor(cameraVisBiasNumberEditor);
		
		JSpinner.NumberEditor specDetThreshNumberEditor = new JSpinner.NumberEditor(specDetThreshSpinner, "0.0000");
		specDetThreshSpinner.setEditor(specDetThreshNumberEditor);
		
		Box executeBox = new Box(BoxLayout.X_AXIS);
		JPanel executeWrapper = new JPanel();
		executeButton = new JButton("Execute...");
		executeWrapper.add(executeButton);
		executeWrapper.setBorder(new EmptyBorder(0, 10, 0, 10));
		executeBox.add(executeWrapper);
		frame.add(executeBox);
		
		frame.pack();
		
		JFrame loadingFrame = new JFrame("Loading...");
		loadingFrame.setUndecorated(true);
		JProgressBar loadingBar = new JProgressBar();
		loadingBar.setIndeterminate(true);
		loadingFrame.add(loadingBar);
		loadingFrame.pack();
		loadingFrame.setLocationRelativeTo(null);
		
		loadButton.addActionListener(e -> 
		{
			JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
			fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
			fileChooser.setFileFilter(new FileNameExtensionFilter("View Set files (.vset)", "vset"));
			if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
			{
				selectedVsetFile = fileChooser.getSelectedFile();
				String fileString = selectedVsetFile.toString();
				fileLabel.setText(fileString.length() < 32 ? fileString : "..." + fileString.substring(fileString.length() - 32));
			}
		});
	}
	
	public File getSelectedFile()
	{
		return selectedVsetFile;
	}
	
	private float getValueAsFloat(JSpinner spinner)
	{
		return (float)((double)((Double)spinner.getValue()));
	}
	
	private int getValueAsInt(JSpinner spinner)
	{
		return (int)Math.round(((double)((Double)spinner.getValue())));
	}
	
	public TexGenParameters getParameters()
	{
		TexGenParameters param = new TexGenParameters();
		param.setGamma(getValueAsFloat(this.gammaSpinner));
		param.setCameraVisibilityTestEnabled(this.cameraVisCheckBox.isSelected());
		param.setCameraVisibilityTestBias(getValueAsFloat(this.cameraVisBiasSpinner));
		param.setTextureSize(getValueAsInt(this.textureSizeSpinner));
		param.setTextureSubdivision(getValueAsInt(this.textureSubdivSpinner));
		param.setImagePreprojectionUseEnabled(this.imagePreprojUseCheckBox.isSelected());
		param.setImagePreprojectionGenerationEnabled(this.imagePreprojGenCheckBox.isSelected());
		param.setDiffuseDelta(getValueAsFloat(this.diffuseDeltaSpinner));
		param.setDiffuseIterations(getValueAsInt(this.diffuseIterationsSpinner));
		param.setDiffuseComputedNormalWeight(this.diffuseCompNormalInfCheckBox.isSelected() ? 
				Float.MAX_VALUE : getValueAsFloat(this.diffuseCompNormalSpinner));
		param.setDiffuseInputNormalWeight(this.diffuseInputNormalInfCheckBox.isSelected() ?
				Float.MAX_VALUE : getValueAsFloat(this.diffuseInputNormalSpinner));
		param.setSpecularRoughnessComputationEnabled(this.specRoughCheckBox.isSelected());
		param.setSpecularNormalComputationEnabled(this.specNormalCheckBox.isSelected());
		param.setTrueBlinnPhongSpecularEnabled(this.blinnPhongCheckBox.isSelected());
		param.setSpecularSubtractDiffuseAmount(getValueAsFloat(this.specSubDiffuseSpinner));
		param.setSpecularInfluenceScale(getValueAsFloat(this.specInfluenceSpinner));
		param.setSpecularDeterminantThreshold(getValueAsFloat(this.specDetThreshSpinner));
		param.setSpecularComputedNormalWeight(getValueAsFloat(this.specCompNormalSpinner));
		param.setSpecularInputNormalComputedRoughnessWeight(getValueAsFloat(this.specCompRoughSpinner));
		param.setSpecularInputNormalDefaultRoughnessWeight(getValueAsFloat(this.specDefaultRoughSpinner));
		param.setDefaultSpecularRoughness(getValueAsFloat(this.defaultRoughnessSpinner));
		param.setSpecularRoughnessCap(getValueAsFloat(this.roughnessCapSpinner));
		return param;
	}
	
	public void addExecuteButtonActionListener(ActionListener actionListener)
	{
		executeButton.addActionListener(actionListener);
	}
	
	public void addWindowListener(WindowListener windowListener)
	{
		frame.addWindowListener(windowListener);
	}

	public void show()
	{
		frame.setVisible(true);
	}
}
