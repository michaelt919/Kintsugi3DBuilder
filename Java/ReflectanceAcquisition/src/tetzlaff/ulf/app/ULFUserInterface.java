package tetzlaff.ulf.app;

import java.awt.Dimension;
import java.io.IOException;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import tetzlaff.ulf.ULFDrawable;
import tetzlaff.ulf.ULFListModel;
import tetzlaff.ulf.ULFLoadingMonitor;
import tetzlaff.ulf.ULFMorphRenderer;

public class ULFUserInterface
{
	private JFrame frame;
	private JComboBox<ULFDrawable> selector;
	
	public ULFUserInterface(ULFListModel model) 
	{
		this.frame = new JFrame("Light Field Config");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setLocation(10, 10);
		frame.setSize(256, 256);
		frame.setResizable(false);
		
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		
		this.selector = new JComboBox<ULFDrawable>();
		selector.setModel(model);
		selector.setBorder(new EmptyBorder(10, 10, 10, 10));
		frame.add(selector);
		
		JSlider morphSlider = new JSlider();
		morphSlider.setMaximum(0);
		morphSlider.setValue(0);
		morphSlider.setEnabled(false);
		morphSlider.setBorder(new EmptyBorder(0, 10, 0, 10));
		frame.add(morphSlider);
		
		Box loadBox = new Box(BoxLayout.X_AXIS);
		JPanel loadSingleWrapper = new JPanel();
		JButton loadSingleButton = new JButton("Load Single...");
		loadSingleWrapper.add(loadSingleButton);
		loadSingleWrapper.setBorder(new EmptyBorder(0, 10, 0, 10));
		loadBox.add(loadSingleWrapper);
		JPanel loadMorphWrapper = new JPanel();
		JButton loadMorphButton = new JButton("Load Morph...");
		loadMorphWrapper.add(loadMorphButton);
		loadMorphWrapper.setBorder(new EmptyBorder(0, 10, 0, 10));
		loadBox.add(loadMorphWrapper);
		frame.add(loadBox);
		
		Box gammaBox = new Box(BoxLayout.X_AXIS);
		JLabel gammaLabel = new JLabel("Gamma:");
		gammaLabel.setPreferredSize(new Dimension(128, 16));
		gammaBox.add(gammaLabel);
		SpinnerNumberModel gammaModel = new SpinnerNumberModel(2.2f, 1.0f, 100.0f, 0.1f);
		JSpinner gammaSpinner = new JSpinner(gammaModel);
		gammaBox.add(gammaSpinner);
		gammaBox.setBorder(new EmptyBorder(10, 10, 10, 10));
		frame.add(gammaBox);
		
		Box weightExpBox = new Box(BoxLayout.X_AXIS);
		JLabel weightExpLabel = new JLabel("Weight Exponent:");
		weightExpLabel.setPreferredSize(new Dimension(128, 16));
		weightExpBox.add(weightExpLabel);
		SpinnerNumberModel weightExpModel = new SpinnerNumberModel(16.0f, 1.0f, 1000.0f, 1.0f);
		JSpinner weightExpSpinner = new JSpinner(weightExpModel);
		weightExpBox.add(weightExpSpinner);
		weightExpBox.setBorder(new EmptyBorder(0, 10, 10, 10));
		frame.add(weightExpBox);
		
		JCheckBox occlusionCheckBox = new JCheckBox("Enable Occlusion", true);
		occlusionCheckBox.setBorder(new EmptyBorder(10, 10, 10, 10));
		frame.add(occlusionCheckBox);
		
		Box occlusionBiasBox = new Box(BoxLayout.X_AXIS);
		JLabel occlusionBiasLabel = new JLabel("Occlusion Bias:");
		occlusionBiasLabel.setPreferredSize(new Dimension(128, 16));
		occlusionBiasBox.add(occlusionBiasLabel);
		SpinnerNumberModel occlusionBiasModel = new SpinnerNumberModel(0.0025f, 0.0f, 1.0f, 0.0001f);
		JSpinner occlusionBiasSpinner = new JSpinner(occlusionBiasModel);
		JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(occlusionBiasSpinner, "0.0000");
		occlusionBiasSpinner.setEditor(numberEditor);
		occlusionBiasBox.add(occlusionBiasSpinner);
		occlusionBiasBox.setBorder(new EmptyBorder(10, 10, 10, 10));
		frame.add(occlusionBiasBox);
		
		Box resampleBox = new Box(BoxLayout.X_AXIS);
		JPanel resampleWrapper = new JPanel();
		JButton resampleButton = new JButton("Resample...");
		resampleWrapper.add(resampleButton);
		resampleWrapper.setBorder(new EmptyBorder(0, 10, 0, 10));
		resampleBox.add(resampleWrapper);
		JPanel resampleSizeWrapper = new JPanel();
		JLabel resampleSizeLabel = new JLabel("Size:");
		resampleSizeWrapper.add(resampleSizeLabel);
		SpinnerNumberModel resampleSizeModel = new SpinnerNumberModel(1024.0f, 1.0f, 8192.0f, 1.0f);
		JSpinner resampleSizeSpinner = new JSpinner(resampleSizeModel);
		resampleSizeWrapper.add(resampleSizeSpinner);
		resampleSizeWrapper.setBorder(new EmptyBorder(5, 10, 0, 10));
		resampleBox.add(resampleSizeWrapper);
		resampleBox.setBorder(new EmptyBorder(0, 10, 10, 10));
		frame.add(resampleBox);
		
		frame.pack();
		
		JFrame loadingFrame = new JFrame("Loading...");
		loadingFrame.setUndecorated(true);
		JProgressBar loadingBar = new JProgressBar();
		loadingBar.setIndeterminate(true);
		loadingFrame.add(loadingBar);
		loadingFrame.pack();
		loadingFrame.setLocationRelativeTo(null);
		
		if (model.getSelectedItem() == null)
		{
			gammaSpinner.setEnabled(false);
			weightExpSpinner.setEnabled(false);
			occlusionCheckBox.setEnabled(false);
			occlusionBiasSpinner.setEnabled(false);
		}
		else
		{
			gammaSpinner.setEnabled(true);
			weightExpSpinner.setEnabled(true);
			occlusionCheckBox.setEnabled(true);
			occlusionBiasSpinner.setEnabled(true);
			
			gammaSpinner.setValue(model.getSelectedItem().getGamma());
			weightExpSpinner.setValue(model.getSelectedItem().getWeightExponent());
			occlusionCheckBox.setSelected(model.getSelectedItem().isOcclusionEnabled());
			occlusionBiasSpinner.setValue(model.getSelectedItem().getOcclusionBias());
		}
		
		selector.addItemListener(e ->
		{
			if (model.getSelectedItem() == null)
			{
				gammaSpinner.setEnabled(false);
				weightExpSpinner.setEnabled(false);
				occlusionCheckBox.setEnabled(false);
				occlusionBiasSpinner.setEnabled(false);
				morphSlider.setEnabled(false);
			}
			else
			{
				gammaSpinner.setEnabled(true);
				weightExpSpinner.setEnabled(true);
				occlusionCheckBox.setEnabled(true);
				occlusionBiasSpinner.setEnabled(true);
				
				gammaSpinner.setValue(model.getSelectedItem().getGamma());
				weightExpSpinner.setValue(model.getSelectedItem().getWeightExponent());
				occlusionCheckBox.setSelected(model.getSelectedItem().isOcclusionEnabled());
				occlusionBiasSpinner.setValue(model.getSelectedItem().getOcclusionBias());
				
				if (model.getSelectedItem() instanceof ULFMorphRenderer)
				{
					ULFMorphRenderer morph = (ULFMorphRenderer)(model.getSelectedItem());
					int currentStage = morph.getCurrentStage();
					morphSlider.setEnabled(true);
					morphSlider.setMaximum(morph.getStageCount() - 1);
					morphSlider.setValue(currentStage);
				}
				else
				{
					morphSlider.setMaximum(0);
					morphSlider.setValue(0);
					morphSlider.setEnabled(false);
				}
			}
		});
		
		loadSingleButton.addActionListener(e -> 
		{
			JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
			fileChooser.setDialogTitle("Select a camera definition file");
			fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
			fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Agisoft Photoscan XML files", "xml"));
			fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("View Set files", "vset"));
			fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Zip files", "zip"));
			if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
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
					if (file.getName().toUpperCase().endsWith(".XML"))
					{
						JFileChooser meshChooser = new JFileChooser(file.getParentFile());
						meshChooser.setDialogTitle("Select the corresponding mesh");
						meshChooser.removeChoosableFileFilter(meshChooser.getAcceptAllFileFilter());
						meshChooser.addChoosableFileFilter(new FileNameExtensionFilter("Wavefront OBJ files", "obj"));
						
						if (meshChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
						{
							JFileChooser imageChooser = new JFileChooser(file.getParentFile());
							imageChooser.setDialogTitle("Select the undistorted image directory");
							imageChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
							
							if (imageChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
							{
								model.addFromAgisoftXMLFile(file, meshChooser.getSelectedFile(), imageChooser.getSelectedFile());
							}
						}
					}
					else
					{
						model.addFromVSETFile(file);
						loadingBar.setIndeterminate(true);
						loadingFrame.setVisible(true);
					}
				} 
				catch (IOException ex) 
				{
					ex.printStackTrace();
				}
			}
		});
		
		loadMorphButton.addActionListener(e -> 
		{
			JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
			fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
			fileChooser.setFileFilter(new FileNameExtensionFilter("Light Field Morph files (.lfm)", "lfm"));
			if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
			{
				try 
				{
					model.addMorphFromLFMFile(fileChooser.getSelectedFile());
					loadingBar.setIndeterminate(true);
					loadingFrame.setVisible(true);
				} 
				catch (IOException ex) 
				{
					ex.printStackTrace();
				}
			}
		});
		
		resampleButton.addActionListener(e -> 
		{
			JFileChooser vsetFileChooser = new JFileChooser(new File("").getAbsolutePath());
			vsetFileChooser.setDialogTitle("Choose a Target VSET File");
			vsetFileChooser.removeChoosableFileFilter(vsetFileChooser.getAcceptAllFileFilter());
			vsetFileChooser.setFileFilter(new FileNameExtensionFilter("View Set files (.vset)", "vset"));
			if (vsetFileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
			{
				JFileChooser exportFileChooser = new JFileChooser(vsetFileChooser.getSelectedFile().getParentFile());
				exportFileChooser.setDialogTitle("Choose an Export Directory");
				exportFileChooser.removeChoosableFileFilter(exportFileChooser.getAcceptAllFileFilter());
				exportFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
				if (exportFileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
				{
					try 
					{
						loadingBar.setIndeterminate(true);
						loadingFrame.setVisible(true);
						model.getSelectedItem().requestResample(
							(int)Math.round((Double)resampleSizeSpinner.getValue()), 
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
		
		gammaSpinner.addChangeListener(e ->
		{
			model.getSelectedItem().setGamma(gammaModel.getNumber().floatValue());
		});
		
		weightExpSpinner.addChangeListener(e ->
		{
			model.getSelectedItem().setWeightExponent(weightExpModel.getNumber().floatValue());
		});
		
		occlusionCheckBox.addChangeListener(e ->
		{
			model.getSelectedItem().setOcclusionEnabled(occlusionCheckBox.isSelected());
		});
		
		occlusionBiasSpinner.addChangeListener(e ->
		{
			model.getSelectedItem().setOcclusionBias(occlusionBiasModel.getNumber().floatValue());
		});
		
		morphSlider.addChangeListener(e ->
		{
			if (model.getSelectedItem() instanceof ULFMorphRenderer)
			{
				((ULFMorphRenderer)(model.getSelectedItem())).setCurrentStage(morphSlider.getValue());
			}
		});
		
		model.setLoadingMonitor(new ULFLoadingMonitor()
		{
			@Override
			public void setProgress(double progress)
			{
				loadingBar.setIndeterminate(false);
				loadingBar.setValue((int)Math.round(progress * 100));
			}

			@Override
			public void loadingComplete()
			{
				loadingFrame.setVisible(false);
			}
		});
	}

	public void show()
	{
		frame.setVisible(true);
	}
}
