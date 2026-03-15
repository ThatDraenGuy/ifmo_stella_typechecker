package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorDuplicateRecordFields extends TypeCheckException {
    private final StellaParser.RecordContext record;
    private final String field;

    public ErrorDuplicateRecordFields(StellaParser.RecordContext record, String field) {
        this.record = record;
        this.field = field;
    }

    @Override
    protected String reportText() {
        return reportSource(record) + "Поле \"" + field + "\" объявлено больше одного раза";
    }
}
