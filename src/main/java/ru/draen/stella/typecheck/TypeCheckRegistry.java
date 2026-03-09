package ru.draen.stella.typecheck;

import ru.draen.stella.typecheck.exceptions.ErrorDuplicateFunctionDeclaration;

import java.util.*;

public class TypeCheckRegistry {
    private final LinkedList<Scope> scopeStack = new LinkedList<>();
    private Optional<StellaType> expectedType = Optional.empty();
//    private final Map<String, StellaFunction> funcs = new HashMap<>(); //без локальных функций

    public TypeCheckRegistry() {
        enterScope("GLOBAL");
    }

//    public void addFunc(StellaFunction func) {
//        StellaFunction old = funcs.put(func.name(), func);
//        if (old != null) {
//            throw new ErrorDuplicateFunctionDeclaration(old, func);
//        }
//    }
//
//    public Optional<StellaFunction> getFunc(String name) {
//        return Optional.ofNullable(funcs.get(name));
//    }

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

    public void addVar(String name, StellaType type) {
        Objects.requireNonNull(scopeStack.peek()).vars.put(name, type);
    }

    public Optional<StellaType> getVar(String name) {
        for (Scope scope : scopeStack) {
            StellaType type = scope.vars.get(name);
            if (type != null) return Optional.of(type);
        }
        return Optional.empty();
    }

    private record Scope(String marker, Map<String, StellaType> vars) {
        private Scope(String marker) {
            this(marker, new HashMap<>());
        }
    }
}
