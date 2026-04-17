package ru.draen.stella.typecheck.exceptions;

public abstract class ErrorUnexpectedTupleLength extends TypeCheckException {
    @Override
    protected ErrorType getErrorType() {
        return ErrorType.getByException(ErrorUnexpectedTupleLength.class);
    }
}
