package tetzlaff.ulf.app;

import com.trolltech.qt.core.Qt.WindowModality;
import com.trolltech.qt.gui.QDialog;
import com.trolltech.qt.gui.QWidget;

public class ULFAboutQDialog extends QDialog {

	Ui_AboutDialog gui;
	
	public ULFAboutQDialog(QWidget parent) {
		super(parent);
		
		gui = new Ui_AboutDialog();
		gui.setupUi(this);
		
		this.setWindowModality(WindowModality.ApplicationModal);
		this.setModal(true);
	}
}
