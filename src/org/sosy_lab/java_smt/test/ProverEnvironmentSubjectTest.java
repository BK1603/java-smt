/*
 *  JavaSMT is an API wrapper for a collection of SMT solvers.
 *  This file is part of JavaSMT.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.java_smt.test;

import static com.google.common.truth.ExpectFailure.assertThat;
import static org.sosy_lab.java_smt.test.ProverEnvironmentSubject.proverEnvironments;

import com.google.common.base.Throwables;
import com.google.common.truth.ExpectFailure;
import com.google.common.truth.ExpectFailure.SimpleSubjectBuilderCallback;
import com.google.common.truth.SimpleSubjectBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@RunWith(Parameterized.class)
public class ProverEnvironmentSubjectTest extends SolverBasedTest0 {

  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter public Solvers solver;

  @Override
  protected Solvers solverToUse() {
    return solver;
  }

  private BooleanFormula simpleFormula;
  private BooleanFormula contradiction;

  @Before
  public void setupFormulas() {
    simpleFormula = imgr.equal(imgr.makeVariable("a"), imgr.makeNumber(1));
    contradiction = bmgr.and(simpleFormula, imgr.equal(imgr.makeVariable("a"), imgr.makeNumber(2)));
  }

  @Test
  public void testIsSatisfiableYes() throws SolverException, InterruptedException {
    try (ProverEnvironment env = context.newProverEnvironment()) {
      env.push(simpleFormula);
      assertThatEnvironment(env).isSatisfiable();
    }
  }

  @Test
  public void testIsSatisfiableNo() throws InterruptedException {
    try (ProverEnvironment env = context.newProverEnvironment(ProverOptions.GENERATE_UNSAT_CORE)) {
      env.push(contradiction);
      AssertionError failure = expectFailure(whenTesting -> whenTesting.that(env).isSatisfiable());
      assertThat(failure).factValue("with unsat core").isNotEmpty();
    }
  }

  @Test
  public void testIsUnsatisfiableYes() throws SolverException, InterruptedException {
    try (ProverEnvironment env = context.newProverEnvironment()) {
      env.push(contradiction);
      assertThatEnvironment(env).isUnsatisfiable();
    }
  }

  @Test
  public void testIsUnsatisfiableNo() throws InterruptedException {
    try (ProverEnvironment env = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      env.push(simpleFormula);
      AssertionError failure =
          expectFailure(whenTesting -> whenTesting.that(env).isUnsatisfiable());
      assertThat(failure).factValue("which has model").isNotEmpty();
    }
  }

  private AssertionError expectFailure(ExpectFailureCallback expectFailureCallback) {
    return ExpectFailure.expectFailureAbout(proverEnvironments(), expectFailureCallback);
  }

  /** Variant of {@link SimpleSubjectBuilderCallback} that allows checked exception. */
  private interface ExpectFailureCallback
      extends SimpleSubjectBuilderCallback<ProverEnvironmentSubject, BasicProverEnvironment<?>> {

    void invokeAssertionUnchecked(
        SimpleSubjectBuilder<ProverEnvironmentSubject, BasicProverEnvironment<?>> pWhenTesting)
        throws Exception;

    @Override
    default void invokeAssertion(
        SimpleSubjectBuilder<ProverEnvironmentSubject, BasicProverEnvironment<?>> pWhenTesting) {
      try {
        invokeAssertionUnchecked(pWhenTesting);
      } catch (Exception e) {
        Throwables.throwIfUnchecked(e);
        throw new RuntimeException(e);
      }
    }
  }
}
