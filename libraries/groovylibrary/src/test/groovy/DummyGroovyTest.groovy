import com.uber.okbuck.groovy.example.DummyGroovy
import com.uber.okbuck.groovy.example.DummyJava
import org.junit.Test

class DummyGroovyTest {

    @Test
    public void subtractTest() {
        assert DummyGroovy.subtract(5, 2) == 3
    }

    @Test
    public void addTest() {
        assert DummyJava.add(5, 2) == 7
    }
}
