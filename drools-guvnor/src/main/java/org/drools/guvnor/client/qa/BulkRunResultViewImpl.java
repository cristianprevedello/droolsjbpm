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

import org.drools.guvnor.client.common.SmallLabel;
import org.drools.guvnor.client.messages.Constants;
import org.drools.guvnor.client.packages.PackageBuilderWidget;
import org.drools.guvnor.client.rpc.BuilderResult;
import org.drools.guvnor.client.rpc.ScenarioResultSummary;
import org.drools.guvnor.client.rulelist.OpenItemCommand;
import org.drools.guvnor.client.util.Format;
import org.drools.guvnor.client.util.PercentageBar;
import org.drools.guvnor.client.util.ToggleLabel;
import org.drools.guvnor.client.util.ValueList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This presents the results of a bulk run.
 * @author Michael Neale
 */
public class BulkRunResultViewImpl extends Composite
    implements
    BulkRunResultView {

    interface BulkRunResultViewImplBinder
        extends
        UiBinder<Widget, BulkRunResultViewImpl> {
    }

    private static BulkRunResultViewImplBinder uiBinder  = GWT.create( BulkRunResultViewImplBinder.class );

    private Constants                          constants = GWT.create( Constants.class );

    private Presenter                          presenter;

    @UiField
    ToggleLabel                                overAll;

    @UiField
    PercentageBar                              resultsBar;

    @UiField
    SmallLabel                                 failuresOutOfExpectations;

    @UiField
    PercentageBar                              coveredPercentBar;

    @UiField
    SmallLabel                                 ruleCoveragePercent;

    @UiField
    ValueList                                  uncoveredRules;

    @UiField(provided = true)
    SummaryTableView                           summaryTableView;

    private SummaryTable                       summaryTable;

    public BulkRunResultViewImpl() {
        summaryTableView = new SummaryTableViewImpl();
        summaryTable = new SummaryTable( summaryTableView );
        initWidget( uiBinder.createAndBindUi( this ) );
    }

    @UiHandler("closeButton")
    void close(ClickEvent clickEvent) {
        presenter.onClose();
    }

    public void addSummary(ScenarioResultSummary summary) {
        summaryTable.addRow( summary );
    }

    public void showErrors(BuilderResult errors,
                           OpenItemCommand editEvent) {

        Panel err = new SimplePanel();

        PackageBuilderWidget.showBuilderErrors( errors,
                                                err,
                                                editEvent );
    }

    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    public void setFailed() {
        overAll.setValue( false );
    }

    public void setSuccess() {
        overAll.setValue( true );
    }

    public void setFailuresOutOfExpectation(int totalFailures,
                                            int grandTotal) {

        failuresOutOfExpectations.setText( Format.format( constants.failuresOutOFExpectations(),
                                                          totalFailures,
                                                          grandTotal ) );
    }

    public void setResultsPercent(int i) {
        resultsBar.setValue( i );
    }

    public void setRulesCoveredPercent(int percentCovered) {
        coveredPercentBar.setValue( percentCovered );
        ruleCoveragePercent.setText( Format.format( constants.RuleCoveragePercent(),
                                                    percentCovered ) );
    }

    public void addUncoveredRules(String uncoveredRule) {
        uncoveredRules.addItem( uncoveredRule );
    }
}
