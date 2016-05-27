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
 ** Form generated from reading ui file 'ULFRendererQMainWindow.ui.jui'
 **
 ** Created by: Qt User Interface Compiler version 4.8.6
 **
 ** WARNING! All changes made in this file will be lost when recompiling ui file!
 ********************************************************************************/
package tetzlaff.ulf.app;

import com.trolltech.qt.core.*;
import com.trolltech.qt.gui.*;

public class Ui_ULFRendererMainWindow implements com.trolltech.qt.QUiForm<QMainWindow>
{
    public QAction actionLoad_single_model;
    public QAction actionLoad_model_sequence;
    public QAction actionQuit;
    public QAction actionAbout_ULF_Renderer;
    public QAction actionHelp;
    public QWidget centralwidget;
    public QVBoxLayout verticalLayout_6;
    public QGroupBox loadOptionsGroupBox;
    public QVBoxLayout verticalLayout_2;
    public QHBoxLayout horizontalLayout_3;
    public QCheckBox compressCheckBox;
    public QCheckBox mipmapsCheckbox;
    public QFrame frame;
    public QVBoxLayout verticalLayout;
    public QCheckBox generateDepthImagesCheckBox;
    public QHBoxLayout horizontalLayout;
    public QLabel depImageDimensionsLabel;
    public QSpinBox depthImageWidthSpinner;
    public QLabel depthImageXLabel;
    public QSpinBox depthImageHeightSpinner;
    public QGroupBox modelOptionsGroupBox;
    public QVBoxLayout verticalLayout_3;
    public QComboBox modelComboBox;
    public QSlider modelSlider;
    public QCheckBox showCamerasCheckBox;
    public QGroupBox renderingOptionsGroupBox;
    public QVBoxLayout verticalLayout_4;
    public QHBoxLayout horizontalLayout_9;
    public QCheckBox kNearestNeighborsCheckBox;
    public QLabel kNeighborCountLabel;
    public QSpinBox kNeighborCountSpinBox;
    public QHBoxLayout horizontalLayout_10;
    public QLabel backgroundColorLabel;
    public QToolButton backgroundColorButton;
    public QSpacerItem horizontalSpacer_3;
    public QFrame line_2;
    public QGridLayout gridLayout;
    public QLabel gammaLabel;
    public QDoubleSpinBox gammaSpinBox;
    public QLabel exponentLabel;
    public QDoubleSpinBox exponentSpinBox;
    public QFrame line;
    public QHBoxLayout horizontalLayout_4;
    public QFrame line_3;
    public QCheckBox visibilityCheckBox;
    public QLabel visibilityBiasLabel;
    public QDoubleSpinBox visibilityBiasSpinBox;
    public QGroupBox qualityOptionsGroupBox;
    public QHBoxLayout horizontalLayout_5;
    public QCheckBox halfResCheckBox;
    public QCheckBox multisamplingCheckBox;
    public QGroupBox resampleGroupBox;
    public QVBoxLayout verticalLayout_5;
    public QHBoxLayout horizontalLayout_6;
    public QLabel resampleDimensionsLabel;
    public QSpinBox resampleWidthSpinner;
    public QLabel resampleXLabel;
    public QSpinBox resampleHeightSpinner;
    public QHBoxLayout horizontalLayout_7;
    public QSpacerItem horizontalSpacer;
    public QPushButton resampleButton;
    public QHBoxLayout horizontalLayout_8;
    public QSpacerItem horizontalSpacer_2;
    public QToolButton reportBugButton;
    public QSpacerItem verticalSpacer;
    public QMenuBar menubar;
    public QMenu menuFile;
    public QMenu menuHelp;
    public QStatusBar statusbar;

    public Ui_ULFRendererMainWindow() { super(); }

    public void setupUi(QMainWindow ULFRendererMainWindow)
    {
        ULFRendererMainWindow.setObjectName("ULFRendererMainWindow");
        ULFRendererMainWindow.resize(new QSize(345, 780).expandedTo(ULFRendererMainWindow.minimumSizeHint()));
        ULFRendererMainWindow.setMinimumSize(new QSize(345, 780));
        ULFRendererMainWindow.setMaximumSize(new QSize(345, 780));
        actionLoad_single_model = new QAction(ULFRendererMainWindow);
        actionLoad_single_model.setObjectName("actionLoad_single_model");
        actionLoad_model_sequence = new QAction(ULFRendererMainWindow);
        actionLoad_model_sequence.setObjectName("actionLoad_model_sequence");
        actionQuit = new QAction(ULFRendererMainWindow);
        actionQuit.setObjectName("actionQuit");
        actionAbout_ULF_Renderer = new QAction(ULFRendererMainWindow);
        actionAbout_ULF_Renderer.setObjectName("actionAbout_ULF_Renderer");
        actionHelp = new QAction(ULFRendererMainWindow);
        actionHelp.setObjectName("actionHelp");
        centralwidget = new QWidget(ULFRendererMainWindow);
        centralwidget.setObjectName("centralwidget");
        verticalLayout_6 = new QVBoxLayout(centralwidget);
        verticalLayout_6.setObjectName("verticalLayout_6");
        loadOptionsGroupBox = new QGroupBox(centralwidget);
        loadOptionsGroupBox.setObjectName("loadOptionsGroupBox");
        verticalLayout_2 = new QVBoxLayout(loadOptionsGroupBox);
        verticalLayout_2.setSpacing(5);
        verticalLayout_2.setObjectName("verticalLayout_2");
        horizontalLayout_3 = new QHBoxLayout();
        horizontalLayout_3.setObjectName("horizontalLayout_3");
        compressCheckBox = new QCheckBox(loadOptionsGroupBox);
        compressCheckBox.setObjectName("compressCheckBox");
        compressCheckBox.setChecked(true);

        horizontalLayout_3.addWidget(compressCheckBox);

        mipmapsCheckbox = new QCheckBox(loadOptionsGroupBox);
        mipmapsCheckbox.setObjectName("mipmapsCheckbox");
        mipmapsCheckbox.setChecked(true);

        horizontalLayout_3.addWidget(mipmapsCheckbox);


        verticalLayout_2.addLayout(horizontalLayout_3);

        frame = new QFrame(loadOptionsGroupBox);
        frame.setObjectName("frame");
        frame.setFrameShape(com.trolltech.qt.gui.QFrame.Shape.StyledPanel);
        frame.setFrameShadow(com.trolltech.qt.gui.QFrame.Shadow.Raised);
        verticalLayout = new QVBoxLayout(frame);
        verticalLayout.setSpacing(5);
        verticalLayout.setMargin(5);
        verticalLayout.setObjectName("verticalLayout");
        generateDepthImagesCheckBox = new QCheckBox(frame);
        generateDepthImagesCheckBox.setObjectName("generateDepthImagesCheckBox");
        generateDepthImagesCheckBox.setChecked(true);

        verticalLayout.addWidget(generateDepthImagesCheckBox);

        horizontalLayout = new QHBoxLayout();
        horizontalLayout.setObjectName("horizontalLayout");
        depImageDimensionsLabel = new QLabel(frame);
        depImageDimensionsLabel.setObjectName("depImageDimensionsLabel");

        horizontalLayout.addWidget(depImageDimensionsLabel);

        depthImageWidthSpinner = new QSpinBox(frame);
        depthImageWidthSpinner.setObjectName("depthImageWidthSpinner");
        QSizePolicy sizePolicy = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy.setHorizontalStretch((byte)0);
        sizePolicy.setVerticalStretch((byte)0);
        sizePolicy.setHeightForWidth(depthImageWidthSpinner.sizePolicy().hasHeightForWidth());
        depthImageWidthSpinner.setSizePolicy(sizePolicy);
        depthImageWidthSpinner.setMinimum(1);
        depthImageWidthSpinner.setMaximum(8192);
        depthImageWidthSpinner.setValue(1024);

        horizontalLayout.addWidget(depthImageWidthSpinner);

        depthImageXLabel = new QLabel(frame);
        depthImageXLabel.setObjectName("depthImageXLabel");

        horizontalLayout.addWidget(depthImageXLabel);

        depthImageHeightSpinner = new QSpinBox(frame);
        depthImageHeightSpinner.setObjectName("depthImageHeightSpinner");
        QSizePolicy sizePolicy1 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy1.setHorizontalStretch((byte)0);
        sizePolicy1.setVerticalStretch((byte)0);
        sizePolicy1.setHeightForWidth(depthImageHeightSpinner.sizePolicy().hasHeightForWidth());
        depthImageHeightSpinner.setSizePolicy(sizePolicy1);
        depthImageHeightSpinner.setMinimum(1);
        depthImageHeightSpinner.setMaximum(8192);
        depthImageHeightSpinner.setValue(1024);

        horizontalLayout.addWidget(depthImageHeightSpinner);


        verticalLayout.addLayout(horizontalLayout);


        verticalLayout_2.addWidget(frame);


        verticalLayout_6.addWidget(loadOptionsGroupBox);

        modelOptionsGroupBox = new QGroupBox(centralwidget);
        modelOptionsGroupBox.setObjectName("modelOptionsGroupBox");
        verticalLayout_3 = new QVBoxLayout(modelOptionsGroupBox);
        verticalLayout_3.setObjectName("verticalLayout_3");
        modelComboBox = new QComboBox(modelOptionsGroupBox);
        modelComboBox.setObjectName("modelComboBox");

        verticalLayout_3.addWidget(modelComboBox);

        modelSlider = new QSlider(modelOptionsGroupBox);
        modelSlider.setObjectName("modelSlider");
        modelSlider.setOrientation(com.trolltech.qt.core.Qt.Orientation.Horizontal);

        verticalLayout_3.addWidget(modelSlider);

        showCamerasCheckBox = new QCheckBox(modelOptionsGroupBox);
        showCamerasCheckBox.setObjectName("showCamerasCheckBox");

        verticalLayout_3.addWidget(showCamerasCheckBox);


        verticalLayout_6.addWidget(modelOptionsGroupBox);

        renderingOptionsGroupBox = new QGroupBox(centralwidget);
        renderingOptionsGroupBox.setObjectName("renderingOptionsGroupBox");
        verticalLayout_4 = new QVBoxLayout(renderingOptionsGroupBox);
        verticalLayout_4.setObjectName("verticalLayout_4");
        horizontalLayout_9 = new QHBoxLayout();
        horizontalLayout_9.setObjectName("horizontalLayout_9");
        horizontalLayout_9.setContentsMargins(-1, 0, -1, -1);
        kNearestNeighborsCheckBox = new QCheckBox(renderingOptionsGroupBox);
        kNearestNeighborsCheckBox.setObjectName("kNearestNeighborsCheckBox");

        horizontalLayout_9.addWidget(kNearestNeighborsCheckBox);

        kNeighborCountLabel = new QLabel(renderingOptionsGroupBox);
        kNeighborCountLabel.setObjectName("kNeighborCountLabel");
        kNeighborCountLabel.setEnabled(false);

        horizontalLayout_9.addWidget(kNeighborCountLabel);

        kNeighborCountSpinBox = new QSpinBox(renderingOptionsGroupBox);
        kNeighborCountSpinBox.setObjectName("kNeighborCountSpinBox");
        kNeighborCountSpinBox.setEnabled(false);
        QSizePolicy sizePolicy2 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy2.setHorizontalStretch((byte)0);
        sizePolicy2.setVerticalStretch((byte)0);
        sizePolicy2.setHeightForWidth(kNeighborCountSpinBox.sizePolicy().hasHeightForWidth());
        kNeighborCountSpinBox.setSizePolicy(sizePolicy2);
        kNeighborCountSpinBox.setMinimum(1);
        kNeighborCountSpinBox.setMaximum(10);
        kNeighborCountSpinBox.setValue(5);

        horizontalLayout_9.addWidget(kNeighborCountSpinBox);


        verticalLayout_4.addLayout(horizontalLayout_9);

        horizontalLayout_10 = new QHBoxLayout();
        horizontalLayout_10.setObjectName("horizontalLayout_10");
        horizontalLayout_10.setContentsMargins(-1, 0, -1, -1);
        backgroundColorLabel = new QLabel(renderingOptionsGroupBox);
        backgroundColorLabel.setObjectName("backgroundColorLabel");

        horizontalLayout_10.addWidget(backgroundColorLabel);

        backgroundColorButton = new QToolButton(renderingOptionsGroupBox);
        backgroundColorButton.setObjectName("backgroundColorButton");

        horizontalLayout_10.addWidget(backgroundColorButton);

        horizontalSpacer_3 = new QSpacerItem(40, 20, com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Minimum);

        horizontalLayout_10.addItem(horizontalSpacer_3);


        verticalLayout_4.addLayout(horizontalLayout_10);

        line_2 = new QFrame(renderingOptionsGroupBox);
        line_2.setObjectName("line_2");
        line_2.setFrameShape(QFrame.Shape.HLine);

        verticalLayout_4.addWidget(line_2);

        gridLayout = new QGridLayout();
        gridLayout.setObjectName("gridLayout");
        gammaLabel = new QLabel(renderingOptionsGroupBox);
        gammaLabel.setObjectName("gammaLabel");
        gammaLabel.setAlignment(com.trolltech.qt.core.Qt.AlignmentFlag.createQFlags(com.trolltech.qt.core.Qt.AlignmentFlag.AlignRight,com.trolltech.qt.core.Qt.AlignmentFlag.AlignVCenter));

        gridLayout.addWidget(gammaLabel, 0, 0, 1, 1);

        gammaSpinBox = new QDoubleSpinBox(renderingOptionsGroupBox);
        gammaSpinBox.setObjectName("gammaSpinBox");
        QSizePolicy sizePolicy3 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy3.setHorizontalStretch((byte)0);
        sizePolicy3.setVerticalStretch((byte)0);
        sizePolicy3.setHeightForWidth(gammaSpinBox.sizePolicy().hasHeightForWidth());
        gammaSpinBox.setSizePolicy(sizePolicy3);
        gammaSpinBox.setMinimum(1);
        gammaSpinBox.setMaximum(100);
        gammaSpinBox.setSingleStep(0.01);
        gammaSpinBox.setValue(2.2);

        gridLayout.addWidget(gammaSpinBox, 0, 1, 1, 1);

        exponentLabel = new QLabel(renderingOptionsGroupBox);
        exponentLabel.setObjectName("exponentLabel");
        exponentLabel.setAlignment(com.trolltech.qt.core.Qt.AlignmentFlag.createQFlags(com.trolltech.qt.core.Qt.AlignmentFlag.AlignRight,com.trolltech.qt.core.Qt.AlignmentFlag.AlignVCenter));

        gridLayout.addWidget(exponentLabel, 1, 0, 1, 1);

        exponentSpinBox = new QDoubleSpinBox(renderingOptionsGroupBox);
        exponentSpinBox.setObjectName("exponentSpinBox");
        QSizePolicy sizePolicy4 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy4.setHorizontalStretch((byte)0);
        sizePolicy4.setVerticalStretch((byte)0);
        sizePolicy4.setHeightForWidth(exponentSpinBox.sizePolicy().hasHeightForWidth());
        exponentSpinBox.setSizePolicy(sizePolicy4);
        exponentSpinBox.setDecimals(0);
        exponentSpinBox.setMinimum(1);
        exponentSpinBox.setMaximum(1000);
        exponentSpinBox.setValue(16);

        gridLayout.addWidget(exponentSpinBox, 1, 1, 1, 1);


        verticalLayout_4.addLayout(gridLayout);

        line = new QFrame(renderingOptionsGroupBox);
        line.setObjectName("line");
        line.setFrameShape(QFrame.Shape.HLine);

        verticalLayout_4.addWidget(line);

        horizontalLayout_4 = new QHBoxLayout();
        horizontalLayout_4.setObjectName("horizontalLayout_4");
        line_3 = new QFrame(renderingOptionsGroupBox);
        line_3.setObjectName("line_3");
        line_3.setFrameShape(QFrame.Shape.HLine);

        horizontalLayout_4.addWidget(line_3);

        visibilityCheckBox = new QCheckBox(renderingOptionsGroupBox);
        visibilityCheckBox.setObjectName("visibilityCheckBox");
        QSizePolicy sizePolicy5 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Fixed, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy5.setHorizontalStretch((byte)0);
        sizePolicy5.setVerticalStretch((byte)0);
        sizePolicy5.setHeightForWidth(visibilityCheckBox.sizePolicy().hasHeightForWidth());
        visibilityCheckBox.setSizePolicy(sizePolicy5);
        visibilityCheckBox.setChecked(true);

        horizontalLayout_4.addWidget(visibilityCheckBox);

        visibilityBiasLabel = new QLabel(renderingOptionsGroupBox);
        visibilityBiasLabel.setObjectName("visibilityBiasLabel");
        QSizePolicy sizePolicy6 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Fixed, com.trolltech.qt.gui.QSizePolicy.Policy.Preferred);
        sizePolicy6.setHorizontalStretch((byte)0);
        sizePolicy6.setVerticalStretch((byte)0);
        sizePolicy6.setHeightForWidth(visibilityBiasLabel.sizePolicy().hasHeightForWidth());
        visibilityBiasLabel.setSizePolicy(sizePolicy6);

        horizontalLayout_4.addWidget(visibilityBiasLabel);

        visibilityBiasSpinBox = new QDoubleSpinBox(renderingOptionsGroupBox);
        visibilityBiasSpinBox.setObjectName("visibilityBiasSpinBox");
        QSizePolicy sizePolicy7 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy7.setHorizontalStretch((byte)0);
        sizePolicy7.setVerticalStretch((byte)0);
        sizePolicy7.setHeightForWidth(visibilityBiasSpinBox.sizePolicy().hasHeightForWidth());
        visibilityBiasSpinBox.setSizePolicy(sizePolicy7);
        visibilityBiasSpinBox.setDecimals(5);
        visibilityBiasSpinBox.setMinimum(1e-05);
        visibilityBiasSpinBox.setMaximum(1);
        visibilityBiasSpinBox.setSingleStep(0.0001);
        visibilityBiasSpinBox.setValue(0.002);

        horizontalLayout_4.addWidget(visibilityBiasSpinBox);


        verticalLayout_4.addLayout(horizontalLayout_4);


        verticalLayout_6.addWidget(renderingOptionsGroupBox);

        qualityOptionsGroupBox = new QGroupBox(centralwidget);
        qualityOptionsGroupBox.setObjectName("qualityOptionsGroupBox");
        horizontalLayout_5 = new QHBoxLayout(qualityOptionsGroupBox);
        horizontalLayout_5.setObjectName("horizontalLayout_5");
        halfResCheckBox = new QCheckBox(qualityOptionsGroupBox);
        halfResCheckBox.setObjectName("halfResCheckBox");

        horizontalLayout_5.addWidget(halfResCheckBox);

        multisamplingCheckBox = new QCheckBox(qualityOptionsGroupBox);
        multisamplingCheckBox.setObjectName("multisamplingCheckBox");
        multisamplingCheckBox.setChecked(true);

        horizontalLayout_5.addWidget(multisamplingCheckBox);


        verticalLayout_6.addWidget(qualityOptionsGroupBox);

        resampleGroupBox = new QGroupBox(centralwidget);
        resampleGroupBox.setObjectName("resampleGroupBox");
        verticalLayout_5 = new QVBoxLayout(resampleGroupBox);
        verticalLayout_5.setObjectName("verticalLayout_5");
        horizontalLayout_6 = new QHBoxLayout();
        horizontalLayout_6.setObjectName("horizontalLayout_6");
        resampleDimensionsLabel = new QLabel(resampleGroupBox);
        resampleDimensionsLabel.setObjectName("resampleDimensionsLabel");

        horizontalLayout_6.addWidget(resampleDimensionsLabel);

        resampleWidthSpinner = new QSpinBox(resampleGroupBox);
        resampleWidthSpinner.setObjectName("resampleWidthSpinner");
        QSizePolicy sizePolicy8 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy8.setHorizontalStretch((byte)0);
        sizePolicy8.setVerticalStretch((byte)0);
        sizePolicy8.setHeightForWidth(resampleWidthSpinner.sizePolicy().hasHeightForWidth());
        resampleWidthSpinner.setSizePolicy(sizePolicy8);
        resampleWidthSpinner.setMinimum(1);
        resampleWidthSpinner.setMaximum(8192);
        resampleWidthSpinner.setValue(1024);

        horizontalLayout_6.addWidget(resampleWidthSpinner);

        resampleXLabel = new QLabel(resampleGroupBox);
        resampleXLabel.setObjectName("resampleXLabel");

        horizontalLayout_6.addWidget(resampleXLabel);

        resampleHeightSpinner = new QSpinBox(resampleGroupBox);
        resampleHeightSpinner.setObjectName("resampleHeightSpinner");
        QSizePolicy sizePolicy9 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy9.setHorizontalStretch((byte)0);
        sizePolicy9.setVerticalStretch((byte)0);
        sizePolicy9.setHeightForWidth(resampleHeightSpinner.sizePolicy().hasHeightForWidth());
        resampleHeightSpinner.setSizePolicy(sizePolicy9);
        resampleHeightSpinner.setMinimum(1);
        resampleHeightSpinner.setMaximum(8192);
        resampleHeightSpinner.setValue(1024);

        horizontalLayout_6.addWidget(resampleHeightSpinner);


        verticalLayout_5.addLayout(horizontalLayout_6);

        horizontalLayout_7 = new QHBoxLayout();
        horizontalLayout_7.setObjectName("horizontalLayout_7");
        horizontalSpacer = new QSpacerItem(40, 20, com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Minimum);

        horizontalLayout_7.addItem(horizontalSpacer);

        resampleButton = new QPushButton(resampleGroupBox);
        resampleButton.setObjectName("resampleButton");

        horizontalLayout_7.addWidget(resampleButton);


        verticalLayout_5.addLayout(horizontalLayout_7);


        verticalLayout_6.addWidget(resampleGroupBox);

        horizontalLayout_8 = new QHBoxLayout();
        horizontalLayout_8.setObjectName("horizontalLayout_8");
        horizontalSpacer_2 = new QSpacerItem(40, 20, com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Minimum);

        horizontalLayout_8.addItem(horizontalSpacer_2);

        reportBugButton = new QToolButton(centralwidget);
        reportBugButton.setObjectName("reportBugButton");

        horizontalLayout_8.addWidget(reportBugButton);


        verticalLayout_6.addLayout(horizontalLayout_8);

        verticalSpacer = new QSpacerItem(20, 40, com.trolltech.qt.gui.QSizePolicy.Policy.Minimum, com.trolltech.qt.gui.QSizePolicy.Policy.Expanding);

        verticalLayout_6.addItem(verticalSpacer);

        ULFRendererMainWindow.setCentralWidget(centralwidget);
        menubar = new QMenuBar(ULFRendererMainWindow);
        menubar.setObjectName("menubar");
        menubar.setGeometry(new QRect(0, 0, 345, 22));
        menuFile = new QMenu(menubar);
        menuFile.setObjectName("menuFile");
        menuHelp = new QMenu(menubar);
        menuHelp.setObjectName("menuHelp");
        ULFRendererMainWindow.setMenuBar(menubar);
        statusbar = new QStatusBar(ULFRendererMainWindow);
        statusbar.setObjectName("statusbar");
        ULFRendererMainWindow.setStatusBar(statusbar);
        depImageDimensionsLabel.setBuddy(depthImageWidthSpinner);
        depthImageXLabel.setBuddy(depthImageHeightSpinner);
        kNeighborCountLabel.setBuddy(kNeighborCountSpinBox);
        backgroundColorLabel.setBuddy(backgroundColorButton);
        gammaLabel.setBuddy(gammaSpinBox);
        exponentLabel.setBuddy(exponentSpinBox);
        visibilityBiasLabel.setBuddy(visibilityBiasSpinBox);
        resampleDimensionsLabel.setBuddy(resampleWidthSpinner);
        resampleXLabel.setBuddy(resampleHeightSpinner);

        menubar.addAction(menuFile.menuAction());
        menubar.addAction(menuHelp.menuAction());
        menuFile.addAction(actionLoad_single_model);
        menuFile.addAction(actionLoad_model_sequence);
        menuFile.addSeparator();
        menuFile.addAction(actionQuit);
        menuHelp.addAction(actionHelp);
        menuHelp.addSeparator();
        menuHelp.addAction(actionAbout_ULF_Renderer);
        retranslateUi(ULFRendererMainWindow);

        ULFRendererMainWindow.connectSlotsByName();
    } // setupUi

    void retranslateUi(QMainWindow ULFRendererMainWindow)
    {
        ULFRendererMainWindow.setWindowTitle(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "ULF Renderer Settings", null));
        actionLoad_single_model.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Load single model ...", null));
        actionLoad_single_model.setToolTip(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Load a single model into the renderer from data stored on the drive.", null));
        actionLoad_model_sequence.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Load model sequence ...", null));
        actionLoad_model_sequence.setToolTip(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Load a model sequence file describing several models into the renderer from data stored on the drive.", null));
        actionQuit.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Quit", null));
        actionAbout_ULF_Renderer.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "About ULF Renderer", null));
        actionAbout_ULF_Renderer.setToolTip(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Get more information about the ULF Renderer application.", null));
        actionHelp.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Show Help ...", null));
        loadOptionsGroupBox.setTitle(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Load Options", null));
        compressCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Compress Images", null));
        mipmapsCheckbox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Use Mipmaps", null));
        generateDepthImagesCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Generate Depth Images", null));
        depImageDimensionsLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Dimensions:", null));
        depthImageXLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "X", null));
        modelOptionsGroupBox.setTitle(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Model Options", null));
        showCamerasCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Show view cameras", null));
        renderingOptionsGroupBox.setTitle(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Rendering Options", null));
        kNearestNeighborsCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "k-nearest mode", null));
        kNeighborCountLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "- k-samples:", null));
        backgroundColorLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Background Color: ", null));
        backgroundColorButton.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "...", null));
        gammaLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Gamma:", null));
        exponentLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Weight Exponent:", null));
        visibilityCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Visibility Testing -", null));
        visibilityBiasLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Bias:", null));
        qualityOptionsGroupBox.setTitle(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Quality Options", null));
        halfResCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Half-Resolution", null));
        multisamplingCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Multisampling", null));
        resampleGroupBox.setTitle(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Resample Light Field", null));
        resampleDimensionsLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Dimensions:", null));
        resampleXLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "X", null));
        resampleButton.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Resample", null));
        reportBugButton.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Report Bug", null));
        menuFile.setTitle(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "File", null));
        menuHelp.setTitle(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindow", "Help", null));
    } // retranslateUi

}

