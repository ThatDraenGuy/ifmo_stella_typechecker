package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedRecordFields extends TypeCheckException {
    private final StellaParser.RecordContext record;
    private final StellaType expected;
    private final String fieldName;

    public ErrorUnexpectedRecordFields(StellaParser.RecordContext record, StellaType expected, String fieldName) {
        this.record = record;
        this.expected = expected;
        this.fieldName = fieldName;
    }

    @Override
    protected String reportText() {
        return reportSource(record) + "Ожидаемый тип выражения не содержит поле с именем \"" + fieldName
                + "\";\nОжидаемый тип выражения: " + expected;
    }
}
