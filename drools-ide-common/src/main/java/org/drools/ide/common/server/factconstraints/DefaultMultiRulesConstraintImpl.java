/**
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

package org.drools.ide.common.server.factconstraints;

import java.util.Map;

import org.drools.ide.common.server.factconstraints.DefaultFieldConstraintImpl;
import org.drools.ide.common.client.factconstraints.ConstraintConfiguration;

/**
 * Default implementation for constraints that creates more than one rule
 * in its {@link #getVerifierRule()} method.
 * This class contains an internal counter to avoid multiple declaration of
 * package, imports and globals.
 * The counter must be handled by subclasses.
 * @author esteban.aliverti@gmail.com
 */
public abstract class DefaultMultiRulesConstraintImpl extends DefaultFieldConstraintImpl {

	private static final long serialVersionUID = 501l;
	protected static final String RULE_COUNT = "ruleCount";
	
    protected void resetRuleCount(Map<String, Object> context){
        context.put(RULE_COUNT, 0);
    }

    protected void incrementRuleCount(Map<String, Object> context){
    	int rc = (Integer) context.get(RULE_COUNT);
        context.put(RULE_COUNT, ++rc);
    }

    protected int getRuleCount(Map<String, Object> context){
    	return (Integer) context.get(RULE_COUNT);
    }
    
    @Override
    protected String getVerifierGlobalsPrefixTemplate(ConstraintConfiguration config, Map<String, Object> context) {
        if (getRuleCount(context) == 0) {
            return super.getVerifierGlobalsPrefixTemplate(config, context);
        }
        return "";
    }

    @Override
    protected String getVerifierGlobalsSufixTemplate(ConstraintConfiguration config, Map<String, Object> context) {
        if (getRuleCount(context) == 0) {
            return super.getVerifierGlobalsSufixTemplate(config, context);
        }
        return "";
    }

    @Override
    protected String getVerifierGlobalsTemplate(ConstraintConfiguration config, Map<String, Object> context) {
        if (getRuleCount(context) == 0) {
            return super.getVerifierGlobalsTemplate(config, context);
        }
        return "";
    }

    @Override
    protected String getVerifierImportsPrefixTemplate(ConstraintConfiguration config, Map<String, Object> context) {
        if (getRuleCount(context) == 0) {
            return super.getVerifierImportsPrefixTemplate(config, context);
        }
        return "";
    }

    @Override
    protected String getVerifierImportsSufixTemplate(ConstraintConfiguration config, Map<String, Object> context) {
        if (getRuleCount(context) == 0) {
            return super.getVerifierImportsSufixTemplate(config, context);
        }
        return "";
    }

    @Override
    protected String getVerifierImportsTemplate(ConstraintConfiguration config, Map<String, Object> context) {
        if (getRuleCount(context) == 0) {
            return super.getVerifierImportsTemplate(config, context);
        }
        return "";
    }

    @Override
    protected String getVerifierPackagePrefixTemplate(ConstraintConfiguration config, Map<String, Object> context) {
        if (getRuleCount(context) == 0) {
            return super.getVerifierPackagePrefixTemplate(config, context);
        }
        return "";
    }

    @Override
    protected String getVerifierPackageSufixTemplate(ConstraintConfiguration config, Map<String, Object> context) {
        if (getRuleCount(context) == 0) {
            return super.getVerifierPackageSufixTemplate(config, context);
        }
        return "";
    }

    @Override
    protected String getVerifierPackageTemplate(ConstraintConfiguration config, Map<String, Object> context) {
        if (getRuleCount(context) == 0) {
            return super.getVerifierPackageTemplate(config, context);
        }
        return "";
    }
}
