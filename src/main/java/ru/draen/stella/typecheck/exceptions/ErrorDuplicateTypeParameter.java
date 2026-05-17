package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorDuplicateTypeParameter extends TypeCheckException {
    private final StellaType.NamedVar typeParam;
    private final ParserRuleContext genericCtx;

    public ErrorDuplicateTypeParameter(StellaType.NamedVar typeParam, StellaParser.DeclFunGenericContext genericCtx) {
        this.typeParam = typeParam;
        this.genericCtx = genericCtx;
    }
    public ErrorDuplicateTypeParameter(StellaType.NamedVar typeParam, StellaParser.TypeAbstractionContext genericCtx) {
        this.typeParam = typeParam;
        this.genericCtx = genericCtx;
    }
    public ErrorDuplicateTypeParameter(StellaType.NamedVar typeParam, StellaParser.TypeForAllContext genericCtx) {
        this.typeParam = typeParam;
        this.genericCtx = genericCtx;
    }

    @Override
    protected ParserRuleContext getSource() {
        return genericCtx;
    }

    @Override
    protected String reportText() {
        return "Переменная типа с именем " + typeParam + " задана несколько раз в одном списке переменных типа";
    }
}
