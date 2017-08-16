package tetzlaff.texturefit;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import tetzlaff.ibr.SampledLuminanceEncoding;
import tetzlaff.ibr.ViewSet;

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
    private JSpinner textureBlockSizeSpinner;
    private JCheckBox imagePreprojUseCheckBox;
    private JCheckBox imagePreprojReuseCheckBox;

    private JSpinner diffuseDeltaSpinner;
    private JSpinner diffuseIterationsSpinner;
    private JSpinner diffuseCompNormalSpinner;

    private JComboBox<String> primaryViewComboBox;

    private ViewSet currentViewSet;

    private JSpinner spinnerXRiteWhite;
    private JSpinner spinnerXRiteNeutral80;
    private JSpinner spinnerXRiteNeutral65;
    private JSpinner spinnerXRiteNeutral50;
    private JSpinner spinnerXRiteNeutral35;
    private JSpinner spinnerXRiteBlack;
    private JCheckBox chckbxUseXriteMeasurements;
    private JCheckBox chckbxComputeDiffuseTexture;
    private JCheckBox chckbxComputeSpecularTexture;
    private JCheckBox chckbxEstimateLightOffset;
    private JCheckBox chckbxComputeNormalMap;
    private JCheckBox checkBoxEstimateGlobalLightIntensity;
    private JCheckBox chckbxDebugMode;
    private JCheckBox chckbxLevenbergMarquardtSpecularOptimization;

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
        getContentPane().add(tabbedPane);

        TextureFitParameters defaults = new TextureFitParameters();

        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));

        vsetFilePicker = new FilePicker();

        JFileChooser vsetFileChooser = new JFileChooser();

        objFilePicker = new FilePicker();

        JFileChooser fileChooser = new JFileChooser();

        imageDirectoryPicker = new FilePicker();

        JFileChooser fileChooser1 = new JFileChooser();
        fileChooser1.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        maskDirectoryPicker = new FilePicker();

        JFileChooser fileChooser2 = new JFileChooser();
        fileChooser2.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        rescaleDirectoryPicker = new FilePicker();

        JFileChooser fileChooser3 = new JFileChooser();
        fileChooser3.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        outputDirectoryPicker = new FilePicker();

        JFileChooser fileChooser4 = new JFileChooser();
        fileChooser4.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        tabbedPane.addTab("Files and Directories", filePanel);

        JPanel panel_1 = new JPanel();
        panel_1.setBorder(new TitledBorder(null, "Input Files and Directories", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        filePanel.add(panel_1);
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));

        Box vsetLoadBox = new Box(BoxLayout.X_AXIS);
        panel_1.add(vsetLoadBox);
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

        Box loadBox = new Box(BoxLayout.X_AXIS);
        panel_1.add(loadBox);
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

        Box loadBox1 = new Box(BoxLayout.X_AXIS);
        panel_1.add(loadBox1);
        JPanel loadWrapper1 = new JPanel();
        JButton loadButton1 = new JButton("Select Undistorted Photo Directory...");
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

        Box loadBox2 = new Box(BoxLayout.X_AXIS);
        panel_1.add(loadBox2);
        JPanel loadWrapper2 = new JPanel();
        JButton loadButton2 = new JButton("Select Undistorted Mask Directory...");
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

        loadButton1.addActionListener(e1 ->
        {
            if (imageDirectoryPicker.file == null && vsetFilePicker.file != null)
            {
                fileChooser1.setCurrentDirectory(vsetFilePicker.file.getParentFile());
            }

            if (fileChooser1.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            {
                imageDirectoryPicker.file = fileChooser1.getSelectedFile();

                // Clear mask directory
                maskDirectoryPicker.file = null;
                label2.setText("");

                String fileString = imageDirectoryPicker.file.toString();
                label1.setText(fileString.length() < 32 ? fileString : "..." + fileString.substring(fileString.length() - 32));
            }
        });

        loadButton.addActionListener(e ->
        {
            if (objFilePicker.file == null && vsetFilePicker.file != null)
            {
                fileChooser.setCurrentDirectory(vsetFilePicker.file.getParentFile());
            }

            fileChooser.resetChoosableFileFilters();
            fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());

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

        vsetLoadButton.addActionListener(e ->
        {
            if (vsetFilePicker.file == null && vsetFilePicker.file != null)
            {
                vsetFileChooser.setCurrentDirectory(vsetFilePicker.file.getParentFile());
            }

            vsetFileChooser.resetChoosableFileFilters();
            vsetFileChooser.removeChoosableFileFilter(vsetFileChooser.getAcceptAllFileFilter());

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

                try
                {
                    if (fileString.endsWith(".vset"))
                    {
                        currentViewSet = ViewSet.loadFromVSETFile(new File(fileString));
                    }
                    else if (fileString.endsWith(".xml"))
                    {
                        currentViewSet = ViewSet.loadFromAgisoftXMLFile(new File(fileString));
                    }

                    List<String> cameraPoseNames = new ArrayList<String>();
                    for (int i = 0; i < currentViewSet.getCameraPoseCount(); i++)
                    {
                        cameraPoseNames.add(currentViewSet.getImageFileName(i));
                    }

                    String primaryViewName = cameraPoseNames.get(0);

                    cameraPoseNames.sort((e1, e2) -> e1.compareTo(e2));

                    String[] cameraPoseNameArray = new String[cameraPoseNames.size()];
                    cameraPoseNames.toArray(cameraPoseNameArray);

                    primaryViewComboBox.setModel(new DefaultComboBoxModel<String>(cameraPoseNameArray));
                    primaryViewComboBox.setSelectedItem(primaryViewName);

                    SampledLuminanceEncoding luminanceEncoding = currentViewSet.getLuminanceEncoding();

                    if (luminanceEncoding != null)
                    {
                        spinnerXRiteBlack.setValue((int)Math.round(luminanceEncoding.encodeFunction.applyAsDouble(0.031)));
                        spinnerXRiteNeutral35.setValue((int)Math.round(luminanceEncoding.encodeFunction.applyAsDouble(0.090)));
                        spinnerXRiteNeutral50.setValue((int)Math.round(luminanceEncoding.encodeFunction.applyAsDouble(0.198)));
                        spinnerXRiteNeutral65.setValue((int)Math.round(luminanceEncoding.encodeFunction.applyAsDouble(0.362)));
                        spinnerXRiteNeutral80.setValue((int)Math.round(luminanceEncoding.encodeFunction.applyAsDouble(0.591)));
                        spinnerXRiteWhite.setValue((int)Math.round(luminanceEncoding.encodeFunction.applyAsDouble(0.9)));
                    }
                }
                catch(IOException ex)
                {
                    ex.printStackTrace();
                }
            }
        });

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Output Directories", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        filePanel.add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Box loadBox3 = new Box(BoxLayout.X_AXIS);
        panel.add(loadBox3);
        JPanel loadWrapper3 = new JPanel();
        JButton loadButton3 = new JButton("Select Destination for Resized Photos...");
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

        Box loadBox4 = new Box(BoxLayout.X_AXIS);
        panel.add(loadBox4);
        JPanel loadWrapper4 = new JPanel();
        JButton loadButton4 = new JButton("Select Destination for Model/Textures...");
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

        JPanel panel_4 = new JPanel();
        filePanel.add(panel_4);
        GridBagLayout gbl_panel_4 = new GridBagLayout();
        gbl_panel_4.columnWidths = new int[] {80, 400};
        gbl_panel_4.rowHeights = new int[] {30};
        gbl_panel_4.columnWeights = new double[]{0.0, 0.0};
        gbl_panel_4.rowWeights = new double[]{0.0};
        panel_4.setLayout(gbl_panel_4);

        JLabel lblPrimaryView = new JLabel("Primary View:");
        GridBagConstraints gbc_lblPrimaryView = new GridBagConstraints();
        gbc_lblPrimaryView.anchor = GridBagConstraints.EAST;
        gbc_lblPrimaryView.insets = new Insets(0, 0, 0, 5);
        gbc_lblPrimaryView.gridx = 0;
        gbc_lblPrimaryView.gridy = 0;
        panel_4.add(lblPrimaryView, gbc_lblPrimaryView);

        primaryViewComboBox = new JComboBox<String>();
        GridBagConstraints gbc_comboBox = new GridBagConstraints();
        gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
        gbc_comboBox.insets = new Insets(0, 0, 0, 5);
        gbc_comboBox.gridx = 1;
        gbc_comboBox.gridy = 0;
        panel_4.add(primaryViewComboBox, gbc_comboBox);

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

        JPanel basicSettingsPanel = new JPanel();

        JPanel colorCheckerPanel = new JPanel();
        tabbedPane.addTab("ColorChecker Measurements", null, colorCheckerPanel, null);
        GridBagLayout gbl_colorCheckerPanel = new GridBagLayout();
        gbl_colorCheckerPanel.columnWidths = new int[] {60, 60};
        gbl_colorCheckerPanel.rowHeights = new int[] {0, 0, 30, 30, 30, 30, 30, 30};
        gbl_colorCheckerPanel.columnWeights = new double[]{0.0, 0.0};
        gbl_colorCheckerPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        colorCheckerPanel.setLayout(gbl_colorCheckerPanel);

        JLabel label_1 = new JLabel("Gamma:");
        GridBagConstraints gbc_label_1 = new GridBagConstraints();
        gbc_label_1.insets = new Insets(0, 0, 5, 5);
        gbc_label_1.gridx = 0;
        gbc_label_1.gridy = 0;
        colorCheckerPanel.add(label_1, gbc_label_1);

        gammaSpinner = new JSpinner(new SpinnerNumberModel((double) defaults.getGamma(), (double) 1.0f, (double) 99.0f, (double) 0.1f));
        GridBagConstraints gbc_spinner = new GridBagConstraints();
        gbc_spinner.insets = new Insets(0, 0, 5, 0);
        gbc_spinner.gridx = 1;
        gbc_spinner.gridy = 0;
        colorCheckerPanel.add(gammaSpinner, gbc_spinner);

        chckbxUseXriteMeasurements = new JCheckBox("Use ColorChecker measurements");
        GridBagConstraints gbc_chckbxUseXriteMeasurements = new GridBagConstraints();
        gbc_chckbxUseXriteMeasurements.gridwidth = 2;
        gbc_chckbxUseXriteMeasurements.insets = new Insets(0, 0, 5, 0);
        gbc_chckbxUseXriteMeasurements.gridx = 0;
        gbc_chckbxUseXriteMeasurements.gridy = 1;
        colorCheckerPanel.add(chckbxUseXriteMeasurements, gbc_chckbxUseXriteMeasurements);

        JLabel lblWhite = new JLabel("White:");
        GridBagConstraints gbc_lblWhite = new GridBagConstraints();
        gbc_lblWhite.insets = new Insets(0, 0, 5, 5);
        gbc_lblWhite.gridx = 0;
        gbc_lblWhite.gridy = 2;
        colorCheckerPanel.add(lblWhite, gbc_lblWhite);

        spinnerXRiteWhite = new JSpinner();
        spinnerXRiteWhite.setModel(new SpinnerNumberModel(243, 0, 255, 1));
        GridBagConstraints gbc_spinnerXRiteWhite = new GridBagConstraints();
        gbc_spinnerXRiteWhite.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerXRiteWhite.gridx = 1;
        gbc_spinnerXRiteWhite.gridy = 2;
        colorCheckerPanel.add(spinnerXRiteWhite, gbc_spinnerXRiteWhite);

        JLabel labelXRiteNeutral80 = new JLabel("Neutral 8");
        GridBagConstraints gbc_labelXRiteNeutral80 = new GridBagConstraints();
        gbc_labelXRiteNeutral80.insets = new Insets(0, 0, 5, 5);
        gbc_labelXRiteNeutral80.gridx = 0;
        gbc_labelXRiteNeutral80.gridy = 3;
        colorCheckerPanel.add(labelXRiteNeutral80, gbc_labelXRiteNeutral80);

        spinnerXRiteNeutral80 = new JSpinner();
        spinnerXRiteNeutral80.setModel(new SpinnerNumberModel(201, 0, 255, 1));
        GridBagConstraints gbc_spinnerXRiteNeutral80 = new GridBagConstraints();
        gbc_spinnerXRiteNeutral80.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerXRiteNeutral80.gridx = 1;
        gbc_spinnerXRiteNeutral80.gridy = 3;
        colorCheckerPanel.add(spinnerXRiteNeutral80, gbc_spinnerXRiteNeutral80);

        JLabel labelXRiteNeutral65 = new JLabel("Neutral 6.5");
        GridBagConstraints gbc_labelXRiteNeutral65 = new GridBagConstraints();
        gbc_labelXRiteNeutral65.insets = new Insets(0, 0, 5, 5);
        gbc_labelXRiteNeutral65.gridx = 0;
        gbc_labelXRiteNeutral65.gridy = 4;
        colorCheckerPanel.add(labelXRiteNeutral65, gbc_labelXRiteNeutral65);

        spinnerXRiteNeutral65 = new JSpinner();
        spinnerXRiteNeutral65.setModel(new SpinnerNumberModel(161, 0, 255, 1));
        GridBagConstraints gbc_spinnerXRiteNeutral65 = new GridBagConstraints();
        gbc_spinnerXRiteNeutral65.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerXRiteNeutral65.gridx = 1;
        gbc_spinnerXRiteNeutral65.gridy = 4;
        colorCheckerPanel.add(spinnerXRiteNeutral65, gbc_spinnerXRiteNeutral65);

        JLabel lblNeutral_3 = new JLabel("Neutral 5");
        GridBagConstraints gbc_lblNeutral_3 = new GridBagConstraints();
        gbc_lblNeutral_3.insets = new Insets(0, 0, 5, 5);
        gbc_lblNeutral_3.gridx = 0;
        gbc_lblNeutral_3.gridy = 5;
        colorCheckerPanel.add(lblNeutral_3, gbc_lblNeutral_3);

        spinnerXRiteNeutral50 = new JSpinner();
        spinnerXRiteNeutral50.setModel(new SpinnerNumberModel(122, 0, 255, 1));
        GridBagConstraints gbc_spinnerXRiteNeutral50 = new GridBagConstraints();
        gbc_spinnerXRiteNeutral50.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerXRiteNeutral50.gridx = 1;
        gbc_spinnerXRiteNeutral50.gridy = 5;
        colorCheckerPanel.add(spinnerXRiteNeutral50, gbc_spinnerXRiteNeutral50);

        JLabel lblNeutral_1 = new JLabel("Neutral 3.5");
        GridBagConstraints gbc_lblNeutral_1 = new GridBagConstraints();
        gbc_lblNeutral_1.insets = new Insets(0, 0, 5, 5);
        gbc_lblNeutral_1.gridx = 0;
        gbc_lblNeutral_1.gridy = 6;
        colorCheckerPanel.add(lblNeutral_1, gbc_lblNeutral_1);

        spinnerXRiteNeutral35 = new JSpinner();
        spinnerXRiteNeutral35.setModel(new SpinnerNumberModel(85, 0, 255, 1));
        GridBagConstraints gbc_spinnerXRiteNeutral35 = new GridBagConstraints();
        gbc_spinnerXRiteNeutral35.insets = new Insets(0, 0, 5, 0);
        gbc_spinnerXRiteNeutral35.gridx = 1;
        gbc_spinnerXRiteNeutral35.gridy = 6;
        colorCheckerPanel.add(spinnerXRiteNeutral35, gbc_spinnerXRiteNeutral35);

        JLabel lblBlack = new JLabel("Black:");
        GridBagConstraints gbc_lblBlack = new GridBagConstraints();
        gbc_lblBlack.insets = new Insets(0, 0, 0, 5);
        gbc_lblBlack.gridx = 0;
        gbc_lblBlack.gridy = 7;
        colorCheckerPanel.add(lblBlack, gbc_lblBlack);

        spinnerXRiteBlack = new JSpinner();
        spinnerXRiteBlack.setModel(new SpinnerNumberModel(53, 0, 255, 1));
        GridBagConstraints gbc_spinnerXRiteBlack = new GridBagConstraints();
        gbc_spinnerXRiteBlack.gridx = 1;
        gbc_spinnerXRiteBlack.gridy = 7;
        colorCheckerPanel.add(spinnerXRiteBlack, gbc_spinnerXRiteBlack);
        GridBagLayout gbl_basicSettingsPanel = new GridBagLayout();
        gbl_basicSettingsPanel.columnWidths = new int[]{510, 0};
        gbl_basicSettingsPanel.rowHeights = new int[]{123, 88, 153, 0};
        gbl_basicSettingsPanel.columnWeights = new double[]{0.0, Double.MIN_VALUE};
        gbl_basicSettingsPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
        basicSettingsPanel.setLayout(gbl_basicSettingsPanel);

        JPanel panel_2 = new JPanel();
        panel_2.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Photo Resolution", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        GridBagConstraints gbc_panel_2 = new GridBagConstraints();
        gbc_panel_2.fill = GridBagConstraints.BOTH;
        gbc_panel_2.insets = new Insets(0, 0, 5, 0);
        gbc_panel_2.gridx = 0;
        gbc_panel_2.gridy = 0;
        basicSettingsPanel.add(panel_2, gbc_panel_2);
        panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.Y_AXIS));

        JPanel panel_3 = new JPanel();
        panel_2.add(panel_3);
        GridBagLayout gbl_panel_3 = new GridBagLayout();
        gbl_panel_3.columnWidths = new int[] {60, 60, 60, 60, 235};
        gbl_panel_3.rowHeights = new int[] {20, 20, 0, 0};
        gbl_panel_3.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0};
        gbl_panel_3.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
        panel_3.setLayout(gbl_panel_3);

        imageRescaleCheckBox = new JCheckBox("Resize Photos", defaults.isImageRescalingEnabled());
        GridBagConstraints gbc_imageRescaleCheckBox = new GridBagConstraints();
        gbc_imageRescaleCheckBox.gridwidth = 4;
        gbc_imageRescaleCheckBox.insets = new Insets(0, 0, 5, 5);
        gbc_imageRescaleCheckBox.anchor = GridBagConstraints.NORTHWEST;
        gbc_imageRescaleCheckBox.gridx = 0;
        gbc_imageRescaleCheckBox.gridy = 0;
        panel_3.add(imageRescaleCheckBox, gbc_imageRescaleCheckBox);
        imageRescaleCheckBox.setBorder(new EmptyBorder(5, 10, 5, 10));
        JLabel imageWidthLabel = new JLabel("Width:");
        GridBagConstraints gbc_imageWidthLabel = new GridBagConstraints();
        gbc_imageWidthLabel.insets = new Insets(0, 0, 5, 5);
        gbc_imageWidthLabel.gridx = 0;
        gbc_imageWidthLabel.gridy = 1;
        panel_3.add(imageWidthLabel, gbc_imageWidthLabel);
        imageWidthSpinner = new JSpinner(new SpinnerNumberModel((double) defaults.getImageWidth(), (double) 1.0f, (double) 8192.0f, (double) 1.0f));
        GridBagConstraints gbc_imageWidthSpinner = new GridBagConstraints();
        gbc_imageWidthSpinner.insets = new Insets(0, 0, 5, 5);
        gbc_imageWidthSpinner.gridx = 1;
        gbc_imageWidthSpinner.gridy = 1;
        panel_3.add(imageWidthSpinner, gbc_imageWidthSpinner);
        JLabel imageHeightLabel = new JLabel("Height:");
        GridBagConstraints gbc_imageHeightLabel = new GridBagConstraints();
        gbc_imageHeightLabel.insets = new Insets(0, 0, 5, 5);
        gbc_imageHeightLabel.gridx = 2;
        gbc_imageHeightLabel.gridy = 1;
        panel_3.add(imageHeightLabel, gbc_imageHeightLabel);
        imageHeightSpinner = new JSpinner(new SpinnerNumberModel((double) defaults.getImageHeight(), (double) 1.0f, (double) 8192.0f, (double) 1.0f));
        GridBagConstraints gbc_imageHeightSpinner = new GridBagConstraints();
        gbc_imageHeightSpinner.insets = new Insets(0, 0, 5, 5);
        gbc_imageHeightSpinner.gridx = 3;
        gbc_imageHeightSpinner.gridy = 1;
        panel_3.add(imageHeightSpinner, gbc_imageHeightSpinner);

        imagePreprojUseCheckBox = new JCheckBox("Pre-project photos (reduces graphics memory usage but takes longer)", defaults.isImagePreprojectionUseEnabled());
        GridBagConstraints gbc_imagePreprojUseCheckBox = new GridBagConstraints();
        gbc_imagePreprojUseCheckBox.anchor = GridBagConstraints.WEST;
        gbc_imagePreprojUseCheckBox.gridwidth = 5;
        gbc_imagePreprojUseCheckBox.insets = new Insets(0, 0, 0, 5);
        gbc_imagePreprojUseCheckBox.gridx = 0;
        gbc_imagePreprojUseCheckBox.gridy = 2;
        panel_3.add(imagePreprojUseCheckBox, gbc_imagePreprojUseCheckBox);
        imagePreprojUseCheckBox.setBorder(new EmptyBorder(5, 10, 5, 10));

        Box imageHeightBox = new Box(BoxLayout.X_AXIS);
        panel_2.add(imageHeightBox);
        imageHeightBox.setBorder(new EmptyBorder(5, 10, 5, 10));

        JPanel panel_5 = new JPanel();
        panel_5.setBorder(new TitledBorder(null, "Output Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        GridBagConstraints gbc_panel_5 = new GridBagConstraints();
        gbc_panel_5.fill = GridBagConstraints.BOTH;
        gbc_panel_5.insets = new Insets(0, 0, 5, 0);
        gbc_panel_5.gridx = 0;
        gbc_panel_5.gridy = 1;
        basicSettingsPanel.add(panel_5, gbc_panel_5);
        GridBagLayout gbl_panel_5 = new GridBagLayout();
        gbl_panel_5.columnWidths = new int[] {140, 80, 260};
        gbl_panel_5.rowHeights = new int[]{20, 0, 0, 0};
        gbl_panel_5.columnWeights = new double[]{0.0, 0.0};
        gbl_panel_5.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
        panel_5.setLayout(gbl_panel_5);
        JLabel textureSizeLabel = new JLabel("Texture Resolution:");
        GridBagConstraints gbc_textureSizeLabel = new GridBagConstraints();
        gbc_textureSizeLabel.insets = new Insets(0, 5, 5, 5);
        gbc_textureSizeLabel.anchor = GridBagConstraints.EAST;
        gbc_textureSizeLabel.gridx = 0;
        gbc_textureSizeLabel.gridy = 0;
        panel_5.add(textureSizeLabel, gbc_textureSizeLabel);
        textureSizeSpinner = new JSpinner(new SpinnerNumberModel((double) defaults.getTextureSize(), (double) 1.0f, (double) 8192.0f, (double) 1.0f));
        GridBagConstraints gbc_textureSizeSpinner = new GridBagConstraints();
        gbc_textureSizeSpinner.insets = new Insets(0, 0, 5, 5);
        gbc_textureSizeSpinner.anchor = GridBagConstraints.NORTHWEST;
        gbc_textureSizeSpinner.gridx = 1;
        gbc_textureSizeSpinner.gridy = 0;
        panel_5.add(textureSizeSpinner, gbc_textureSizeSpinner);

        chckbxComputeDiffuseTexture = new JCheckBox("Compute diffuse texture");
        chckbxComputeDiffuseTexture.setSelected(true);
        GridBagConstraints gbc_chckbxComputeDiffuseTexture = new GridBagConstraints();
        gbc_chckbxComputeDiffuseTexture.gridwidth = 2;
        gbc_chckbxComputeDiffuseTexture.insets = new Insets(0, 0, 5, 5);
        gbc_chckbxComputeDiffuseTexture.gridx = 0;
        gbc_chckbxComputeDiffuseTexture.gridy = 1;
        panel_5.add(chckbxComputeDiffuseTexture, gbc_chckbxComputeDiffuseTexture);

        chckbxComputeSpecularTexture = new JCheckBox("Compute specular texture");
        chckbxComputeSpecularTexture.setSelected(true);
        GridBagConstraints gbc_chckbxComputeSpecularTexture = new GridBagConstraints();
        gbc_chckbxComputeSpecularTexture.gridwidth = 2;
        gbc_chckbxComputeSpecularTexture.insets = new Insets(0, 0, 0, 5);
        gbc_chckbxComputeSpecularTexture.gridx = 0;
        gbc_chckbxComputeSpecularTexture.gridy = 2;
        panel_5.add(chckbxComputeSpecularTexture, gbc_chckbxComputeSpecularTexture);
        tabbedPane.addTab("Basic Settings", basicSettingsPanel);

        JPanel advancedSettingsPanel = new JPanel();
        GridBagLayout gbl_advancedSettingsPanel = new GridBagLayout();
        gbl_advancedSettingsPanel.columnWidths = new int[] {30, 30, 30, 30};
        gbl_advancedSettingsPanel.rowHeights = new int[] {30, 30, 30, 30, 30, 0, 30, 30, 30, 30, 30};
        gbl_advancedSettingsPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0};
        gbl_advancedSettingsPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        advancedSettingsPanel.setLayout(gbl_advancedSettingsPanel);

        chckbxDebugMode = new JCheckBox("Debug mode");
        GridBagConstraints gbc_chckbxDebugMode = new GridBagConstraints();
        gbc_chckbxDebugMode.insets = new Insets(0, 0, 5, 5);
        gbc_chckbxDebugMode.gridx = 0;
        gbc_chckbxDebugMode.gridy = 0;
        advancedSettingsPanel.add(chckbxDebugMode, gbc_chckbxDebugMode);

        cameraVisCheckBox = new JCheckBox("Enable camera visibility test", defaults.isCameraVisibilityTestEnabled());
        GridBagConstraints gbc_cameraVisCheckBox = new GridBagConstraints();
        gbc_cameraVisCheckBox.gridwidth = 2;
        gbc_cameraVisCheckBox.anchor = GridBagConstraints.WEST;
        gbc_cameraVisCheckBox.insets = new Insets(0, 0, 5, 5);
        gbc_cameraVisCheckBox.gridx = 0;
        gbc_cameraVisCheckBox.gridy = 1;
        advancedSettingsPanel.add(cameraVisCheckBox, gbc_cameraVisCheckBox);
        cameraVisCheckBox.setBorder(new EmptyBorder(5, 10, 5, 10));
        JLabel cameraVisBiasLabel = new JLabel("Camera visibility test bias:");
        GridBagConstraints gbc_cameraVisBiasLabel = new GridBagConstraints();
        gbc_cameraVisBiasLabel.insets = new Insets(0, 0, 5, 5);
        gbc_cameraVisBiasLabel.gridx = 0;
        gbc_cameraVisBiasLabel.gridy = 2;
        advancedSettingsPanel.add(cameraVisBiasLabel, gbc_cameraVisBiasLabel);
        cameraVisBiasSpinner = new JSpinner(new SpinnerNumberModel((double) defaults.getCameraVisibilityTestBias(), (double) 0.0f, (double) 1.0f, (double) 0.0001f));
        GridBagConstraints gbc_cameraVisBiasSpinner = new GridBagConstraints();
        gbc_cameraVisBiasSpinner.insets = new Insets(0, 0, 5, 5);
        gbc_cameraVisBiasSpinner.gridx = 1;
        gbc_cameraVisBiasSpinner.gridy = 2;
        advancedSettingsPanel.add(cameraVisBiasSpinner, gbc_cameraVisBiasSpinner);

        JSpinner.NumberEditor cameraVisBiasNumberEditor = new JSpinner.NumberEditor(cameraVisBiasSpinner, "0.0000");
        cameraVisBiasSpinner.setEditor(cameraVisBiasNumberEditor);
        JLabel textureBlockSizeLabel = new JLabel("Texture block size:");
        GridBagConstraints gbc_textureBlockSizeLabel = new GridBagConstraints();
        gbc_textureBlockSizeLabel.insets = new Insets(0, 0, 5, 5);
        gbc_textureBlockSizeLabel.gridx = 0;
        gbc_textureBlockSizeLabel.gridy = 3;
        advancedSettingsPanel.add(textureBlockSizeLabel, gbc_textureBlockSizeLabel);
        textureBlockSizeSpinner = new JSpinner(new SpinnerNumberModel(256.0, 1.0, 8192.0, 1.0));
        GridBagConstraints gbc_textureBlockSizeSpinner = new GridBagConstraints();
        gbc_textureBlockSizeSpinner.insets = new Insets(0, 0, 5, 5);
        gbc_textureBlockSizeSpinner.gridx = 1;
        gbc_textureBlockSizeSpinner.gridy = 3;
        advancedSettingsPanel.add(textureBlockSizeSpinner, gbc_textureBlockSizeSpinner);

        imagePreprojReuseCheckBox = new JCheckBox("Reuse pre-projected photos", defaults.isImagePreprojectionGenerationEnabled());
        GridBagConstraints gbc_imagePreprojReuseCheckBox = new GridBagConstraints();
        gbc_imagePreprojReuseCheckBox.gridwidth = 2;
        gbc_imagePreprojReuseCheckBox.anchor = GridBagConstraints.WEST;
        gbc_imagePreprojReuseCheckBox.insets = new Insets(0, 0, 5, 5);
        gbc_imagePreprojReuseCheckBox.gridx = 0;
        gbc_imagePreprojReuseCheckBox.gridy = 4;
        advancedSettingsPanel.add(imagePreprojReuseCheckBox, gbc_imagePreprojReuseCheckBox);
        imagePreprojReuseCheckBox.setBorder(new EmptyBorder(5, 10, 5, 10));

        checkBoxEstimateGlobalLightIntensity = new JCheckBox("Estimate global light intensity from primary view");
        checkBoxEstimateGlobalLightIntensity.setSelected(true);
        GridBagConstraints gbc_checkBoxEstimateGlobalLightIntensity = new GridBagConstraints();
        gbc_checkBoxEstimateGlobalLightIntensity.anchor = GridBagConstraints.WEST;
        gbc_checkBoxEstimateGlobalLightIntensity.gridwidth = 2;
        gbc_checkBoxEstimateGlobalLightIntensity.insets = new Insets(0, 0, 5, 5);
        gbc_checkBoxEstimateGlobalLightIntensity.gridx = 0;
        gbc_checkBoxEstimateGlobalLightIntensity.gridy = 5;
        advancedSettingsPanel.add(checkBoxEstimateGlobalLightIntensity, gbc_checkBoxEstimateGlobalLightIntensity);
        JLabel diffuseDeltaLabel = new JLabel("\"Delta\" for diffuse fit:");
        GridBagConstraints gbc_diffuseDeltaLabel = new GridBagConstraints();
        gbc_diffuseDeltaLabel.insets = new Insets(0, 0, 5, 5);
        gbc_diffuseDeltaLabel.gridx = 0;
        gbc_diffuseDeltaLabel.gridy = 6;
        advancedSettingsPanel.add(diffuseDeltaLabel, gbc_diffuseDeltaLabel);
        diffuseDeltaSpinner = new JSpinner(new SpinnerNumberModel((double) defaults.getDiffuseDelta(), (double) 0.0f, (double) 1.0f, (double) 0.01f));
        GridBagConstraints gbc_diffuseDeltaSpinner = new GridBagConstraints();
        gbc_diffuseDeltaSpinner.fill = GridBagConstraints.HORIZONTAL;
        gbc_diffuseDeltaSpinner.insets = new Insets(0, 0, 5, 5);
        gbc_diffuseDeltaSpinner.gridx = 1;
        gbc_diffuseDeltaSpinner.gridy = 6;
        advancedSettingsPanel.add(diffuseDeltaSpinner, gbc_diffuseDeltaSpinner);
        JLabel diffuseIterationsLabel = new JLabel("Diffuse fit iterations:");
        GridBagConstraints gbc_diffuseIterationsLabel = new GridBagConstraints();
        gbc_diffuseIterationsLabel.insets = new Insets(0, 0, 5, 5);
        gbc_diffuseIterationsLabel.gridx = 0;
        gbc_diffuseIterationsLabel.gridy = 7;
        advancedSettingsPanel.add(diffuseIterationsLabel, gbc_diffuseIterationsLabel);
        diffuseIterationsSpinner = new JSpinner(new SpinnerNumberModel((double) defaults.getDiffuseIterations(), (double) 0.0f, (double) 999.0f, (double) 1.0f));
        GridBagConstraints gbc_diffuseIterationsSpinner = new GridBagConstraints();
        gbc_diffuseIterationsSpinner.insets = new Insets(0, 0, 5, 5);
        gbc_diffuseIterationsSpinner.gridx = 1;
        gbc_diffuseIterationsSpinner.gridy = 7;
        advancedSettingsPanel.add(diffuseIterationsSpinner, gbc_diffuseIterationsSpinner);

        tabbedPane.addTab("Advanced Settings", advancedSettingsPanel);

        JPanel experimentalSettingsPanel = new JPanel();
        tabbedPane.addTab("Experimental Settings", experimentalSettingsPanel);
        GridBagLayout gbl_experimentalSettingsPanel = new GridBagLayout();
        gbl_experimentalSettingsPanel.columnWidths = new int[] {127, 127, 0};
        gbl_experimentalSettingsPanel.rowHeights = new int[] {30, 30, 30, 0, 0};
        gbl_experimentalSettingsPanel.columnWeights = new double[]{0.0, 0.0, 0.0};
        gbl_experimentalSettingsPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0};
        experimentalSettingsPanel.setLayout(gbl_experimentalSettingsPanel);

        chckbxEstimateLightOffset = new JCheckBox("Estimate light offset");
        GridBagConstraints gbc_chckbxEstimateLightOffset = new GridBagConstraints();
        gbc_chckbxEstimateLightOffset.gridwidth = 2;
        gbc_chckbxEstimateLightOffset.anchor = GridBagConstraints.NORTHWEST;
        gbc_chckbxEstimateLightOffset.insets = new Insets(0, 0, 5, 5);
        gbc_chckbxEstimateLightOffset.gridx = 0;
        gbc_chckbxEstimateLightOffset.gridy = 0;
        experimentalSettingsPanel.add(chckbxEstimateLightOffset, gbc_chckbxEstimateLightOffset);

        chckbxComputeNormalMap = new JCheckBox("Compute normal map");
        GridBagConstraints gbc_chckbxComputeNormalMap = new GridBagConstraints();
        gbc_chckbxComputeNormalMap.gridwidth = 2;
        gbc_chckbxComputeNormalMap.anchor = GridBagConstraints.NORTHWEST;
        gbc_chckbxComputeNormalMap.insets = new Insets(0, 0, 5, 5);
        gbc_chckbxComputeNormalMap.gridx = 0;
        gbc_chckbxComputeNormalMap.gridy = 1;
        experimentalSettingsPanel.add(chckbxComputeNormalMap, gbc_chckbxComputeNormalMap);
        JLabel diffuseComplNormalLabel = new JLabel("Computed normal weight:");
        GridBagConstraints gbc_diffuseComplNormalLabel = new GridBagConstraints();
        gbc_diffuseComplNormalLabel.anchor = GridBagConstraints.WEST;
        gbc_diffuseComplNormalLabel.insets = new Insets(0, 0, 5, 5);
        gbc_diffuseComplNormalLabel.gridx = 0;
        gbc_diffuseComplNormalLabel.gridy = 2;
        experimentalSettingsPanel.add(diffuseComplNormalLabel, gbc_diffuseComplNormalLabel);
        diffuseCompNormalSpinner = new JSpinner(new SpinnerNumberModel((double) Math.min(9999.0f, defaults.getDiffuseComputedNormalWeight()), (double) 0.0f, (double) 9999.0f, (double) 0.1f));
        GridBagConstraints gbc_diffuseCompNormalSpinner = new GridBagConstraints();
        gbc_diffuseCompNormalSpinner.insets = new Insets(0, 0, 5, 5);
        gbc_diffuseCompNormalSpinner.anchor = GridBagConstraints.WEST;
        gbc_diffuseCompNormalSpinner.gridx = 1;
        gbc_diffuseCompNormalSpinner.gridy = 2;
        experimentalSettingsPanel.add(diffuseCompNormalSpinner, gbc_diffuseCompNormalSpinner);

        chckbxLevenbergMarquardtSpecularOptimization = new JCheckBox("Levenberg-Marquardt specular optimization");
        GridBagConstraints gbc_chckbxLevenbergMarquardtSpecularOptimization = new GridBagConstraints();
        gbc_chckbxLevenbergMarquardtSpecularOptimization.anchor = GridBagConstraints.WEST;
        gbc_chckbxLevenbergMarquardtSpecularOptimization.gridwidth = 2;
        gbc_chckbxLevenbergMarquardtSpecularOptimization.insets = new Insets(0, 0, 5, 5);
        gbc_chckbxLevenbergMarquardtSpecularOptimization.gridx = 0;
        gbc_chckbxLevenbergMarquardtSpecularOptimization.gridy = 3;
        experimentalSettingsPanel.add(chckbxLevenbergMarquardtSpecularOptimization, gbc_chckbxLevenbergMarquardtSpecularOptimization);

        Box executeBox = new Box(BoxLayout.X_AXIS);
        JPanel executeWrapper = new JPanel();
        executeButton = new JButton("Execute...");
        executeWrapper.add(executeButton);
        executeWrapper.setBorder(new EmptyBorder(0, 10, 0, 10));
        executeBox.add(executeWrapper);
        getContentPane().add(executeBox);

        this.pack();

        JFrame loadingFrame = new JFrame("Loading...");
        loadingFrame.setUndecorated(true);
        JProgressBar loadingBar = new JProgressBar();
        loadingBar.setIndeterminate(true);
        loadingFrame.getContentPane().add(loadingBar);
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

        int textureSubdiv = (int)Math.ceil((double)getValueAsInt(this.textureSizeSpinner) / (double)getValueAsInt(this.textureBlockSizeSpinner));
        param.setTextureSubdivision(textureSubdiv);

        param.setImagePreprojectionUseEnabled(this.imagePreprojUseCheckBox.isSelected());
        param.setImagePreprojectionGenerationEnabled(this.imagePreprojUseCheckBox.isSelected() && !this.imagePreprojReuseCheckBox.isSelected());
        param.setImageRescalingEnabled(this.imageRescaleCheckBox.isSelected());
        param.setImageWidth(getValueAsInt(this.imageWidthSpinner));
        param.setImageHeight(getValueAsInt(this.imageHeightSpinner));
        param.setDiffuseDelta(getValueAsFloat(this.diffuseDeltaSpinner));
        param.setDiffuseIterations(getValueAsInt(this.diffuseIterationsSpinner));
        param.setDiffuseComputedNormalWeight(this.chckbxComputeNormalMap.isSelected() ? getValueAsFloat(this.diffuseCompNormalSpinner) : 0.0f);
        param.setDiffuseInputNormalWeight(Float.MAX_VALUE);
        param.setDiffuseTextureEnabled(this.chckbxComputeDiffuseTexture.isSelected());
        param.setNormalTextureEnabled(this.chckbxComputeNormalMap.isSelected());
        param.setSpecularTextureEnabled(this.chckbxComputeSpecularTexture.isSelected());
        param.setLightIntensityEstimationEnabled(this.checkBoxEstimateGlobalLightIntensity.isSelected());
        param.setLightOffsetEstimationEnabled(this.chckbxEstimateLightOffset.isSelected());
        param.setLevenbergMarquardtOptimizationEnabled(this.chckbxLevenbergMarquardtSpecularOptimization.isSelected());
        param.setDebugModeEnabled(this.chckbxDebugMode.isSelected());

        param.setPrimaryViewName((String)this.primaryViewComboBox.getSelectedItem());

        if (this.chckbxUseXriteMeasurements.isSelected())
        {
            param.setLinearLuminanceValues(new double[] { 0.031, 0.090, 0.198, 0.362, 0.591, 0.900 });
            param.setEncodedLuminanceValues(new byte[]
            {
                (byte)((int)((Integer)this.spinnerXRiteBlack.getValue())),
                (byte)((int)((Integer)this.spinnerXRiteNeutral35.getValue())),
                (byte)((int)((Integer)this.spinnerXRiteNeutral50.getValue())),
                (byte)((int)((Integer)this.spinnerXRiteNeutral65.getValue())),
                (byte)((int)((Integer)this.spinnerXRiteNeutral80.getValue())),
                (byte)((int)((Integer)this.spinnerXRiteWhite.getValue()))
            });
        }
        else
        {
            // Use pre-existing luminance levels or defaults
            param.setLinearLuminanceValues(null);
            param.setEncodedLuminanceValues(null);
        }

        return param;
    }

    public void addExecuteButtonActionListener(ActionListener actionListener)
    {
        executeButton.addActionListener(actionListener);
    }
}
