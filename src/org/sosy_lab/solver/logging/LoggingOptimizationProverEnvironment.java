/*
 *  JavaSMT is an API wrapper for a collection of SMT solvers.
 *  This file is part of JavaSMT.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.solver.logging;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.Model;
import org.sosy_lab.solver.api.OptimizationProverEnvironment;

import java.util.logging.Level;

import javax.annotation.Nullable;

/**
 * Wrapper for an optimizing solver.
 */
public class LoggingOptimizationProverEnvironment implements OptimizationProverEnvironment {

  private final OptimizationProverEnvironment wrapped;
  private final LogManager logger;

  public LoggingOptimizationProverEnvironment(LogManager logger, OptimizationProverEnvironment oe) {
    this.wrapped = checkNotNull(oe);
    this.logger = checkNotNull(logger);
  }

  @Override
  public Void addConstraint(BooleanFormula constraint) {
    logger.log(Level.FINE, "Asserting: " + constraint);
    return wrapped.addConstraint(constraint);
  }

  @Override
  public int maximize(Formula objective) {
    logger.log(Level.FINE, "Maximizing: " + objective);
    return wrapped.maximize(objective);
  }

  @Override
  public int minimize(Formula objective) {
    logger.log(Level.FINE, "Minimizing: " + objective);
    return wrapped.minimize(objective);
  }

  @Override
  public OptStatus check() throws InterruptedException, SolverException {
    logger.log(Level.FINE, "Performing optimization");
    return wrapped.check();
  }

  @Override
  public void push() {
    logger.log(Level.FINE, "Creating backtracking point");
    wrapped.push();
  }

  @Override
  public boolean isUnsat() throws SolverException, InterruptedException {
    logger.log(Level.FINE, "Checking satisfiability");
    return wrapped.isUnsat();
  }

  @Nullable
  @Override
  public Void push(BooleanFormula f) {
    logger.log(Level.FINE, "Pushing", f, "and creating a backtracking point");
    return wrapped.push(f);
  }

  @Override
  public void pop() {
    logger.log(Level.FINE, "Backtracking one level");
    wrapped.pop();
  }

  @Override
  public Optional<Rational> upper(int handle, Rational epsilon) {
    return wrapped.upper(handle, epsilon);
  }

  @Override
  public Optional<Rational> lower(int handle, Rational epsilon) {
    return wrapped.lower(handle, epsilon);
  }

  @Override
  public Model getModel() throws SolverException {
    return wrapped.getModel();
  }

  @Override
  public void close() {
    wrapped.close();
    logger.log(Level.FINER, "closed");
  }
}
