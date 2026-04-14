package ru.draen.stella.typecheck.exceptions;

public abstract class ErrorUnexpectedNumberOfParametersInLambda extends TypeCheckException {
    @Override
    protected ErrorType getErrorType() {
        return ErrorType.getByException(ErrorUnexpectedNumberOfParametersInLambda.class);
    }
}
