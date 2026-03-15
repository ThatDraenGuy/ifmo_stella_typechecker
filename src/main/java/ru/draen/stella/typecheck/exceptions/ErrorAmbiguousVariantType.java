package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorAmbiguousVariantType extends TypeCheckException {
    private final StellaParser.VariantContext variant;

    public ErrorAmbiguousVariantType(StellaParser.VariantContext variant) {
        this.variant = variant;
    }

    @Override
    protected ParserRuleContext getSource() {
        return variant;
    }

    @Override
    protected String reportText() {
        return "Не удалось определить полный тип типа-варианта";
    }
}
