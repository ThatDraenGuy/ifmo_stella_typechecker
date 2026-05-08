package ru.draen.stella.typecheck.exceptions;

public abstract class ErrorUnexpectedTypeForExpression extends TypeCheckException {
    @Override
    protected ErrorType getErrorType() {
        return ErrorType.getByException(ErrorUnexpectedTypeForExpression.class);
    }
}
