package demo.di

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(DripCoffeeModule::class))
interface TestCoffeeShop : CoffeeShop {
}
