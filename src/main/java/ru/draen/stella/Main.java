package ru.draen.stella;


import org.antlr.v4.runtime.CharStreams;
import ru.draen.stella.typecheck.TypeChecker;
import ru.draen.stella.typecheck.exceptions.TypeCheckException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        try {
            new TypeChecker().checkTypes(CharStreams.fromStream(System.in));
        } catch (TypeCheckException e) {
            System.err.println(e.report());
            System.exit(1);
        }
    }
}