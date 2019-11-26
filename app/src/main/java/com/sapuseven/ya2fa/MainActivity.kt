package com.sapuseven.ya2fa

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var listRefreshHandler: Handler
    private lateinit var adapter: EntryListAdapter

    private val otpUpdate = object : Runnable {
        override fun run() {
            adapter.notifyDataSetChanged()
            listRefreshHandler.postDelayed(this, millisUntilNextUpdate())
        }
    }

    private fun millisUntilNextUpdate(): Long {
        return (30 * 1000) - System.currentTimeMillis() % (30 * 1000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val items = arrayListOf(TokenEntry("test", "Test", "Issuer"))
        adapter = EntryListAdapter(items)

        rvEntries.adapter = adapter
        rvEntries.layoutManager = LinearLayoutManager(this)

        listRefreshHandler = Handler(Looper.getMainLooper())

        listRefreshHandler.post(otpUpdate)
    }

    override fun onPause() {
        super.onPause()
        listRefreshHandler.removeCallbacks(otpUpdate)
    }

    override fun onResume() {
        super.onResume()
        listRefreshHandler.post(otpUpdate)
    }
}
