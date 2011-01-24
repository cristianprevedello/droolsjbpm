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

package org.drools.planner.core.localsearch.decider.acceptor.tabu;

import java.util.Collection;
import java.util.Collections;

import org.drools.planner.core.localsearch.LocalSearchStepScope;
import org.drools.planner.core.localsearch.decider.MoveScope;
import org.drools.planner.core.move.Move;

/**
 * @author Geoffrey De Smet
 */
public class MoveTabuAcceptor extends AbstractTabuAcceptor {

    protected boolean useUndoMoveAsTabuMove = true;

    public void setUseUndoMoveAsTabuMove(boolean useUndoMoveAsTabuMove) {
        this.useUndoMoveAsTabuMove = useUndoMoveAsTabuMove;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    protected Collection<? extends Object> findTabu(MoveScope moveScope) {
        return Collections.singletonList(moveScope.getMove());
    }

    @Override
    protected Collection<? extends Object> findNewTabu(LocalSearchStepScope localSearchStepScope) {
        Move tabuMove;
        if (useUndoMoveAsTabuMove) {
            tabuMove = localSearchStepScope.getUndoStep();
        } else {
            tabuMove = localSearchStepScope.getStep();
        }
        return Collections.singletonList(tabuMove);
    }

}
