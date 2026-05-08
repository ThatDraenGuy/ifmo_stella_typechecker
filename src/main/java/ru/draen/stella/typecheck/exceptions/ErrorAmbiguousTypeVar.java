package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.typecheck.StellaType;


public class ErrorAmbiguousTypeVar extends TypeCheckException {
    private final StellaType.FreshVar typeVar;

    public ErrorAmbiguousTypeVar(StellaType.FreshVar typeVar) {
        this.typeVar = typeVar;
    }

    @Override
    protected ParserRuleContext getSource() {
        return typeVar.source();
    }

    @Override
    protected String reportText() {
        return "Не удалось определить полный тип переменной типа " + typeVar;
    }
}
