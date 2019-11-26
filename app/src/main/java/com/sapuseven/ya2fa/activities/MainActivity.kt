package com.sapuseven.ya2fa.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.sapuseven.ya2fa.adapters.TokenListAdapter
import com.sapuseven.ya2fa.R
import com.sapuseven.ya2fa.adapters.TokenEntry
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var listRefreshHandler: Handler
    private lateinit var adapter: TokenListAdapter

    companion object {
        private const val REQUEST_CODE_SCANNER = 1
        private const val PERMISSION_REQUEST_CAMERA = 2
    }

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
        adapter = TokenListAdapter(items)

        rvEntries.adapter = adapter
        rvEntries.layoutManager = LinearLayoutManager(this)

        listRefreshHandler = Handler(Looper.getMainLooper())
        listRefreshHandler.post(otpUpdate)

        fab.addOnMenuItemClickListener { _, _, itemId ->
            when (itemId) {
                R.id.fab_scan -> {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    )
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.CAMERA),
                            PERMISSION_REQUEST_CAMERA
                        )
                    else
                        startActivityForResult(
                            Intent(this, ScannerActivity::class.java),
                            REQUEST_CODE_SCANNER
                        )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CAMERA ->
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                    startActivityForResult(
                        Intent(this, ScannerActivity::class.java),
                        REQUEST_CODE_SCANNER
                    )
        }
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
