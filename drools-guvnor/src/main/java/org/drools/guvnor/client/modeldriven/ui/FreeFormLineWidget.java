/*
 * Copyright 2005 JBoss Inc
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

package org.drools.guvnor.client.modeldriven.ui;

import org.drools.guvnor.client.common.DirtyableFlexTable;
import org.drools.guvnor.client.messages.Constants;
import org.drools.ide.common.client.modeldriven.brl.FreeFormLine;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Free form DRL line widget
 * @author esteban.aliverti@gmail.com
 *
 */
public class FreeFormLineWidget extends RuleModellerWidget {

    private FreeFormLine action;
    private DirtyableFlexTable layout = new DirtyableFlexTable();
    private Constants constants = ((Constants) GWT.create(Constants.class));
    private boolean readOnly;

    public FreeFormLineWidget(RuleModeller mod, FreeFormLine p) {
        this(mod, p, null);
    }

    /**
     * Creates a new FactPatternWidget
     * @param mod
     * @param p
     * @param readOnly if the widget should be in RO mode. If this parameter
     * is null, the readOnly attribute is calculated.
     */
    public FreeFormLineWidget(RuleModeller mod, FreeFormLine p,
            Boolean readOnly) {
        super(mod);
        this.action = p;

        if (readOnly == null) {
            this.readOnly = false;
        } else {
            this.readOnly = readOnly;
        }

        layout.setWidget(0, 0, createTextBox());
        FlexCellFormatter formatter = layout.getFlexCellFormatter();
        formatter.setAlignment(0, 0, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_BOTTOM);

        if (this.readOnly) {
            this.layout.addStyleName("editor-disabled-widget");
        }

        initWidget(layout);

    }

    private Widget createTextBox() {
        final TextBox tb = new TextBox();
        tb.setText(this.action.text);
        tb.setTitle(constants.ThisIsADrlExpressionFreeForm());

        if (!this.readOnly) {
        	tb.addChangeHandler(new ChangeHandler() {
				
				public void onChange(ChangeEvent event) {
					  setModified(true);
	                    action.text = tb.getText();
					
				}
			});

            
        } else {
            tb.setEnabled(false);
        }
        return tb;
    }

    @Override
    public boolean isDirty() {
        return layout.hasDirty();
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly;
    }
}
