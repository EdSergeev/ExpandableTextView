package com.github.edsergeev.expandabletextview.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.edsergeev.expandabletextview.ExpandableTextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ExpandableTextView>(R.id.expanded_expand_tv).expand()
    }
}