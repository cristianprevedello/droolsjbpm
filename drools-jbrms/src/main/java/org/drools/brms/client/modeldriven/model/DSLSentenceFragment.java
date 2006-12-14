package org.drools.brms.client.modeldriven.model;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This holds the value of a fragment of a DSL.
 * It can either be a sentence fragment (a display only thing) or a 
 * value which the user can change.
 *  
 * @author Michael Neale
 *
 */
public class DSLSentenceFragment
    implements
    IsSerializable {

    public String  value;
    public boolean isEditableField;

    /**
     * @param fragment
     * @param isEditableField true if it is a editable value. false means display only.
     */
    public DSLSentenceFragment(String fragment,
                               boolean isEditableField) {
        value = fragment;
        this.isEditableField = isEditableField;
    }

}
