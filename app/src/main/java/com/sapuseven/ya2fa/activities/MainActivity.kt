package com.sapuseven.ya2fa.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.sapuseven.ya2fa.R
import com.sapuseven.ya2fa.adapters.TokenListAdapter
import com.sapuseven.ya2fa.data.Token
import com.sapuseven.ya2fa.data.TokenDatabase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var listRefreshHandler: Handler
    private lateinit var adapter: TokenListAdapter
    private lateinit var tokenDatabase: TokenDatabase

    private val tokens = ArrayList<Token>()

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

        tokenDatabase = Room
            .databaseBuilder(applicationContext, TokenDatabase::class.java, "tokens")
            .build()

        adapter = TokenListAdapter(tokens)

        rvEntries.adapter = adapter
        rvEntries.layoutManager = LinearLayoutManager(this@MainActivity)

        listRefreshHandler = Handler(Looper.getMainLooper())
        listRefreshHandler.post(otpUpdate)

        fab.addOnMenuItemClickListener { _, _, itemId ->
            when (itemId) {
                R.id.fab_scan -> {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    )
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.CAMERA),
                            PERMISSION_REQUEST_CAMERA
                        )
                    else
                        startActivityForResult(
                            Intent(this@MainActivity, ScannerActivity::class.java),
                            REQUEST_CODE_SCANNER
                        )
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            tokens.addAll(tokenDatabase.tokenDao().getAll())
            pbLoading.visibility = View.GONE

            adapter.notifyDataSetChanged()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_SCANNER -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    addItem(Token.fromUrl(Uri.parse(data.getStringExtra(ScannerActivity.EXTRA_STRING_URL))))
                }
            }
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

    private fun addItem(token: Token) {
        CoroutineScope(Dispatchers.IO).launch {
            tokenDatabase.tokenDao().insertAll(token)

            CoroutineScope(Dispatchers.Main).launch {
                tokens.add(token)
                adapter.notifyDataSetChanged()
            }
        }
    }
}
