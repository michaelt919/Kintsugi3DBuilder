/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
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
    public QVBoxLayout verticalLayout_2;
    public QLabel iconLabel;
    public QSpacerItem verticalSpacer;
    public QLabel textLabel;
    public QDialogButtonBox buttonBox;

    public Ui_AboutDialog() { super(); }

    public void setupUi(QDialog AboutDialog)
    {
        AboutDialog.setObjectName("AboutDialog");
        AboutDialog.resize(new QSize(500, 320).expandedTo(AboutDialog.minimumSizeHint()));
        AboutDialog.setMinimumSize(new QSize(500, 320));
        AboutDialog.setMaximumSize(new QSize(500, 320));
        verticalLayout = new QVBoxLayout(AboutDialog);
        verticalLayout.setObjectName("verticalLayout");
        horizontalLayout = new QHBoxLayout();
        horizontalLayout.setSpacing(10);
        horizontalLayout.setObjectName("horizontalLayout");
        verticalLayout_2 = new QVBoxLayout();
        verticalLayout_2.setObjectName("verticalLayout_2");
        verticalLayout_2.setContentsMargins(0, -1, -1, -1);
        iconLabel = new QLabel(AboutDialog);
        iconLabel.setObjectName("iconLabel");
        QSizePolicy sizePolicy = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Fixed, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy.setHorizontalStretch((byte)0);
        sizePolicy.setVerticalStretch((byte)0);
        sizePolicy.setHeightForWidth(iconLabel.sizePolicy().hasHeightForWidth());
        iconLabel.setSizePolicy(sizePolicy);
        iconLabel.setMinimumSize(new QSize(120, 120));
        iconLabel.setMaximumSize(new QSize(120, 120));
        iconLabel.setPixmap(new QPixmap(("../icons/icon.png")));
        iconLabel.setScaledContents(true);
        iconLabel.setAlignment(com.trolltech.qt.core.Qt.AlignmentFlag.createQFlags(com.trolltech.qt.core.Qt.AlignmentFlag.AlignLeft,com.trolltech.qt.core.Qt.AlignmentFlag.AlignVCenter));

        verticalLayout_2.addWidget(iconLabel);

        verticalSpacer = new QSpacerItem(20, 40, com.trolltech.qt.gui.QSizePolicy.Policy.Minimum, com.trolltech.qt.gui.QSizePolicy.Policy.Expanding);

        verticalLayout_2.addItem(verticalSpacer);


        horizontalLayout.addLayout(verticalLayout_2);

        textLabel = new QLabel(AboutDialog);
        textLabel.setObjectName("textLabel");
        QSizePolicy sizePolicy1 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Preferred);
        sizePolicy1.setHorizontalStretch((byte)0);
        sizePolicy1.setVerticalStretch((byte)0);
        sizePolicy1.setHeightForWidth(textLabel.sizePolicy().hasHeightForWidth());
        textLabel.setSizePolicy(sizePolicy1);
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
"<p>Licensed under GPLv3 (<a href=\"http://www.gnu.org/licenses/gpl-3.0.html\">license text</a>)</p>\n"+
"<p>Requests for source code, comments,<br/>or bug reports should be sent to:<br/>\n"+
"<a href=\"mailto:ulfrenderer@gmail.com\">ulfrenderer@gmail.com</a></p>\n"+
"</body></html>", null));
    } // retranslateUi

}

