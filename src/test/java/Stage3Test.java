import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Stage3Test extends TypecheckerTestBase {
    @Override
    protected String stage() {
        return "stage3";
    }
}