package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorDuplicateRecordPatternFields extends TypeCheckException {
    private final StellaParser.PatternRecordContext record;
    private final String field;

    public ErrorDuplicateRecordPatternFields(StellaParser.PatternRecordContext record, String field) {
        this.record = record;
        this.field = field;
    }

    @Override
    protected String reportText() {
        return reportSource(record) + "Поле \"" + field + "\" объявлено больше одного раза";
    }
}
