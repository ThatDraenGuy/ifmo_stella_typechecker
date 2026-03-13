package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedRecord extends TypeCheckException {
    private final StellaParser.RecordContext record;
    private final StellaType expected;

    public ErrorUnexpectedRecord(StellaParser.RecordContext record, StellaType expected) {
        this.record = record;
        this.expected = expected;
    }
}
