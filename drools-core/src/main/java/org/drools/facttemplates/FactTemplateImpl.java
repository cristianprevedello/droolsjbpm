/*
 * Copyright 2002-2005 Peter Lin & RuleML.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://ruleml-dev.sourceforge.net/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.drools.facttemplates;

import java.util.Iterator;
import java.util.List;


/**
 * @author Peter Lin
 * Deftemplate is equivalent to CLIPS deftemplate<br/>
 * 
 * Some general design notes about the current implementation. In the
 * case where a class is declared to create the deftemplate, the order
 * of the slots are based on java Introspection. In the case where an
 * user declares the deftemplate from console or directly, the order
 * is the same as the string equivalent.
 * The current implementation does not address redeclaring a deftemplate
 * for a couple of reasons. The primary one is how does it affect the
 * existing RETE nodes. One possible approach is to always add new slots
 * to the end of the deftemplate and ignore the explicit order. Another
 * is to recompute the deftemplate, binds and all nodes. The second
 * approach is very costly and would make redeclaring a deftemplate
 * undesirable.
 */
public class FactTemplateImpl implements FactTemplate {
    private FieldTemplate[] fields;
    private String templateName = null;

    public FactTemplateImpl(String name, FieldTemplate[] fields){
        this.templateName = name;
        this.fields = fields;
    }
           
    /**
     * the template name is an alias for an object
     * @param name
     */
    public String getName(){
        return this.templateName;
    }

    /**
     * Return the number of slots in the deftemplate
     * @return
     */
    public int getNumberOfFields(){
        return this.fields.length;
    }
    
    /**
     * Return all the slots
     * @return
     */
    public FieldTemplate[] getAllFieldTemplates(){
        return this.fields;
    }
    
    /**
     * A convienance method for finding the slot matching
     * the String name.
     * @param name
     * @return
     */
    public FieldTemplate getFieldTemplate(String name) {
        for (int idx=0; idx < this.fields.length; idx++) {
            if (this.fields[idx].getName().equals(name)) {
                return this.fields[idx];
            }
        }
        return null;
    }
    
    /**
     * get the Slot at the given column id
     * @param id
     * @return
     */
    public FieldTemplate getFieldTemplate(int index) {
        return this.fields[index];
    }
    
    /**
     * Look up the column index of the slot
     * @param name
     * @return
     */
    public int getFieldTemplateIndex(String name) {
        for (int index=0; index < this.fields.length; index++) {
            if (this.fields[index].getName().equals(name)) {
                return index;
            }
        }
        return -1;
    }
    
    /**
     * Method takes a list of Slots and creates a deffact from it.
     * @param data
     * @param id
     * @return
     */
    public Fact createFact(long id) {
        return new FactImpl(this, id);
    }
    
    /**
     * Method will return a string format with the int type code
     * for the slot type
     */
    public String toString(){
        StringBuffer buf = new StringBuffer();
        buf.append("(" + this.templateName + " ");
//        for (int idx=0; idx < this.slots.length; idx++){
//            buf.append("(" + this.slots[idx].getName() + 
//                    " (type " + ConversionUtils.getTypeName(
//                            this.slots[idx].getValueType()) +
//                    ") ) ");
//        }
//        if (this.clazz != null){
//            buf.append("[" + this.clazz.getClassObject().getName() + "] ");
//        }
        buf.append(")");
        return buf.toString();
    }
    
}
