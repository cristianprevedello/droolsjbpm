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

package org.drools.runtime.pipeline.impl;

import java.util.ArrayList;
import java.util.List;

import org.drools.runtime.pipeline.Action;
import org.drools.runtime.pipeline.Callable;
import org.drools.runtime.pipeline.Expression;
import org.drools.runtime.pipeline.ListAdapter;
import org.drools.runtime.pipeline.PipelineFactory;
import org.drools.runtime.pipeline.Splitter;
import org.mvel2.MVEL;
import org.mvel2.optimizers.OptimizerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class MvelActionTest {
    @Test
    public void testAction() {
        MockClass mock = new MockClass();
        List<Integer> list = new ArrayList<Integer>();
        
        Action action = PipelineFactory.newMvelAction( "this.setValues( [0, 1, 2, 3, 4] ) " );  

        assertNull( mock.getValues() );
        action.receive( mock, new BasePipelineContext( Thread.currentThread().getContextClassLoader() ) );

        
        System.out.println( mock.getValues().get( 0 ));
        assertEquals( 5, mock.getValues().size());
        assertEquals( 0, mock.getValues().get( 0 ).intValue() );
        assertEquals( 4, mock.getValues().get( 4 ).intValue() );
    }
    
    public static class MockClass {        
        private List<Integer> values;

        public List<Integer> getValues() {
            return values;
        }

        public void setValues(List<Integer> values) {
            this.values = values;
        }                              
    }
}
