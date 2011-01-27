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

package org.drools.planner.core.localsearch.termination;

import org.drools.planner.core.localsearch.LocalSearchStepScope;

/**
 * @author Geoffrey De Smet
 */
public class AndCompositeTermination extends AbstractCompositeTermination {

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    /**
     * @return true if all the Terminations are terminated.
     * @param localSearchStepScope never null
     */
    public boolean isTerminated(LocalSearchStepScope localSearchStepScope) {
        for (Termination termination : terminationList) {
            if (!termination.isTerminated(localSearchStepScope)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the minimum timeGradient of all Terminations.
     * Not supported timeGradients (-1.0) are ignored. 
     * @return the minimum timeGradient of the Terminations.
     * @param localSearchStepScope never null
     */
    public double calculateTimeGradient(LocalSearchStepScope localSearchStepScope) {
        double timeGradient = 1.0;
        for (Termination termination : terminationList) {
            double nextTimeGradient = termination.calculateTimeGradient(localSearchStepScope);
            if (nextTimeGradient >= 0.0) {
                timeGradient = Math.min(timeGradient, nextTimeGradient);
            }
        }
        return timeGradient;
    }

}
