package demo.di

import org.junit.Test

/**
 * Assures both main kapt and testKapt works
 */
class CoffeeTest {

    @Test
    fun mainComponent() {
        val coffee = DaggerCoffeeShop.builder().build()
    }

    @Test
    fun testComponent() {
        val coffeeShop =  DaggerTestCoffeeShop.builder().build()
    }
}
