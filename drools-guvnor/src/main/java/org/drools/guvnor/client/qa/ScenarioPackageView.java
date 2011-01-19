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

package org.drools.guvnor.client.qa;

import org.drools.guvnor.client.common.AssetFormats;
import org.drools.guvnor.client.common.GenericCallback;
import org.drools.guvnor.client.common.LoadingPopup;
import org.drools.guvnor.client.common.PrettyFormLayout;
import org.drools.guvnor.client.explorer.ExplorerViewCenterPanel;
import org.drools.guvnor.client.messages.Constants;
import org.drools.guvnor.client.resources.Images;
import org.drools.guvnor.client.rpc.BulkTestRunResult;
import org.drools.guvnor.client.rpc.RepositoryServiceFactory;
import org.drools.guvnor.client.rpc.TableDataResult;
import org.drools.guvnor.client.rulelist.AssetItemGrid;
import org.drools.guvnor.client.rulelist.AssetItemGridDataLoader;
import org.drools.guvnor.client.rulelist.OpenItemCommand;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * This shows a list of scenarios in a package.
 * And allows them to be run in bulk.
 * @author Michael Neale
 */
public class ScenarioPackageView extends Composite {

    private Constants     constants = ((Constants) GWT.create( Constants.class ));
    private static Images images    = GWT.create( Images.class );

    private VerticalPanel layout;

    private AssetItemGrid grid;

    public ScenarioPackageView(final String packageUUID,
                               String packageName,
                               OpenItemCommand editEvent,
                               ExplorerViewCenterPanel centerPanel) {

        grid = new AssetItemGrid( editEvent,
                                  AssetItemGrid.RULE_LIST_TABLE_ID,
                                  new AssetItemGridDataLoader() {
                                      public void loadData(int startRow,
                                                           int numberOfRows,
                                                           GenericCallback<TableDataResult> cb) {
                                          RepositoryServiceFactory.getService().listAssets( packageUUID,
                                                                                            new String[]{AssetFormats.TEST_SCENARIO},
                                                                                            startRow,
                                                                                            numberOfRows,
                                                                                            AssetItemGrid.RULE_LIST_TABLE_ID,
                                                                                            cb );
                                      }
                                  } );

        layout = new VerticalPanel();
        layout.setWidth( "100%" );
        PrettyFormLayout pf = new PrettyFormLayout();

        VerticalPanel vert = new VerticalPanel();
        vert.add( new HTML( "<b>" + constants.ScenariosForPackage1() + "</b>" + packageName ) );
        Button run = new Button( constants.RunAllScenarios() );
        run.addClickHandler( new ClickHandler() {
            public void onClick(ClickEvent event) {
                runAllScenarios( packageUUID );
            }
        } );

        vert.add( run );

        pf.addHeader( images.scenarioLarge(),
                      vert );

        layout.add( pf );
        layout.add( grid );

        initWidget( layout );

    }

    private void refreshShowGrid() {
        layout.remove( 1 );
        layout.add( grid );
    }

    /**
     * Run all the scenarios, obviously !
     */
    private void runAllScenarios(String uuid) {
        LoadingPopup.showMessage( constants.BuildingAndRunningScenarios() );
        RepositoryServiceFactory.getService().runScenariosInPackage( uuid,
                                                                     new GenericCallback<BulkTestRunResult>() {
                                                                         public void onSuccess(BulkTestRunResult d) {
                                                                             BulkRunResultViewImpl view = new BulkRunResultViewImpl();
                                                                             BulkRunResult w = new BulkRunResult( view );

                                                                             w.setBulkTestRunResult( d );
                                                                             w.setCloseCommand( new Command() {
                                                                                 public void execute() {
                                                                                     refreshShowGrid();
                                                                                 }

                                                                             } );
                                                                             layout.remove( 1 );
                                                                             layout.add( view );
                                                                             LoadingPopup.close();
                                                                         }
                                                                     } );
    }

}
