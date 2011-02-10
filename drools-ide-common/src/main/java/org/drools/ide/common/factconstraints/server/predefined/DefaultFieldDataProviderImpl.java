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

package org.drools.ide.common.factconstraints.server.predefined;

import java.util.HashMap;
import java.util.Map;

import org.drools.ide.common.factconstraints.client.ArgumentNotSetException;
import org.drools.ide.common.factconstraints.client.dataprovider.FieldDataProvider;

/**
 *
 * @author esteban.aliverti@gmail.com
 */
public abstract class DefaultFieldDataProviderImpl implements FieldDataProvider {

    private String factType;
    private String fieldName;
    private Map<String, Object> arguments = new HashMap<String, Object>();

    public DefaultFieldDataProviderImpl() {

    }

    protected Object getMandatoryArgument(String key) throws ArgumentNotSetException {
        if (!this.arguments.containsKey(key)) {
            throw new ArgumentNotSetException("The argument " + key + " doesn't exist.");
        }

        Object value = this.getArgumentValue(key);

        if (value == null) {
            throw new ArgumentNotSetException("The argument " + key + " is null.");
        }

        return value;
    }

    public void setFactType(String factType) {
        this.factType = factType;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFactType() {
        return factType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String[] getArgumentKeys() {
        return this.arguments.keySet().toArray(new String[this.arguments.size()]);
    }

    public Object getArgumentValue(String key) {
        return this.arguments.get(key);
    }

    public void setArgumentValue(String key, Object value) {
        this.arguments.put(key, value);
    }
}