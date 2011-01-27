/*
 * Copyright 2011 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.drools.guvnor.client.decisiontable.widget;

import org.drools.guvnor.client.widgets.decoratedgrid.HasRows;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;

/**
 * Simple container for controls to manipulate a Decision Table
 * 
 * @author manstis
 * 
 */
public class DecisionTableControlsWidget extends Composite {

	private Panel panel = new HorizontalPanel();

	public DecisionTableControlsWidget(final VerticalDecisionTableWidget dtable) {

		// Add row button
		Button btnAddRow = new Button("Add Row", new ClickHandler() {

			public void onClick(ClickEvent event) {
				dtable.appendRow();
			}
		});
		panel.add(btnAddRow);
		
	      // Add redraw header button
        Button btnRedrawHeader = new Button("RH", new ClickHandler() {

            public void onClick(ClickEvent event) {
                dtable.redrawHeader();
            }
        });
        panel.add(btnRedrawHeader);

		initWidget(panel);

	}

}
