/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.ide.common.client.modeldriven.dt;

public class ActionInsertFactCol extends ActionCol {

	private static final long serialVersionUID = 510l;

	/**
	 * The fact type (class) that is to be created. eg Driver, Person, Cheese.
	 */
	private String factType;

	/**
	 * The bound name of the variable to be effected. If the same name appears
	 * twice, is it merged into the same action.
	 */
	private String boundName;

	/**
	 * The field on the fact being effected.
	 */
	private String factField;

	/**
	 * Same as the type in ActionFieldValue - eg, either a String, or Numeric.
	 * Refers to the data type of the literal value in the cell. Refer to the
	 * types in SuggestionCompletionEngine.
	 */
	private String type;

	/**
	 * An optional comman separated list of values.
	 */
	private String valueList;

	public void setFactType(String factType) {
		this.factType = factType;
	}

	public String getFactType() {
		return factType;
	}

	public void setBoundName(String boundName) {
		this.boundName = boundName;
	}

	public String getBoundName() {
		return boundName;
	}

	public void setFactField(String factField) {
		this.factField = factField;
	}

	public String getFactField() {
		return factField;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setValueList(String valueList) {
		this.valueList = valueList;
	}

	public String getValueList() {
		return valueList;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ActionInsertFactCol)) {
			return false;
		}
		ActionInsertFactCol that = (ActionInsertFactCol) obj;
		return nullOrEqual(this.factType, that.factType)
				&& nullOrEqual(this.boundName, that.boundName)
				&& nullOrEqual(this.factField, that.factField)
				&& nullOrEqual(this.type, that.type)
				&& nullOrEqual(this.valueList, that.valueList)
				&& super.equals(obj);
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + (factType == null ? 0 : factType.hashCode());
		hash = hash * 31 + (boundName == null ? 0 : boundName.hashCode());
		hash = hash * 31 + (factField == null ? 0 : factField.hashCode());
		hash = hash * 31 + (type == null ? 0 : type.hashCode());
		hash = hash * 31 + (valueList == null ? 0 : valueList.hashCode());
		hash = hash * 31 + super.hashCode();
		return hash;
	}

	private boolean nullOrEqual(Object thisAttr, Object thatAttr) {
		if (thisAttr == null && thatAttr == null) {
			return true;
		}
		if (thisAttr == null && thatAttr != null) {
			return false;
		}
		return thisAttr.equals(thatAttr);
	}

}
