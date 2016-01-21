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
package org.sosy_lab.solver.z3;

import static org.sosy_lab.solver.z3.Z3NativeApi.ast_to_string;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_app_arg;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_app_decl;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_app_num_args;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_arity;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_ast_kind;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_decl_kind;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_decl_name;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_index_value;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_quantifier_body;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_quantifier_bound_name;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_quantifier_bound_sort;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_quantifier_num_bound;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_symbol_int;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_symbol_kind;
import static org.sosy_lab.solver.z3.Z3NativeApi.get_symbol_string;
import static org.sosy_lab.solver.z3.Z3NativeApi.is_quantifier_forall;
import static org.sosy_lab.solver.z3.Z3NativeApi.mk_app;
import static org.sosy_lab.solver.z3.Z3NativeApi.mk_const;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_APP_AST;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_FUNC_DECL_AST;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_INT_SYMBOL;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_NUMERAL_AST;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_ADD;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_AND;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_DISTINCT;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_DIV;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_EQ;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_FALSE;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_GE;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_GT;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_IFF;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_IMPLIES;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_ITE;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_LE;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_LT;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_MOD;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_MUL;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_NOT;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_OR;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_SUB;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_TRUE;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_UNINTERPRETED;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_OP_XOR;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_QUANTIFIER_AST;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_SORT_AST;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_STRING_SYMBOL;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_UNKNOWN_AST;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.Z3_VAR_AST;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Longs;

import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.FuncDecl;
import org.sosy_lab.solver.api.FuncDeclKind;
import org.sosy_lab.solver.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.solver.basicimpl.AbstractIntrospectionFormulaManager;
import org.sosy_lab.solver.visitors.FormulaVisitor;

import java.util.ArrayList;
import java.util.List;

class Z3IntrospectionFormulaManager extends AbstractIntrospectionFormulaManager<Long, Long, Long> {

  private final long z3context;
  private final Z3FormulaCreator formulaCreator;

  Z3IntrospectionFormulaManager(Z3FormulaCreator pCreator) {
    super(pCreator);
    this.z3context = pCreator.getEnv();
    formulaCreator = pCreator;
  }

  final static ImmutableSet<Integer> NON_ATOMIC_OP_TYPES =
      ImmutableSet.of(Z3_OP_AND, Z3_OP_OR, Z3_OP_IMPLIES, Z3_OP_ITE, Z3_OP_NOT);

  public int getArity(Long t) {
    Preconditions.checkArgument(get_ast_kind(z3context, t) != Z3_QUANTIFIER_AST);
    return get_app_num_args(z3context, t);
  }

  private boolean isFunctionApplication(long t) {
    return get_ast_kind(z3context, t) == Z3_APP_AST;
  }

  public String getName(Long t) {
    int astKind = get_ast_kind(z3context, t);
    if (astKind == Z3_VAR_AST) {
      return "?" + get_index_value(z3context, t);
    } else {
      long funcDecl = get_app_decl(z3context, t);
      long symbol = get_decl_name(z3context, funcDecl);
      switch (get_symbol_kind(z3context, symbol)) {
        case Z3_INT_SYMBOL:
          return Integer.toString(get_symbol_int(z3context, symbol));
        case Z3_STRING_SYMBOL:
          return get_symbol_string(z3context, symbol);
        default:
          throw new AssertionError();
      }
    }
  }

  public Long replaceArgs(Long t, List<Long> newArgs) {
    Preconditions.checkState(get_app_num_args(z3context, t) == newArgs.size());
    long[] newParams = Longs.toArray(newArgs);
    // TODO check equality of sort of each oldArg and newArg
    long funcDecl = get_app_decl(z3context, t);
    return mk_app(z3context, funcDecl, newParams);
  }

  @Override
  public <R> R visit(FormulaVisitor<R> visitor, final Formula formula, final Long f) {
    switch (get_ast_kind(z3context, f)) {
      case Z3_NUMERAL_AST:
        // TODO extract logic from Z3Model for conversion from string to number and use it here
        return visitor.visitConstant(formula, ast_to_string(z3context, f));
      case Z3_APP_AST:
        String name = getName(f);
        int arity = get_app_num_args(z3context, f);

        if (arity == 0) {

          // true/false.
          long declKind = get_decl_kind(z3context, get_app_decl(z3context, f));
          if (declKind == Z3_OP_TRUE || declKind == Z3_OP_FALSE) {
            return visitor.visitConstant(formula, declKind == Z3_OP_TRUE);
          } else {

            // Has to be a variable otherwise.
            // TODO: assert that.
            return visitor.visitFreeVariable(formula, name);
          }
        }

        List<Formula> args = new ArrayList<>(arity);
        for (int i = 0; i < arity; i++) {
          long arg = get_app_arg(z3context, f, i);
          FormulaType<?> argumentType = formulaCreator.getFormulaType(arg);
          args.add(formulaCreator.encapsulate(argumentType, arg));
        }

        // Any function application.
        Function<List<Formula>, Formula> constructor =
            new Function<List<Formula>, Formula>() {
              @Override
              public Formula apply(List<Formula> formulas) {
                return formulaCreator.encapsulateWithTypeOf(replaceArgs(f, extractInfo(formulas)));
              }
            };
        return visitor.visitFuncApp(
            formula, args, FuncDecl.of(name, getDeclarationKind(f)), constructor);
      case Z3_VAR_AST:
        int deBruijnIdx = get_index_value(z3context, f);
        return visitor.visitBoundVariable(formula, deBruijnIdx);
      case Z3_QUANTIFIER_AST:
        BooleanFormula body = formulaCreator.encapsulateBoolean(get_quantifier_body(z3context, f));
        Quantifier q = is_quantifier_forall(z3context, f) ? Quantifier.FORALL : Quantifier.EXISTS;
        return visitor.visitQuantifier((BooleanFormula) formula, q, getBoundVars(f), body);

      case Z3_SORT_AST:
      case Z3_FUNC_DECL_AST:
      case Z3_UNKNOWN_AST:
      default:
        throw new UnsupportedOperationException(
            "Input should be a formula AST, " + "got unexpected type instead");
    }
  }

  List<Formula> getBoundVars(long f) {
    int numBound = get_quantifier_num_bound(z3context, f);
    List<Formula> boundVars = new ArrayList<>(numBound);
    for (int i = 0; i < numBound; i++) {
      long varName = get_quantifier_bound_name(z3context, f, i);
      long varSort = get_quantifier_bound_sort(z3context, f, i);
      boundVars.add(
          formulaCreator.encapsulate(
              formulaCreator.getFormulaTypeFromSort(varSort),
              mk_const(z3context, varName, varSort)));
    }
    return boundVars;
  }

  private FuncDeclKind getDeclarationKind(long f) {
    int decl = get_decl_kind(z3context, get_app_decl(z3context, f));

    if (get_arity(z3context, f) == 0) {
      return FuncDeclKind.VAR;
    }

    switch (decl) {
      case Z3_OP_AND:
        return FuncDeclKind.AND;
      case Z3_OP_NOT:
        return FuncDeclKind.NOT;
      case Z3_OP_OR:
        return FuncDeclKind.OR;
      case Z3_OP_IFF:
        return FuncDeclKind.IFF;
      case Z3_OP_ITE:
        return FuncDeclKind.ITE;
      case Z3_OP_XOR:
        return FuncDeclKind.XOR;
      case Z3_OP_DISTINCT:
        return FuncDeclKind.DISTINCT;

      case Z3_OP_SUB:
        return FuncDeclKind.SUB;
      case Z3_OP_ADD:
        return FuncDeclKind.ADD;
      case Z3_OP_DIV:
        return FuncDeclKind.DIV;
      case Z3_OP_MUL:
        return FuncDeclKind.MUL;
      case Z3_OP_MOD:
        return FuncDeclKind.MODULO;

      case Z3_OP_UNINTERPRETED:
        return FuncDeclKind.UF;

      case Z3_OP_LT:
        return FuncDeclKind.LT;
      case Z3_OP_LE:
        return FuncDeclKind.LTE;
      case Z3_OP_GT:
        return FuncDeclKind.GT;
      case Z3_OP_GE:
        return FuncDeclKind.GTE;
      case Z3_OP_EQ:
        return FuncDeclKind.EQ;

      default:
        return FuncDeclKind.OTHER;
    }
  }
}