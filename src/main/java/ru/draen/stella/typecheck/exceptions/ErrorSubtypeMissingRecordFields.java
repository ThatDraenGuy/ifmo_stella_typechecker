package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.typecheck.StellaType;

public class ErrorSubtypeMissingRecordFields extends ErrorMissingRecordFields {
    private final ParserRuleContext record;
    private final StellaType.Record expected;
    private final StellaType.Record actual;
    private final String fieldName;

    public ErrorSubtypeMissingRecordFields(ParserRuleContext record, StellaType.Record expected, StellaType.Record actual, String fieldName) {
        this.record = record;
        this.expected = expected;
        this.actual = actual;
        this.fieldName = fieldName;
    }

    @Override
    protected ParserRuleContext getSource() {
        return record;
    }

    @Override
    protected String reportText() {
        return "В записи отсутствует необходимое поле \"" + fieldName
                + "\";\nОжидаемый тип записи: " + expected
                + "\";\nВыведенный тип записи: " + actual;
    }
}
