package demo 

import kotlin.test.assertEquals
import org.junit.Test

class HelloWorldTest {
    @Test fun f() {
        assertEquals("Hello, world!", getGreeting())
    }
    @Test fun fooVoid() {
        assertEquals("Hello, world!", JavaClass().kotlinGreeting)
    }
}
