package demo 

import org.junit.Test
import kotlin.test.assertEquals

class HelloWorldTest {
    @Test fun f() {
        assertEquals("Hello, world!", getGreeting())
    }
    @Test fun fooVoid() {
        assertEquals("Hello, world!", JavaClass().kotlinGreeting)
    }
}
