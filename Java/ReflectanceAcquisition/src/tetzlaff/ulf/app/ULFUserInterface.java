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
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import tetzlaff.ulf.ULFListModel;
import tetzlaff.ulf.UnstructuredLightField;

public class ULFUserInterface
{
	private JFrame frame;
	private JComboBox<UnstructuredLightField> selector;
	private ULFListModel model;
	
	public ULFUserInterface(ULFListModel model) 
	{
		this.model = model;
		
		this.frame = new JFrame("Light Field Config");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setLocation(10, 10);
		frame.setSize(256, 256);
		frame.setResizable(false);
		
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		
		this.selector = new JComboBox<UnstructuredLightField>();
		selector.setModel(model);
		selector.setBorder(new EmptyBorder(10, 10, 10, 10));
		frame.add(selector);
		
		JButton loadButton = new JButton("Load...");
		loadButton.setBorder(new EmptyBorder(10, 10, 10, 10));
		frame.add(loadButton);
		
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
		SpinnerNumberModel occlusionBiasModel = new SpinnerNumberModel(0.005f, 0.0f, 1.0f, 0.001f);
		JSpinner occlusionBiasSpinner = new JSpinner(occlusionBiasModel);
		occlusionBiasBox.add(occlusionBiasSpinner);
		occlusionBiasBox.setBorder(new EmptyBorder(10, 10, 10, 10));
		frame.add(occlusionBiasBox);
		
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
			
			gammaSpinner.setValue(model.getSelectedItem().settings.getGamma());
			weightExpSpinner.setValue(model.getSelectedItem().settings.getWeightExponent());
			occlusionCheckBox.setSelected(model.getSelectedItem().settings.isOcclusionEnabled());
			occlusionBiasSpinner.setValue(model.getSelectedItem().settings.getOcclusionBias());
		}
		
		selector.addItemListener(e ->
		{
			loadingFrame.setVisible(false);
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
				
				gammaSpinner.setValue(model.getSelectedItem().settings.getGamma());
				weightExpSpinner.setValue(model.getSelectedItem().settings.getWeightExponent());
				occlusionCheckBox.setSelected(model.getSelectedItem().settings.isOcclusionEnabled());
				occlusionBiasSpinner.setValue(model.getSelectedItem().settings.getOcclusionBias());
			}
		});
		
		loadButton.addActionListener(e -> 
		{
			JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
			fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
			fileChooser.setFileFilter(new FileNameExtensionFilter("View Set files (.vset)", "vset"));
			if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
			{
				try 
				{
					model.addFromDirectory(fileChooser.getSelectedFile().getParent());
					loadingFrame.setVisible(true);
				} 
				catch (IOException ex) 
				{
					ex.printStackTrace();
				}
			}
		});
		
		gammaSpinner.addChangeListener(e ->
		{
			model.getSelectedItem().settings.setGamma(gammaModel.getNumber().floatValue());
		});
		
		weightExpSpinner.addChangeListener(e ->
		{
			model.getSelectedItem().settings.setWeightExponent(weightExpModel.getNumber().floatValue());
		});
		
		occlusionCheckBox.addChangeListener(e ->
		{
			model.getSelectedItem().settings.setOcclusionEnabled(occlusionCheckBox.isSelected());
		});
		
		occlusionBiasSpinner.addChangeListener(e ->
		{
			model.getSelectedItem().settings.setOcclusionBias(occlusionBiasModel.getNumber().floatValue());
		});
	}
	
	public void addSelectedLightFieldListener(SelectedLightFieldListener l)
	{
		this.selector.addItemListener(e ->
		{
			l.lightFieldSelected(model.getSelectedItem());
		});
	}

	public void show()
	{
		frame.setVisible(true);
	}
}
