package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaExceptionType;

public class ErrorConflictingExceptionDeclarations extends TypeCheckException {
    private final ExceptionContext exception;

    public ErrorConflictingExceptionDeclarations(StellaParser.DeclExceptionTypeContext exception) {
        this.exception = new ExceptionContext.Type(exception);
    }

    public ErrorConflictingExceptionDeclarations(StellaParser.DeclExceptionVariantContext exception) {
        this.exception = new ExceptionContext.Variant(exception);
    }

    @Override
    protected ParserRuleContext getSource() {
        return switch (exception) {
            case ExceptionContext.Type type -> type.type;
            case ExceptionContext.Variant variant -> variant.variant;
        };
    }

    @Override
    protected String reportText() {
        return "В одной области видимости объявлены декларации и типа ошибок (exception type), и открытых вариантов ошибок (exception variant)";
    }

    private sealed interface ExceptionContext {
        record Type(StellaParser.DeclExceptionTypeContext type) implements ExceptionContext {}
        record Variant(StellaParser.DeclExceptionVariantContext variant) implements ExceptionContext {}
    }
}
