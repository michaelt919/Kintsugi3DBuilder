/********************************************************************************
 ** Form generated from reading ui file 'ULFRendererQMainWindowToolbox.ui.jui'
 **
 ** Created by: Qt User Interface Compiler version 4.8.6
 **
 ** WARNING! All changes made in this file will be lost when recompiling ui file!
 ********************************************************************************/
package tetzlaff.ulf.app;

import com.trolltech.qt.core.*;
import com.trolltech.qt.gui.*;

public class Ui_ULFRendererMainWindowToolbox implements com.trolltech.qt.QUiForm<QMainWindow>
{
    public QAction actionLoad_Single_Model;
    public QAction actionLoad_Model_Sequence;
    public QAction actionQuit;
    public QAction actionShow_User_Guide;
    public QAction actionAbout_ULF_Renderer;
    public QWidget centralwidget;
    public QVBoxLayout verticalLayout_9;
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
    public QFrame frame_2;
    public QVBoxLayout verticalLayout_6;
    public QToolBox optionsToolox;
    public QWidget renderingOptionsPage;
    public QVBoxLayout verticalLayout_5;
    public QHBoxLayout horizontalLayout_2;
    public QCheckBox halfResCheckBox;
    public QCheckBox multisamplingCheckBox;
    public QHBoxLayout horizontalLayout_9;
    public QCheckBox kNearestNeighborsCheckBox;
    public QLabel kNeighborCountLabel;
    public QSpinBox kNeighborCountSpinBox;
    public QFrame line_4;
    public QCheckBox showCamerasCheckBox;
    public QHBoxLayout horizontalLayout_10;
    public QLabel backgroundColorLabel;
    public QToolButton backgroundColorButton;
    public QSpacerItem horizontalSpacer_3;
    public QSpacerItem verticalSpacer_2;
    public QWidget qualityOptionsPage;
    public QVBoxLayout verticalLayout_4;
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
    public QSpacerItem verticalSpacer;
    public QWidget resampleLFPage;
    public QVBoxLayout verticalLayout_8;
    public QHBoxLayout horizontalLayout_11;
    public QLabel resampleDimensionsLabel;
    public QSpinBox resampleWidthSpinner;
    public QLabel resampleXLabel;
    public QSpinBox resampleHeightSpinner;
    public QHBoxLayout horizontalLayout_12;
    public QSpacerItem horizontalSpacer_4;
    public QPushButton resampleButton;
    public QSpacerItem verticalSpacer_3;
    public QHBoxLayout horizontalLayout_8;
    public QSpacerItem horizontalSpacer_2;
    public QToolButton reportBugButton;
    public QMenuBar menubar;
    public QMenu menuFile;
    public QMenu menuHelp;
    public QStatusBar statusbar;

    public Ui_ULFRendererMainWindowToolbox() { super(); }

    public void setupUi(QMainWindow ULFRendererMainWindowToolbox)
    {
        ULFRendererMainWindowToolbox.setObjectName("ULFRendererMainWindowToolbox");
        ULFRendererMainWindowToolbox.resize(new QSize(350, 636).expandedTo(ULFRendererMainWindowToolbox.minimumSizeHint()));
        ULFRendererMainWindowToolbox.setMinimumSize(new QSize(350, 636));
        ULFRendererMainWindowToolbox.setMaximumSize(new QSize(350, 636));
        actionLoad_Single_Model = new QAction(ULFRendererMainWindowToolbox);
        actionLoad_Single_Model.setObjectName("actionLoad_Single_Model");
        actionLoad_Model_Sequence = new QAction(ULFRendererMainWindowToolbox);
        actionLoad_Model_Sequence.setObjectName("actionLoad_Model_Sequence");
        actionQuit = new QAction(ULFRendererMainWindowToolbox);
        actionQuit.setObjectName("actionQuit");
        actionQuit.setMenuRole(com.trolltech.qt.gui.QAction.MenuRole.QuitRole);
        actionShow_User_Guide = new QAction(ULFRendererMainWindowToolbox);
        actionShow_User_Guide.setObjectName("actionShow_User_Guide");
        actionAbout_ULF_Renderer = new QAction(ULFRendererMainWindowToolbox);
        actionAbout_ULF_Renderer.setObjectName("actionAbout_ULF_Renderer");
        centralwidget = new QWidget(ULFRendererMainWindowToolbox);
        centralwidget.setObjectName("centralwidget");
        verticalLayout_9 = new QVBoxLayout(centralwidget);
        verticalLayout_9.setObjectName("verticalLayout_9");
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


        verticalLayout_9.addWidget(loadOptionsGroupBox);

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


        verticalLayout_9.addWidget(modelOptionsGroupBox);

        frame_2 = new QFrame(centralwidget);
        frame_2.setObjectName("frame_2");
        frame_2.setFrameShape(com.trolltech.qt.gui.QFrame.Shape.Panel);
        frame_2.setFrameShadow(com.trolltech.qt.gui.QFrame.Shadow.Raised);
        frame_2.setLineWidth(2);
        verticalLayout_6 = new QVBoxLayout(frame_2);
        verticalLayout_6.setObjectName("verticalLayout_6");
        optionsToolox = new QToolBox(frame_2);
        optionsToolox.setObjectName("optionsToolox");
        renderingOptionsPage = new QWidget();
        renderingOptionsPage.setObjectName("renderingOptionsPage");
        renderingOptionsPage.setGeometry(new QRect(0, 0, 298, 182));
        verticalLayout_5 = new QVBoxLayout(renderingOptionsPage);
        verticalLayout_5.setObjectName("verticalLayout_5");
        horizontalLayout_2 = new QHBoxLayout();
        horizontalLayout_2.setObjectName("horizontalLayout_2");
        halfResCheckBox = new QCheckBox(renderingOptionsPage);
        halfResCheckBox.setObjectName("halfResCheckBox");

        horizontalLayout_2.addWidget(halfResCheckBox);

        multisamplingCheckBox = new QCheckBox(renderingOptionsPage);
        multisamplingCheckBox.setObjectName("multisamplingCheckBox");
        multisamplingCheckBox.setChecked(true);

        horizontalLayout_2.addWidget(multisamplingCheckBox);


        verticalLayout_5.addLayout(horizontalLayout_2);

        horizontalLayout_9 = new QHBoxLayout();
        horizontalLayout_9.setObjectName("horizontalLayout_9");
        horizontalLayout_9.setContentsMargins(-1, 0, -1, -1);
        kNearestNeighborsCheckBox = new QCheckBox(renderingOptionsPage);
        kNearestNeighborsCheckBox.setObjectName("kNearestNeighborsCheckBox");

        horizontalLayout_9.addWidget(kNearestNeighborsCheckBox);

        kNeighborCountLabel = new QLabel(renderingOptionsPage);
        kNeighborCountLabel.setObjectName("kNeighborCountLabel");
        kNeighborCountLabel.setEnabled(false);

        horizontalLayout_9.addWidget(kNeighborCountLabel);

        kNeighborCountSpinBox = new QSpinBox(renderingOptionsPage);
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


        verticalLayout_5.addLayout(horizontalLayout_9);

        line_4 = new QFrame(renderingOptionsPage);
        line_4.setObjectName("line_4");
        line_4.setFrameShape(QFrame.Shape.HLine);

        verticalLayout_5.addWidget(line_4);

        showCamerasCheckBox = new QCheckBox(renderingOptionsPage);
        showCamerasCheckBox.setObjectName("showCamerasCheckBox");

        verticalLayout_5.addWidget(showCamerasCheckBox);

        horizontalLayout_10 = new QHBoxLayout();
        horizontalLayout_10.setObjectName("horizontalLayout_10");
        horizontalLayout_10.setContentsMargins(-1, 0, -1, -1);
        backgroundColorLabel = new QLabel(renderingOptionsPage);
        backgroundColorLabel.setObjectName("backgroundColorLabel");

        horizontalLayout_10.addWidget(backgroundColorLabel);

        backgroundColorButton = new QToolButton(renderingOptionsPage);
        backgroundColorButton.setObjectName("backgroundColorButton");

        horizontalLayout_10.addWidget(backgroundColorButton);

        horizontalSpacer_3 = new QSpacerItem(40, 20, com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Minimum);

        horizontalLayout_10.addItem(horizontalSpacer_3);


        verticalLayout_5.addLayout(horizontalLayout_10);

        verticalSpacer_2 = new QSpacerItem(20, 40, com.trolltech.qt.gui.QSizePolicy.Policy.Minimum, com.trolltech.qt.gui.QSizePolicy.Policy.Expanding);

        verticalLayout_5.addItem(verticalSpacer_2);

        optionsToolox.addItem(renderingOptionsPage, com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Performance and Visual Settings", null));
        qualityOptionsPage = new QWidget();
        qualityOptionsPage.setObjectName("qualityOptionsPage");
        qualityOptionsPage.setGeometry(new QRect(0, 0, 298, 182));
        verticalLayout_4 = new QVBoxLayout(qualityOptionsPage);
        verticalLayout_4.setObjectName("verticalLayout_4");
        gridLayout = new QGridLayout();
        gridLayout.setObjectName("gridLayout");
        gammaLabel = new QLabel(qualityOptionsPage);
        gammaLabel.setObjectName("gammaLabel");
        gammaLabel.setAlignment(com.trolltech.qt.core.Qt.AlignmentFlag.createQFlags(com.trolltech.qt.core.Qt.AlignmentFlag.AlignRight,com.trolltech.qt.core.Qt.AlignmentFlag.AlignVCenter));

        gridLayout.addWidget(gammaLabel, 0, 0, 1, 1);

        gammaSpinBox = new QDoubleSpinBox(qualityOptionsPage);
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

        exponentLabel = new QLabel(qualityOptionsPage);
        exponentLabel.setObjectName("exponentLabel");
        exponentLabel.setAlignment(com.trolltech.qt.core.Qt.AlignmentFlag.createQFlags(com.trolltech.qt.core.Qt.AlignmentFlag.AlignRight,com.trolltech.qt.core.Qt.AlignmentFlag.AlignVCenter));

        gridLayout.addWidget(exponentLabel, 1, 0, 1, 1);

        exponentSpinBox = new QDoubleSpinBox(qualityOptionsPage);
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

        line = new QFrame(qualityOptionsPage);
        line.setObjectName("line");
        line.setFrameShape(QFrame.Shape.HLine);

        verticalLayout_4.addWidget(line);

        horizontalLayout_4 = new QHBoxLayout();
        horizontalLayout_4.setObjectName("horizontalLayout_4");
        line_3 = new QFrame(qualityOptionsPage);
        line_3.setObjectName("line_3");
        line_3.setFrameShape(QFrame.Shape.HLine);

        horizontalLayout_4.addWidget(line_3);

        visibilityCheckBox = new QCheckBox(qualityOptionsPage);
        visibilityCheckBox.setObjectName("visibilityCheckBox");
        QSizePolicy sizePolicy5 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Fixed, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy5.setHorizontalStretch((byte)0);
        sizePolicy5.setVerticalStretch((byte)0);
        sizePolicy5.setHeightForWidth(visibilityCheckBox.sizePolicy().hasHeightForWidth());
        visibilityCheckBox.setSizePolicy(sizePolicy5);
        visibilityCheckBox.setChecked(true);

        horizontalLayout_4.addWidget(visibilityCheckBox);

        visibilityBiasLabel = new QLabel(qualityOptionsPage);
        visibilityBiasLabel.setObjectName("visibilityBiasLabel");
        QSizePolicy sizePolicy6 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Fixed, com.trolltech.qt.gui.QSizePolicy.Policy.Preferred);
        sizePolicy6.setHorizontalStretch((byte)0);
        sizePolicy6.setVerticalStretch((byte)0);
        sizePolicy6.setHeightForWidth(visibilityBiasLabel.sizePolicy().hasHeightForWidth());
        visibilityBiasLabel.setSizePolicy(sizePolicy6);

        horizontalLayout_4.addWidget(visibilityBiasLabel);

        visibilityBiasSpinBox = new QDoubleSpinBox(qualityOptionsPage);
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

        verticalSpacer = new QSpacerItem(20, 40, com.trolltech.qt.gui.QSizePolicy.Policy.Minimum, com.trolltech.qt.gui.QSizePolicy.Policy.Expanding);

        verticalLayout_4.addItem(verticalSpacer);

        optionsToolox.addItem(qualityOptionsPage, com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Advanced Rendering Options", null));
        resampleLFPage = new QWidget();
        resampleLFPage.setObjectName("resampleLFPage");
        resampleLFPage.setGeometry(new QRect(0, 0, 298, 182));
        verticalLayout_8 = new QVBoxLayout(resampleLFPage);
        verticalLayout_8.setObjectName("verticalLayout_8");
        horizontalLayout_11 = new QHBoxLayout();
        horizontalLayout_11.setObjectName("horizontalLayout_11");
        resampleDimensionsLabel = new QLabel(resampleLFPage);
        resampleDimensionsLabel.setObjectName("resampleDimensionsLabel");

        horizontalLayout_11.addWidget(resampleDimensionsLabel);

        resampleWidthSpinner = new QSpinBox(resampleLFPage);
        resampleWidthSpinner.setObjectName("resampleWidthSpinner");
        QSizePolicy sizePolicy8 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy8.setHorizontalStretch((byte)0);
        sizePolicy8.setVerticalStretch((byte)0);
        sizePolicy8.setHeightForWidth(resampleWidthSpinner.sizePolicy().hasHeightForWidth());
        resampleWidthSpinner.setSizePolicy(sizePolicy8);
        resampleWidthSpinner.setMinimum(1);
        resampleWidthSpinner.setMaximum(8192);
        resampleWidthSpinner.setValue(1024);

        horizontalLayout_11.addWidget(resampleWidthSpinner);

        resampleXLabel = new QLabel(resampleLFPage);
        resampleXLabel.setObjectName("resampleXLabel");

        horizontalLayout_11.addWidget(resampleXLabel);

        resampleHeightSpinner = new QSpinBox(resampleLFPage);
        resampleHeightSpinner.setObjectName("resampleHeightSpinner");
        QSizePolicy sizePolicy9 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy9.setHorizontalStretch((byte)0);
        sizePolicy9.setVerticalStretch((byte)0);
        sizePolicy9.setHeightForWidth(resampleHeightSpinner.sizePolicy().hasHeightForWidth());
        resampleHeightSpinner.setSizePolicy(sizePolicy9);
        resampleHeightSpinner.setMinimum(1);
        resampleHeightSpinner.setMaximum(8192);
        resampleHeightSpinner.setValue(1024);

        horizontalLayout_11.addWidget(resampleHeightSpinner);


        verticalLayout_8.addLayout(horizontalLayout_11);

        horizontalLayout_12 = new QHBoxLayout();
        horizontalLayout_12.setObjectName("horizontalLayout_12");
        horizontalSpacer_4 = new QSpacerItem(40, 20, com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Minimum);

        horizontalLayout_12.addItem(horizontalSpacer_4);

        resampleButton = new QPushButton(resampleLFPage);
        resampleButton.setObjectName("resampleButton");

        horizontalLayout_12.addWidget(resampleButton);


        verticalLayout_8.addLayout(horizontalLayout_12);

        verticalSpacer_3 = new QSpacerItem(20, 40, com.trolltech.qt.gui.QSizePolicy.Policy.Minimum, com.trolltech.qt.gui.QSizePolicy.Policy.Expanding);

        verticalLayout_8.addItem(verticalSpacer_3);

        optionsToolox.addItem(resampleLFPage, com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Resample Light Field", null));

        verticalLayout_6.addWidget(optionsToolox);


        verticalLayout_9.addWidget(frame_2);

        horizontalLayout_8 = new QHBoxLayout();
        horizontalLayout_8.setObjectName("horizontalLayout_8");
        horizontalSpacer_2 = new QSpacerItem(40, 20, com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Minimum);

        horizontalLayout_8.addItem(horizontalSpacer_2);

        reportBugButton = new QToolButton(centralwidget);
        reportBugButton.setObjectName("reportBugButton");

        horizontalLayout_8.addWidget(reportBugButton);


        verticalLayout_9.addLayout(horizontalLayout_8);

        ULFRendererMainWindowToolbox.setCentralWidget(centralwidget);
        menubar = new QMenuBar(ULFRendererMainWindowToolbox);
        menubar.setObjectName("menubar");
        menubar.setGeometry(new QRect(0, 0, 350, 22));
        menuFile = new QMenu(menubar);
        menuFile.setObjectName("menuFile");
        menuHelp = new QMenu(menubar);
        menuHelp.setObjectName("menuHelp");
        ULFRendererMainWindowToolbox.setMenuBar(menubar);
        statusbar = new QStatusBar(ULFRendererMainWindowToolbox);
        statusbar.setObjectName("statusbar");
        ULFRendererMainWindowToolbox.setStatusBar(statusbar);
        depImageDimensionsLabel.setBuddy(depthImageWidthSpinner);
        depthImageXLabel.setBuddy(depthImageHeightSpinner);
        kNeighborCountLabel.setBuddy(kNeighborCountSpinBox);
        backgroundColorLabel.setBuddy(backgroundColorButton);
        gammaLabel.setBuddy(gammaSpinBox);
        exponentLabel.setBuddy(exponentSpinBox);
        visibilityBiasLabel.setBuddy(visibilityBiasSpinBox);

        menubar.addAction(menuFile.menuAction());
        menubar.addAction(menuHelp.menuAction());
        menuFile.addAction(actionLoad_Single_Model);
        menuFile.addAction(actionLoad_Model_Sequence);
        menuFile.addSeparator();
        menuFile.addAction(actionQuit);
        menuHelp.addAction(actionShow_User_Guide);
        menuHelp.addSeparator();
        menuHelp.addAction(actionAbout_ULF_Renderer);
        retranslateUi(ULFRendererMainWindowToolbox);

        optionsToolox.setCurrentIndex(0);


        ULFRendererMainWindowToolbox.connectSlotsByName();
    } // setupUi

    void retranslateUi(QMainWindow ULFRendererMainWindowToolbox)
    {
        ULFRendererMainWindowToolbox.setWindowTitle(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "ULF Renderer Settings", null));
        actionLoad_Single_Model.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "&Load Single Model ...", null));
        actionLoad_Model_Sequence.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Load Model Sequence ...", null));
        actionLoad_Model_Sequence.setIconText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Load Model &Sequence", null));
        actionQuit.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "&Quit", null));
        actionQuit.setToolTip(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Exit ULF Renderer", null));
        actionShow_User_Guide.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Show &User Guide ...", null));
        actionShow_User_Guide.setToolTip(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Show the HTML users guide with info on creating and loading models, settings, and troubleshooting.", null));
        actionAbout_ULF_Renderer.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "&About ULF Renderer", null));
        actionAbout_ULF_Renderer.setToolTip(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Information about version, authors, copyright, licensing, and support.", null));
        loadOptionsGroupBox.setTitle(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Loading Models", null));
        compressCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Compress Images", null));
        mipmapsCheckbox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Use Mipmaps", null));
        generateDepthImagesCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Generate Depth Images", null));
        depImageDimensionsLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Dimensions:", null));
        depthImageXLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "X", null));
        modelOptionsGroupBox.setTitle(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Current Model", null));
        halfResCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Half-Resolution", null));
        multisamplingCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Multisampling", null));
        kNearestNeighborsCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "k-nearest mode", null));
        kNeighborCountLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "- k-samples:", null));
        showCamerasCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Show view cameras", null));
        backgroundColorLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Background Color: ", null));
        backgroundColorButton.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "...", null));
        optionsToolox.setItemText(optionsToolox.indexOf(renderingOptionsPage), com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Performance and Visual Settings", null));
        gammaLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Gamma:", null));
        exponentLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Weight Exponent:", null));
        visibilityCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Visibility Testing -", null));
        visibilityBiasLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Bias:", null));
        optionsToolox.setItemText(optionsToolox.indexOf(qualityOptionsPage), com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Advanced Rendering Options", null));
        resampleDimensionsLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Dimensions:", null));
        resampleXLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "X", null));
        resampleButton.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Resample", null));
        optionsToolox.setItemText(optionsToolox.indexOf(resampleLFPage), com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Resample Light Field", null));
        reportBugButton.setText(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "Report Bug", null));
        menuFile.setTitle(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "&File", null));
        menuHelp.setTitle(com.trolltech.qt.core.QCoreApplication.translate("ULFRendererMainWindowToolbox", "&Help", null));
    } // retranslateUi

}

