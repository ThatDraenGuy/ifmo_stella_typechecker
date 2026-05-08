package ru.draen.stella.typecheck;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.typecheck.exceptions.ErrorDuplicateFunctionDeclaration;

import java.util.*;

public class TypeCheckRegistry {
    private final LinkedList<Scope> scopeStack = new LinkedList<>();
    private Optional<StellaType> expectedType = Optional.empty();
    private boolean isDeclarationPass = false; //костыль для объявления функций (в стелле нету форвард-декларэйшина)
    private boolean typeReconstructionEnabled = false;
    private boolean universalTypesEnabled = false;

    private final ConstraintUnifier constraintUnifier = new ConstraintUnifier();

    public TypeCheckRegistry() {
        enterScope("GLOBAL");
    }


    public void enterScope(String marker) {
        scopeStack.push(new Scope(marker));
    }

    public void exitScope() {
        scopeStack.pop();
    }

    public void addExpectedType(StellaType expected) {
        expectedType = Optional.of(expected);
    }

    public Optional<StellaType> consumeExpectedType() {
        Optional<StellaType> res = expectedType;
        expectedType = Optional.empty();
        return res;
    }

    public boolean addVar(String name, StellaType type) {
        return Objects.requireNonNull(scopeStack.peek()).vars.put(name, type) != null;
    }
    public boolean addTypeVar(String name) {
        return Objects.requireNonNull(scopeStack.peek()).typeVars.put(name, new StellaType.NamedVar(name)) != null;
    }

    public Optional<StellaType> getVar(String name) {
        for (Scope scope : scopeStack) {
            StellaType type = scope.vars.get(name);
            if (type != null) return Optional.of(type);
        }
        return Optional.empty();
    }
    public Optional<StellaType.NamedVar> getTypeVar(String name) {
        for (Scope scope : scopeStack) {
            StellaType.NamedVar var = scope.typeVars.get(name);
            if (var != null) return Optional.of(var);
        }
        return Optional.empty();
    }

    public void addConstraint(Constraint constraint) {
        constraintUnifier.add(constraint);
    }
    public void unifyConstraints() {
        var mappings = constraintUnifier.unify();
        List<StellaType> freshVars = StellaType.FreshVar.all();
        for (var mapping : mappings) {
            freshVars = freshVars.stream().map(var -> var.replace(mapping)).toList();
        }
        freshVars.forEach(StellaType::checkAmbiguity);
    }

    public boolean isDeclarationPass() {
        return isDeclarationPass;
    }

    public void setDeclarationPass(boolean declarationPass) {
        isDeclarationPass = declarationPass;
    }

    public boolean isTypeReconstructionEnabled() {
        return typeReconstructionEnabled;
    }

    public void setTypeReconstructionEnabled(boolean typeReconstructionEnabled) {
        this.typeReconstructionEnabled = typeReconstructionEnabled;
    }

    public boolean isUniversalTypesEnabled() {
        return universalTypesEnabled;
    }

    public void setUniversalTypesEnabled(boolean universalTypesEnabled) {
        this.universalTypesEnabled = universalTypesEnabled;
    }

    private record Scope(String marker, Map<String, StellaType> vars, Map<String, StellaType.NamedVar> typeVars) {
        private Scope(String marker) {
            this(marker, new HashMap<>(), new HashMap<>());
        }
    }
}
