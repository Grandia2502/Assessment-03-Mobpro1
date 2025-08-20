package com.grandiamuhammad3096.assessment03.ui.crop

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.canhub.cropper.CropImageView
import com.grandiamuhammad3096.assessment03.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyCropperActivity : AppCompatActivity() {

    private lateinit var cropView: CropImageView
    private lateinit var btnSave: com.google.android.material.button.MaterialButton
    private var hasImage = false

    private lateinit var pickGallery: ActivityResultLauncher<String>
    private lateinit var takePicture: ActivityResultLauncher<Uri>
    private var cameraTempUri: Uri? = null

    private lateinit var tvNoImage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_cropper)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.potong_foto)

        tvNoImage = findViewById(R.id.tvNoImage)
        cropView = findViewById(R.id.cropView)
        btnSave  = findViewById(R.id.btnSave)

        // Guidelines (coba method, fallback property)
        try { cropView.guidelines = CropImageView.Guidelines.ON } catch (_: Throwable) {}
        cropView.setFixedAspectRatio(true)

        // Activity Result
        pickGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                hasImage = true
                updateSaveUI()
                cropView.setImageUriAsync(uri)
            }
        }
        takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            if (ok && cameraTempUri != null) {
                hasImage = true
                updateSaveUI()
                cropView.setImageUriAsync(cameraTempUri)
            }
        }

        // Start: kalau ada intent data → anggap sudah punya gambar
        intent?.data?.let {
            hasImage = true
            updateSaveUI()
            cropView.setImageUriAsync(it)
        } ?: run {
            hasImage = false
            updateSaveUI()
            // (opsional) langsung buka galeri:
            // pickGallery.launch("image/*")
        }

        findViewById<View>(R.id.btnGallery).setOnClickListener { pickGallery.launch("image/*") }
        findViewById<View>(R.id.btnCamera).setOnClickListener {
            cameraTempUri = createTempImageUri()
            takePicture.launch(cameraTempUri!!)
        }
        btnSave.setOnClickListener {
            if (!hasImage) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            } else {
                saveCroppedAndFinish()
            }
        }
    }

    private fun updateSaveUI() {
        // Ubah teks tombol bawah
        btnSave.text = getString(if (hasImage) R.string.pilih else R.string.batal)
        // Tampilkan teks "Belum ada foto" kalau tidak ada gambar
        tvNoImage.visibility = if (hasImage) View.GONE else View.VISIBLE
        // Ubah judul item menu toolbar
        invalidateOptionsMenu()
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.my_cropper_menu, menu)
//        return true
//    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_save)?.title =
            getString(if (hasImage) R.string.pilih else R.string.batal)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home   -> { finish(); true }
        R.id.action_save    -> {
            if (!hasImage) {
                setResult(Activity.RESULT_CANCELED); finish()
            } else {
                saveCroppedAndFinish()
            }
            true
        }
        R.id.action_gallery -> { pickGallery.launch("image/*"); true }
        R.id.action_camera  -> { cameraTempUri = createTempImageUri(); takePicture.launch(cameraTempUri!!); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun createTempImageUri(): Uri {
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File.createTempFile("camera_$time", ".jpg", cacheDir)
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }

    private fun saveCroppedAndFinish() {
        val bmp: Bitmap? = cropView.getCroppedImage() // sinkron
        if (bmp == null) { setResult(Activity.RESULT_CANCELED); finish(); return }
        val outFile = File.createTempFile("crop_", ".jpg", cacheDir)
        FileOutputStream(outFile).use {
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, it)
            it.flush()
        }
        val outUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", outFile)
        setResult(Activity.RESULT_OK, Intent().setData(outUri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        finish()
    }
}