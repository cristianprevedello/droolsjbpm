package org.drools.decisiontable.parser;

/*
 * Copyright 2005 JBoss Inc
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.drools.decisiontable.model.Condition;
import org.drools.decisiontable.model.Consequence;
import org.drools.decisiontable.model.Duration;
import org.drools.decisiontable.model.Global;
import org.drools.decisiontable.model.Import;
import org.drools.decisiontable.model.Package;
import org.drools.decisiontable.model.Rule;
import org.drools.decisiontable.parser.xls.PropertiesSheetListener;

/**
 * @author <a href="mailto:shaun.addison@gmail.com"> Shaun Addison </a><a
 *         href="mailto:michael.neale@gmail.com"> Michael Neale </a>
 * 
 * Define a ruleset spreadsheet which contains one or more decision tables.
 * 
 * Stay calm, deep breaths... this is a little bit scary, its where it all
 * happens.
 * 
 * A table is identifed by a cell beginning with the text "RuleTable". The first
 * row after the table identifier defines the column type: either a condition
 * ("C") or consequence ("A" for action), and so on.
 * 
 * The second row identifies the java code block associated with the condition
 * or consequence. This code block will include a parameter marker for the
 * attribute defined by that column.
 * 
 * The third row is a label for the attribute associated with that column.
 * 
 * All subsequent rows identify rules with the set.
 */
public class RuleSheetListener
    implements
    SheetListener {

    //keywords
    public static final String      FUNCTIONS_TAG          = "Functions";
    public static final String      IMPORT_TAG             = "Import";
    public static final String      SEQUENTIAL_FLAG        = "Sequential";
    public static final String      VARIABLES_TAG          = "Variables";
    public static final String      RULE_TABLE_TAG         = "RuleTable";
    public static final String      RULESET_TAG            = "RuleSet";
    private static final int        ACTION_ROW             = 1;
    private static final int        CODE_ROW               = 2;
    private static final int        LABEL_ROW              = 3;

    //state machine variables for this parser
    private boolean                 _isInRuleTable         = false;
    private int                     _ruleRow;
    private int                     _ruleStartColumn;
    private int                     _ruleStartRow;
    private Rule                    _currentRule;
    private String                  _currentRulePrefix;
    private boolean                 _currentSequentialFlag = false;                        // indicates that we are in sequential mode

    //accumulated output
    private Map                     _actions;
    private final HashMap                 _cellComments          = new HashMap();
    private final List                    _ruleList              = new LinkedList();

    private final PropertiesSheetListener _propertiesListner     = new PropertiesSheetListener();

    /**
     * Return the rule sheet properties
     */
    public Properties getProperties() {
        return this._propertiesListner.getProperties();
    }

    /**
     * Build the final ruleset as parsed.
     */
    public Package getRuleSet() {
        if ( this._ruleList.isEmpty() ) {
            throw new DecisionTableParseException( "No RuleTable's were found in spreadsheet." );
        }
        final Package ruleset = buildRuleSet();
        return ruleset;
    }

    private Package buildRuleSet() {
        final String rulesetName = getProperties().getProperty( RuleSheetListener.RULESET_TAG,
                                                          "rule_table" );
        final Package ruleset = new Package( rulesetName );
        for ( final Iterator it = this._ruleList.iterator(); it.hasNext(); ) {
            ruleset.addRule( (Rule) it.next() );
        }
        final List importList = RuleSheetParserUtil.getImportList( getProperties().getProperty( RuleSheetListener.IMPORT_TAG ) );
        for ( final Iterator it = importList.iterator(); it.hasNext(); ) {
            ruleset.addImport( (Import) it.next() );
        }
        final List variableList = RuleSheetParserUtil.getVariableList( getProperties().getProperty( RuleSheetListener.VARIABLES_TAG ) ); // Set the list of variables to
        // be added to the
        // application-data tags
        for ( final Iterator it = variableList.iterator(); it.hasNext(); ) {
            ruleset.addVariable( (Global) it.next() );
        }

        final String functions = getProperties().getProperty( RuleSheetListener.FUNCTIONS_TAG );
        ruleset.addFunctions( functions );
        return ruleset;
    }

    /*
     * (non-Javadoc)
     * 
     * @see my.hssf.util.SheetListener#startSheet(java.lang.String)
     */
    public void startSheet(final String name) {
        // nothing to see here... move along..
    }

    /*
     * (non-Javadoc)
     * 
     * @see my.hssf.util.SheetListener#finishSheet()
     */
    public void finishSheet() {
        this._propertiesListner.finishSheet();
        finishRuleTable();
    }

    /*
     * (non-Javadoc)
     * 
     * @see my.hssf.util.SheetListener#newRow()
     */
    public void newRow(final int rowNumber,
                       final int columns) {
        // nothing to see here... these aren't the droids your looking for..
        // move along...
    }

    /*
     * (non-Javadoc)
     * 
     * @see my.hssf.util.SheetListener#newCell(int, int, java.lang.String)
     */
    public void newCell(final int row,
                        final int column,
                        final String value) {
        if ( isCellValueEmpty( value ) ) {
            return;
        }
        if ( this._isInRuleTable ) {
            processRuleCell( row,
                             column,
                             value );
        } else {
            processNonRuleCell( row,
                                column,
                                value );
        }
    }

    /**
     * This gets called each time a "new" rule table is found.
     */
    private void initRuleTable(final int row,
                               final int column,
                               final String value) {

        this._isInRuleTable = true;
        this._actions = new HashMap();
        this._ruleStartColumn = column;
        this._ruleStartRow = row;
        this._ruleRow = row + RuleSheetListener.LABEL_ROW + 1;

        // setup stuff for the rules to come.. (the order of these steps are
        // important !)
        this._currentRulePrefix = RuleSheetParserUtil.getRuleName( value );
        this._currentSequentialFlag = getSequentialFlag();

        this._currentRule = createNewRuleForRow( this._ruleRow );

        this._ruleList.add( this._currentRule );

    }

    private boolean getSequentialFlag() {
        final String seqFlag = getProperties().getProperty( RuleSheetListener.SEQUENTIAL_FLAG );
        return RuleSheetParserUtil.isStringMeaningTrue( seqFlag );
    }

    private void finishRuleTable() {
        if ( this._isInRuleTable ) {
            this._currentSequentialFlag = false;
            this._isInRuleTable = false;
        }
    }

    private void processNonRuleCell(final int row,
                                    final int column,
                                    final String value) {
        if ( value.startsWith( RuleSheetListener.RULE_TABLE_TAG ) ) {
            initRuleTable( row,
                           column,
                           value );
        } else {
            this._propertiesListner.newCell( row,
                                        column,
                                        value );
        }
    }

    private void processRuleCell(final int row,
                                 final int column,
                                 final String value) {
        if ( value.startsWith( RuleSheetListener.RULE_TABLE_TAG ) ) {
            finishRuleTable();
            initRuleTable( row,
                           column,
                           value );
            return;
        }

        // Ignore any comments cells preceeding the first rule table column
        if ( column < this._ruleStartColumn ) {
            return;
        }

        // Ignore any further cells from the rule def row
        if ( row == this._ruleStartRow ) {
            return;
        }

        switch ( row - this._ruleStartRow ) {
            case ACTION_ROW :
                ActionType.addNewActionType( this._actions,
                                             value,
                                             column,
                                             row );
                break;
            case CODE_ROW :
                codeRow( row,
                         column,
                         value );
                break;
            case LABEL_ROW :
                labelRow( row,
                          column,
                          value );
                break;
            default :
                nextRule( row,
                          column,
                          value );
                break;
        }
    }

    private void codeRow(final int row,
                         final int column,
                         final String value) {
        final ActionType actionType = getActionForColumn( row,
                                                    column );

        if ( value.trim().equals( "" ) && (actionType.type == ActionType.ACTION || actionType.type == ActionType.CONDITION) ) {
            throw new DecisionTableParseException( "Code description - row:" + (row + 1) + " cell number:" + (column + 1) + " - does not contain any code specification. It should !" );
        }

        actionType.value = value;
    }

    private void labelRow(final int row,
                          final int column,
                          final String value) {
        final ActionType actionType = getActionForColumn( row,
                                                    column );

        if ( !value.trim().equals( "" ) && (actionType.type == ActionType.ACTION || actionType.type == ActionType.CONDITION) ) {
            this._cellComments.put( new Integer( column ),
                               value );
        } else {
            this._cellComments.put( new Integer( column ),
                               "From column: " + Rule.convertColNumToColName( column ) );
        }
    }

    private ActionType getActionForColumn(final int row,
                                          final int column) {
        final ActionType actionType = (ActionType) this._actions.get( new Integer( column ) );

        if ( actionType == null ) {
            throw new DecisionTableParseException( "Code description - row number:" + (row + 1) + " cell number:" + (column + 1) + " - does not have an 'ACTION' or 'CONDITION' column header." );
        }

        return actionType;
    }

    private void nextRule(final int row,
                          final int column,
                          final String value) {
        final ActionType actionType = getActionForColumn( row,
                                                    column );

        if ( row - this._ruleRow > 1 ) {
            // Encountered a row gap from the last rule.
            // This is not part of the ruleset.
            finishRuleTable();
            processNonRuleCell( row,
                                column,
                                value );
            return;
        }

        if ( row > this._ruleRow ) {
            // In a new row/rule
            this._currentRule = createNewRuleForRow( row );

            this._ruleList.add( this._currentRule );
            this._ruleRow++;
        }

        //if the rule set is not sequential and the actionType type is PRIORITY then set the current Rule's salience paramenter with the value got from the cell
        if ( actionType.type == ActionType.PRIORITY && !this._currentSequentialFlag ) {
            this._currentRule.setSalience( new Integer( value ) );
        } else if ( actionType.type == ActionType.NAME ) // if the actionType
        // type is PRIORITY then
        // set the current
        // Rule's name
        // paramenter with the
        // value got from the
        // cell
        {
            this._currentRule.setName( value );
        } else if ( actionType.type == ActionType.DESCRIPTION ) // if the
        // actionType
        // type is
        // DESCRIPTION
        // then set the
        // current
        // Rule's
        // description
        // paramenter
        // with the
        // value got
        // from the cell
        {
            this._currentRule.setDescription( value );
        } else if ( actionType.type == ActionType.ACTIVATIONGROUP ) // if the actionType
        // type is NOLOOP
        // then set the
        // current Rule's
        // no-loop
        // paramenter with
        // the value got
        // from the cell
        {
            this._currentRule.setActivationrGroup( value );
        } else if ( actionType.type == ActionType.NOLOOP ) // if the actionType
        // type is NOLOOP
        // then set the
        // current Rule's
        // no-loop
        // paramenter with
        // the value got
        // from the cell
        {
            this._currentRule.setNoLoop( value );
        } else if ( actionType.type == ActionType.DURATION ) // if the actionType
        // type is DURATION
        // then creates a
        // new duration tag
        // with the value
        // got from the cell
        {
            createDuration( column,
                            value,
                            actionType );
        } else if ( actionType.type == ActionType.CONDITION ) {
            createCondition( column,
                             value,
                             actionType );
        } else if ( actionType.type == ActionType.ACTION ) {
            createConsequence( column,
                               value,
                               actionType );
        }

    }

    private Rule createNewRuleForRow(final int row) {

        Integer salience = null;
        if ( this._currentSequentialFlag ) {
            salience = new Integer( Rule.calcSalience( row ) );
        }
        final int spreadsheetRow = row + 1;
        final String name = this._currentRulePrefix + "_" + spreadsheetRow;
        final Rule rule = new Rule( name,
                              salience,
                              spreadsheetRow );
        rule.setComment( "From row number: " + (spreadsheetRow) );

        return rule;
    }

    private void createCondition(final int column,
                                 final String value,
                                 final ActionType actionType) {

        final Condition cond = new Condition();
        cond.setSnippet( actionType.getSnippet( value ) );
        cond.setComment( cellComment( column ) );
        this._currentRule.addCondition( cond );
    }

    // 08 - 16 - 2005 RIK: This function creates a new DURATION TAG if apply.
    // The value in the cell must be made with the first character of the
    // parameter and the value next to it, separated by ":" character
    // Examples: w1:d3:h4 mean weeks="1" days="3" hours="4", m=1:s=45 means
    // minutes="1" seconds="45"

    private void createDuration(final int column,
                                final String value,
                                final ActionType actionType) {

        final Duration dur = new Duration();
        dur.setSnippet( value );
        dur.setComment( cellComment( column ) );
        this._currentRule.setDuration( dur );
    }

    private void createConsequence(final int column,
                                   final String value,
                                   final ActionType actionType) {

        final Consequence cons = new Consequence();
        cons.setSnippet( actionType.getSnippet( value ) );
        cons.setComment( cellComment( column ) );
        this._currentRule.addConsequence( cons );
    }

    private boolean isCellValueEmpty(final String value) {
        return value == null || "".equals( value.trim() );
    }

    private String cellComment(final int column) {
        return "From column: " + Rule.convertColNumToColName( column );
    }

}