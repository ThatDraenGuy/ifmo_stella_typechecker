package ru.draen.stella.typecheck;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import ru.draen.stella.generated.StellaLexer;
import ru.draen.stella.generated.StellaParser;

public class TypeChecker {
    public void checkTypes(CharStream input) {
        StellaLexer lexer = new StellaLexer(input);
        StellaParser parser = new StellaParser(new CommonTokenStream(lexer));
        parser.start_Program().accept(new TypeCheckVisitor(new TypeCheckRegistry()));
    }
}
