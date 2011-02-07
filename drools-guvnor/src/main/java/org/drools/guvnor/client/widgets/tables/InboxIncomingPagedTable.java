/*
 * Copyright 2011 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.drools.guvnor.client.widgets.tables;

import org.drools.guvnor.client.rpc.InboxIncomingPageRow;
import org.drools.guvnor.client.rpc.InboxPageRow;
import org.drools.guvnor.client.rulelist.OpenItemCommand;

import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;

/**
 * Widget with a table of inbox entries results.
 * 
 * @author manstis
 */
public class InboxIncomingPagedTable extends InboxPagedTable {

    public InboxIncomingPagedTable(
                                   final String inboxName,
                                   OpenItemCommand editEvent) {
        super( inboxName,
               editEvent );
    }

    @Override
    protected void addAncillaryColumns(ColumnPicker<InboxPageRow> columnPicker,
                                       SortableHeaderGroup<InboxPageRow> sortableHeaderGroup) {

        // Include general "Inbox" columns
        super.addAncillaryColumns( columnPicker,
                                   sortableHeaderGroup );

        Column<InboxPageRow, String> fromColumn = new TextColumn<InboxPageRow>() {
            public String getValue(InboxPageRow row) {
                return ((InboxIncomingPageRow) row).getFrom();
            }
        };
        columnPicker.addColumn( fromColumn,
                                    new SortableHeader<InboxPageRow, String>(
                                                                                      sortableHeaderGroup,
                                                                                      constants.From(),
                                                                                      fromColumn ),
                                    true );
    }

}
