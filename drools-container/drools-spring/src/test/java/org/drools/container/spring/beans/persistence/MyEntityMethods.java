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

package org.drools.container.spring.beans.persistence;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 *
 * @author salaboy
 */
@Entity
public class MyEntityMethods
    implements
    Serializable {
    private static final long serialVersionUID = 510l;

    private Long              id;
    private String            test;

    public MyEntityMethods() {
    }

    public MyEntityMethods(String string) {
        this.test = string;
    }

    /**
     * @return the id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the test
     */
    public String getTest() {
        return test;
    }

    /**
     * @param test the test to set
     */
    public void setTest(String test) {
        this.test = test;
    }

    public String toString() {
        return "VARIABLE: " + this.getId() + " - " + this.getTest();
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        final MyEntityMethods other = (MyEntityMethods) obj;
        if ( this.id != other.id && (this.id == null || !this.id.equals( other.id )) ) {
            return false;
        }
        if ( (this.test == null) ? (other.test != null) : !this.test.equals( other.test ) ) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 41 * hash + (this.test != null ? this.test.hashCode() : 0);
        return hash;
    }

}
