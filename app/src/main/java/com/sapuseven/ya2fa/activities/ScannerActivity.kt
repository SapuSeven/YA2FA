package com.sapuseven.ya2fa.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.google.zxing.BarcodeFormat
import com.sapuseven.ya2fa.R

class ScannerActivity : AppCompatActivity() {
    private lateinit var codeScanner: CodeScanner

    companion object {
        const val EXTRA_STRING_URL = "com.sapuseven.ya2fa.activities.ScannerActivity.url"
        const val EXTRA_STRING_ERROR = "com.sapuseven.ya2fa.activities.ScannerActivity.error"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        codeScanner = CodeScanner(this, findViewById(R.id.scanner_view))

        codeScanner.apply {
            formats = listOf(BarcodeFormat.QR_CODE)

            decodeCallback = DecodeCallback {
                val data = Intent()
                data.putExtra(EXTRA_STRING_URL, it.text)
                setResult(Activity.RESULT_OK, data)
                finish()
            }

            errorCallback = ErrorCallback {
                val data = Intent()
                data.putExtra(EXTRA_STRING_ERROR, it.message)
                setResult(Activity.RESULT_CANCELED, data)
                finish()
            }
        }

        codeScanner.startPreview()
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }
}