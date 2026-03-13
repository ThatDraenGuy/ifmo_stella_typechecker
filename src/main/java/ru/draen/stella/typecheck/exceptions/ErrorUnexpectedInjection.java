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
}
