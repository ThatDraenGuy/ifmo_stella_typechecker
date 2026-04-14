package ru.draen.stella.typecheck.exceptions;

public abstract class ErrorUnexpectedVariantLabel extends TypeCheckException {
    @Override
    protected ErrorType getErrorType() {
        return ErrorType.getByException(ErrorUnexpectedVariantLabel.class);
    }
}
