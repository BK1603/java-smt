package org.sosy_lab.solver.api;

/**
 * Instances of this interface provide access to an SMT solver.
 * A single SolverContext should be used only from a single thread.
 *
 * <p>If you wish to use multiple contexts (even for the same solver),
 * create one SolverContext per each.
 * Formulas can be transferred between different contexts using
 * {@link FormulaManager#translate(BooleanFormula, SolverContext)}.
 */
public interface SolverContext extends AutoCloseable {

  /**
   * Get the formula manager, which is used for formula manipulation.
   */
  FormulaManager getFormulaManager();

  /**
   * Options for the prover environment.
   */
  enum ProverOptions {

    /**
     * Whether the solver should generate models (i.e., satisfying assignments)
     * for satisfiable formulas.
     */
    GENERATE_MODELS,

    /**
     * Whether the solver should generate an unsat core
     * for unsatisfiable formulas.
     */
    GENERATE_UNSAT_CORE
  }

  /**
   * Create a fresh new {@link ProverEnvironment} which encapsulates an assertion stack
   * and can be used to check formulas for unsatisfiability.
   *
   * @param options Options specified for the prover environment.
   *                All of the options specified in {@link ProverOptions}
   *                are turned off by default.
   */
  ProverEnvironment newProverEnvironment(ProverOptions... options);

  /**
   * Create a fresh new {@link InterpolatingProverEnvironment} which encapsulates an assertion stack
   * and allows to generate and retrieve interpolants for unsatisfiable formulas.
   * If the SMT solver is able to handle satisfiability tests with assumptions please consider
   * implementing the {@link InterpolatingProverEnvironmentWithAssumptions} interface, and return
   * an Object of this type here.
   */
  InterpolatingProverEnvironmentWithAssumptions<?> newProverEnvironmentWithInterpolation();

  /**
   * Create a fresh new {@link OptimizationProverEnvironment} which encapsulates an assertion stack
   * and allows to solve optimization queries.
   */
  OptimizationProverEnvironment newOptimizationProverEnvironment();

  /**
   * Get version information out of the solver.
   */
  String getVersion();

  /**
   * Close the solver context.
   * Necessary for solvers implemented in native code, frees the associated
   * memory.
   */
  @Override
  void close();
}
