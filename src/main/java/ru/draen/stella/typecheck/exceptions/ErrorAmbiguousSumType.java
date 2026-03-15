package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;

public class ErrorAmbiguousSumType extends TypeCheckException {
    private final InjectionContext injection;

    public ErrorAmbiguousSumType(StellaParser.InlContext inl) {
        this.injection = new InjectionContext.Inl(inl);
    }

    public ErrorAmbiguousSumType(StellaParser.InrContext inr) {
        this.injection = new InjectionContext.Inr(inr);
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
        }) + "Не удалось определить полный тип типа-суммы";
    }
}
