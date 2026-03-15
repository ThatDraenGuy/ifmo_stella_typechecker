package ru.draen.stella.typecheck.exceptions;

import ru.draen.stella.generated.StellaParser;
import ru.draen.stella.typecheck.StellaType;

public class ErrorUnexpectedList extends TypeCheckException {
    private final ListContext list;
    private final StellaType expected;


    public ErrorUnexpectedList(StellaParser.ConsListContext list, StellaType expected) {
        this.list = new ListContext.Cons(list);
        this.expected = expected;
    }

    public ErrorUnexpectedList(StellaParser.ListContext list, StellaType expected) {
        this.list = new ListContext.List(list);
        this.expected = expected;
    }

    private sealed interface ListContext {
        record List(StellaParser.ListContext list) implements ListContext {}
        record Cons(StellaParser.ConsListContext cons) implements ListContext {}
    }

    @Override
    protected String reportText() {
        return reportSource(switch (list) {
            case ListContext.List list1 -> list1.list;
            case ListContext.Cons cons -> cons.cons;
        }) + "Ожидаемый тип выражения не является списком;\nОжидаемый тип выражения: " + expected;
    }
}
