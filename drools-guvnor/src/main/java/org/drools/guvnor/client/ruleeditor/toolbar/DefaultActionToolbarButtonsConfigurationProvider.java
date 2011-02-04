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

package org.drools.guvnor.client.ruleeditor.toolbar;

import org.drools.guvnor.client.modeldriven.ui.RuleModelEditor;
import org.drools.guvnor.client.rpc.RuleAsset;
import org.drools.guvnor.client.ruleeditor.EditorWidget;

import static org.drools.guvnor.client.common.AssetFormats.*;

/**
 * @author esteban.aliverti
 */
public class DefaultActionToolbarButtonsConfigurationProvider
        implements
        ActionToolbarButtonsConfigurationProvider {


    private static String[] VALIDATING_FORMATS = new String[]{BUSINESS_RULE, DSL_TEMPLATE_RULE, DECISION_SPREADSHEET_XLS, DRL, ENUMERATION, DECISION_TABLE_GUIDED, DRL_MODEL, DSL, FUNCTION, RULE_TEMPLATE, SPRING_CONTEXT};
    private static String[] SOURCE_FORMATS = new String[]{BUSINESS_RULE, DSL_TEMPLATE_RULE, DRL, DECISION_TABLE_GUIDED, RULE_TEMPLATE, BPMN2_PROCESS, BPMN_PROCESS};

    private static String[] VERIFY_FORMATS = new String[]{BUSINESS_RULE, DECISION_SPREADSHEET_XLS, DRL, DECISION_TABLE_GUIDED, DRL_MODEL, RULE_TEMPLATE};

    private RuleAsset asset;
    private EditorWidget editor;

    public DefaultActionToolbarButtonsConfigurationProvider(RuleAsset asset,
                                                            EditorWidget editor) {
        this.asset = asset;
        this.editor = editor;
    }

    public boolean showSaveButton() {
        return true;
    }

    public boolean showSaveAndCloseButton() {
        return true;
    }

    public boolean showCopyButton() {
        return true;
    }

    public boolean showPromoteToGlobalButton() {
        return true;
    }

    public boolean showArchiveButton() {
        return asset.metaData.versionNumber != 0;
    }

    public boolean showDeleteButton() {
        return asset.metaData.versionNumber == 0;
    }

    public boolean showChangeStatusButton() {
        return true;
    }

    public boolean showSelectWorkingSetsButton() {
        return this.isValidatorTypeAsset() && editor instanceof RuleModelEditor;
    }

    public boolean showValidateButton() {
        return this.isValidatorTypeAsset();
    }

    public boolean showVerifyButton() {
        return this.isVerificationTypeAsset();
    }

    public boolean showViewSourceButton() {
        return isMemberOfFormats(asset.metaData.format, SOURCE_FORMATS);
    }

    public boolean showStateLabel() {
        return true;
    }

    private boolean isValidatorTypeAsset() {
        return isMemberOfFormats(asset.metaData.format, VALIDATING_FORMATS);
    }

    private boolean isVerificationTypeAsset() {
        return isMemberOfFormats(asset.metaData.format, VERIFY_FORMATS);
    }

    private boolean isMemberOfFormats(String format, String[] formats) {
        for (String fmt : formats) {
            if (fmt.equals(format)) {
                return true;
            }
        }
        return false;
    }

}
