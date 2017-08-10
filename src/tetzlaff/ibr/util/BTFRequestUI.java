package tetzlaff.ibr.util;

import java.awt.Component;

import javax.swing.JFileChooser;
import javax.swing.JSpinner;

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.rendering2.to_sort.IBRSettingsModel;

public class BTFRequestUI implements IBRRequestUI
{
	private Component parent;
	private JFileChooser fileChooser;
	private JSpinner spinnerWidth;
	private JSpinner spinnerHeight;
	private IBRSettingsModel settings;
	private Vector3 lightColor;
	
	public BTFRequestUI(Component parent, JFileChooser fileChooser, JSpinner spinnerWidth, JSpinner spinnerHeight, IBRSettingsModel settings, Vector3 lightColor)
	{
		this.parent = parent;
		this.fileChooser = fileChooser;
		this.spinnerWidth = spinnerWidth;
		this.spinnerHeight = spinnerHeight;
		this.settings = settings;
		this.lightColor = lightColor;
	}
	
	@Override
	public BTFRequest prompt() 
	{
		fileChooser.setDialogTitle("Choose an Export Directory");
		fileChooser.resetChoosableFileFilters();
		fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			return new BTFRequest(
				((Number)spinnerWidth.getValue()).intValue(),
					((Number)spinnerHeight.getValue()).intValue(),
					fileChooser.getSelectedFile(), settings, lightColor);
		}
		
		return null;
	}
}
