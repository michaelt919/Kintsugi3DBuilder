package tetzlaff.ulf.app;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.BoxLayout;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.Color;

import javax.swing.JButton;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JCheckBox;
import javax.swing.JSeparator;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import tetzlaff.ulf.ULFDrawable;
import tetzlaff.ulf.ULFListModel;
import tetzlaff.ulf.ULFLoadOptions;
import tetzlaff.ulf.ULFLoadingMonitor;
import tetzlaff.ulf.ULFMorphRenderer;
import tetzlaff.ulf.ViewSetImageOptions;

/**
 * Swing GUI for managing the settings of a list of ULFRenderer objects.  This is an update of the
 * interface in ULFUserInterface offering the same options but uses a more flexible approach to layout
 * and groups controls into title frames wherever possible.  This was designed with WindowBuilder
 * inside Eclipse and should be able to be updated/edited in that same program.
 * 
 * @author Seth Berrier
 */
public class ULFConfigFrame extends JFrame {

	private static final long serialVersionUID = 3234328215460573228L;

	/**
	 * The central panel of this frame where all widgets are laid out.
	 */
	private JPanel contentPane;

	/**
	 * Build and layout the GUI in the central content pane.  Also sets all appropriate listeners to update
	 * the given ULFListModel parameter.
	 * 
	 * @param model The model that abstracts the underlying light field data and provides entries for the combo
	 * box.  It is expected that this will be empty (but not null) when the GUI is initially constructed and
	 * the listeners for the loading buttons will add items to the model.
	 * @param isHighDPI Is the display a high DPI display (a.k.a. retina).  If so, the half resolution option
	 * defaults to being on.
	 */
	public ULFConfigFrame(ULFListModel model, boolean isHighDPI)
	{		
		setResizable(false);
		setTitle("Light Field Config");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(10, 10, 332, 539);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		
		JPanel loadingPanel = new JPanel();
		loadingPanel.setBorder(new TitledBorder(null, "Load Options", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		contentPane.add(loadingPanel);
		GridBagLayout gbl_loadingPanel = new GridBagLayout();
		gbl_loadingPanel.columnWidths = new int[] {140, 140};
		gbl_loadingPanel.rowHeights = new int[] {0, 0, 0, 0};
		gbl_loadingPanel.columnWeights = new double[]{1.0, 0.0};
		gbl_loadingPanel.rowWeights = new double[]{0.0, 1.0, 1.0, 0.0};
		loadingPanel.setLayout(gbl_loadingPanel);
		
		JCheckBox chckbxCompressImages = new JCheckBox("Compress Images");
		chckbxCompressImages.setSelected(true);
		GridBagConstraints gbc_chckbxCompressImages = new GridBagConstraints();
		gbc_chckbxCompressImages.anchor = GridBagConstraints.WEST;
		gbc_chckbxCompressImages.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxCompressImages.gridx = 0;
		gbc_chckbxCompressImages.gridy = 0;
		loadingPanel.add(chckbxCompressImages, gbc_chckbxCompressImages);
		
		JCheckBox chckbxUseMipmaps = new JCheckBox("Use Mipmaps");
		chckbxUseMipmaps.setSelected(true);
		GridBagConstraints gbc_chckbxUseMipmaps = new GridBagConstraints();
		gbc_chckbxUseMipmaps.anchor = GridBagConstraints.WEST;
		gbc_chckbxUseMipmaps.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxUseMipmaps.gridx = 1;
		gbc_chckbxUseMipmaps.gridy = 0;
		loadingPanel.add(chckbxUseMipmaps, gbc_chckbxUseMipmaps);
			
		JPanel panel = new JPanel();
		panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.gridwidth = 2;
		gbc_panel.insets = new Insets(0, 0, 5, 0);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 1;
		loadingPanel.add(panel, gbc_panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0, 0, 0, 0};
		gbl_panel.rowHeights = new int[]{0, 0, 0};
		gbl_panel.columnWeights = new double[]{0.0, 1.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);
		
		JCheckBox chckbxGenerateDepthImages = new JCheckBox("Generate Depth Images");
		GridBagConstraints gbc_chckbxGenerateDepthImages = new GridBagConstraints();
		gbc_chckbxGenerateDepthImages.anchor = GridBagConstraints.WEST;
		gbc_chckbxGenerateDepthImages.gridwidth = 4;
		gbc_chckbxGenerateDepthImages.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxGenerateDepthImages.gridx = 0;
		gbc_chckbxGenerateDepthImages.gridy = 0;
		panel.add(chckbxGenerateDepthImages, gbc_chckbxGenerateDepthImages);
		
		JLabel lblDimensions = new JLabel("Dimensions:");
		lblDimensions.setEnabled(false);
		GridBagConstraints gbc_lblDimensions = new GridBagConstraints();
		gbc_lblDimensions.insets = new Insets(0, 5, 0, 5);
		gbc_lblDimensions.gridx = 0;
		gbc_lblDimensions.gridy = 1;
		panel.add(lblDimensions, gbc_lblDimensions);
		
		JSpinner spinnerDepthWidth = new JSpinner();
		lblDimensions.setLabelFor(spinnerDepthWidth);
		spinnerDepthWidth.setEnabled(false);
		spinnerDepthWidth.setModel(new SpinnerNumberModel(new Integer(1024), new Integer(1), null, new Integer(1)));
		GridBagConstraints gbc_spinnerDepthWidth = new GridBagConstraints();
		gbc_spinnerDepthWidth.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinnerDepthWidth.insets = new Insets(0, 0, 0, 5);
		gbc_spinnerDepthWidth.gridx = 1;
		gbc_spinnerDepthWidth.gridy = 1;
		panel.add(spinnerDepthWidth, gbc_spinnerDepthWidth);
		
		JLabel lblX_1 = new JLabel("X");
		lblX_1.setEnabled(false);
		GridBagConstraints gbc_lblX_1 = new GridBagConstraints();
		gbc_lblX_1.insets = new Insets(0, 0, 0, 5);
		gbc_lblX_1.gridx = 2;
		gbc_lblX_1.gridy = 1;
		panel.add(lblX_1, gbc_lblX_1);
		
		JSpinner spinnerDepthHeight = new JSpinner();
		lblX_1.setLabelFor(spinnerDepthHeight);
		spinnerDepthHeight.setEnabled(false);
		spinnerDepthHeight.setModel(new SpinnerNumberModel(new Integer(1024), new Integer(1), null, new Integer(1)));
		GridBagConstraints gbc_spinner_1 = new GridBagConstraints();
		gbc_spinner_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinner_1.insets = new Insets(0, 0, 0, 5);
		gbc_spinner_1.gridx = 3;
		gbc_spinner_1.gridy = 1;
		panel.add(spinnerDepthHeight, gbc_spinner_1);
				
		JButton btnLoadSingle = new JButton("Load Single...");
		GridBagConstraints gbc_btnLoadSingle = new GridBagConstraints();
		gbc_btnLoadSingle.anchor = GridBagConstraints.NORTH;
		gbc_btnLoadSingle.insets = new Insets(0, 0, 0, 5);
		gbc_btnLoadSingle.gridx = 0;
		gbc_btnLoadSingle.gridy = 3;
		loadingPanel.add(btnLoadSingle, gbc_btnLoadSingle);
		btnLoadSingle.setToolTipText("Load a single object");
		
		JButton btnLoadMultiple = new JButton("Load Multiple...");
		GridBagConstraints gbc_btnLoadMultiple = new GridBagConstraints();
		gbc_btnLoadMultiple.anchor = GridBagConstraints.NORTH;
		gbc_btnLoadMultiple.gridx = 1;
		gbc_btnLoadMultiple.gridy = 3;
		loadingPanel.add(btnLoadMultiple, gbc_btnLoadMultiple);
		btnLoadMultiple.setToolTipText("Load multiple objects");
		
		JPanel selectionPanel = new JPanel();
		selectionPanel.setToolTipText("Options for loading and changing the current object");
		selectionPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Model Options", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		contentPane.add(selectionPanel);
		GridBagLayout gbl_selectionPanel = new GridBagLayout();
		gbl_selectionPanel.columnWidths = new int[] {0, 0, 0};
		gbl_selectionPanel.rowHeights = new int[] {0, 0};
		gbl_selectionPanel.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_selectionPanel.rowWeights = new double[]{0.0, 0.0};
		selectionPanel.setLayout(gbl_selectionPanel);
		
		JComboBox<ULFDrawable> comboBoxObjects = new JComboBox<ULFDrawable>();
		GridBagConstraints gbc_comboBoxObjects = new GridBagConstraints();
		gbc_comboBoxObjects.gridwidth = 2;
		gbc_comboBoxObjects.insets = new Insets(0, 0, 5, 0);
		gbc_comboBoxObjects.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBoxObjects.gridx = 0;
		gbc_comboBoxObjects.gridy = 0;
		selectionPanel.add(comboBoxObjects, gbc_comboBoxObjects);
		
		JSlider sliderObjects = new JSlider();
		sliderObjects.setValue(0);
		GridBagConstraints gbc_sliderObjects = new GridBagConstraints();
		gbc_sliderObjects.insets = new Insets(0, 0, 5, 0);
		gbc_sliderObjects.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderObjects.gridwidth = 2;
		gbc_sliderObjects.gridx = 0;
		gbc_sliderObjects.gridy = 1;
		selectionPanel.add(sliderObjects, gbc_sliderObjects);
		
		// Add listener for changes to the morph slider.
		sliderObjects.addChangeListener(e ->
		{
			if (model.getSelectedItem() instanceof ULFMorphRenderer<?>)
			{
				((ULFMorphRenderer<?>)(model.getSelectedItem())).setCurrentStage(sliderObjects.getValue());
			}
		});
		
		JPanel renderingOptionsPanel = new JPanel();
		renderingOptionsPanel.setToolTipText("Options to control the ULF rendering algorithm");
		renderingOptionsPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Rendering Options", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		contentPane.add(renderingOptionsPanel);
		GridBagLayout gbl_renderingOptionsPanel = new GridBagLayout();
		gbl_renderingOptionsPanel.columnWidths = new int[]{0, 0, 0, 0, 0};
		gbl_renderingOptionsPanel.rowHeights = new int[]{0, 0, 0, 0, 0};
		gbl_renderingOptionsPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_renderingOptionsPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		renderingOptionsPanel.setLayout(gbl_renderingOptionsPanel);
		
		JLabel lblGamma = new JLabel("Gamma:");
		GridBagConstraints gbc_lblGamma = new GridBagConstraints();
		gbc_lblGamma.anchor = GridBagConstraints.EAST;
		gbc_lblGamma.insets = new Insets(0, 0, 5, 5);
		gbc_lblGamma.gridx = 0;
		gbc_lblGamma.gridy = 0;
		renderingOptionsPanel.add(lblGamma, gbc_lblGamma);
		
		JSpinner spinnerGamma = new JSpinner(new SpinnerNumberModel(2.2f, 1.0f, 100.0f, 0.1f));
		lblGamma.setLabelFor(spinnerGamma);
		spinnerGamma.setToolTipText("Gamma color correction applied to the final rendered image");
		GridBagConstraints gbc_spinnerGamma = new GridBagConstraints();
		gbc_spinnerGamma.gridwidth = 2;
		gbc_spinnerGamma.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinnerGamma.insets = new Insets(0, 0, 5, 0);
		gbc_spinnerGamma.gridx = 2;
		gbc_spinnerGamma.gridy = 0;
		renderingOptionsPanel.add(spinnerGamma, gbc_spinnerGamma);
		
		JLabel lblWeightExponent = new JLabel("Weight Exponent:");
		GridBagConstraints gbc_lblWeightExponent = new GridBagConstraints();
		gbc_lblWeightExponent.insets = new Insets(0, 0, 5, 5);
		gbc_lblWeightExponent.anchor = GridBagConstraints.EAST;
		gbc_lblWeightExponent.gridx = 0;
		gbc_lblWeightExponent.gridy = 1;
		renderingOptionsPanel.add(lblWeightExponent, gbc_lblWeightExponent);
		
		JSpinner spinnerExponent = new JSpinner(new SpinnerNumberModel(16.0f, 1.0f, 1000.0f, 1.0f));
		lblWeightExponent.setLabelFor(spinnerExponent);
		spinnerExponent.setToolTipText("Exponent controlling the number of views used per pixel (too low causes blank/black areas, too high causes blurry results)");
		GridBagConstraints gbc_spinnerExponent = new GridBagConstraints();
		gbc_spinnerExponent.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinnerExponent.gridwidth = 2;
		gbc_spinnerExponent.insets = new Insets(0, 0, 5, 0);
		gbc_spinnerExponent.gridx = 2;
		gbc_spinnerExponent.gridy = 1;
		renderingOptionsPanel.add(spinnerExponent, gbc_spinnerExponent);
		
		JSeparator separator = new JSeparator();
		separator.setForeground(UIManager.getColor("Separator.foreground"));
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator.gridwidth = 4;
		gbc_separator.insets = new Insets(0, 0, 5, 0);
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 2;
		renderingOptionsPanel.add(separator, gbc_separator);
		
		JCheckBox chckbxOcclusion = new JCheckBox("Visibility Testing");
		chckbxOcclusion.setSelected(true);
		chckbxOcclusion.setToolTipText("Detect and eliminate views that are occluded");
		GridBagConstraints gbc_chckbxOcclusion = new GridBagConstraints();
		gbc_chckbxOcclusion.anchor = GridBagConstraints.WEST;
		gbc_chckbxOcclusion.insets = new Insets(0, 0, 0, 5);
		gbc_chckbxOcclusion.gridx = 0;
		gbc_chckbxOcclusion.gridy = 3;
		renderingOptionsPanel.add(chckbxOcclusion, gbc_chckbxOcclusion);
		
		JLabel label = new JLabel("-");
		GridBagConstraints gbc_label = new GridBagConstraints();
		gbc_label.insets = new Insets(0, 0, 0, 5);
		gbc_label.gridx = 1;
		gbc_label.gridy = 3;
		renderingOptionsPanel.add(label, gbc_label);
		
		JLabel lblBias = new JLabel("Bias:");
		GridBagConstraints gbc_lblBias = new GridBagConstraints();
		gbc_lblBias.insets = new Insets(0, 0, 0, 5);
		gbc_lblBias.gridx = 2;
		gbc_lblBias.gridy = 3;
		renderingOptionsPanel.add(lblBias, gbc_lblBias);
		
		JSpinner spinnerOccBias = new JSpinner(new SpinnerNumberModel(0.0025f, 0.0f, 1.0f, 0.0001f));
		lblBias.setLabelFor(spinnerOccBias);
		spinnerOccBias.setToolTipText("Control the bias factor to help prevent bleading of views into occluded areas");
		GridBagConstraints gbc_spinnerOccBias = new GridBagConstraints();
		gbc_spinnerOccBias.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinnerOccBias.gridx = 3;
		gbc_spinnerOccBias.gridy = 3;
		renderingOptionsPanel.add(spinnerOccBias, gbc_spinnerOccBias);
		
		JPanel qualitySettings = new JPanel();
		qualitySettings.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Quality Settings", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		contentPane.add(qualitySettings);
		GridBagLayout gbl_qualitySettings = new GridBagLayout();
		gbl_qualitySettings.columnWidths = new int[]{0, 0, 0};
		gbl_qualitySettings.rowHeights = new int[]{0, 0};
		gbl_qualitySettings.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_qualitySettings.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		qualitySettings.setLayout(gbl_qualitySettings);
		
		JCheckBox chckbxHalfRes = new JCheckBox("Half Resolution");
		chckbxHalfRes.setToolTipText("Cut the resolution in half but keep window full size (useful for High DPI / Retina displays)");
		GridBagConstraints gbc_chckbxHalfRes = new GridBagConstraints();
		gbc_chckbxHalfRes.insets = new Insets(0, 0, 0, 5);
		gbc_chckbxHalfRes.gridx = 0;
		gbc_chckbxHalfRes.gridy = 0;
		qualitySettings.add(chckbxHalfRes, gbc_chckbxHalfRes);
		
		JCheckBox chckbxMultisampling = new JCheckBox("Multisampling");
		chckbxMultisampling.setSelected(true);
		chckbxMultisampling.setToolTipText("Enable full screen multisampling for anti-aliasing (helps eliminate jagged edges)");
		GridBagConstraints gbc_chckbxMultisampling = new GridBagConstraints();
		gbc_chckbxMultisampling.gridx = 1;
		gbc_chckbxMultisampling.gridy = 0;
		qualitySettings.add(chckbxMultisampling, gbc_chckbxMultisampling);
		
		JPanel resamplePanel = new JPanel();
		resamplePanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Resample Light Field", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		contentPane.add(resamplePanel);
		GridBagLayout gbl_resamplePanel = new GridBagLayout();
		gbl_resamplePanel.columnWidths = new int[]{0, 0, 0, 0, 0};
		gbl_resamplePanel.rowHeights = new int[]{0, 0, 0};
		gbl_resamplePanel.columnWeights = new double[]{0.0, 1.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_resamplePanel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		resamplePanel.setLayout(gbl_resamplePanel);
		
		JLabel lblNewDimensions = new JLabel("New Dimensions:");
		GridBagConstraints gbc_lblNewDimensions = new GridBagConstraints();
		gbc_lblNewDimensions.insets = new Insets(0, 5, 5, 5);
		gbc_lblNewDimensions.gridx = 0;
		gbc_lblNewDimensions.gridy = 0;
		resamplePanel.add(lblNewDimensions, gbc_lblNewDimensions);
		
		JSpinner spinnerWidth = new JSpinner(new SpinnerNumberModel(1024, 1, 8192, 1));
		lblNewDimensions.setLabelFor(spinnerWidth);
		spinnerWidth.setToolTipText("New view width");
		GridBagConstraints gbc_spinnerWidth = new GridBagConstraints();
		gbc_spinnerWidth.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinnerWidth.insets = new Insets(0, 0, 5, 5);
		gbc_spinnerWidth.gridx = 1;
		gbc_spinnerWidth.gridy = 0;
		resamplePanel.add(spinnerWidth, gbc_spinnerWidth);
		
		JLabel lblX = new JLabel("X");
		GridBagConstraints gbc_lblX = new GridBagConstraints();
		gbc_lblX.insets = new Insets(0, 0, 5, 5);
		gbc_lblX.gridx = 2;
		gbc_lblX.gridy = 0;
		resamplePanel.add(lblX, gbc_lblX);
		
		JSpinner spinnerHeight = new JSpinner(new SpinnerNumberModel(1024, 1, 8192, 1));
		spinnerHeight.setToolTipText("New view height");
		GridBagConstraints gbc_spinnerHeight = new GridBagConstraints();
		gbc_spinnerHeight.insets = new Insets(0, 0, 5, 0);
		gbc_spinnerHeight.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinnerHeight.gridx = 3;
		gbc_spinnerHeight.gridy = 0;
		resamplePanel.add(spinnerHeight, gbc_spinnerHeight);
		
		JButton btnResample = new JButton("Resample");
		btnResample.setToolTipText("Resample all images of the currently active object to the above dimensions");
		GridBagConstraints gbc_btnResample = new GridBagConstraints();
		gbc_btnResample.anchor = GridBagConstraints.EAST;
		gbc_btnResample.gridwidth = 4;
		gbc_btnResample.gridx = 0;
		gbc_btnResample.gridy = 1;
		resamplePanel.add(btnResample, gbc_btnResample);

		// Set the combo box model to the parameter
		if(model != null) { comboBoxObjects.setModel(model); }
		
		// Create a separate loading window positioned over the GLFW window with
		// nothing but an infinite progress bar.  Make it undecorated.
		JFrame loadingFrame = new JFrame("Loading...");
		loadingFrame.setUndecorated(true);
		JProgressBar loadingBar = new JProgressBar();
		loadingBar.setIndeterminate(true);
		loadingFrame.getContentPane().add(loadingBar);
		loadingFrame.pack();
		loadingFrame.setLocationRelativeTo(null);
		
		// Set initial values from the 'model' parameter
		if (model == null || model.getSelectedItem() == null)
		{
			comboBoxObjects.setEnabled(false);
			sliderObjects.setEnabled(false);

			lblGamma.setEnabled(false);
			spinnerGamma.setEnabled(false);
			lblWeightExponent.setEnabled(false);
			spinnerExponent.setEnabled(false);
			chckbxOcclusion.setEnabled(false);
			lblBias.setEnabled(false);
			spinnerOccBias.setEnabled(false);
			
			chckbxHalfRes.setSelected(isHighDPI);
			chckbxHalfRes.setEnabled(false);
			chckbxMultisampling.setEnabled(false);

			lblNewDimensions.setEnabled(false);
			spinnerWidth.setEnabled(false);
			lblX.setEnabled(false);
			spinnerHeight.setEnabled(false);
			btnResample.setEnabled(false);
		}
		else
		{
			comboBoxObjects.setEnabled(model.getSize()>1?true:false);
			sliderObjects.setEnabled(model.getSize()>1?true:false);

			lblGamma.setEnabled(true);
			spinnerGamma.setEnabled(true);
			lblWeightExponent.setEnabled(true);
			spinnerExponent.setEnabled(true);
			chckbxOcclusion.setEnabled(true);
			lblBias.setEnabled(true);
			spinnerOccBias.setEnabled(true);

			chckbxHalfRes.setEnabled(true);
			chckbxMultisampling.setEnabled(true);
			
			lblNewDimensions.setEnabled(true);
			spinnerWidth.setEnabled(true);
			lblX.setEnabled(true);
			spinnerHeight.setEnabled(true);
			btnResample.setEnabled(true);
			
			spinnerGamma.setValue(model.getSelectedItem().getGamma());
			spinnerExponent.setValue(model.getSelectedItem().getWeightExponent());
			chckbxOcclusion.setSelected(model.getSelectedItem().isOcclusionEnabled());
			spinnerOccBias.setValue(model.getSelectedItem().getOcclusionBias());

			model.getSelectedItem().setHalfResolution(isHighDPI);
			chckbxHalfRes.setSelected(isHighDPI);
		}
		
		// Add listener for the 'single' load button to read a single light field object.
		btnLoadSingle.addActionListener(e -> 
		{
			JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
			fileChooser.setDialogTitle("Select a camera definition file");
			fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
			fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Agisoft Photoscan XML files", "xml"));
			fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("View Set files", "vset"));
			fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Zip files", "zip"));
			if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				File file = fileChooser.getSelectedFile();
				if(file.getName().toUpperCase().endsWith(".ZIP"))
				{
					String vsetName = file.getPath();
					vsetName = vsetName.replace(".zip", "/default.vset");
					file = new File(vsetName);
					System.out.printf("Zip file name converted to '%s'\n", vsetName);
				}
				
				try 
				{
					ULFLoadOptions loadOptions = new ULFLoadOptions(
							new ViewSetImageOptions(null, true, chckbxUseMipmaps.isSelected(), chckbxCompressImages.isSelected()),
							chckbxGenerateDepthImages.isSelected(), (Integer)spinnerDepthWidth.getValue(), (Integer)spinnerDepthHeight.getValue());
					
					if (file.getName().toUpperCase().endsWith(".XML"))
					{
						JFileChooser meshChooser = new JFileChooser(file.getParentFile());
						meshChooser.setDialogTitle("Select the corresponding mesh");
						meshChooser.removeChoosableFileFilter(meshChooser.getAcceptAllFileFilter());
						meshChooser.addChoosableFileFilter(new FileNameExtensionFilter("Wavefront OBJ files", "obj"));
						
						if (meshChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
						{
							JFileChooser imageChooser = new JFileChooser(file.getParentFile());
							imageChooser.setDialogTitle("Select the undistorted image directory");
							imageChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
							
							if (imageChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
							{
								loadOptions.getImageOptions().setFilePath(imageChooser.getSelectedFile());
								model.addFromAgisoftXMLFile(file, meshChooser.getSelectedFile(), loadOptions);
							}
						}
					}
					else
					{
						model.addFromVSETFile(file, loadOptions);
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run() {
								loadingBar.setIndeterminate(true);
								loadingFrame.setVisible(true);
							}
						});
					}
				} 
				catch (IOException ex) 
				{
					ex.printStackTrace();
				}
			}
		});
		
		// Add listener for the 'morph' load button to read many light field objects.
		btnLoadMultiple.addActionListener(e -> 
		{
			ULFLoadOptions loadOptions = new ULFLoadOptions(
					new ViewSetImageOptions(null, true, chckbxUseMipmaps.isSelected(), chckbxCompressImages.isSelected()),
					chckbxGenerateDepthImages.isSelected(), (Integer)spinnerDepthWidth.getValue(), (Integer)spinnerDepthHeight.getValue());
			
			JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
			fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
			fileChooser.setFileFilter(new FileNameExtensionFilter("Light Field Morph files (.lfm)", "lfm"));
			if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				try 
				{
					model.addMorphFromLFMFile(fileChooser.getSelectedFile(), loadOptions);
					loadingBar.setIndeterminate(true);
					loadingFrame.setVisible(true);
				} 
				catch (IOException ex) 
				{
					ex.printStackTrace();
				}
			}
		});
		
		// Respond to combo box item changed event
		comboBoxObjects.addItemListener(e ->
		{
			if (model == null || model.getSelectedItem() == null)
			{
				lblGamma.setEnabled(false);
				spinnerGamma.setEnabled(false);
				lblWeightExponent.setEnabled(false);
				spinnerExponent.setEnabled(false);
				chckbxOcclusion.setEnabled(false);
				lblBias.setEnabled(false);
				spinnerOccBias.setEnabled(false);

				chckbxHalfRes.setEnabled(false);
				chckbxMultisampling.setEnabled(false);

				lblNewDimensions.setEnabled(false);
				spinnerWidth.setEnabled(false);
				lblX.setEnabled(false);
				spinnerHeight.setEnabled(false);
				btnResample.setEnabled(false);
			}
			else
			{
				lblGamma.setEnabled(true);
				spinnerGamma.setEnabled(true);
				lblWeightExponent.setEnabled(true);
				spinnerExponent.setEnabled(true);
				chckbxOcclusion.setEnabled(true);
				lblBias.setEnabled(true);
				spinnerOccBias.setEnabled(true);

				chckbxHalfRes.setEnabled(true);
				chckbxMultisampling.setEnabled(true);
				
				lblNewDimensions.setEnabled(true);
				spinnerWidth.setEnabled(true);
				lblX.setEnabled(true);
				spinnerHeight.setEnabled(true);
				btnResample.setEnabled(true);
				
				spinnerGamma.setValue(model.getSelectedItem().getGamma());
				spinnerExponent.setValue(model.getSelectedItem().getWeightExponent());
				chckbxOcclusion.setSelected(model.getSelectedItem().isOcclusionEnabled());
				spinnerOccBias.setValue(model.getSelectedItem().getOcclusionBias());
				chckbxHalfRes.setSelected(model.getSelectedItem().getHalfResolution());
				
				if (model.getSelectedItem() instanceof ULFMorphRenderer<?>)
				{
					ULFMorphRenderer<?> morph = (ULFMorphRenderer<?>)(model.getSelectedItem());
					int currentStage = morph.getCurrentStage();
					sliderObjects.setEnabled(true);
					sliderObjects.setMaximum(morph.getStageCount() - 1);
					sliderObjects.setValue(currentStage);
				}
				else
				{
					sliderObjects.setMaximum(0);
					sliderObjects.setValue(0);
					sliderObjects.setEnabled(false);
				}
			}
		});
		
		// Add listener for the 'resample' button to generate new vies for the current light field.
		btnResample.addActionListener(e -> 
		{
			JFileChooser vsetFileChooser = new JFileChooser(new File("").getAbsolutePath());
			vsetFileChooser.setDialogTitle("Choose a Target VSET File");
			vsetFileChooser.removeChoosableFileFilter(vsetFileChooser.getAcceptAllFileFilter());
			vsetFileChooser.setFileFilter(new FileNameExtensionFilter("View Set files (.vset)", "vset"));
			if (vsetFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				JFileChooser exportFileChooser = new JFileChooser(vsetFileChooser.getSelectedFile().getParentFile());
				exportFileChooser.setDialogTitle("Choose an Export Directory");
				exportFileChooser.removeChoosableFileFilter(exportFileChooser.getAcceptAllFileFilter());
				exportFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
				if (exportFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
				{
					try 
					{
						loadingBar.setIndeterminate(true);
						loadingFrame.setVisible(true);
						model.getSelectedItem().requestResample(
							(Integer)spinnerWidth.getValue(),
							(Integer)spinnerHeight.getValue(), 
							vsetFileChooser.getSelectedFile(), 
							exportFileChooser.getSelectedFile());
					} 
					catch (IOException ex)
					{
						ex.printStackTrace();
					}
				}
			}
		});
		
		chckbxGenerateDepthImages.addChangeListener(e ->
		{
			boolean enabled = chckbxGenerateDepthImages.isSelected();
			lblDimensions.setEnabled(enabled);
			spinnerDepthWidth.setEnabled(enabled);
			lblX_1.setEnabled(enabled);
			spinnerDepthHeight.setEnabled(enabled);
		});
		
		// Add listener for changes to the gamma spinner.
		spinnerGamma.addChangeListener(e ->
		{
			model.getSelectedItem().setGamma((Float)spinnerGamma.getModel().getValue());
		});
		
		// Add listener for changes to the alpha weight spinner.
		spinnerExponent.addChangeListener(e ->
		{
			model.getSelectedItem().setWeightExponent((Float)spinnerExponent.getModel().getValue());
		});
		
		// Add listener for changes to half resolution checkbox.
		chckbxHalfRes.addChangeListener(e ->
		{
			model.getSelectedItem().setHalfResolution(chckbxHalfRes.isSelected());
		});

		// Add listener for changes to occlusion checkbox.
		chckbxOcclusion.addChangeListener(e ->
		{			
			boolean selected = chckbxOcclusion.isSelected();
			model.getSelectedItem().setOcclusionEnabled(selected);
			lblBias.setEnabled(selected);
			spinnerOccBias.setEnabled(selected);
		});
		
		// Add listener for changes to the occlusion bias spinner.
		spinnerOccBias.addChangeListener(e ->
		{
			model.getSelectedItem().setOcclusionBias((Float)spinnerOccBias.getModel().getValue());
		});
		
		// Create callback monitor to show the loading window when the model is being read
		if(model != null)
		{
			model.setLoadingMonitor(new ULFLoadingMonitor()
			{
				private double maximum = 100;
				
				@Override
				public void startLoading() {}

				@Override
				public void setMaximum(double maximum)
				{
					this.maximum = maximum;
				}
				
				@Override
				public void setProgress(double progress)
				{
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							loadingBar.setIndeterminate(false);
							loadingBar.setValue((int)Math.round((progress/maximum) * 100));
						}						
					});
				}
	
				@Override
				public void loadingComplete()
				{
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							loadingFrame.setVisible(false);
						}
					});
				}
			});
		}
	}
	
	/**
	 * This should be called once after the object is constructed but before the event loop is begun.
	 * This is a convenience method so it is compatible with the ULFUserInterface class (which was the
	 * old GUI class). This makes the two classes essentially interchangeable.
	 */
	public void showGUI()
	{
		this.setVisible(true);
	}
}
