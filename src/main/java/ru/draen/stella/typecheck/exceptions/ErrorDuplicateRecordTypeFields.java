package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorDuplicateRecordTypeFields extends TypeCheckException {
    private final StellaParser.TypeRecordContext record;
    private final String field;

    public ErrorDuplicateRecordTypeFields(StellaParser.TypeRecordContext record, String field) {
        this.record = record;
        this.field = field;
    }

    @Override
    protected String reportText() {
        return reportSource(record) + "Поле \"" + field + "\" объявлено больше одного раза";
    }
}
