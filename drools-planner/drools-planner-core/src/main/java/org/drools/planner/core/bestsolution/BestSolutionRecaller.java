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

package org.drools.planner.core.bestsolution;

import org.drools.planner.core.Solver;
import org.drools.planner.core.event.SolverEventSupport;
import org.drools.planner.core.score.Score;
import org.drools.planner.core.solution.Solution;
import org.drools.planner.core.solver.AbstractSolverLifecycleListener;
import org.drools.planner.core.solver.AbstractSolverScope;
import org.drools.planner.core.solver.AbstractStepScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A BestSolutionRecaller remembers the best solution that a {@link Solver} encounters.
 * @author Geoffrey De Smet
 */
public class BestSolutionRecaller implements AbstractSolverLifecycleListener {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected SolverEventSupport solverEventSupport;

    public void setSolverEventSupport(SolverEventSupport solverEventSupport) {
        this.solverEventSupport = solverEventSupport;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public void solvingStarted(AbstractSolverScope abstractSolverScope) {
        Score initialScore = abstractSolverScope.getStartingScore();
        logger.info("Initialization time spend ({}) for score ({}). Updating best solution and best score.",
                abstractSolverScope.calculateTimeMillisSpend(), initialScore);
        abstractSolverScope.setBestSolutionStepIndex(-1);
        abstractSolverScope.setBestSolution(abstractSolverScope.getWorkingSolution().cloneSolution());
        abstractSolverScope.setBestScore(initialScore);
    }

    public void stepTaken(AbstractStepScope abstractStepScope) {
        AbstractSolverScope abstractSolverScope = abstractStepScope.getAbstractSolverScope();
        Score newScore = abstractStepScope.getScore();
        Score bestScore = abstractSolverScope.getBestScore();
        boolean bestScoreImproved = newScore.compareTo(bestScore) > 0;
        abstractStepScope.setBestScoreImproved(bestScoreImproved);
        if (bestScoreImproved) {
            abstractSolverScope.setBestSolutionStepIndex(abstractStepScope.getStepIndex());
            Solution newBestSolution = abstractStepScope.createOrGetClonedSolution();
            abstractSolverScope.setBestSolution(newBestSolution);
            abstractSolverScope.setBestScore(newBestSolution.getScore());
            solverEventSupport.fireBestSolutionChanged(newBestSolution);
        }
    }

    public void solvingEnded(AbstractSolverScope abstractSolverScope) {
    }

}
