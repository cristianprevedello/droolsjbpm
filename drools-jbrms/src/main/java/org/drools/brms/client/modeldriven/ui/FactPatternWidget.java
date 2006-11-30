package org.drools.brms.client.modeldriven.ui;

import org.drools.brms.client.modeldriven.SuggestionCompletionEngine;
import org.drools.brms.client.modeldriven.model.ConnectiveConstraint;
import org.drools.brms.client.modeldriven.model.Constraint;
import org.drools.brms.client.modeldriven.model.FactPattern;
import org.drools.brms.client.modeldriven.model.IPattern;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * This is the new smart widget that works off the model.
 * @author Michael Neale
 *
 */
public class FactPatternWidget extends Composite {

    private FactPattern pattern;
    private FlexTable layout = new FlexTable();
    private SuggestionCompletionEngine completions;
    
    
    public FactPatternWidget(IPattern p, SuggestionCompletionEngine com) {
        this.pattern = (FactPattern) p;
        this.completions = com;
        layout.setWidget( 0, 0, getPatternLabel() );
        
        final FlexTable inner = new FlexTable();
        
        layout.setWidget( 0, 1, inner );
        
        for ( int row = 0; row < pattern.constraints.length; row++ ) {
            final Constraint c = pattern.constraints[row];
            final int currentRow = row;
            inner.setWidget( row, 0, fieldDropDown(c, new Command() {
                public void execute() {
                    inner.setWidget( currentRow, 1, operatorDropDown( c ));
                }
                
            }) );
            inner.setWidget( row, 1, operatorDropDown(c) );
            inner.setWidget( row, 2, valueEditor(c) );            
            inner.setWidget( row, 3, connectives(c) );

        }
        layout.setStyleName( "model-builder-Background" );
        initWidget( layout );
        
        
    }


    private Label getPatternLabel() {
        if (pattern.boundName != null) {
            return new Label(pattern.factType + " [" + pattern.boundName + "]" );
        } else {
            return new Label(pattern.factType);
        }
    }


    private Widget connectives(Constraint c) {
        if (c.connectives != null && c.connectives.length > 0) {
            HorizontalPanel horiz = new HorizontalPanel();
            for ( int i = 0; i < c.connectives.length; i++ ) {
                ConnectiveConstraint con = c.connectives[i];
                horiz.add( connectiveOperatorDropDown(con, c.fieldName) );
                horiz.add( connectiveValueEditor(con) );
            }        
            return horiz;
        } else {
            //nothing to do
            return null;
        }
        
    }


    private Widget connectiveValueEditor(final ConnectiveConstraint con) {
        
        final TextBox box = new TextBox();
        box.setVisibleLength( 4 );
        box.setText( con.value );
        box.addChangeListener( new ChangeListener() {
            public void onChange(Widget w) {
                con.value = box.getText();                
            }            
        });
        return box;
    }


    private Widget connectiveOperatorDropDown(final ConnectiveConstraint con, String fieldName) {
        String[] ops = completions.getConnectiveOperatorCompletions( pattern.factType, fieldName );
        final ListBox box = new ListBox();
        for ( int i = 0; i < ops.length; i++ ) {
            box.addItem( ops[i] );
            if (ops[i].equals( con.operator )) {
                box.setSelectedIndex( i );
            }

        }
        
        box.addChangeListener( new ChangeListener() {
            public void onChange(Widget w) {
                con.operator = box.getItemText( box.getSelectedIndex() );                
            }            
        });
        
        return box;
    }


    private Widget valueEditor(final Constraint c) {
        final TextBox box = new TextBox();
        box.setText( c.value );
        box.addChangeListener( new ChangeListener() {
            public void onChange(Widget w) {
                c.value = box.getText();                
            }
            
        });
        return box;
    }


    private Widget operatorDropDown(final Constraint c) {
        String[] ops = completions.getOperatorCompletions( pattern.factType, c.fieldName );
        final ListBox box = new ListBox();
        for ( int i = 0; i < ops.length; i++ ) {
            box.addItem( ops[i] );
            if (ops[i].equals( c.operator )) {
                box.setSelectedIndex( i );
            }

        }
        
        box.addChangeListener( new ChangeListener() {
            public void onChange(Widget w) {
                c.operator = box.getItemText( box.getSelectedIndex() );                
            }            
        });
        
        return box;
    }


    private Widget fieldDropDown(final Constraint con, final Command onChange) {
        HorizontalPanel horiz = new HorizontalPanel();

        final ListBox box = new ListBox();
        
        String[] com = completions.getFieldCompletions( this.pattern.factType );
        for ( int i = 0; i < com.length; i++ ) {
            box.addItem( com[i] );
            if (com[i].equals( con.fieldName )) {
                box.setSelectedIndex( i );
            }
        }
        
        box.addChangeListener( new ChangeListener() {

            public void onChange(Widget w) {
                con.fieldName = box.getItemText( box.getSelectedIndex() );
                onChange.execute();
            }
            
        });
        horiz.add( box );
        
        if (con.fieldBinding != null) {
            horiz.add( new Label( "[" + con.fieldBinding + "]") );
        }
        
        return horiz;
    }
    
}
