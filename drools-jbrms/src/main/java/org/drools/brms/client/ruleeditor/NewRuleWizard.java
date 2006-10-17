package org.drools.brms.client.ruleeditor;

import org.drools.brms.client.categorynav.CategoryExplorerWidget;
import org.drools.brms.client.categorynav.CategorySelectHandler;
import org.drools.brms.client.common.ErrorPopup;
import org.drools.brms.client.common.RulePackageSelector;
import org.drools.brms.client.common.WarningPopup;
import org.drools.brms.client.rpc.RepositoryServiceFactory;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * This provides a popup for creating a new rule from scratch.
 * reuses a few other widgets.
 */
public class NewRuleWizard extends PopupPanel {

    private FlexTable              table       = new FlexTable(); //Using this table for the form layout
    private TextBox                name        = new TextBox();
    private TextArea               description = new TextArea();
    private String                 initialCategory;
    private CategoryExplorerWidget catChooser  = new CategoryExplorerWidget( new CategorySelectHandler() {
                                                   public void selected(String selectedPath) {
                                                       initialCategory = selectedPath;
                                                   }
                                               }, false );
    private RulePackageSelector packageSelector = new RulePackageSelector();

    /** This is used when creating a new rule. */
    public NewRuleWizard() {
        super( true );
        
        super.setWidth( "60%" );
        table.setWidth( "100%" );
        name.setWidth( "100%" );
        
        table.setWidget( 0,
                         0,
                         new Image( "images/new_wiz.gif" ) );
        table.setWidget( 0,
                         1,
                         new Label( "Create a new rule" ) );        

        table.setWidget( 1,
                         0,
                         new Label( "Rule name" ) );
        table.setWidget( 1,
                         1,
                         name );
        
        table.setWidget( 2, 0, new Label("Initial category") );
        table.setWidget( 2, 1, catChooser );
        
        table.setWidget( 3, 0, new Label("Package") );
        table.setWidget( 3, 1, packageSelector );

        description.setVisibleLines( 4 );
        description.setWidth( "100%" );
        table.setWidget( 4,
                         0,
                         new Label( "Initial Description" ) );
        table.setWidget( 4,
                         1,
                         description );

        Button ok = new Button( "OK" );
        ok.addClickListener( new ClickListener() {
            public void onClick(Widget arg0) {
                ok();
            }

        } );

        table.setWidget( 5,
                         0,
                         ok );

        Button cancel = new Button( "Cancel" );
        cancel.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                cancel();
            }

        } );

        table.setWidget( 5,
                         1,
                         cancel );

        add( table );
        setStyleName( "ks-popups-Popup" );
    }

    /**
     * When OK is pressed, it will update the repository with the new rule.
     */
    void ok() {
        
        if (this.initialCategory == null) {            
            WarningPopup.showMessage( "You have to pick an initial category.", this.getAbsoluteLeft(), this.getAbsoluteTop() );
            return;
        } else if (this.name.getText() == null || "".equals( this.name.getText() )) {
            WarningPopup.showMessage( "Rule must have a name", this.getAbsoluteLeft(), this.getAbsoluteTop() );
            return;
        }
        
        AsyncCallback cb = new AsyncCallback() {

            public void onFailure(Throwable err) {
                ErrorPopup.showMessage( err.getMessage() );
            }

            public void onSuccess(Object result) {
                if ( ((Boolean) result).booleanValue() ) {
                    hide();
                } else {
                    ErrorPopup.showMessage( "Unable to create the rule." );
                }
            }
        };

        if ( this.name.equals( "" ) ) {
            ErrorPopup.showMessage( "Can't have an empty category name." );
        } else {
            RepositoryServiceFactory.getService().createNewRule( name.getText(),
                                                              description.getText(),
                                                              initialCategory,
                                                              packageSelector.getSelectedPackage(),
                                                              cb );

        }
    }

    void cancel() {
        hide();
    }

}
