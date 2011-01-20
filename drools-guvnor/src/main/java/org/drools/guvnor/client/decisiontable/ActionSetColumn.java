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

package org.drools.guvnor.client.decisiontable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.drools.guvnor.client.common.FormStylePopup;
import org.drools.guvnor.client.common.ImageButton;
import org.drools.guvnor.client.common.InfoPopup;
import org.drools.guvnor.client.common.SmallLabel;
import org.drools.guvnor.client.decisiontable.widget.DecisionTableWidget;
import org.drools.guvnor.client.messages.Constants;
import org.drools.guvnor.client.resources.Images;
import org.drools.ide.common.client.modeldriven.SuggestionCompletionEngine;
import org.drools.ide.common.client.modeldriven.dt.ActionCol;
import org.drools.ide.common.client.modeldriven.dt.ActionSetFieldCol;
import org.drools.ide.common.client.modeldriven.dt.ConditionCol;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class ActionSetColumn extends FormStylePopup {

	private Constants constants = GWT.create(Constants.class);
	private static Images images = (Images) GWT.create(Images.class);

	private ActionSetFieldCol editingCol;
	private SmallLabel bindingLabel = new SmallLabel();
	private TextBox fieldLabel = getFieldLabel();
	private DecisionTableWidget dtable;
	private SuggestionCompletionEngine sce;

	public ActionSetColumn(SuggestionCompletionEngine sce,
			final DecisionTableWidget dtable, final Command refreshGrid,
			final ActionSetFieldCol col, final boolean isNew) {
		this.editingCol = new ActionSetFieldCol();
		this.dtable = dtable;
		this.sce = sce;

		editingCol.setBoundName(col.getBoundName());
		editingCol.setFactField(col.getFactField());
		editingCol.setHeader(col.getHeader());
		editingCol.setType(col.getType());
		editingCol.setValueList(col.getValueList());
		editingCol.setUpdate(col.isUpdate());
		editingCol.setDefaultValue(col.getDefaultValue());
		editingCol.setHideColumn(col.isHideColumn());

		super.setModal(false);
		setTitle(constants.ColumnConfigurationSetAFieldOnAFact());

		HorizontalPanel pattern = new HorizontalPanel();
		pattern.add(bindingLabel);
		doBindingLabel();

		Image changePattern = new ImageButton(images.edit(),
				constants.ChooseABoundFactThatThisColumnPertainsTo(),
				new ClickHandler() {
					public void onClick(ClickEvent w) {
						showChangeFact(w);
					}
				});
		pattern.add(changePattern);
		addAttribute(constants.Fact(), pattern);

		HorizontalPanel field = new HorizontalPanel();
		field.add(fieldLabel);
		Image editField = new ImageButton(images.edit(),
				constants.EditTheFieldThatThisColumnOperatesOn(),
				new ClickHandler() {
					public void onClick(ClickEvent w) {
						showFieldChange();
					}
				});
		field.add(editField);
		addAttribute(constants.Field(), field);
		doFieldLabel();

		final TextBox valueList = new TextBox();
		valueList.setText(editingCol.getValueList());
		valueList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				editingCol.setValueList(valueList.getText());
			}
		});
		HorizontalPanel vl = new HorizontalPanel();
		vl.add(valueList);
		vl.add(new InfoPopup(constants.ValueList(), constants
				.ValueListsExplanation()));
		addAttribute(constants.optionalValueList(), vl);

		final TextBox header = new TextBox();
		header.setText(col.getHeader());
		header.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				editingCol.setHeader(header.getText());
			}
		});
		addAttribute(constants.ColumnHeaderDescription(), header);

		addAttribute(constants.UpdateEngineWithChanges(), doUpdate());

		addAttribute(constants.DefaultValue(),
				GuidedDTColumnConfig.getDefaultEditor(editingCol));

		Button apply = new Button(constants.ApplyChanges());
		apply.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent w) {
				if (null == editingCol.getHeader()
						|| "".equals(editingCol.getHeader())) {
					Window.alert(constants
							.YouMustEnterAColumnHeaderValueDescription());
					return;
				}
				if (isNew) {
					if (!unique(editingCol.getHeader())) {
						Window.alert(constants
								.ThatColumnNameIsAlreadyInUsePleasePickAnother());
						return;
					}
					dtable.addColumn(editingCol);
					dtable.getModel().getActionCols().add(editingCol);

				} else {
					if (!col.getHeader().equals(editingCol.getHeader())) {
						if (!unique(editingCol.getHeader())) {
							Window.alert(constants
									.ThatColumnNameIsAlreadyInUsePleasePickAnother());
							return;
						}
					}

					col.setBoundName(editingCol.getBoundName());
					col.setFactField(editingCol.getFactField());
					col.setHeader(editingCol.getHeader());
					col.setType(editingCol.getType());
					col.setValueList(editingCol.getValueList());
					col.setUpdate(editingCol.isUpdate());
					col.setDefaultValue(editingCol.getDefaultValue());
					col.setHideColumn(editingCol.isHideColumn());
				}
				refreshGrid.execute();
				hide();

			}
		});
		addAttribute("", apply);

	}

	private boolean unique(String header) {
		for (ActionCol o : dtable.getModel().getActionCols()) {
			if (o.getHeader().equals(header))
				return false;
		}
		return true;
	}

	private Widget doUpdate() {
		HorizontalPanel hp = new HorizontalPanel();

		final CheckBox cb = new CheckBox();
		cb.setEnabled(editingCol.isUpdate());
		cb.setText("");
		cb.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent arg0) {
				if (sce.isGlobalVariable(editingCol.getBoundName())) {
					cb.setEnabled(false);
					editingCol.setUpdate(false);
				} else {
					editingCol.setUpdate(cb.isEnabled());
				}
			}
		});
		hp.add(cb);
		hp.add(new InfoPopup(constants.UpdateFact(), constants
				.UpdateDescription()));
		return hp;
	}

	private TextBox getFieldLabel() {
		final TextBox box = new TextBox();
		box.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				editingCol.setFactField(box.getText());
			}
		});
		return box;
	}

	private void showFieldChange() {
		final FormStylePopup pop = new FormStylePopup();
		pop.setModal(false);

		final String factType = getFactType();
		String[] fields = this.sce.getFieldCompletions(factType);
		final ListBox box = new ListBox();
		for (int i = 0; i < fields.length; i++) {
			box.addItem(fields[i]);
		}
		pop.addAttribute(constants.Field(), box);
		Button b = new Button(constants.OK());
		pop.addAttribute("", b);
		b.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent w) {
				editingCol.setFactField(box.getItemText(box.getSelectedIndex()));
				editingCol.setType(sce.getFieldType(factType,
						editingCol.getFactField()));
				doFieldLabel();
				pop.hide();
			}
		});
		pop.show();

	}

	private String getFactType() {
		if (sce.isGlobalVariable(editingCol.getBoundName())) {
			return sce.getGlobalVariable(editingCol.getBoundName());
		}
		return getFactType(this.editingCol.getBoundName());
	}

	private void doFieldLabel() {
		if (this.editingCol.getFactField() != null) {
			this.fieldLabel.setText(this.editingCol.getFactField());
		} else {
			this.fieldLabel.setText(constants.pleaseChooseAFactPatternFirst());
		}
	}

	private String getFactType(String boundName) {
		for (Iterator<ConditionCol> iterator = dtable.getModel()
				.getConditionCols().iterator(); iterator.hasNext();) {
			ConditionCol col = (ConditionCol) iterator.next();
			if (col.getBoundName().equals(boundName)) {
				return col.getFactField();
			}
		}
		return "";
	}

	private void showChangeFact(ClickEvent w) {
		final FormStylePopup pop = new FormStylePopup();

		final ListBox pats = this.loadBoundFacts();
		pop.addAttribute(constants.ChooseFact(), pats);
		Button ok = new Button(constants.OK());
		pop.addAttribute("", ok);

		ok.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent w) {
				String val = pats.getValue(pats.getSelectedIndex());
				editingCol.setBoundName(val);
				doBindingLabel();
				pop.hide();
			}
		});

		pop.show();

	}

	private ListBox loadBoundFacts() {
		Set<String> facts = new HashSet<String>();
		for (int i = 0; i < this.dtable.getModel().getConditionCols().size(); i++) {
			ConditionCol c = (ConditionCol) dtable.getModel()
					.getConditionCols().get(i);
			facts.add(c.getBoundName());
		}

		ListBox box = new ListBox();
		for (Iterator<String> iterator = facts.iterator(); iterator.hasNext();) {
			String b = (String) iterator.next();
			box.addItem(b);
		}

		String[] globs = this.sce.getGlobalVariables();
		for (int i = 0; i < globs.length; i++) {
			box.addItem(globs[i]);
		}

		return box;
	}

	private void doBindingLabel() {
		if (this.editingCol.getBoundName() != null) {
			this.bindingLabel.setText("" + this.editingCol.getBoundName());
		} else {
			this.bindingLabel.setText(constants
					.pleaseChooseABoundFactForThisColumn());
		}
	}

}
