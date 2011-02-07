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

package org.drools.guvnor.client.rulelist;

import org.drools.guvnor.client.common.GenericCallback;
import org.drools.guvnor.client.rpc.TableDataResult;

/**
 * This is used by the grid view to load data, in a paged fashion (if possible
 * to do paging).
 * 
 * @author Michael Neale
 * @deprecated in favor of {@link AbstractPagedTable}
 */
public interface AssetItemGridDataLoader {

    /**
     * This will be called by the grid when loading data, needs to know how to
     * skip, and how many rows to load up.
     * 
     * cb will return TableDataResult type.
     */
    void loadData(int startRow,
                  int numberOfRows,
                  GenericCallback<TableDataResult> cb);
}
