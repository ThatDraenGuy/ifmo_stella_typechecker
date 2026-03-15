package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorDuplicateRecordFields extends TypeCheckException {
    private final StellaParser.RecordContext record;
    private final String field;

    public ErrorDuplicateRecordFields(StellaParser.RecordContext record, String field) {
        this.record = record;
        this.field = field;
    }

    @Override
    protected ParserRuleContext getSource() {
        return record;
    }

    @Override
    protected String reportText() {
        return "Поле \"" + field + "\" объявлено больше одного раза";
    }
}
