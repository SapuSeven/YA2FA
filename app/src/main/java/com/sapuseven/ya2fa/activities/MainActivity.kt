package com.sapuseven.ya2fa.activities

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.sapuseven.ya2fa.R
import com.sapuseven.ya2fa.adapters.TokenListAdapter
import com.sapuseven.ya2fa.data.Token
import com.sapuseven.ya2fa.data.TokenDatabase
import com.sapuseven.ya2fa.utils.TokenCalculator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.codec.binary.Base32

class MainActivity : AppCompatActivity() {
    private lateinit var listRefreshHandler: Handler
    private lateinit var adapter: TokenListAdapter
    private lateinit var tokenDatabase: TokenDatabase
    private lateinit var sharedPrefs: SharedPreferences

    private val tokens = mutableListOf<Token>()

    companion object {
        private const val REQUEST_CODE_SCANNER = 1
        private const val PERMISSION_REQUEST_CAMERA = 2

        private const val PREFERENCE_KEY_FORCE_DARK_THEME = "force_dark_theme"
    }

    private val otpUpdate = object : Runnable {
        override fun run() {
            adapter.notifyDataSetChanged()
            millisUntilNextUpdate().let { millis ->
                listRefreshHandler.postDelayed(this, millis)
                startProgressBarAnimation(millis)
            }
        }
    }

    private fun startProgressBarAnimation(duration: Long) {
        val animation = ObjectAnimator.ofInt(pbInterval, "progress", (duration / 10).toInt(), 0)
        animation.duration = duration
        animation.interpolator = LinearInterpolator()
        animation.start()
    }

    private fun millisUntilNextUpdate(): Long {
        return (30 * 1000) - System.currentTimeMillis() % (30 * 1000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPrefs = getDefaultSharedPreferences(this)

        if (sharedPrefs.getBoolean(PREFERENCE_KEY_FORCE_DARK_THEME, false))
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        else
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tokenDatabase = Room
            .databaseBuilder(applicationContext, TokenDatabase::class.java, "tokens")
            .build()

        adapter = TokenListAdapter(tokens, View.OnClickListener { v ->
            val itemPosition = rvEntries.getChildLayoutPosition(v)
            val item = adapter.getItemAt(itemPosition)

            val code = TokenCalculator.TOTP_RFC6238(
                Base32().decode(item.secret),
                item.period ?: TokenCalculator.TOTP_DEFAULT_PERIOD,
                item.length,
                TokenCalculator.HashAlgorithm.valueOf(item.algorithm)
            )

            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("OTP Code", code))

            Toast.makeText(this, "Auth code copied to clipboard", Toast.LENGTH_SHORT).show()
        }, View.OnLongClickListener { v ->
            val itemPosition = rvEntries.getChildLayoutPosition(v)
            val item = adapter.getItemAt(itemPosition)

            val dialogView = layoutInflater.inflate(R.layout.dialog_add_token, null)

            dialogView.findViewById<TextInputEditText>(R.id.etLabelInput).setText(item.label)
            dialogView.findViewById<TextInputEditText>(R.id.etIssuerInput).setText(item.issuer)
            dialogView.findViewById<TextInputEditText>(R.id.etKeyInput).setText(item.secret)

            MaterialAlertDialogBuilder(this)
                .setTitle("Edit account")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val newToken = item.copy(
                        label = dialogView.findViewById<TextInputEditText>(R.id.etLabelInput).text.toString(),
                        issuer = dialogView.findViewById<TextInputEditText>(R.id.etIssuerInput).text.toString(),
                        secret = dialogView.findViewById<TextInputEditText>(R.id.etKeyInput).text.toString()
                    )
                    updateItem(newToken)
                }
                .setNegativeButton("Cancel", null)
                /*.setNeutralButton("Delete") { dialogInterface, i ->

                }*/
                .show()

            true
        })

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
                R.id.fab_manual -> {
                    showManualInput()
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            tokens.addAll(tokenDatabase.tokenDao().getAll())
            pbLoading.visibility = View.GONE

            adapter.notifyDataSetChanged()
        }
    }

    private fun showManualInput() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_token, null)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add account")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val newToken = Token(
                    label = dialogView.findViewById<TextInputEditText>(R.id.etLabelInput).text.toString(),
                    issuer = dialogView.findViewById<TextInputEditText>(R.id.etIssuerInput).text.toString(),
                    secret = dialogView.findViewById<TextInputEditText>(R.id.etKeyInput).text.toString()
                )
                addItem(newToken)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_options, menu)
        menu.findItem(R.id.main_options_dark_mode).isChecked =
            sharedPrefs.getBoolean(PREFERENCE_KEY_FORCE_DARK_THEME, false)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.main_options_dark_mode -> {
                sharedPrefs.edit().apply {
                    putBoolean(PREFERENCE_KEY_FORCE_DARK_THEME, !item.isChecked)
                }.apply()
                recreate()
                true
            }
            R.id.main_options_about -> {
                showAbout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAbout() {
        LibsBuilder()
            .withActivityStyle(
                if (
                    sharedPrefs.getBoolean(PREFERENCE_KEY_FORCE_DARK_THEME, false)
                    || applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                )
                    Libs.ActivityStyle.DARK
                else
                    Libs.ActivityStyle.LIGHT_DARK_TOOLBAR
            )
            .withActivityTitle("About")
            .start(this)
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
                    try {
                        addItem(Token.fromUrl(Uri.parse(data.getStringExtra(ScannerActivity.EXTRA_STRING_URL))))
                    } catch (e: Token.Companion.InvalidUriException) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Invalid code")
                            .setMessage("The code you scanned is invalid.\nError message: ${e.message}")
                            .setPositiveButton("OK", null)
                            .show()
                    }
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

    private fun updateItem(token: Token) {
        if (!token.isValid()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Invalid data")
                .setMessage("The item you want to add is invalid.\nPlease check your input values and try again.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            tokenDatabase.tokenDao().update(token)

            CoroutineScope(Dispatchers.Main).launch {
                tokens.replaceAll(token) { it.id == token.id}
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun addItem(token: Token) {
        if (!token.isValid()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Invalid data")
                .setMessage("The item you want to add is invalid.\nPlease check your input values and try again.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            tokenDatabase.tokenDao().insertAll(token)

            CoroutineScope(Dispatchers.Main).launch {
                tokens.add(token)
                adapter.notifyDataSetChanged()
            }
        }
    }
}

private fun <E> MutableList<E>.replaceAll(replacement: E, predicate: (E) -> Boolean) {
    val iterate = listIterator()
    while (iterate.hasNext()) {
        val value = iterate.next()
        if (predicate(value)) iterate.set(replacement)
    }
}
