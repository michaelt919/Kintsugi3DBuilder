package tetzlaff.ibr.alexkautz_workspace.mount_olympus;

import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.ibr.IBRRenderableListModel;
import tetzlaff.ibr.util.IBRRequestQueue;
import tetzlaff.gl.Context;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import tetzlaff.gl.Context;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibr.IBRLoadOptions;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.IBRRenderableListModel;
import tetzlaff.ibr.LoadingMonitor;
import tetzlaff.ibr.util.BTFRequestUI;
import tetzlaff.ibr.util.FidelityMetricRequestUI;
import tetzlaff.ibr.util.IBRRequest;
import tetzlaff.ibr.util.ResampleRequestUI;
import tetzlaff.mvc.models.LightModel;

public class  PassedParameters {
    //local passedParameters

    private String name;

    private String filepath;

    private RenderPerams renderPerams;

    public RenderPerams getRenderPerams() {
        if(renderPerams == null) throw new IllegalStateException("RenderPerams got befor set!"); //KILLME
        return renderPerams;
    }

    public void setRenderPerams(RenderPerams renderPerams) {
        this.renderPerams = renderPerams;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    private PassedParameters(String filepath, String name) {
        this.filepath = filepath;
        this.name = name;
    }

    //static bridge

    private static PassedParameters passedParameters;

    public static void init(String filepath, String name){
        passedParameters = new PassedParameters(filepath, name);
    }

    public static PassedParameters get() {
        return passedParameters;
    }
}
