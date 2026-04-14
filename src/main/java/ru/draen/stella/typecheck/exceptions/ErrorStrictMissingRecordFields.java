package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorStrictMissingRecordFields extends ErrorMissingRecordFields {
    private final StellaParser.RecordContext record;
    private final StellaType expected;
    private final String fieldName;

    public ErrorStrictMissingRecordFields(StellaParser.RecordContext record, StellaType expected, String fieldName) {
        this.record = record;
        this.expected = expected;
        this.fieldName = fieldName;
    }

    @Override
    protected ParserRuleContext getSource() {
        return record;
    }

    @Override
    protected String reportText() {
        return "В записи отсутствует необходимое поле \"" + fieldName
                + "\";\nОжидаемый тип записи: " + expected;
    }
}
