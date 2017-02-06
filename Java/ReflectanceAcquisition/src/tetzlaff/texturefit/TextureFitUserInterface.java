package tetzlaff.texturefit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;

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
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import tetzlaff.gl.helpers.Vector3;

public class TextureFitUserInterface extends JFrame
{
	private static final long serialVersionUID = -8384432968465838469L;

	private JButton executeButton;
	
	private FilePicker vsetFilePicker;
	private FilePicker objFilePicker;
	private FilePicker imageDirectoryPicker;
	private FilePicker maskDirectoryPicker;
	private FilePicker rescaleDirectoryPicker;
	private FilePicker outputDirectoryPicker;
	
	private JSpinner gammaSpinner;
	private JCheckBox cameraVisCheckBox;
	private JSpinner cameraVisBiasSpinner;
	
	private JSpinner imageWidthSpinner;
	private JSpinner imageHeightSpinner;
	private JCheckBox imageRescaleCheckBox;
	
	private JSpinner textureSizeSpinner;
	private JSpinner textureSubdivSpinner;
	private JCheckBox imagePreprojUseCheckBox;
	private JCheckBox imagePreprojGenCheckBox;

	private JSpinner[] lightOffsetSpinners;
	private JSpinner[] lightIntensitySpinners;
	private JCheckBox lightInfiniteCheckBox;
	private JSpinner diffuseDeltaSpinner;
	private JSpinner diffuseIterationsSpinner;
	private JSpinner diffuseCompNormalSpinner;
	private JCheckBox diffuseCompNormalInfCheckBox;
	private JSpinner diffuseInputNormalSpinner;
	private JCheckBox diffuseInputNormalInfCheckBox;
	
	private class FilePicker
	{
		File file = null;
	}
	
	public TextureFitUserInterface() 
	{
		super("Texture Generation Program");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocation(10, 10);
		this.setSize(256, 256);
		this.setResizable(false);
		this.getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
		
		JTabbedPane tabbedPane = new JTabbedPane();
		this.add(tabbedPane);
		
		TextureFitParameters defaults = new TextureFitParameters();

		JPanel filePanel = new JPanel();
		filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));
		
		Box vsetLoadBox = new Box(BoxLayout.X_AXIS);
		JPanel vsetLoadWrapper = new JPanel();
		JButton vsetLoadButton = new JButton("Select Camera File...");
		vsetLoadWrapper.add(vsetLoadButton);
		vsetLoadWrapper.setBorder(new EmptyBorder(0, 10, 0, 10));
		vsetLoadBox.add(vsetLoadWrapper);
		JPanel vsetLabelWrapper = new JPanel();
		vsetLabelWrapper.setLayout(new BorderLayout());
		vsetLabelWrapper.setBorder(new EmptyBorder(0, 10, 0, 10));
		JLabel vsetLabel = new JLabel("No file selected.");
		vsetLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		vsetLabelWrapper.add(vsetLabel);
		vsetLoadBox.add(vsetLabelWrapper);
		filePanel.add(vsetLoadBox);
		
		vsetFilePicker = new FilePicker();
		
		JFileChooser vsetFileChooser = new JFileChooser();
		vsetFileChooser.removeChoosableFileFilter(vsetFileChooser.getAcceptAllFileFilter());
		
		vsetLoadButton.addActionListener(e -> 
		{
			if (vsetFilePicker.file == null && vsetFilePicker.file != null)
			{
				vsetFileChooser.setCurrentDirectory(vsetFilePicker.file.getParentFile());
			}
			
			for (FileFilter filter : Arrays.asList(
							new FileNameExtensionFilter("Agisoft Photoscan XML files (.xml)", "xml"),
							new FileNameExtensionFilter("View Set files (.vset)", "vset")))
			{
				vsetFileChooser.addChoosableFileFilter(filter);
			}
			
			if (vsetFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				vsetFilePicker.file = vsetFileChooser.getSelectedFile();
				String fileString = vsetFilePicker.file.toString();
				vsetLabel.setText(fileString.length() < 32 ? fileString : "..." + fileString.substring(fileString.length() - 32));
			}
		});
		
		Box loadBox = new Box(BoxLayout.X_AXIS);
		JPanel loadWrapper = new JPanel();
		JButton loadButton = new JButton("Select Model File...");
		loadWrapper.add(loadButton);
		loadWrapper.setBorder(new EmptyBorder(0, 10, 0, 10));
		loadBox.add(loadWrapper);
		JPanel labelWrapper = new JPanel();
		labelWrapper.setLayout(new BorderLayout());
		labelWrapper.setBorder(new EmptyBorder(0, 10, 0, 10));
		JLabel label = new JLabel("No file selected.");
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		labelWrapper.add(label);
		loadBox.add(labelWrapper);
		filePanel.add(loadBox);
		
		objFilePicker = new FilePicker();
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
		
		loadButton.addActionListener(e -> 
		{
			if (objFilePicker.file == null && vsetFilePicker.file != null)
			{
				fileChooser.setCurrentDirectory(vsetFilePicker.file.getParentFile());
			}
			
			for (FileFilter filter : Arrays.asList(new FileNameExtensionFilter("Wavefront OBJ files (.obj)", "obj")))
			{
				fileChooser.addChoosableFileFilter(filter);
			}
			
			if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				objFilePicker.file = fileChooser.getSelectedFile();
				String fileString = objFilePicker.file.toString();
				label.setText(fileString.length() < 32 ? fileString : "..." + fileString.substring(fileString.length() - 32));
			}
		});
		
		Box loadBox1 = new Box(BoxLayout.X_AXIS);
		JPanel loadWrapper1 = new JPanel();
		JButton loadButton1 = new JButton("Select Images...");
		loadWrapper1.add(loadButton1);
		loadWrapper1.setBorder(new EmptyBorder(0, 10, 0, 10));
		loadBox1.add(loadWrapper1);
		JPanel labelWrapper1 = new JPanel();
		labelWrapper1.setLayout(new BorderLayout());
		labelWrapper1.setBorder(new EmptyBorder(0, 10, 0, 10));
		JLabel label1 = new JLabel("No file selected.");
		label1.setHorizontalAlignment(SwingConstants.RIGHT);
		labelWrapper1.add(label1);
		loadBox1.add(labelWrapper1);
		filePanel.add(loadBox1);
		
		imageDirectoryPicker = new FilePicker();
		
		JFileChooser fileChooser1 = new JFileChooser();
		fileChooser1.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		loadButton1.addActionListener(e1 -> 
		{
			if (imageDirectoryPicker.file == null && vsetFilePicker.file != null)
			{
				fileChooser1.setCurrentDirectory(vsetFilePicker.file.getParentFile());
			}
			
			if (fileChooser1.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				imageDirectoryPicker.file = fileChooser1.getSelectedFile();
				String fileString = imageDirectoryPicker.file.toString();
				label1.setText(fileString.length() < 32 ? fileString : "..." + fileString.substring(fileString.length() - 32));
			}
		});
		
		Box loadBox2 = new Box(BoxLayout.X_AXIS);
		JPanel loadWrapper2 = new JPanel();
		JButton loadButton2 = new JButton("Select Masks...");
		loadWrapper2.add(loadButton2);
		loadWrapper2.setBorder(new EmptyBorder(0, 10, 0, 10));
		loadBox2.add(loadWrapper2);
		JPanel labelWrapper2 = new JPanel();
		labelWrapper2.setLayout(new BorderLayout());
		labelWrapper2.setBorder(new EmptyBorder(0, 10, 0, 10));
		JLabel label2 = new JLabel("No file selected.");
		label2.setHorizontalAlignment(SwingConstants.RIGHT);
		labelWrapper2.add(label2);
		loadBox2.add(labelWrapper2);
		filePanel.add(loadBox2);
		
		maskDirectoryPicker = new FilePicker();
		
		JFileChooser fileChooser2 = new JFileChooser();
		fileChooser2.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		loadButton2.addActionListener(e2 -> 
		{
			if (maskDirectoryPicker.file == null && vsetFilePicker.file != null)
			{
				fileChooser2.setCurrentDirectory(vsetFilePicker.file.getParentFile());
			}
			
			if (fileChooser2.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				maskDirectoryPicker.file = fileChooser2.getSelectedFile();
				String fileString = maskDirectoryPicker.file.toString();
				label2.setText(fileString.length() < 32 ? fileString : "..." + fileString.substring(fileString.length() - 32));
			}
		});
		
		Box loadBox3 = new Box(BoxLayout.X_AXIS);
		JPanel loadWrapper3 = new JPanel();
		JButton loadButton3 = new JButton("Rescale Directory...");
		loadWrapper3.add(loadButton3);
		loadWrapper3.setBorder(new EmptyBorder(0, 10, 0, 10));
		loadBox3.add(loadWrapper3);
		JPanel labelWrapper3 = new JPanel();
		labelWrapper3.setLayout(new BorderLayout());
		labelWrapper3.setBorder(new EmptyBorder(0, 10, 0, 10));
		JLabel label3 = new JLabel("No file selected.");
		label3.setHorizontalAlignment(SwingConstants.RIGHT);
		labelWrapper3.add(label3);
		loadBox3.add(labelWrapper3);
		filePanel.add(loadBox3);
		
		rescaleDirectoryPicker = new FilePicker();
		
		JFileChooser fileChooser3 = new JFileChooser();
		fileChooser3.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		loadButton3.addActionListener(e3 -> 
		{
			if (rescaleDirectoryPicker.file == null && vsetFilePicker.file != null)
			{
				fileChooser3.setCurrentDirectory(vsetFilePicker.file.getParentFile());
			}
			
			if (fileChooser3.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				rescaleDirectoryPicker.file = fileChooser3.getSelectedFile();
				String fileString = rescaleDirectoryPicker.file.toString();
				label3.setText(fileString.length() < 32 ? fileString : "..." + fileString.substring(fileString.length() - 32));
			}
		});
		
		Box loadBox4 = new Box(BoxLayout.X_AXIS);
		JPanel loadWrapper4 = new JPanel();
		JButton loadButton4 = new JButton("Output Directory...");
		loadWrapper4.add(loadButton4);
		loadWrapper4.setBorder(new EmptyBorder(0, 10, 0, 10));
		loadBox4.add(loadWrapper4);
		JPanel labelWrapper4 = new JPanel();
		labelWrapper4.setLayout(new BorderLayout());
		labelWrapper4.setBorder(new EmptyBorder(0, 10, 0, 10));
		JLabel label4 = new JLabel("No file selected.");
		label4.setHorizontalAlignment(SwingConstants.RIGHT);
		labelWrapper4.add(label4);
		loadBox4.add(labelWrapper4);
		filePanel.add(loadBox4);
		
		outputDirectoryPicker = new FilePicker();
		
		JFileChooser fileChooser4 = new JFileChooser();
		fileChooser4.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		loadButton4.addActionListener(e4 -> 
		{
			if (outputDirectoryPicker.file == null && vsetFilePicker.file != null)
			{
				fileChooser4.setCurrentDirectory(vsetFilePicker.file.getParentFile());
			}
			
			if (fileChooser4.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				outputDirectoryPicker.file = fileChooser4.getSelectedFile();
				String fileString = outputDirectoryPicker.file.toString();
				label4.setText(fileString.length() < 32 ? fileString : "..." + fileString.substring(fileString.length() - 32));
			}
		});
		
		tabbedPane.addTab("Files", filePanel);
		
		JPanel samplingPanel = new JPanel();
		samplingPanel.setLayout(new BoxLayout(samplingPanel, BoxLayout.Y_AXIS));
		
		Box gammaBox = new Box(BoxLayout.X_AXIS);
		JLabel gammaLabel = new JLabel("Gamma" + ":");
		gammaLabel.setPreferredSize(new Dimension(256, 16));
		gammaBox.add(gammaLabel);
		SpinnerNumberModel gammaModel = new SpinnerNumberModel((double) defaults.getGamma(), (double) 1.0f, (double) 99.0f, (double) 0.1f);
		gammaSpinner = new JSpinner(gammaModel);
		gammaBox.add(gammaSpinner);
		gammaBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		samplingPanel.add(gammaBox);
		
		imageRescaleCheckBox = new JCheckBox("Rescale Images", defaults.isImageRescalingEnabled());
		imageRescaleCheckBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		samplingPanel.add(imageRescaleCheckBox);
		
		Box imageWidthBox = new Box(BoxLayout.X_AXIS);
		JLabel imageWidthLabel = new JLabel("Rescaled Image Width" + ":");
		imageWidthLabel.setPreferredSize(new Dimension(256, 16));
		imageWidthBox.add(imageWidthLabel);
		SpinnerNumberModel imageWidthModel = new SpinnerNumberModel((double) defaults.getImageWidth(), (double) 1.0f, (double) 8192.0f, (double) 1.0f);
		imageWidthSpinner = new JSpinner(imageWidthModel);
		imageWidthBox.add(imageWidthSpinner);
		imageWidthBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		samplingPanel.add(imageWidthBox);
		
		Box imageHeightBox = new Box(BoxLayout.X_AXIS);
		JLabel imageHeightLabel = new JLabel("Rescaled Image Height" + ":");
		imageHeightLabel.setPreferredSize(new Dimension(256, 16));
		imageHeightBox.add(imageHeightLabel);
		SpinnerNumberModel imageHeightModel = new SpinnerNumberModel((double) defaults.getImageHeight(), (double) 1.0f, (double) 8192.0f, (double) 1.0f);
		imageHeightSpinner = new JSpinner(imageHeightModel);
		imageHeightBox.add(imageHeightSpinner);
		imageHeightBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		samplingPanel.add(imageHeightBox);
		
		cameraVisCheckBox = new JCheckBox("Enable Camera Visibility Test", defaults.isCameraVisibilityTestEnabled());
		cameraVisCheckBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		samplingPanel.add(cameraVisCheckBox);
		
		Box cameraVisBiasBox = new Box(BoxLayout.X_AXIS);
		JLabel cameraVisBiasLabel = new JLabel("Camera Visibility Test Bias" + ":");
		cameraVisBiasLabel.setPreferredSize(new Dimension(256, 16));
		cameraVisBiasBox.add(cameraVisBiasLabel);
		SpinnerNumberModel cameraVisBiasModel = new SpinnerNumberModel((double) defaults.getCameraVisibilityTestBias(), (double) 0.0f, (double) 1.0f, (double) 0.0001f);
		cameraVisBiasSpinner = new JSpinner(cameraVisBiasModel);
		cameraVisBiasBox.add(cameraVisBiasSpinner);
		cameraVisBiasBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		samplingPanel.add(cameraVisBiasBox);
		
		Box textureSizeBox = new Box(BoxLayout.X_AXIS);
		JLabel textureSizeLabel = new JLabel("Texture Size" + ":");
		textureSizeLabel.setPreferredSize(new Dimension(256, 16));
		textureSizeBox.add(textureSizeLabel);
		SpinnerNumberModel textureSizeModel = new SpinnerNumberModel((double) defaults.getTextureSize(), (double) 1.0f, (double) 8192.0f, (double) 1.0f);
		textureSizeSpinner = new JSpinner(textureSizeModel);
		textureSizeBox.add(textureSizeSpinner);
		textureSizeBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		samplingPanel.add(textureSizeBox);
		
		Box textureSubdivBox = new Box(BoxLayout.X_AXIS);
		JLabel textureSubdivLabel = new JLabel("Texture Subdivision" + ":");
		textureSubdivLabel.setPreferredSize(new Dimension(256, 16));
		textureSubdivBox.add(textureSubdivLabel);
		SpinnerNumberModel textureSubdivModel = new SpinnerNumberModel((double) defaults.getTextureSubdivision(), (double) 1.0f, (double) 8192.0f, (double) 1.0f);
		textureSubdivSpinner = new JSpinner(textureSubdivModel);
		textureSubdivBox.add(textureSubdivSpinner);
		textureSubdivBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		samplingPanel.add(textureSubdivBox);
		
		imagePreprojGenCheckBox = new JCheckBox("Generate Pre-projected images", defaults.isImagePreprojectionGenerationEnabled());
		imagePreprojGenCheckBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		samplingPanel.add(imagePreprojGenCheckBox);
		
		imagePreprojUseCheckBox = new JCheckBox("Use Pre-projected images", defaults.isImagePreprojectionUseEnabled());
		imagePreprojUseCheckBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		samplingPanel.add(imagePreprojUseCheckBox);
		tabbedPane.addTab("Sampling", samplingPanel);
		
		JPanel diffusePanel = new JPanel();
		diffusePanel.setLayout(new BoxLayout(diffusePanel, BoxLayout.Y_AXIS));
		Box lightOffsetBox = new Box(BoxLayout.X_AXIS);
		JLabel lightOffsetLabel = new JLabel("Light Offset" + ":");
		lightOffsetLabel.setPreferredSize(new Dimension(256, 16));
		lightOffsetBox.add(lightOffsetLabel);
		SpinnerNumberModel lightOffsetModelX = new SpinnerNumberModel(0.0f, -99.0f, 99.0f, 0.001f);
		JSpinner lightOffsetSpinnerX = new JSpinner(lightOffsetModelX);
		lightOffsetBox.add(lightOffsetSpinnerX);
		SpinnerNumberModel lightOffsetModelY = new SpinnerNumberModel(0.0f, -99.0f, 99.0f, 0.001f);
		JSpinner lightOffsetSpinnerY = new JSpinner(lightOffsetModelY);
		lightOffsetBox.add(lightOffsetSpinnerY);
		SpinnerNumberModel lightOffsetModelZ = new SpinnerNumberModel(0.0f, -99.0f, 99.0f, 0.001f);
		JSpinner lightOffsetSpinnerZ = new JSpinner(lightOffsetModelZ);
		lightOffsetBox.add(lightOffsetSpinnerZ);
		lightOffsetBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		diffusePanel.add(lightOffsetBox);
		
		lightOffsetSpinners = new JSpinner[] { lightOffsetSpinnerX, lightOffsetSpinnerY, lightOffsetSpinnerZ };
		
		Box lightIntensityBox = new Box(BoxLayout.X_AXIS);
		JLabel lightIntensityLabel = new JLabel("Light Intensity" + ":");
		lightIntensityLabel.setPreferredSize(new Dimension(256, 16));
		lightIntensityBox.add(lightIntensityLabel);
		SpinnerNumberModel lightIntensityModelX = new SpinnerNumberModel(1.0f, 0.0f, 9999.0f, 0.001f);
		JSpinner lightIntensitySpinnerX = new JSpinner(lightIntensityModelX);
		lightIntensityBox.add(lightIntensitySpinnerX);
		SpinnerNumberModel lightIntensityModelY = new SpinnerNumberModel(1.0f, 0.0f, 9999.0f, 0.001f);
		JSpinner lightIntensitySpinnerY = new JSpinner(lightIntensityModelY);
		lightIntensityBox.add(lightIntensitySpinnerY);
		SpinnerNumberModel lightIntensityModelZ = new SpinnerNumberModel(1.0f, 0.0f, 9999.0f, 0.001f);
		JSpinner lightIntensitySpinnerZ = new JSpinner(lightIntensityModelZ);
		lightIntensityBox.add(lightIntensitySpinnerZ);
		lightIntensityBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		diffusePanel.add(lightIntensityBox);
		
		lightIntensitySpinners = new JSpinner[] { lightIntensitySpinnerX, lightIntensitySpinnerY, lightIntensitySpinnerZ };
		
		lightInfiniteCheckBox = new JCheckBox("Infinite Light Source", defaults.areLightSourcesInfinite());
		lightInfiniteCheckBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		diffusePanel.add(lightInfiniteCheckBox);
		
		Box diffuseDeltaBox = new Box(BoxLayout.X_AXIS);
		JLabel diffuseDeltaLabel = new JLabel("Delta" + ":");
		diffuseDeltaLabel.setPreferredSize(new Dimension(256, 16));
		diffuseDeltaBox.add(diffuseDeltaLabel);
		SpinnerNumberModel diffuseDeltaModel = new SpinnerNumberModel((double) defaults.getDiffuseDelta(), (double) 0.0f, (double) 1.0f, (double) 0.01f);
		diffuseDeltaSpinner = new JSpinner(diffuseDeltaModel);
		diffuseDeltaBox.add(diffuseDeltaSpinner);
		diffuseDeltaBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		diffusePanel.add(diffuseDeltaBox);
				
		Box diffuseIterationsBox = new Box(BoxLayout.X_AXIS);
		JLabel diffuseIterationsLabel = new JLabel("Iterations" + ":");
		diffuseIterationsLabel.setPreferredSize(new Dimension(256, 16));
		diffuseIterationsBox.add(diffuseIterationsLabel);
		SpinnerNumberModel diffuseIterationsModel = new SpinnerNumberModel((double) defaults.getDiffuseIterations(), (double) 0.0f, (double) 999.0f, (double) 1.0f);
		diffuseIterationsSpinner = new JSpinner(diffuseIterationsModel);
		diffuseIterationsBox.add(diffuseIterationsSpinner);
		diffuseIterationsBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		diffusePanel.add(diffuseIterationsBox);
		
		Box diffuseCompNormalBox = new Box(BoxLayout.X_AXIS);
		JLabel diffuseComplNormalLabel = new JLabel("Computed Normal Weight" + ":");
		diffuseComplNormalLabel.setPreferredSize(new Dimension(256, 16));
		diffuseCompNormalBox.add(diffuseComplNormalLabel);
		SpinnerNumberModel diffuseCompNormalModel = new SpinnerNumberModel((double) Math.min(9999.0f, defaults.getDiffuseComputedNormalWeight()), (double) 0.0f, (double) 9999.0f, (double) 0.1f);
		diffuseCompNormalSpinner = new JSpinner(diffuseCompNormalModel);
		diffuseCompNormalBox.add(diffuseCompNormalSpinner);
		diffuseCompNormalBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		diffusePanel.add(diffuseCompNormalBox);
		
		diffuseCompNormalInfCheckBox = new JCheckBox("(infinite)", defaults.getDiffuseComputedNormalWeight() >= Float.MAX_VALUE);
		diffuseCompNormalInfCheckBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		diffusePanel.add(diffuseCompNormalInfCheckBox);
		
		Box diffuseInputNormalBox = new Box(BoxLayout.X_AXIS);
		JLabel diffuseInputNormalLabel = new JLabel("Input Normal Weight" + ":");
		diffuseInputNormalLabel.setPreferredSize(new Dimension(256, 16));
		diffuseInputNormalBox.add(diffuseInputNormalLabel);
		SpinnerNumberModel diffuseInputNormalModel = new SpinnerNumberModel((double) Math.min(9999.0f, defaults.getDiffuseInputNormalWeight()), (double) 0.0f, (double) 9999.0f, (double) 0.1f);
		diffuseInputNormalSpinner = new JSpinner(diffuseInputNormalModel);
		diffuseInputNormalBox.add(diffuseInputNormalSpinner);
		diffuseInputNormalBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		diffusePanel.add(diffuseInputNormalBox);
		
		diffuseInputNormalInfCheckBox = new JCheckBox("(infinite)", defaults.getDiffuseInputNormalWeight() >= Float.MAX_VALUE);
		diffuseInputNormalInfCheckBox.setBorder(new EmptyBorder(5, 10, 5, 10));
		diffusePanel.add(diffuseInputNormalInfCheckBox);
		
		tabbedPane.addTab("Lights/Diffuse", diffusePanel);
		
		JPanel specularPanel = new JPanel();
		specularPanel.setLayout(new BoxLayout(specularPanel, BoxLayout.Y_AXIS));
		tabbedPane.addTab("Specular", specularPanel);
		
		JSpinner.NumberEditor cameraVisBiasNumberEditor = new JSpinner.NumberEditor(cameraVisBiasSpinner, "0.0000");
		cameraVisBiasSpinner.setEditor(cameraVisBiasNumberEditor);
		
		Box executeBox = new Box(BoxLayout.X_AXIS);
		JPanel executeWrapper = new JPanel();
		executeButton = new JButton("Execute...");
		executeWrapper.add(executeButton);
		executeWrapper.setBorder(new EmptyBorder(0, 10, 0, 10));
		executeBox.add(executeWrapper);
		this.add(executeBox);
		
		this.pack();
		
		JFrame loadingFrame = new JFrame("Loading...");
		loadingFrame.setUndecorated(true);
		JProgressBar loadingBar = new JProgressBar();
		loadingBar.setIndeterminate(true);
		loadingFrame.add(loadingBar);
		loadingFrame.pack();
		loadingFrame.setLocationRelativeTo(null);
	}
	
	public File getCameraFile()
	{
		return vsetFilePicker.file;
	}
	
	public File getModelFile()
	{
		return objFilePicker.file;
	}
	
	public File getImageDirectory()
	{
		return imageDirectoryPicker.file;
	}
	
	public File getMaskDirectory()
	{
		return maskDirectoryPicker.file;
	}
	
	public File getRescaleDirectory()
	{
		return rescaleDirectoryPicker.file;
	}
	
	public File getOutputDirectory()
	{
		return outputDirectoryPicker.file;
	}
	
	public Vector3 getLightOffset()
	{
		return new Vector3(
			getValueAsFloat(this.lightOffsetSpinners[0]),
			getValueAsFloat(this.lightOffsetSpinners[1]),
			getValueAsFloat(this.lightOffsetSpinners[2]));
	}
	
	public Vector3 getLightIntensity()
	{
		return new Vector3(
			getValueAsFloat(this.lightIntensitySpinners[0]),
			getValueAsFloat(this.lightIntensitySpinners[1]),
			getValueAsFloat(this.lightIntensitySpinners[2]));
	}
	
	private float getValueAsFloat(JSpinner spinner)
	{
		return (float)((double)((Double)spinner.getValue()));
	}
	
	private int getValueAsInt(JSpinner spinner)
	{
		return (int)Math.round((((Double)spinner.getValue())));
	}
	
	public TextureFitParameters getParameters()
	{
		TextureFitParameters param = new TextureFitParameters();
		param.setGamma(getValueAsFloat(this.gammaSpinner));
		param.setCameraVisibilityTestEnabled(this.cameraVisCheckBox.isSelected());
		param.setCameraVisibilityTestBias(getValueAsFloat(this.cameraVisBiasSpinner));
		param.setTextureSize(getValueAsInt(this.textureSizeSpinner));
		param.setTextureSubdivision(getValueAsInt(this.textureSubdivSpinner));
		param.setImagePreprojectionUseEnabled(this.imagePreprojUseCheckBox.isSelected());
		param.setImagePreprojectionGenerationEnabled(this.imagePreprojGenCheckBox.isSelected());
		param.setImageRescalingEnabled(this.imageRescaleCheckBox.isSelected());
		param.setImageWidth(getValueAsInt(this.imageWidthSpinner));
		param.setImageHeight(getValueAsInt(this.imageHeightSpinner));
		param.setLightSourcesInfinite(this.lightInfiniteCheckBox.isSelected());
		param.setDiffuseDelta(getValueAsFloat(this.diffuseDeltaSpinner));
		param.setDiffuseIterations(getValueAsInt(this.diffuseIterationsSpinner));
		param.setDiffuseComputedNormalWeight(this.diffuseCompNormalInfCheckBox.isSelected() ? 
				Float.MAX_VALUE : getValueAsFloat(this.diffuseCompNormalSpinner));
		param.setDiffuseInputNormalWeight(this.diffuseInputNormalInfCheckBox.isSelected() ?
				Float.MAX_VALUE : getValueAsFloat(this.diffuseInputNormalSpinner));
		return param;
	}
	
	public void addExecuteButtonActionListener(ActionListener actionListener)
	{
		executeButton.addActionListener(actionListener);
	}
}
