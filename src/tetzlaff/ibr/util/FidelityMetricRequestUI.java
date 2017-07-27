package tetzlaff.ibr.util;

import tetzlaff.ibr.IBRSettings;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class FidelityMetricRequestUI implements IBRRequestUI 
{
	private Component parent;
	private JFileChooser fileChooser;
	private IBRSettings settings;
	
	public FidelityMetricRequestUI(Component parent, JFileChooser fileChooser, IBRSettings settings) 
	{
		this.parent = parent;
		this.fileChooser = fileChooser;
		this.settings = settings;
	}

	@Override
	public FidelityMetricRequest prompt() 
	{
		fileChooser.setDialogTitle("Choose an Export Filename");
		fileChooser.resetChoosableFileFilters();
		fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setFileFilter(new FileNameExtensionFilter("Text files (.txt)", "txt"));
		
		if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			File fidelityOutputFile = fileChooser.getSelectedFile();
			
			fileChooser.setDialogTitle("Choose a Target VSET File");
			fileChooser.resetChoosableFileFilters();
			fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setFileFilter(new FileNameExtensionFilter("View Set files (.vset)", "vset"));
			
			if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
			{
				File targetVSETFile = fileChooser.getSelectedFile();
				
				return new FidelityMetricRequest(fidelityOutputFile, targetVSETFile, settings);
			}
		}
		
		return null;
	}

}
