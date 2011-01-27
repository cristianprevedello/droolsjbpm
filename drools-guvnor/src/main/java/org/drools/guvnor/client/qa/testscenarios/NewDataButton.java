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

package org.drools.guvnor.client.qa.testscenarios;

import java.util.List;

import org.drools.guvnor.client.common.SmallLabel;
import org.drools.guvnor.client.resources.Images;
import org.drools.guvnor.client.util.Format;
import org.drools.ide.common.client.modeldriven.testing.ActivateRuleFlowGroup;
import org.drools.ide.common.client.modeldriven.testing.ExecutionTrace;
import org.drools.ide.common.client.modeldriven.testing.FactData;
import org.drools.ide.common.client.modeldriven.testing.Fixture;
import org.drools.ide.common.client.modeldriven.testing.RetractFact;
import org.drools.ide.common.client.modeldriven.testing.Scenario;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.TextBox;

/**
 * 
 * This button gives a choice of modifying data, based on the positional context.
 *     
 * @author rikkola
 *
 */
public class NewDataButton extends TestScenarioButton {

    private static Images        images = GWT.create( Images.class );

    private final ExecutionTrace currentEx;

    public NewDataButton(final ExecutionTrace previousEx,
                         final Scenario scenario,
                         final ExecutionTrace currentEx,
                         ScenarioWidget scenarioWidget) {
        super( images.newItem(),
               constants.AddANewDataInputToThisScenario(),
               previousEx,
               scenario,
               scenarioWidget );

        this.currentEx = currentEx;
    }

    @Override
    protected TestScenarioButtonPopup getPopUp() {
        return new NewInputPopup();
    }

    class NewInputPopup extends TestScenarioButtonPopup {
        public NewInputPopup() {
            super( images.ruleAsset(),
                   constants.NewInput() );

            addAttribute( constants.InsertANewFact1(),
                          new InsertFactPanel() );

            List<String> varsInScope = scenario.getFactNamesInScope( currentEx,
                                                                     false );
            //now we do modifies & retracts
            if ( varsInScope.size() > 0 ) {
                addAttribute( constants.ModifyAnExistingFactScenario(),
                              new ModifyFactPanel( varsInScope ) );

                addAttribute( constants.RetractAnExistingFactScenario(),
                              new ExtractFactPanel( varsInScope ) );

            }

            addAttribute( constants.ActivateRuleFlowGroup(),
                          new ActivateRuleFlowPanel() );
        }

        class ActivateRuleFlowPanel extends BasePanel<TextBox> {

            @Override
            public TextBox getWidget() {
                return new TextBox();
            }

            @Override
            public Fixture getFixture() {
                return new ActivateRuleFlowGroup( valueWidget.getText() );
            }
        }

        class ExtractFactPanel extends ListBoxBasePanel {

            public ExtractFactPanel(List<String> listItems) {
                super( listItems );
            }

            @Override
            public Fixture getFixture() {
                String factName = valueWidget.getItemText( valueWidget.getSelectedIndex() );

                return new RetractFact( factName );
            }
        }

        class ModifyFactPanel extends ListBoxBasePanel {

            public ModifyFactPanel(List<String> listItems) {
                super( listItems );
            }

            @Override
            public Fixture getFixture() {
                String factName = valueWidget.getItemText( valueWidget.getSelectedIndex() );
                String type = scenario.getVariableTypes().get( factName );

                return new FactData( type,
                                     factName,
                                     true );

            }
        }

        class InsertFactPanel extends ListBoxBasePanel {
            TextBox factNameTextBox;

            public InsertFactPanel() {
                super( suggestionCompletionEngine.getFactTypes() );
            }

            @Override
            protected void addAddButtonClickHandler() {
                add.addClickHandler( new ClickHandler() {

                    public void onClick(ClickEvent event) {
                        String factName = ("" + factNameTextBox.getText()).trim();
                        if ( factName.equals( "" ) || factNameTextBox.getText().indexOf( ' ' ) > -1 ) {
                            Window.alert( constants.YouMustEnterAValidFactName() );
                        } else {
                            if ( scenario.isFactNameReserved( factName ) ) {
                                Window.alert( Format.format( constants.TheFactName0IsAlreadyInUsePleaseChooseAnotherName(),
                                                             factName ) );
                            } else {
                                scenario.insertBetween( previousEx,
                                                        getFixture() );
                                parent.renderEditor();
                                hide();
                            }
                        }
                    }
                } );

            }

            @Override
            protected void initWidgets() {
                factNameTextBox = new TextBox();
                factNameTextBox.setVisibleLength( 5 );

                add( valueWidget );
                add( new SmallLabel( constants.FactName() ) );
                add( factNameTextBox );
                add( add );
            }

            @Override
            public Fixture getFixture() {
                return new FactData( valueWidget.getItemText( valueWidget.getSelectedIndex() ),
                                     factNameTextBox.getText(),
                                     false );
            }
        }
    }

}
