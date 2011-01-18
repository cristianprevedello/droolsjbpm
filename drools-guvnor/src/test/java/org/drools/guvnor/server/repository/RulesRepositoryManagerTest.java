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

package org.drools.guvnor.server.repository;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import javax.jcr.Repository;

import org.drools.guvnor.server.util.TestEnvironmentSessionHelper;
import org.drools.repository.RulesRepositoryConfigurator;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

public class RulesRepositoryManagerTest {

    @Test
    public void testDecorator() {
        RulesRepositoryManager dec = new RulesRepositoryManager();
        RepositoryStartupService config = new RepositoryStartupService();
        config.properties.put( RulesRepositoryConfigurator.CONFIGURATOR_CLASS,
                               "org.drools.repository.jackrabbit.JackrabbitRepositoryConfigurator" );
        Repository repo = config.getRepositoryInstance();
        config.repository = repo;
        dec.repositoryConfiguration = config;

        dec.create();

        assertNotNull( dec.getRepository().getSession() );

        assertTrue( dec.getRepository().getSession().isLive() );
        dec.close();
        assertFalse( dec.getRepository().getSession().isLive() );

    }

    @After
    public void tearDown() throws Exception {
        TestEnvironmentSessionHelper.shutdown();
    }

}
