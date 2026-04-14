package ru.draen.stella.typecheck.exceptions;

public abstract class ErrorMissingRecordFields extends TypeCheckException {
    @Override
    protected ErrorType getErrorType() {
        return ErrorType.getByException(ErrorMissingRecordFields.class);
    }
}
