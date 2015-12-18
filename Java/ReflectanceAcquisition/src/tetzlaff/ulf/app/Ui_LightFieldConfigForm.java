/********************************************************************************
 ** Form generated from reading ui file 'LightFieldConfig.ui.jui'
 **
 ** Created by: Qt User Interface Compiler version 4.8.6
 **
 ** WARNING! All changes made in this file will be lost when recompiling ui file!
 ********************************************************************************/
package tetzlaff.ulf.app;

import com.trolltech.qt.core.QSize;
import com.trolltech.qt.gui.QCheckBox;
import com.trolltech.qt.gui.QComboBox;
import com.trolltech.qt.gui.QDoubleSpinBox;
import com.trolltech.qt.gui.QFrame;
import com.trolltech.qt.gui.QGridLayout;
import com.trolltech.qt.gui.QGroupBox;
import com.trolltech.qt.gui.QHBoxLayout;
import com.trolltech.qt.gui.QLabel;
import com.trolltech.qt.gui.QProgressBar;
import com.trolltech.qt.gui.QPushButton;
import com.trolltech.qt.gui.QSizePolicy;
import com.trolltech.qt.gui.QSlider;
import com.trolltech.qt.gui.QSpacerItem;
import com.trolltech.qt.gui.QSpinBox;
import com.trolltech.qt.gui.QVBoxLayout;
import com.trolltech.qt.gui.QWidget;

public class Ui_LightFieldConfigForm implements com.trolltech.qt.QUiForm<QWidget>
{
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
    public QHBoxLayout horizontalLayout_2;
    public QPushButton loadSingleButton;
    public QPushButton loadMultipleButton;
    public QGroupBox modelOptionsGroupBox;
    public QVBoxLayout verticalLayout_3;
    public QComboBox modelComboBox;
    public QSlider modelSlider;
    public QGroupBox renderingOptionsGroupBox;
    public QVBoxLayout verticalLayout_4;
    public QGridLayout gridLayout;
    public QLabel gammaLabel;
    public QDoubleSpinBox gammaSpinBox;
    public QLabel exponentLabel;
    public QDoubleSpinBox exponentSpinBox;
    public QFrame line;
    public QHBoxLayout horizontalLayout_4;
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
    public QProgressBar loadingProgressBar;
    public QSpacerItem verticalSpacer;

    public Ui_LightFieldConfigForm() { super(); }

    public void setupUi(QWidget LightFieldConfigForm)
    {
        LightFieldConfigForm.setObjectName("LightFieldConfigForm");
        LightFieldConfigForm.resize(new QSize(345, 674).expandedTo(LightFieldConfigForm.minimumSizeHint()));
        LightFieldConfigForm.setMinimumSize(new QSize(345, 638));
        LightFieldConfigForm.setMaximumSize(new QSize(345, 674));
        verticalLayout_6 = new QVBoxLayout(LightFieldConfigForm);
        verticalLayout_6.setObjectName("verticalLayout_6");
        loadOptionsGroupBox = new QGroupBox(LightFieldConfigForm);
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

        horizontalLayout_2 = new QHBoxLayout();
        horizontalLayout_2.setObjectName("horizontalLayout_2");
        loadSingleButton = new QPushButton(loadOptionsGroupBox);
        loadSingleButton.setObjectName("loadSingleButton");

        horizontalLayout_2.addWidget(loadSingleButton);

        loadMultipleButton = new QPushButton(loadOptionsGroupBox);
        loadMultipleButton.setObjectName("loadMultipleButton");

        horizontalLayout_2.addWidget(loadMultipleButton);


        verticalLayout_2.addLayout(horizontalLayout_2);


        verticalLayout_6.addWidget(loadOptionsGroupBox);

        modelOptionsGroupBox = new QGroupBox(LightFieldConfigForm);
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


        verticalLayout_6.addWidget(modelOptionsGroupBox);

        renderingOptionsGroupBox = new QGroupBox(LightFieldConfigForm);
        renderingOptionsGroupBox.setObjectName("renderingOptionsGroupBox");
        verticalLayout_4 = new QVBoxLayout(renderingOptionsGroupBox);
        verticalLayout_4.setObjectName("verticalLayout_4");
        gridLayout = new QGridLayout();
        gridLayout.setObjectName("gridLayout");
        gammaLabel = new QLabel(renderingOptionsGroupBox);
        gammaLabel.setObjectName("gammaLabel");
        gammaLabel.setAlignment(com.trolltech.qt.core.Qt.AlignmentFlag.createQFlags(com.trolltech.qt.core.Qt.AlignmentFlag.AlignRight,com.trolltech.qt.core.Qt.AlignmentFlag.AlignVCenter));

        gridLayout.addWidget(gammaLabel, 0, 0, 1, 1);

        gammaSpinBox = new QDoubleSpinBox(renderingOptionsGroupBox);
        gammaSpinBox.setObjectName("gammaSpinBox");
        QSizePolicy sizePolicy2 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy2.setHorizontalStretch((byte)0);
        sizePolicy2.setVerticalStretch((byte)0);
        sizePolicy2.setHeightForWidth(gammaSpinBox.sizePolicy().hasHeightForWidth());
        gammaSpinBox.setSizePolicy(sizePolicy2);
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
        QSizePolicy sizePolicy3 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy3.setHorizontalStretch((byte)0);
        sizePolicy3.setVerticalStretch((byte)0);
        sizePolicy3.setHeightForWidth(exponentSpinBox.sizePolicy().hasHeightForWidth());
        exponentSpinBox.setSizePolicy(sizePolicy3);
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
        visibilityCheckBox = new QCheckBox(renderingOptionsGroupBox);
        visibilityCheckBox.setObjectName("visibilityCheckBox");
        QSizePolicy sizePolicy4 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Fixed, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy4.setHorizontalStretch((byte)0);
        sizePolicy4.setVerticalStretch((byte)0);
        sizePolicy4.setHeightForWidth(visibilityCheckBox.sizePolicy().hasHeightForWidth());
        visibilityCheckBox.setSizePolicy(sizePolicy4);
        visibilityCheckBox.setChecked(true);

        horizontalLayout_4.addWidget(visibilityCheckBox);

        visibilityBiasLabel = new QLabel(renderingOptionsGroupBox);
        visibilityBiasLabel.setObjectName("visibilityBiasLabel");
        QSizePolicy sizePolicy5 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Fixed, com.trolltech.qt.gui.QSizePolicy.Policy.Preferred);
        sizePolicy5.setHorizontalStretch((byte)0);
        sizePolicy5.setVerticalStretch((byte)0);
        sizePolicy5.setHeightForWidth(visibilityBiasLabel.sizePolicy().hasHeightForWidth());
        visibilityBiasLabel.setSizePolicy(sizePolicy5);

        horizontalLayout_4.addWidget(visibilityBiasLabel);

        visibilityBiasSpinBox = new QDoubleSpinBox(renderingOptionsGroupBox);
        visibilityBiasSpinBox.setObjectName("visibilityBiasSpinBox");
        QSizePolicy sizePolicy6 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy6.setHorizontalStretch((byte)0);
        sizePolicy6.setVerticalStretch((byte)0);
        sizePolicy6.setHeightForWidth(visibilityBiasSpinBox.sizePolicy().hasHeightForWidth());
        visibilityBiasSpinBox.setSizePolicy(sizePolicy6);
        visibilityBiasSpinBox.setDecimals(5);
        visibilityBiasSpinBox.setMinimum(1e-05);
        visibilityBiasSpinBox.setMaximum(1);
        visibilityBiasSpinBox.setSingleStep(0.0001);
        visibilityBiasSpinBox.setValue(0.002);

        horizontalLayout_4.addWidget(visibilityBiasSpinBox);


        verticalLayout_4.addLayout(horizontalLayout_4);


        verticalLayout_6.addWidget(renderingOptionsGroupBox);

        qualityOptionsGroupBox = new QGroupBox(LightFieldConfigForm);
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

        resampleGroupBox = new QGroupBox(LightFieldConfigForm);
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
        QSizePolicy sizePolicy7 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy7.setHorizontalStretch((byte)0);
        sizePolicy7.setVerticalStretch((byte)0);
        sizePolicy7.setHeightForWidth(resampleWidthSpinner.sizePolicy().hasHeightForWidth());
        resampleWidthSpinner.setSizePolicy(sizePolicy7);
        resampleWidthSpinner.setMinimum(1);
        resampleWidthSpinner.setMaximum(8192);
        resampleWidthSpinner.setValue(1024);

        horizontalLayout_6.addWidget(resampleWidthSpinner);

        resampleXLabel = new QLabel(resampleGroupBox);
        resampleXLabel.setObjectName("resampleXLabel");

        horizontalLayout_6.addWidget(resampleXLabel);

        resampleHeightSpinner = new QSpinBox(resampleGroupBox);
        resampleHeightSpinner.setObjectName("resampleHeightSpinner");
        QSizePolicy sizePolicy8 = new QSizePolicy(com.trolltech.qt.gui.QSizePolicy.Policy.Expanding, com.trolltech.qt.gui.QSizePolicy.Policy.Fixed);
        sizePolicy8.setHorizontalStretch((byte)0);
        sizePolicy8.setVerticalStretch((byte)0);
        sizePolicy8.setHeightForWidth(resampleHeightSpinner.sizePolicy().hasHeightForWidth());
        resampleHeightSpinner.setSizePolicy(sizePolicy8);
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

        loadingProgressBar = new QProgressBar(LightFieldConfigForm);
        loadingProgressBar.setObjectName("loadingProgressBar");
        loadingProgressBar.setValue(24);

        verticalLayout_6.addWidget(loadingProgressBar);

        verticalSpacer = new QSpacerItem(20, 40, com.trolltech.qt.gui.QSizePolicy.Policy.Minimum, com.trolltech.qt.gui.QSizePolicy.Policy.Expanding);

        verticalLayout_6.addItem(verticalSpacer);

        depImageDimensionsLabel.setBuddy(depthImageWidthSpinner);
        depthImageXLabel.setBuddy(depthImageHeightSpinner);
        gammaLabel.setBuddy(gammaSpinBox);
        exponentLabel.setBuddy(exponentSpinBox);
        resampleDimensionsLabel.setBuddy(resampleWidthSpinner);
        resampleXLabel.setBuddy(resampleHeightSpinner);
        QWidget.setTabOrder(compressCheckBox, mipmapsCheckbox);
        QWidget.setTabOrder(mipmapsCheckbox, generateDepthImagesCheckBox);
        QWidget.setTabOrder(generateDepthImagesCheckBox, depthImageWidthSpinner);
        QWidget.setTabOrder(depthImageWidthSpinner, depthImageHeightSpinner);
        QWidget.setTabOrder(depthImageHeightSpinner, loadSingleButton);
        QWidget.setTabOrder(loadSingleButton, loadMultipleButton);
        QWidget.setTabOrder(loadMultipleButton, modelComboBox);
        QWidget.setTabOrder(modelComboBox, modelSlider);
        QWidget.setTabOrder(modelSlider, gammaSpinBox);
        QWidget.setTabOrder(gammaSpinBox, exponentSpinBox);
        QWidget.setTabOrder(exponentSpinBox, visibilityCheckBox);
        QWidget.setTabOrder(visibilityCheckBox, visibilityBiasSpinBox);
        QWidget.setTabOrder(visibilityBiasSpinBox, halfResCheckBox);
        QWidget.setTabOrder(halfResCheckBox, multisamplingCheckBox);
        QWidget.setTabOrder(multisamplingCheckBox, resampleWidthSpinner);
        QWidget.setTabOrder(resampleWidthSpinner, resampleHeightSpinner);
        QWidget.setTabOrder(resampleHeightSpinner, resampleButton);
        retranslateUi(LightFieldConfigForm);
        generateDepthImagesCheckBox.clicked.connect(depImageDimensionsLabel, "setEnabled(boolean)");
        generateDepthImagesCheckBox.clicked.connect(depthImageWidthSpinner, "setEnabled(boolean)");
        generateDepthImagesCheckBox.clicked.connect(depthImageXLabel, "setEnabled(boolean)");
        generateDepthImagesCheckBox.clicked.connect(depthImageHeightSpinner, "setEnabled(boolean)");
        visibilityCheckBox.clicked.connect(visibilityBiasLabel, "setEnabled(boolean)");
        visibilityCheckBox.clicked.connect(visibilityBiasSpinBox, "setEnabled(boolean)");

        LightFieldConfigForm.connectSlotsByName();
    } // setupUi

    void retranslateUi(QWidget LightFieldConfigForm)
    {
        LightFieldConfigForm.setWindowTitle(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Light Field Config", null));
        loadOptionsGroupBox.setTitle(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Load Options", null));
        compressCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Compress Images", null));
        mipmapsCheckbox.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Use Mipmaps", null));
        generateDepthImagesCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Generate Depth Images", null));
        depImageDimensionsLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Dimensions:", null));
        depthImageXLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "X", null));
        loadSingleButton.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Load Single ...", null));
        loadMultipleButton.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Load Multiple ...", null));
        modelOptionsGroupBox.setTitle(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Model Options", null));
        renderingOptionsGroupBox.setTitle(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Rendering Options", null));
        gammaLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Gamma:", null));
        exponentLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Weight Exponent:", null));
        visibilityCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Visibility Testing -", null));
        visibilityBiasLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Bias:", null));
        qualityOptionsGroupBox.setTitle(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Quality Options", null));
        halfResCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Half-Resolution", null));
        multisamplingCheckBox.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Multisampling", null));
        resampleGroupBox.setTitle(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Resample Light Field", null));
        resampleDimensionsLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Dimensions:", null));
        resampleXLabel.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "X", null));
        resampleButton.setText(com.trolltech.qt.core.QCoreApplication.translate("LightFieldConfigForm", "Resample", null));
    } // retranslateUi

}

