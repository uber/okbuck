package demo;

import org.junit.Test;

import static kotlin.test.AssertionsKt.assertEquals;

public class JavaClassTest {

  @Test public void testGetGreetingJava() {
    assertEquals(HelloWorldKt.getGreeting(), new JavaClass().getKotlinGreeting(),
            "Java and kotlin greeting should be the same");
  }
}
