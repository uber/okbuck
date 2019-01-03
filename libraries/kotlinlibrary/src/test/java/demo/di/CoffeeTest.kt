package demo.di

import org.junit.Test

/**
 * Assures both main kapt and testKapt works
 */
class CoffeeTest {

    @Test
    fun mainComponent() {
        @Suppress("UNUSED_VARIABLE") val coffee = DaggerCoffeeShop.builder().build()
    }

    @Test
    fun testComponent() {
        @Suppress("UNUSED_VARIABLE") val coffeeShop =  DaggerTestCoffeeShop.builder().build()
    }
}
