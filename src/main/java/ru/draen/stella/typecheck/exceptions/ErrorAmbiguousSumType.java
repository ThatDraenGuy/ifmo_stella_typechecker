package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorAmbiguousSumType extends TypeCheckException {
    private final InjectionContext injection;

    public ErrorAmbiguousSumType(StellaParser.InlContext inl) {
        this.injection = new InjectionContext.Inl(inl);
    }

    public ErrorAmbiguousSumType(StellaParser.InrContext inr) {
        this.injection = new InjectionContext.Inr(inr);
    }

    private sealed interface InjectionContext {
        record Inl(StellaParser.InlContext inl) implements ErrorAmbiguousSumType.InjectionContext {}
        record Inr(StellaParser.InrContext inr) implements ErrorAmbiguousSumType.InjectionContext {}
    }
}
