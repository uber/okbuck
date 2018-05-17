package demo;

import static kotlin.test.AssertionsKt.assertEquals;

import org.junit.Test;

public class JavaClassTest {

  @Test
  public void testGetGreetingJava() {
    assertEquals(
        HelloWorldKt.getGreeting(),
        new JavaClass().getKotlinGreeting(),
        "Java and kotlin greeting should be the same");
  }
}
