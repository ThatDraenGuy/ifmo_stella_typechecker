package ru.draen.stella.typecheck;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.typecheck.exceptions.ErrorUnexpectedSubtype;
import ru.draen.stella.typecheck.exceptions.TypeCheckException;

import java.util.*;
import java.util.function.Supplier;

public class TypeCheckRegistry {
    private final LinkedList<Scope> scopeStack = new LinkedList<>();
    private Optional<StellaType> expectedType = Optional.empty();
    private Optional<StellaExceptionType> exceptionType = Optional.empty();
    private boolean isDeclarationPass = false; //костыль для объявления функций (в стелле нету форвард-декларэйшина)

    private boolean subtypingEnabled = false;
    private boolean ambiguousBottomEnabled = false;

    public TypeCheckRegistry() {
        enterScope("GLOBAL");
    }


    public void enterScope(String marker) {
        scopeStack.push(new Scope(marker));
    }

    public void exitScope() {
        scopeStack.pop();
    }

    public boolean isInLocalScope() {
        return scopeStack.size() > 1;
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

    public boolean isSubtypingEnabled() {
        return subtypingEnabled;
    }

    public void setSubtypingEnabled(boolean subtypingEnabled) {
        this.subtypingEnabled = subtypingEnabled;
    }

    public boolean isAmbiguousBottomEnabled() {
        return ambiguousBottomEnabled;
    }

    public void setAmbiguousBottomEnabled(boolean ambiguousBottomEnabled) {
        this.ambiguousBottomEnabled = ambiguousBottomEnabled;
    }

    public void checkTypeMismatch(StellaType expected, StellaType actual, ParserRuleContext ctx, Supplier<TypeCheckException> error) {
        if (isSubtypingEnabled()) {
            if (!actual.isSubtypeOf(expected, ctx)) {
                throw new ErrorUnexpectedSubtype(ctx, expected, actual);
            }
        } else {
            if (!expected.matches(actual)) {
                throw error.get();
            }
        }
    }

    public Optional<StellaExceptionType> getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(StellaExceptionType exceptionType) {
        this.exceptionType = Optional.of(exceptionType);
    }

    private record Scope(String marker, Map<String, StellaType> vars) {
        private Scope(String marker) {
            this(marker, new HashMap<>());
        }
    }
}
