/*************************************************************************\
 * Copyright (c) 2010  UChicago Argonne, LLC
 * This file is distributed subject to a Software License Agreement found
 * in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import java.util.List;

import org.csstudio.opibuilder.adl2boy.utilities.ColorUtilities;
import org.csstudio.opibuilder.model.AbstractContainerModel;
import org.csstudio.opibuilder.model.AbstractPVWidgetModel;
import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.csstudio.opibuilder.script.Expression;
import org.csstudio.opibuilder.script.PVTuple;
import org.csstudio.opibuilder.script.RuleData;
import org.csstudio.opibuilder.script.RulesInput;
import org.csstudio.opibuilder.util.OPIColor;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLBasicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLConnected;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLDynamicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.fileParser.widgets.ADLAbstractWidget;
import org.eclipse.swt.graphics.RGB;

/**
 * @author John Hammonds, Argonne National Laboratory
 * 
 */
public abstract class AbstractADL2Model {
	AbstractWidgetModel widgetModel;
	RGB colorMap[] = new RGB[0];
	protected String className = new String();

	public AbstractADL2Model(final ADLWidget adlWidget, RGB colorMap[],
			AbstractContainerModel parentModel) {
		this.colorMap = colorMap;
		makeModel(adlWidget, parentModel);
		processWidget(adlWidget);
	}

	public AbstractADL2Model(RGB colorMap[]) {
		this.colorMap = colorMap;
	}

	/**
	 * Does the work of converting the adlWidget into the AbstractWidgetModel
	 * 
	 * @param adlWidget
	 */
	abstract public void processWidget(ADLWidget adlWidget);

	/**
	 * Creates the widgetModel appropriate to the adlWidget. Adds the
	 * widgetModel as a child to the parentModel
	 * 
	 * @param adlWidget
	 * @param parentModel
	 */
	abstract public void makeModel(ADLWidget adlWidget,
			AbstractContainerModel parentModel);

	/**
	 * 
	 * @return
	 */
	public AbstractWidgetModel getWidgetModel() {
		return widgetModel;
	}

	/**
	 * set the properties contained in the ADL basic properties section in the
	 * created widgetModel
	 * 
	 * @param adlWidget
	 * @param widgetModel
	 */
	protected void setADLObjectProps(ADLAbstractWidget adlWidget,
			AbstractWidgetModel widgetModel) {
		if (adlWidget.hasADLObject()) {
			ADLObject adlObj = adlWidget.getAdlObject();
			widgetModel.setX(adlObj.getX());
			widgetModel.setY(adlObj.getY());
			widgetModel.setHeight(adlObj.getHeight());
			widgetModel.setWidth(adlObj.getWidth());
		}

	}

	/**
	 * set the properties contained in the ADL basic properties section in the
	 * created widgetModel
	 * 
	 * @param adlWidget
	 * @param widgetModel
	 */
	protected void setADLBasicAttributeProps(ADLAbstractWidget adlWidget,
			AbstractWidgetModel widgetModel, boolean colorForeground) {
		ADLBasicAttribute basAttr;
		if (adlWidget.hasADLBasicAttribute()) {
			basAttr = adlWidget.getAdlBasicAttribute();
		} else {
			basAttr = TranslatorUtils.getDefaultBasicAttribute();
			adlWidget.setAdlBasicAttribute(basAttr);
		}
		if (basAttr.isColorDefined()) {
			if (colorForeground) {
				setColor(basAttr.getClr(),
						AbstractWidgetModel.PROP_COLOR_FOREGROUND);
			} else {
				setColor(basAttr.getClr(),
						AbstractWidgetModel.PROP_COLOR_BACKGROUND);
			}
		} else {
			if (colorForeground) {
				setForegroundColorSameAsParent(widgetModel);
			} else {
				setBackgroundColorSameAsParent(widgetModel);
			}

		}

	}

	/**
	 * 
	 * @param adlWidget
	 * @param widgetModel
	 */
	protected void setADLDynamicAttributeProps(ADLAbstractWidget adlWidget,
			AbstractWidgetModel widgetModel) {
		ADLDynamicAttribute dynAttr;
		if (adlWidget.hasADLDynamicAttribute()) {
			dynAttr = adlWidget.getAdlDynamicAttribute();
		} else {
			dynAttr = TranslatorUtils.getDefaultDynamicAttribute();
			adlWidget.setAdlDynamicAttribute(dynAttr);
		}
		if (!(dynAttr.get_vis().equals("static"))) {
			if (dynAttr.get_chan() != null) {
				if (dynAttr.get_vis().equals("if not zero")) {
					RulesInput ruleInput = widgetModel.getRulesInput();
					List<RuleData> ruleData = ruleInput.getRuleDataList();
					RuleData newRule = new RuleData(widgetModel);
					PVTuple pvs = new PVTuple(dynAttr.get_chan(), true);
					newRule.addPV(pvs);
					newRule.addExpression(new Expression("pv0==0", false));
					ruleData.add(newRule);
					newRule.setName("Visibility");
					newRule.setPropId(AbstractWidgetModel.PROP_VISIBLE);
					widgetModel.setPropertyValue(
							AbstractWidgetModel.PROP_RULES, ruleData);
				} else if (dynAttr.get_vis().equals("if zero")) {
					RulesInput ruleInput = widgetModel.getRulesInput();
					List<RuleData> ruleData = ruleInput.getRuleDataList();
					RuleData newRule = new RuleData(widgetModel);
					PVTuple pvs = new PVTuple(dynAttr.get_chan(), true);
					newRule.addPV(pvs);
					newRule.addExpression(new Expression("!(pv0==0)", false));
					newRule.setName("Visibility");
					newRule.setPropId(AbstractWidgetModel.PROP_VISIBLE);
					ruleData.add(newRule);
					widgetModel.setPropertyValue(
							AbstractWidgetModel.PROP_RULES, ruleData);

				} else if (dynAttr.get_vis().equals("calc")) {
					// TODO Figure out calc option on dynamic attributes
					// AbstractADL2Model
				}
			}
		}

	}

	/**
	 * set the properties contained in the ADL basic properties section in the
	 * created widgetModel
	 * 
	 * @param adlWidget
	 * @param widgetModel
	 */
	protected void setADLControlProps(ADLAbstractWidget adlWidget,
			AbstractWidgetModel widgetModel) {
		if (adlWidget.hasADLControl()) {
			ADLConnected adlConnected = adlWidget.getAdlControl();
			setADLConnectedProps(widgetModel, adlConnected);
		}
	}

	/**
	 * set the properties contained in the ADL basic properties section in the
	 * created widgetModel
	 * 
	 * @param adlWidget
	 * @param widgetModel
	 */
	protected void setADLMonitorProps(ADLAbstractWidget adlWidget,
			AbstractWidgetModel widgetModel) {
		if (adlWidget.hasADLMonitor()) {
			ADLConnected adlConnected = adlWidget.getAdlMonitor();
			setADLConnectedProps(widgetModel, adlConnected);
		}
	}

	/**
	 * @param widgetModel
	 * @param adlConnected
	 */
	public void setADLConnectedProps(AbstractWidgetModel widgetModel,
			ADLConnected adlConnected) {

		if (adlConnected.isForeColorDefined()) {
			setColor(adlConnected.getForegroundColor(),
					AbstractWidgetModel.PROP_COLOR_FOREGROUND);
		} else {
			setForegroundColorSameAsParent(widgetModel);
		}
		if (adlConnected.isBackColorDefined()) {
			setColor(adlConnected.getBackgroundColor(),
					AbstractWidgetModel.PROP_COLOR_BACKGROUND);
		} else {
			setBackgroundColorSameAsParent(widgetModel);
		}
		String channel = adlConnected.getChan();
		if ((channel != null) && (!(channel.equals("")))) {
			widgetModel.setPropertyValue(AbstractPVWidgetModel.PROP_PVNAME,
					channel);
		}
	}

	/**
	 * @param widgetModel
	 */
	public void setBackgroundColorSameAsParent(AbstractWidgetModel widgetModel) {
		widgetModel.setPropertyValue(
				AbstractWidgetModel.PROP_COLOR_BACKGROUND,
				widgetModel.getParent().getPropertyValue(
						AbstractWidgetModel.PROP_COLOR_BACKGROUND));
	}

	/**
	 * @param widgetModel
	 */
	public void setForegroundColorSameAsParent(AbstractWidgetModel widgetModel) {
		widgetModel.setPropertyValue(
				AbstractWidgetModel.PROP_COLOR_FOREGROUND,
				widgetModel.getParent().getPropertyValue(
						AbstractWidgetModel.PROP_COLOR_FOREGROUND));
	}

	/**
	 * @param displayForeColor
	 * @param propertyName
	 */
	public void setColor(int displayForeColor, String propertyName) {
		OPIColor color;
		color = ColorUtilities
				.matchToTableColor(this.colorMap[displayForeColor]);
		widgetModel.setPropertyValue(propertyName, color);
	}
}
