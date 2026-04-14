package ru.draen.stella.typecheck.exceptions;

public abstract class ErrorUnexpectedDataForNullaryLabel extends TypeCheckException {
    @Override
    protected ErrorType getErrorType() {
        return ErrorType.getByException(ErrorUnexpectedDataForNullaryLabel.class);
    }
}
