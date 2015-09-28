#!/bin/bash

for form in *.ui; do
	cp "$form" "${form}.jui"
	sed -i -e s#'<ui version="4.0">'#'<ui version="4.0" language="jambi">'#g "${form}.jui"
	sed -i -e s#'Qt::Vertical'#'com.trolltech.qt.core.Qt.Orientation.Vertical'#g "${form}.jui"
	sed -i -e s#'Qt::Horizontal'#'com.trolltech.qt.core.Qt.Orientation.Horizontal'#g "${form}.jui"
	sed -i -e s#'Qt::LeftDockWidgetArea'#'com.trolltech.qt.core.Qt.DockWidgetArea.LeftDockWidgetArea'#g "${form}.jui"
	sed -i -e s#'Qt::RightDockWidgetArea'#'com.trolltech.qt.core.Qt.DockWidgetArea.RightDockWidgetArea'#g "${form}.jui"
	sed -i -e s#'Qt::WindowModal'#'com.trolltech.qt.core.Qt.WindowModality.WindowModal'#g "${form}.jui"
	sed -i -e s#'Qt::ApplicationModal'#'com.trolltech.qt.core.Qt.WindowModality.ApplicationModal'#g "${form}.jui"
	sed -i -e s#'QAbstractItemView::SingleSelection'#'QAbstractItemView.SelectionMode.SingleSelection'#g "${form}.jui"
	sed -i -e s#'QAbstractItemView::SelectRows'#'QAbstractItemView.SelectionBehavior.SelectRows'#g "${form}.jui"

	sed -i -e s#'Qt::AlignLeading,'##g "${form}.jui"
	sed -i -e s#'Qt::Align'#'com.trolltech.qt.core.Qt.AlignmentFlag.Align'#g "${form}.jui"

	sed -i -e s#'QLayout::SetMinAndMaxSize'#'com.trolltech.qt.gui.QLayout.SizeConstraint.SetMinAndMaxSize'#g "${form}.jui"
	sed -i -e s#'QFormLayout::ExpandingFieldsGrow'#'com.trolltech.qt.gui.QFormLayout.FieldGrowthPolicy.ExpandingFieldsGrow'#g "${form}.jui"
	sed -i -e s#'QFrame::StyledPanel'#'com.trolltech.qt.gui.QFrame.Shape.StyledPanel'#g "${form}.jui"
	sed -i -e s#'QFrame::Raised'#'com.trolltech.qt.gui.QFrame.Shadow.Raised'#g "${form}.jui"
	sed -i -e s#'QDialogButtonBox::'#'com.trolltech.qt.gui.QDialogButtonBox.StandardButton.'#g "${form}.jui"
	sed -i -e s#'QProgressBar::TopToBottom'#'com.trolltech.qt.gui.QProgressBar.Direction.TopToBottom'#g "${form}.jui"
	sed -i -e s#'QDockWidget::'#'com.trolltech.qt.gui.QDockWidget.DockWidgetFeature.'#g "${form}.jui"
	sed -i -e s#'(bool)'#'(boolean)'#g "${form}.jui"

	juic.sh -p tetzlaff.ulf.app "${form}.jui"
	rm "${form}.jui"
done

cp tetzlaff/ulf/app/* ../src/tetzlaff/ulf/app/
rm -rf tetzlaff/
