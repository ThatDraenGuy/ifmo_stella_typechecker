package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedInjection extends TypeCheckException {
    private final InjectionContext injection;
    private final StellaType expected;

    public ErrorUnexpectedInjection(StellaParser.InlContext inl, StellaType expected) {
        this.injection = new InjectionContext.Inl(inl);
        this.expected = expected;
    }

    public ErrorUnexpectedInjection(StellaParser.InrContext inr, StellaType expected) {
        this.injection = new InjectionContext.Inr(inr);
        this.expected = expected;
    }

    private sealed interface InjectionContext {
        record Inl(StellaParser.InlContext inl) implements InjectionContext {}
        record Inr(StellaParser.InrContext inr) implements InjectionContext {}
    }

    @Override
    protected String reportText() {
        return reportSource(switch (injection) {
            case InjectionContext.Inl inl -> inl.inl;
            case InjectionContext.Inr inr -> inr.inr;
        }) + "Ожидаемый тип выражения не является типом-суммой;\nОжидаемый тип выражения: " + expected;
    }
}
