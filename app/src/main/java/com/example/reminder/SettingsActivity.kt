package com.example.reminder

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class SettingsActivity : AppCompatActivity() {

    private lateinit var preview: ImageView

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            if (BackgroundManager.saveBackground(this, uri)) {
                refreshPreview()
                Toast.makeText(this, "背景已更換", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "讀取圖片失敗", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        preview = findViewById(R.id.img_bg_preview)
        refreshPreview()

        findViewById<Button>(R.id.btn_pick_bg).setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        findViewById<Button>(R.id.btn_reset_bg).setOnClickListener {
            BackgroundManager.clearBackground(this)
            refreshPreview()
            Toast.makeText(this, "已恢復預設背景", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_settings_back).setOnClickListener { finish() }
    }

    private fun refreshPreview() {
        if (BackgroundManager.hasCustomBackground(this)) {
            val f = File(filesDir, "custom_bg.jpg")
            preview.setImageBitmap(BitmapFactory.decodeFile(f.absolutePath))
        } else {
            preview.setImageResource(R.drawable.bg_1)
        }
    }
}
