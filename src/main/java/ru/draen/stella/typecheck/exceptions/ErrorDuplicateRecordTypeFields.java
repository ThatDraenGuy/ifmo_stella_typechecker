package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorDuplicateRecordTypeFields extends TypeCheckException {
    private final StellaParser.TypeRecordContext record;
    private final String field;

    public ErrorDuplicateRecordTypeFields(StellaParser.TypeRecordContext record, String field) {
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
