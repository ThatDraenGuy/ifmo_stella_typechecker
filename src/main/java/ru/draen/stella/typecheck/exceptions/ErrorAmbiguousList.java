package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;


public class ErrorAmbiguousList extends TypeCheckException {
    private final StellaParser.ListContext list;

    public ErrorAmbiguousList(StellaParser.ListContext list) {
        this.list = list;
    }

    @Override
    protected ParserRuleContext getSource() {
        return list;
    }

    @Override
    protected String reportText() {
        return "Не удалось определить тип списка";
    }
}
