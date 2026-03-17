package ru.draen.stella.typecheck.exceptions;

import org.antlr.v4.runtime.ParserRuleContext;
import ru.draen.stella.generated.StellaParser;

public class ErrorDuplicateLetBinding extends TypeCheckException {
    private final LetCtx let;
    private final String parameter;

    public ErrorDuplicateLetBinding(StellaParser.LetContext let, String parameter) {
        this.let = new LetCtx.Let(let);
        this.parameter = parameter;
    }

    public ErrorDuplicateLetBinding(StellaParser.LetRecContext letRec, String parameter) {
        this.let = new LetCtx.LetRec(letRec);
        this.parameter = parameter;
    }

    @Override
    protected ParserRuleContext getSource() {
        return switch (let) {
            case LetCtx.Let let1 -> let1.let;
            case LetCtx.LetRec letRec -> letRec.letRec;
        };
    }

    @Override
    protected String reportText() {
        return "Связывание \"" + parameter + "\" объявлено в let-выражении больше одного раза";
    }

    private sealed interface LetCtx {
        record Let(StellaParser.LetContext let) implements LetCtx {}
        record LetRec(StellaParser.LetRecContext letRec) implements LetCtx {}
    }
}
