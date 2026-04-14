package ru.draen.stella.typecheck.exceptions;

public abstract class ErrorMissingDataForLabel extends TypeCheckException {
    @Override
    protected ErrorType getErrorType() {
        return ErrorType.getByException(ErrorMissingDataForLabel.class);
    }
}
