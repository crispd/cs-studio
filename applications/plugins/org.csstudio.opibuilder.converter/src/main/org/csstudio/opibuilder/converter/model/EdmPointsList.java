/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

import org.apache.log4j.Logger;

/**
 * Specific class representing EdmPointsList property.
 * 
 * @author Xihui Chen
 *
 */
public class EdmPointsList extends EdmAttribute {

	private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.parser.EdmPointsList");

	private int[] val;

	/**
	 * Constructor, which parses int property from EdmAttribute general interface.
	 * 
	 * @param genericAttribute	EdmAttribute containing int format data.
	 * @param required false if this attribute is optional, else true
	 * @throws EdmException	if data from EdmAttribute of invalid format.
	 */
	public EdmPointsList(EdmAttribute genericAttribute, boolean required) throws EdmException {
		super(genericAttribute);

		setRequired(required);

		if (genericAttribute == null || getValueCount() == 0) {
			if (isRequired()) {
				log.warn("Missing required property.");
			}
			else {
				log.warn("Missing optional property.");
				return;
			}
		}

		try {
			val = new int[getValueCount()];
			for(int i=0; i<getValueCount(); i++){
				val[i] = Integer.parseInt(getValue(i).split("\\s")[1]);
			}
			setInitialized(true);
			log.debug("Parsed " + this.getClass().getName() + 
					" = " + val);
		}
		catch (Exception e) {
			throw new EdmException(EdmException.INTEGER_FORMAT_ERROR,
			"Invalid integer format.", e);
		}

	}

	/**
	 * Returns the integer value.
	 * @return	Value of EdmInt instance.
	 */
	public int[] get() {
		return val;
	}
}
