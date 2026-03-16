package ru.draen.stella.typecheck;

import ru.draen.stella.typecheck.exceptions.ErrorDuplicateFunctionDeclaration;

import java.util.*;

public class TypeCheckRegistry {
    private final LinkedList<Scope> scopeStack = new LinkedList<>();
    private Optional<StellaType> expectedType = Optional.empty();
    private boolean isDeclarationPass = false; //костыль для объявления функций (в стелле нету форвард-декларэйшина)

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

    public Optional<StellaType> getVar(String name) {
        for (Scope scope : scopeStack) {
            StellaType type = scope.vars.get(name);
            if (type != null) return Optional.of(type);
        }
        return Optional.empty();
    }

    public boolean isDeclarationPass() {
        return isDeclarationPass;
    }

    public void setDeclarationPass(boolean declarationPass) {
        isDeclarationPass = declarationPass;
    }

    private record Scope(String marker, Map<String, StellaType> vars) {
        private Scope(String marker) {
            this(marker, new HashMap<>());
        }
    }
}
