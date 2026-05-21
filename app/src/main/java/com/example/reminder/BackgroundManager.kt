package com.example.reminder

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.view.View
import java.io.File

/**
 * 管理 App 自訂背景圖：選圖後降採樣存到內部空間，套用時疊上半透明白遮罩維持可讀性。
 */
object BackgroundManager {

    private const val PREF = "app_settings"
    private const val KEY_HAS_BG = "has_custom_bg"
    private const val FILE_NAME = "custom_bg.jpg"
    private const val MAX_DIMEN = 1600           // 降採樣目標邊長，避免 OOM

    fun hasCustomBackground(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_HAS_BG, false) &&
            bgFile(context).exists()

    /** 從相簿選的 Uri 讀圖、降採樣後存到內部空間 */
    fun saveBackground(context: Context, uri: Uri): Boolean {
        return try {
            // 先讀邊界算降採樣比例
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            var w = bounds.outWidth
            var h = bounds.outHeight
            while (w / sample > MAX_DIMEN || h / sample > MAX_DIMEN) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return false

            bgFile(context).outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            bitmap.recycle()
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_HAS_BG, true).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 恢復預設背景 */
    fun clearBackground(context: Context) {
        bgFile(context).delete()
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_HAS_BG, false).apply()
    }

    /** 套用到指定 View 的背景（自訂圖則疊白遮罩，否則用預設 drawable） */
    fun applyTo(view: View) {
        val context = view.context
        if (hasCustomBackground(context)) {
            val bmp = BitmapFactory.decodeFile(bgFile(context).absolutePath)
            if (bmp != null) {
                val image = BitmapDrawable(context.resources, bmp).apply {
                    gravity = android.view.Gravity.FILL
                }
                val overlay = ColorDrawable(Color.argb(0x70, 0xFF, 0xFF, 0xFF))
                val layers: Array<Drawable> = arrayOf(image, overlay)
                view.background = LayerDrawable(layers)
                return
            }
        }
        view.setBackgroundResource(R.drawable.overlay_image_bg1)
    }

    private fun bgFile(context: Context): File = File(context.filesDir, FILE_NAME)
}
