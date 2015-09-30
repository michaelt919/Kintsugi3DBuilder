/********************************************************************************
 ** Form generated from reading ui file 'ULFRendererAbout.ui.jui'
 **
 ** Created by: Qt User Interface Compiler version 4.8.6
 **
 ** WARNING! All changes made in this file will be lost when recompiling ui file!
 ********************************************************************************/
package tetzlaff.ulf.app;

import com.trolltech.qt.core.*;
import com.trolltech.qt.gui.*;

public class Ui_AboutDialog implements com.trolltech.qt.QUiForm<QDialog>
{
    public QVBoxLayout verticalLayout;
    public QHBoxLayout horizontalLayout;
    public QLabel iconLabel;
    public QLabel textLabel;
    public QDialogButtonBox buttonBox;

    public Ui_AboutDialog() { super(); }

    public void setupUi(QDialog AboutDialog)
    {
        AboutDialog.setObjectName("AboutDialog");
        AboutDialog.resize(new QSize(449, 270).expandedTo(AboutDialog.minimumSizeHint()));
        AboutDialog.setMinimumSize(new QSize(449, 270));
        AboutDialog.setMaximumSize(new QSize(449, 270));
        verticalLayout = new QVBoxLayout(AboutDialog);
        verticalLayout.setObjectName("verticalLayout");
        horizontalLayout = new QHBoxLayout();
        horizontalLayout.setSpacing(10);
        horizontalLayout.setObjectName("horizontalLayout");
        iconLabel = new QLabel(AboutDialog);
        iconLabel.setObjectName("iconLabel");

        horizontalLayout.addWidget(iconLabel);

        textLabel = new QLabel(AboutDialog);
        textLabel.setObjectName("textLabel");
        textLabel.setWordWrap(true);

        horizontalLayout.addWidget(textLabel);


        verticalLayout.addLayout(horizontalLayout);

        buttonBox = new QDialogButtonBox(AboutDialog);
        buttonBox.setObjectName("buttonBox");
        buttonBox.setOrientation(com.trolltech.qt.core.Qt.Orientation.Horizontal);
        buttonBox.setStandardButtons(com.trolltech.qt.gui.QDialogButtonBox.StandardButton.createQFlags(com.trolltech.qt.gui.QDialogButtonBox.StandardButton.Close));

        verticalLayout.addWidget(buttonBox);

        retranslateUi(AboutDialog);
        buttonBox.accepted.connect(AboutDialog, "accept()");
        buttonBox.rejected.connect(AboutDialog, "reject()");

        AboutDialog.connectSlotsByName();
    } // setupUi

    void retranslateUi(QDialog AboutDialog)
    {
        AboutDialog.setWindowTitle(com.trolltech.qt.core.QCoreApplication.translate("AboutDialog", "About ULF Renderer v1.0", null));
        iconLabel.setText("");
        textLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("AboutDialog", "<html><body>\n"+
"<h2>ULF Renderer</h2>\n"+
"<h3>Version: 1.0 (alpha 2)</h3>\n"+
"\n"+
"<p>Originally written by:<ul>\n"+
"<li>Michael Tetzlaff (University of Minnesota)</li>\n"+
"<li>Seth Berrier (University of Wisconsin Stout)</li>\n"+
"<li>Michael Ludwig (University of Minnesota)</li>\n"+
"</p>\n"+
"\n"+
"<p>Licensed under GPLv3<br/>\n"+
"Requests for source code, comments,<br/>or bug reports should be sent to:<br/>\n"+
"<a href=\"mailto:ulfrenderer@gmail.com\">ulfrenderer@gmail.com</a></p>\n"+
"</body></html>", null));
    } // retranslateUi

}

