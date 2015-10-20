package tetzlaff.ulf.app;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.helpers.Vector4;
import tetzlaff.interactive.EventPollable;
import tetzlaff.ulf.ULFDrawable;
import tetzlaff.ulf.ULFListModel;
import tetzlaff.ulf.ULFLoadOptions;
import tetzlaff.ulf.ULFLoadingMonitor;
import tetzlaff.ulf.ULFMorphRenderer;
import tetzlaff.ulf.ViewSetImageOptions;
import tetzlaff.window.WindowPosition;
import tetzlaff.window.WindowSize;

import com.trolltech.qt.core.QCoreApplication;
import com.trolltech.qt.core.QDir;
import com.trolltech.qt.core.QSize;
import com.trolltech.qt.core.QUrl;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.WindowModality;
import com.trolltech.qt.gui.QCloseEvent;
import com.trolltech.qt.gui.QColor;
import com.trolltech.qt.gui.QColorDialog;
import com.trolltech.qt.gui.QDesktopServices;
import com.trolltech.qt.gui.QDialog;
import com.trolltech.qt.gui.QFileDialog;
import com.trolltech.qt.gui.QPixmap;
import com.trolltech.qt.gui.QFileDialog.Filter;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QMessageBox;
import com.trolltech.qt.gui.QProgressDialog;
import com.trolltech.qt.gui.QSplitter;
import com.trolltech.qt.gui.QWidget;
import com.trolltech.qt.help.QHelpEngine;
import com.trolltech.qt.help.QHelpEngineCore;
import com.trolltech.qt.webkit.QWebPage;
import com.trolltech.qt.webkit.QWebSettings;
import com.trolltech.qt.webkit.QWebView;

public class ULFConfigQWidget extends QMainWindow implements EventPollable {

	private Ui_ULFRendererMainWindowToolbox gui;
	private final ULFListModel model;
	private boolean widgetClosed;
	private boolean halfResDefault;
	private boolean blockSignals;
	private QProgressDialog progressDialog;
	
	private String helpFilename;
	private QHelpEngine helpEngine;
	private QWidget helpWindow;
	private QWebView helpViewer;
	private QUrl helpUrlBase;
	
	private String baseDir;
	private String baseScheme;
	
	public Signal0 loadingFinished;
	
	public ULFConfigQWidget(ULFListModel model, boolean isHighDPI, QWidget parent) {
		super(parent);
		this.blockSignals = true;

		this.loadingFinished = new Signal0();
		loadingFinished.connect(this, "onModelChanged()");
		
		this.halfResDefault = isHighDPI;
		this.widgetClosed = false;
		this.model = model;
		
		gui = new Ui_ULFRendererMainWindowToolbox();
		gui.setupUi(this);
		
		if(this.model != null && this.model.getSelectedItem() != null) {
			this.model.getSelectedItem().setHalfResolution(isHighDPI);
		}
		
		// Setup the help engine
		baseDir = QDir.currentPath();
		helpFilename = baseDir + "/resources/help/userHelp.qhc";
		System.out.println("Looking for help as: " + helpFilename);
		helpEngine = new QHelpEngine(helpFilename);
	    if(!helpEngine.setupData()) {
	    	System.err.println("There was an error loading the help data.");
	    }
	    
	    System.out.println("Namespace is: " + QHelpEngineCore.namespaceName(helpFilename));
	    
	    if(ULFProgram.OS_IS_WINDOWS) { baseScheme = "file:/"; }
	    else { baseScheme = ""; }
	    
	    helpUrlBase = new QUrl(baseDir + "/resources/help/");
	    QWebSettings.globalSettings().setAttribute(QWebSettings.WebAttribute.DeveloperExtrasEnabled, true);
	    
	    QWebPage delegatedPage = new QWebPage();
	    delegatedPage.setLinkDelegationPolicy(QWebPage.LinkDelegationPolicy.DelegateAllLinks);
	    helpViewer = new QWebView(this);
	    helpViewer.setPage(delegatedPage);
	    loadUrlForHelp(new QUrl(baseScheme + helpUrlBase + "index.md.html"));
	    
	    helpEngine.contentWidget().linkActivated.connect(this, "loadUrlForHelp(QUrl)");
	    helpViewer.linkClicked.connect(this, "loadUrlForHelp(QUrl)");
		    
	    QSplitter horizSplitter = new QSplitter(Qt.Orientation.Horizontal);
	    QWidget content = helpEngine.contentWidget();
	    content.setMaximumWidth(200);
	    horizSplitter.insertWidget(0, content);
	    horizSplitter.insertWidget(1, helpViewer);
	    
	    helpWindow = horizSplitter;
	    helpWindow.setWindowTitle("ULF Renderer User Guide");
	    helpWindow.setMinimumSize(640, 480);
	    
		// Setup the loading progress dialog
		progressDialog = new QProgressDialog("Loading Model", "Cancel", 0, 0, this);
		progressDialog.setWindowModality(WindowModality.ApplicationModal);
		progressDialog.setModal(true);
		progressDialog.setHidden(true);
		progressDialog.setCancelButton(null);
		progressDialog.setAutoReset(false);
		progressDialog.setAutoClose(false);
		progressDialog.setMinimumDuration(0);
		
		gui.halfResCheckBox.setChecked(halfResDefault);

		this.model.setLoadingMonitor(new ULFLoadingMonitor() {

			@Override
			public void startLoading()
			{
				// Make dialog visible and set position
				initAndPositionProgressDialog();
			}

			@Override
			public void setMaximum(double maximum)
			{				
				// Set maximum
				progressDialog.setMaximum((int)Math.round(maximum));
			}
			
			@Override
			public void setProgress(double progress)
			{
				// Update the progress
				progressDialog.setValue((int)Math.round(progress));
			}

			@Override
			public void loadingComplete()
			{
				progressDialog.setHidden(true);
				model.getSelectedItem().setHalfResolution(halfResDefault);
				loadingFinished.emit();
			}
		});
		this.blockSignals = false;
		onModelChanged();
	}
	
	// Set values from the 'model' parameter
	public void onModelChanged()
	{
		if(blockSignals) { return; }
		blockSignals = true;
		boolean enable = (model != null && model.getSelectedItem() != null && model.getSelectedItem().toString().compareToIgnoreCase("Error") != 0);
		
		gui.modelSlider.setEnabled(enable);
		gui.kNearestNeighborsCheckBox.setEnabled(enable);
		gui.kNeighborCountLabel.setEnabled(enable);
		gui.kNeighborCountSpinBox.setEnabled(enable);
		gui.backgroundColorLabel.setEnabled(enable);
		gui.backgroundColorButton.setEnabled(enable);
		gui.gammaLabel.setEnabled(enable);
		gui.gammaSpinBox.setEnabled(enable);
		gui.exponentLabel.setEnabled(enable);
		gui.exponentSpinBox.setEnabled(enable);
		gui.visibilityCheckBox.setEnabled(enable);
		gui.visibilityBiasLabel.setEnabled(enable);
		gui.visibilityBiasSpinBox.setEnabled(enable);
		
		gui.showCamerasCheckBox.setEnabled(enable);
		gui.halfResCheckBox.setEnabled(enable);
		gui.multisamplingCheckBox.setEnabled(enable);

		gui.resampleDimensionsLabel.setEnabled(enable);
		gui.resampleWidthSpinner.setEnabled(enable);
		gui.resampleXLabel.setEnabled(enable);
		gui.resampleHeightSpinner.setEnabled(enable);
		
		gui.resampleButton.setEnabled(enable);
		
		if(model != null && model.getSelectedItem() != null)
		{
			gui.modelComboBox.clear();
			gui.modelComboBox.setEnabled(model.getSize()>1);
			String selected = model.getSelectedItem().toString();
			for(int i=0; i<model.getSize(); i++) {
				String nextItem = model.getElementAt(i).toString();
				gui.modelComboBox.addItem(nextItem);
				if(nextItem.equals(selected))
				{
					gui.modelComboBox.setCurrentIndex(i);
				}
			}
			
			if(selected.compareToIgnoreCase("Error") != 0)
			{
				gui.gammaSpinBox.setValue(model.getSelectedItem().getGamma());
				gui.exponentSpinBox.setValue(model.getSelectedItem().getWeightExponent());
				gui.visibilityCheckBox.setChecked(model.getSelectedItem().isOcclusionEnabled());
				gui.visibilityBiasSpinBox.setValue(model.getSelectedItem().getOcclusionBias());
				gui.kNearestNeighborsCheckBox.setChecked(model.getSelectedItem().isKNeighborsEnabled());
				gui.kNeighborCountSpinBox.setValue(model.getSelectedItem().getKNeighborCount());
				
				model.getSelectedItem().setHalfResolution(gui.halfResCheckBox.isChecked());
				model.getSelectedItem().setVisualizeCameras(gui.showCamerasCheckBox.isChecked());

				if (model.getSelectedItem() instanceof ULFMorphRenderer<?>)
				{
					ULFMorphRenderer<?> morph = (ULFMorphRenderer<?>)(model.getSelectedItem());
					int currentStage = morph.getCurrentStage();
					gui.modelSlider.setEnabled(true);
					gui.modelSlider.setMaximum(morph.getStageCount() - 1);
					gui.modelSlider.setValue(currentStage);
				}
				else
				{
					gui.modelSlider.setMaximum(0);
					gui.modelSlider.setValue(0);
					gui.modelSlider.setEnabled(false);
				}
			}
		}
		
		blockSignals = false;
	}
	
	public ULFLoadOptions getLoadOptionsFromGui()
	{
		ViewSetImageOptions vsetOpts = 
				new ViewSetImageOptions(null, true,
						gui.mipmapsCheckbox.isChecked(),
						gui.compressCheckBox.isChecked());
		ULFLoadOptions loadOptions = new ULFLoadOptions(vsetOpts,
				gui.generateDepthImagesCheckBox.isChecked(),
				gui.depthImageWidthSpinner.value(),
				gui.depthImageHeightSpinner.value());
		
		return loadOptions;
	}
	
	private void initAndPositionProgressDialog()
	{
		// Initialize/reset dialog
		progressDialog.setMaximum(0);
		progressDialog.setValue(0);
		
		// Make dialog visible and set position
		QSize dlgSize = progressDialog.size();
		WindowSize winSize = ULFProgram.getRenderingWindowSize();
		WindowPosition winPos = ULFProgram.getRendringWindowPosition();
		progressDialog.move(winPos.x + winSize.width/2 - dlgSize.width()/2,
							winPos.y + winSize.height/2 - dlgSize.height()/2);
		progressDialog.setHidden(false);
	}
	
	@SuppressWarnings("unused")
	private void on_backgroundColorButton_clicked()
	{
		ULFDrawable current = model.getSelectedItem();
		if(current != null && current.toString().compareToIgnoreCase("Error") != 0)
		{
			Vector4 curColor = current.getBackgroundColor();
			QColor curQColor = QColor.fromRgbF(curColor.x, curColor.y, curColor.z, curColor.w);
			QColor newQColor = QColorDialog.getColor(curQColor, this, "Select a Background Color");
			if(newQColor != null && newQColor.isValid())
			{
				current.setBackgroundColor(new Vector4((float)newQColor.redF(),
						(float)newQColor.greenF(), (float)newQColor.blueF(), 1.0f));
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void on_reportBugButton_clicked()
	{
		int result = QMessageBox.question(this, "Create Bug Report?",
				"Would you like to send a manually created bug report?\n\n"+
				"This will create a crash report with useful, anonmous information " +
				"about your system.  Please be sure to enter a detailed description.",
				QMessageBox.StandardButton.Yes, QMessageBox.StandardButton.No);
		
		if(result == QMessageBox.StandardButton.No.value()) return;
		
		try {
			ULFProgram.generateBugReport();
		} catch (IOException e) {}
	}
	
	// Add listener for the 'single' load button to read a single light field object.
	@SuppressWarnings("unused")
	private void on_actionLoad_Single_Model_triggered()
	{
		if(blockSignals) { return; }
		File lastDir = ULFProgram.getLastCamDefFileDirectory();
		String camDefFilename = QFileDialog.getOpenFileName(this,
									"Select a camera definition file", lastDir.getAbsolutePath(),
									new Filter("Agisoft Photoscan XML (*.xml);;" +
											   "View Set Files (*.vset);;" +
											   "Zip Archive (*.zip)"));
		if(camDefFilename.isEmpty()) return;

		try
		{
			// Update last selected cam def file directory
			lastDir = new File(camDefFilename); lastDir.getParentFile();
			ULFProgram.setLastCamDefFileDirectory(lastDir);
			
			if(camDefFilename.toUpperCase().endsWith(".ZIP"))
			{
				camDefFilename = camDefFilename.substring(0, camDefFilename.length()-4);
				camDefFilename += "/default.vset";
				System.out.printf("Zip file name converted to '%s'\n", camDefFilename);
			}
			
			ULFLoadOptions loadOptions = getLoadOptionsFromGui();
			
			if (camDefFilename.toUpperCase().endsWith(".XML"))
			{
				String meshFilename = QFileDialog.getOpenFileName(this,
						"Select the corresponding mesh", lastDir.getAbsolutePath(),
						new Filter("Wavefront OBJ (*.obj)"));
				if(meshFilename.isEmpty()) return;
				
				lastDir = new File(meshFilename); lastDir.getParentFile();
				String undistImageDir = QFileDialog.getExistingDirectory(this,
						"Select the undistorted image directory", lastDir.getAbsolutePath());
				if(undistImageDir.isEmpty()) return;
				
				loadOptions.getImageOptions().setFilePath(new File(undistImageDir));
				model.addFromAgisoftXMLFile(new File(camDefFilename), new File(meshFilename), loadOptions);
			}
			else
			{
				model.addFromVSETFile(new File(camDefFilename), loadOptions);
			}
		}
		catch (IOException ex) 
		{
			System.err.println("Error while loading model.");
			ex.printStackTrace();
		}
	}
	
	@SuppressWarnings("unused")
	private void on_actionQuit_triggered()
	{
		this.close();
	}
	
	// Add listener for the 'morph' load button to read many light field objects.
	@SuppressWarnings("unused")
	private void on_actionLoad_Model_Sequence_triggered()
	{
		if(blockSignals) { return; }
		File lastDir = ULFProgram.getLastSequenceFileDirectory();		
		String sequenceFilename = QFileDialog.getOpenFileName(this,
				"Select a sequence file", lastDir.getAbsolutePath(),
				new Filter("light field sequence file (*.lfm)"));
		if(sequenceFilename.isEmpty()) return;
	
		lastDir = new File(sequenceFilename);
		ULFProgram.setLastSequenceFileDirectory(lastDir);
		
		try 
		{
			model.addMorphFromLFMFile(new File(sequenceFilename), getLoadOptionsFromGui());
			initAndPositionProgressDialog();
		} 
		catch (IOException ex) 
		{
			ex.printStackTrace();
		}
	}
	
	@SuppressWarnings("unused")
	private void on_actionAbout_ULF_Renderer_triggered()
	{
		// Make the about dialog GUI
		QDialog aboutDiag = new QDialog(this);
		Ui_AboutDialog aboutGui = new Ui_AboutDialog();
		aboutGui.setupUi(aboutDiag);
		aboutGui.iconLabel.setPixmap(new QPixmap(baseDir + "/resources/icons/icon.png"));
		
		aboutDiag.setWindowModality(WindowModality.ApplicationModal);
		aboutDiag.setModal(true);

		// Center over rendering window
		WindowSize winSize = ULFProgram.getRenderingWindowSize();
		WindowPosition winPos = ULFProgram.getRendringWindowPosition();
		aboutDiag.move(winPos.x + winSize.width/2 - aboutDiag.width()/2,
					   winPos.y + winSize.height/2 - aboutDiag.height()/2);

		aboutGui.textLabel.linkActivated.connect(this, "launchExternalUrl(String)");
		
		// Show it
		aboutDiag.exec();
	}
	
	@SuppressWarnings("unused")
	private void on_actionShow_User_Guide_triggered()
	{	
		helpWindow.move(100, 100);
	    helpWindow.resize(new QSize(1024, 600));
		helpWindow.setVisible(true);
	}

	@SuppressWarnings("unused")
	private void launchExternalUrl(String url)
	{
		QMessageBox.StandardButton answer = QMessageBox.question(this, "Open Link?",
				"This link will open in an external applicaiton.");
		
		if(answer == QMessageBox.StandardButton.Ok)
		{
			QDesktopServices.openUrl(new QUrl(url));
		}
	}
	
	private void loadUrlForHelp(QUrl url)
	{		
		System.out.println("Loading URL: " + url.toString());
		if(url.scheme().equalsIgnoreCase("qthelp"))
		{
			// Replace the qthelp URL with a normal URL
			String newURL = url.toString();
			newURL = newURL.replace("qthelp://culturalheritageimaging.com.ulfrenderer/help/",
									baseScheme + helpUrlBase.toString());
			helpViewer.setUrl(new QUrl(newURL));
		}
		else
		{
			helpViewer.setUrl(url);
		}
	}
	
	// Respond to combo box item changed event
	@SuppressWarnings("unused")
	private void on_modelComboBox_currentIndexChanged(int newIndex)
	{
		if(blockSignals) { return; }
		model.setSelectedItem(model.getElementAt(newIndex));
		onModelChanged();
	}
	
	// Add listener for the 'resample' button to generate new vies for the current light field.
	@SuppressWarnings("unused")
	private void on_resampleButton_clicked()
	{
		if(blockSignals) { return; }
		String vsetFilename = QFileDialog.getOpenFileName(this,
				"Choose a Target VSET File", "", new Filter("View Set File (*.vset)"));
		if(vsetFilename.isEmpty()) return;
		
		String outputDir = QFileDialog.getOpenFileName(this,
				"Choose an output directory", vsetFilename);
		if(outputDir.isEmpty()) return;
			
		try 
		{
			initAndPositionProgressDialog();
			model.getSelectedItem().requestResample(
				gui.resampleWidthSpinner.value(), gui.resampleHeightSpinner.value(), 
				new File(vsetFilename), new File(outputDir));
		} 
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	@SuppressWarnings("unused")
	private void on_gammaSpinBox_valueChanged(double newValue)
	{
		if(blockSignals) { return; }
		model.getSelectedItem().setGamma((float)newValue);
	}
	
	@SuppressWarnings("unused")
	private void on_exponentSpinBox_valueChanged(double newValue)
	{
		if(blockSignals) { return; }
		model.getSelectedItem().setWeightExponent((float)newValue);
	}
	
	// Add listener for changes to camera visualization checkbox.
	@SuppressWarnings("unused")
	private void on_showCamerasCheckBox_toggled(boolean isChecked)
	{
		if(blockSignals) { return; }
		model.getSelectedItem().setVisualizeCameras(isChecked);
	}

	// Add listener for changes to half resolution checkbox.
	@SuppressWarnings("unused")
	private void on_halfResCheckBox_toggled(boolean isChecked)
	{
		if(blockSignals) { return; }
		model.getSelectedItem().setHalfResolution(isChecked);
	}

	// Add listener for changes to occlusion checkbox.
	@SuppressWarnings("unused")
	private void on_visibilityCheckBox_toggled(boolean isChecked)
	{
		if(blockSignals) { return; }
		model.getSelectedItem().setOcclusionEnabled(isChecked);
	}
	
	// Add listener for changes to the occlusion bias spinner.
	@SuppressWarnings("unused")
	private void on_visibilityBiasSpinBox_valueChanged(double newValue)
	{
		if(blockSignals) { return; }
		model.getSelectedItem().setOcclusionBias((float)newValue);
	}	
	
	@SuppressWarnings("unused")
	private void on_kNearestNeighborsCheckBox_toggled(boolean isChecked)
	{
		if(blockSignals) { return; }
		model.getSelectedItem().setKNeighborsEnabled(isChecked);
	}

	@SuppressWarnings("unused")
	private void on_kNeighborCountSpinBox_valueChanged(int newValue)
	{
		if(blockSignals) { return; }
		model.getSelectedItem().setKNeighborCount(newValue);
	}
	
	protected void closeEvent(QCloseEvent event)
	{
		event.accept();
		widgetClosed = true;
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

	@Override
	public void pollEvents() {
		if(QCoreApplication.hasPendingEvents()) {
			QCoreApplication.sendPostedEvents();
			QCoreApplication.processEvents();
		}
	}

	@Override
	public boolean shouldTerminate() {
		return widgetClosed;
	}
}
