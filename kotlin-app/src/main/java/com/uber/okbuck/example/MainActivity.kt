package com.uber.okbuck.example

import android.graphics.ColorFilter
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import com.uber.okbuck.example.sqldelightmodel.GithubRepo
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var analytics: Analytics

    lateinit var smile : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        smile = findViewById(R.id.icon_smile)
        smile.tag = "test"

        fab.setOnClickListener {
            analytics.sendAnalyticsEvent("FAB Clicked")
        }

        val repo = GithubRepo.create(100, "OkBuck", "auto buck")
        Toast.makeText(this, repo.name + ": " + repo.description, Toast.LENGTH_SHORT).show()
    }

    companion object {
      internal const val TEST_INTERNAL = ""
    }
}
