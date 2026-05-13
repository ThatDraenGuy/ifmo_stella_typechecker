package ru.draen.stella.typecheck;


import java.util.List;
import java.util.Map;

public interface StellaPatternResolver {
    Result resolve(List<StellaPattern> current);
    record Result(Map<String, StellaType> vars, List<StellaPattern> notExhausted) {}
}
