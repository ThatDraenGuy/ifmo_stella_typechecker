import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.draen.stella.typecheck.TypeChecker;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TypecheckerTest {
    private final TypeChecker typeChecker = new TypeChecker();

    private static Stream<Path> validFiles() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(TypecheckerTest.class.getResource("valid")).toURI();
        Path resourcesPath = Paths.get(uri);
        return Files.list(resourcesPath);
    }

    @ParameterizedTest
    @MethodSource("validFiles")
    public void valid(Path filePath) throws IOException {
        CharStream src = CharStreams.fromPath(filePath);
        assertDoesNotThrow(() -> typeChecker.checkTypes(src));
    }

}
