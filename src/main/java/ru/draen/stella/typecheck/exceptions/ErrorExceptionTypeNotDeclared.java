package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorExceptionTypeNotDeclared extends TypeCheckException {
    private final ExceptionContext exception;

    public ErrorExceptionTypeNotDeclared(StellaParser.ThrowContext throwCtx) {
        this.exception = new ExceptionContext.Throw(throwCtx);
    }

    public ErrorExceptionTypeNotDeclared(StellaParser.TryCatchContext catchCtx) {
        this.exception = new ExceptionContext.Catch(catchCtx);
    }

    @Override
    protected ParserRuleContext getSource() {
        return switch (exception) {
            case ExceptionContext.Catch aCatch -> aCatch.catchCtx;
            case ExceptionContext.Throw aThrow -> aThrow.throwCtx;
        };
    }

    @Override
    protected String reportText() {
        return "В программе используются ошибки, но их тип не определён";
    }

    private sealed interface ExceptionContext {
        record Throw(StellaParser.ThrowContext throwCtx) implements ExceptionContext {}
        record Catch(StellaParser.TryCatchContext catchCtx) implements ExceptionContext {}
    }
}
