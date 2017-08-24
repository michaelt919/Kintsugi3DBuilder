package tetzlaff.ibr.app.old.util;

import java.awt.*;
import javax.swing.*;

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.app.old.IBRRequestUI;
import tetzlaff.ibr.core.ReadonlySettingsModel;
import tetzlaff.ibr.export.btf.BTFRequest;

public class BTFRequestUI implements IBRRequestUI
{
    private final Component parent;
    private final JFileChooser fileChooser;
    private final JSpinner spinnerWidth;
    private final JSpinner spinnerHeight;
    private final ReadonlySettingsModel settings;
    private final Vector3 lightColor;

    public BTFRequestUI(Component parent, JFileChooser fileChooser, JSpinner spinnerWidth, JSpinner spinnerHeight, ReadonlySettingsModel settings, Vector3 lightColor)
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
