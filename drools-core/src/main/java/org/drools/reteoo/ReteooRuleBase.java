package org.drools.reteoo;

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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.HashMap;
import java.util.WeakHashMap;

import org.drools.FactException;
import org.drools.FactHandle;
import org.drools.RuleBase;
import org.drools.RuleBaseConfiguration;
import org.drools.WorkingMemory;
import org.drools.common.AbstractRuleBase;
import org.drools.common.DefaultFactHandle;
import org.drools.common.InternalFactHandle;
import org.drools.rule.CompositePackageClassLoader;
import org.drools.rule.InvalidPatternException;
import org.drools.rule.Rule;
import org.drools.spi.FactHandleFactory;
import org.drools.spi.PropagationContext;

/**
 * Implementation of <code>RuleBase</code>.
 * 
 * @author <a href="mailto:bob@werken.com">bob mcwhirter</a>
 * @author <a href="mailto:mark.proctor@jboss.com">Mark Proctor</a> 
 * 
 * @version $Id: RuleBaseImpl.java,v 1.5 2005/08/14 22:44:12 mproctor Exp $
 */
public class ReteooRuleBase extends AbstractRuleBase {
    /**
     * DO NOT CHANGE BELLOW SERIAL_VERSION_ID UNLESS YOU ARE CHANGING DROOLS VERSION
     * SERIAL_VERSION_ID=320 stands for version 3.2.0 
     */
    private static final long serialVersionUID = 320;

    /** The root Rete-OO for this <code>RuleBase</code>. */
    private Rete              rete;

    private ReteooBuilder     reteooBuilder;

    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------

    /**
     * Default constructor - for Externalizable. This should never be used by a user, as it 
     * will result in an invalid state for the instance.
     */
    public ReteooRuleBase() {

    }

    /**
     * Construct.
     * 
     * @param rete
     *            The rete network.
     */
    public ReteooRuleBase(final String id) {
        this( id,
              null,
              new ReteooFactHandleFactory() );
    }

    /**
     * @param factHandleFactory
     */
    public ReteooRuleBase(final String id,
                          final FactHandleFactory factHandleFactory) {
        this( id,
              null,
              factHandleFactory );
    }

    public ReteooRuleBase(final String id,
                          final RuleBaseConfiguration config) {
        this( id,
              config,
              new ReteooFactHandleFactory() );
    }

    /**
     * @param config
     */
    public ReteooRuleBase(final RuleBaseConfiguration config) {
        this( null,
              config,
              new ReteooFactHandleFactory() );
    }

    /**
     * Construct.
     * 
     * @param rete
     *            The rete network.
     */
    public ReteooRuleBase(final String id,
                          final RuleBaseConfiguration config,
                          final FactHandleFactory factHandleFactory) {
        super( id,
               config,
               factHandleFactory );
        this.config = (config != null) ? config : new RuleBaseConfiguration();
        this.config.makeImmutable();

        this.factHandleFactory = factHandleFactory;

        this.packageClassLoader = new CompositePackageClassLoader( Thread.currentThread().getContextClassLoader() );
        this.pkgs = new HashMap();
        this.globals = new HashMap();
        this.workingMemories = new WeakHashMap();

        this.rete = new Rete();
        this.reteooBuilder = new ReteooBuilder( this );
    }

    /**
     * Handles the write serialization of the Package. Patterns in Rules may reference generated data which cannot be serialized by default methods.
     * The Package uses PackageCompilationData to hold a reference to the generated bytecode. The generated bytecode must be restored before any Rules.
     * 
     */
    public void writeExternal(final ObjectOutput stream) throws IOException {
        final Object[] objects = new Object[]{this.rete, this.reteooBuilder};
        doWriteExternal( stream,
                         objects );
    }

    /**
     * Handles the read serialization of the Package. Patterns in Rules may reference generated data which cannot be serialized by default methods.
     * The Package uses PackageCompilationData to hold a reference to the generated bytecode; which must be restored before any Rules.
     * A custom ObjectInputStream, able to resolve classes against the bytecode in the PackageCompilationData, is used to restore the Rules.
     * 
     */
    public void readExternal(final ObjectInput stream) throws IOException,
                                                      ClassNotFoundException {
        final Object[] objects = new Object[2];
        doReadExternal( stream,
                        objects );

        this.rete = (Rete) objects[0];
        this.reteooBuilder = (ReteooBuilder) objects[1];

        this.reteooBuilder.setRuleBase( this );
        this.reteooBuilder.setRete( this.rete );
    }

    // ------------------------------------------------------------
    // Instance methods
    // ------------------------------------------------------------

    /**
     * Retrieve the Rete-OO network for this <code>RuleBase</code>.
     * 
     * @return The RETE-OO network.
     */
    public Rete getRete() {
        return this.rete;
    }

    /**
     * Assert a fact object.
     * 
     * @param handle
     *            The handle.
     * @param object
     *            The fact.
     * @param workingMemory
     *            The working-memory.
     * 
     * @throws FactException
     *             If an error occurs while performing the assertion.
     */
    public void assertObject(final FactHandle handle,
                             final Object object,
                             final PropagationContext context,
                             final ReteooWorkingMemory workingMemory) throws FactException {
        getRete().assertObject( (DefaultFactHandle) handle,
                                context,
                                workingMemory );
    }

    /**
     * Retract a fact object.
     * 
     * @param handle
     *            The handle.
     * @param workingMemory
     *            The working-memory.
     * 
     * @throws FactException
     *             If an error occurs while performing the retraction.
     */
    public void retractObject(final FactHandle handle,
                              final PropagationContext context,
                              final ReteooWorkingMemory workingMemory) throws FactException {
        getRete().retractObject( (InternalFactHandle) handle,
                                 context,
                                 workingMemory );
    }

    /**
     * @see RuleBase
     */
    public WorkingMemory newWorkingMemory(final boolean keepReference) {
        final ReteooWorkingMemory workingMemory = new ReteooWorkingMemory( this.workingMemoryCounter++,
                                                                           this );

        super.addWorkingMemory( workingMemory,
                                keepReference );

        final InitialFactHandle handle = new InitialFactHandle( workingMemory.getFactHandleFactory().newFactHandle( new InitialFactHandleDummyObject() ) );

        workingMemory.queueWorkingMemoryAction( workingMemory.new WorkingMemoryReteAssertAction( handle,
                                                                                                 false,
                                                                                                 true,
                                                                                                 null,
                                                                                                 null ) );

        return workingMemory;
    }

    protected void addRule(final Rule rule) throws InvalidPatternException {
        super.addRule( rule );

        // This adds the rule. ReteBuilder has a reference to the WorkingMemories and will propagate any existing facts.
        this.reteooBuilder.addRule( rule );
    }

    protected void removeRule(final Rule rule) {
        this.reteooBuilder.removeRule( rule );
    }
    
    private static class InitialFactHandleDummyObject implements Serializable {
        private static final long serialVersionUID = 1L;
    }

}
