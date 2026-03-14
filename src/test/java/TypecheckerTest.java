import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.draen.stella.typecheck.TypeChecker;
import ru.draen.stella.typecheck.exceptions.ErrorType;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TypecheckerTest {
    private final TypeChecker typeChecker = new TypeChecker();

    private static Stream<Path> wellTyped() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(TypecheckerTest.class.getResource("test_suite/stage1/well-typed")).toURI();
        Path wellTypedPath = Paths.get(uri);
        return Files.list(wellTypedPath);
    }

    private static Stream<IllTypedFile> illTyped() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(TypecheckerTest.class.getResource("test_suite/stage1/ill-typed")).toURI();
        Path illTypedPath = Paths.get(uri);
        return Arrays.stream(ErrorType.values()).flatMap(errorType -> {
            Path errorTypePath = illTypedPath.resolve(errorType.name().toLowerCase().replace("error_", ""));
            try {
                return Files.list(errorTypePath).map(file -> new IllTypedFile(errorType, file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("wellTyped")
    public void wellTyped(Path filePath) throws IOException {
        CharStream src = CharStreams.fromPath(filePath);
        assertDoesNotThrow(() -> typeChecker.checkTypes(src));
    }

    @ParameterizedTest
    @MethodSource("illTyped")
    public void illTyped(IllTypedFile illTyped) throws IOException {
        CharStream src = CharStreams.fromPath(illTyped.file);
        assertThrows(illTyped.errorType.getErrorClass(), () -> typeChecker.checkTypes(src));
    }

    public record IllTypedFile(ErrorType errorType, Path file) {}

    @Test
    @Disabled
    public void temp() {
        CharStream src = CharStreams.fromString(
                """
language core;
extend with #structural-patterns, #sum-types, #natural-literals, #tuples;
extend with #records, #lists, #unit-type, #variants;

fn main(input : [Nat]) -> Nat {
  return
    match input {
      [] => 0
      |[n] => 0
      |cons(n, cons(succ(m), rest)) => 0
      |cons(0, cons(0, rest)) => 0
      |cons(succ(n), cons(0, rest)) => 0
   }
}
//language core;
//extend with #structural-patterns, #sum-types, #natural-literals, #tuples;
//extend with #records, #lists, #unit-type, #variants;
//
//fn main(input : Nat) -> Nat {
//  return
//    match input {
//      0 => 0
//      | succ(succ(succ(succ(succ(succ(n)))))) => 0
//      | 3 => 0
//      | 1 => 0
//      | 4 => 0
//      | 2 => 0
//      | 5 => 0
//      | 7 => 0              //7
//      | 6 => 0
//      | succ(succ(succ(0))) => 0  //3
//      | succ(0) => 0        //1
//      | 9 => 0
//      | 8 => 0
//      | 10 => 0
//      | succ(succ(succ(succ(succ(succ(succ(succ(succ(succ(succ(n))))))))))) => 0  //11
//      
//      
////      | succ(succ(succ(succ(n)))) => n
//   }
//}
                        """
        );
        assertDoesNotThrow(() -> typeChecker.checkTypes(src));
    }
    //[succ(0-1), succ(3+)]
    //1-2, 4+
    //[succ(0), succ(succ(0)), succ(succ(succ(succ(__something__))))]
    //1, 2, 4+
}
