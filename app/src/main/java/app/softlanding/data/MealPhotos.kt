package app.softlanding.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * Photos of what was eaten (#9): a food diary in pictures, beside the numbers. They are taken with the phone's own
 * camera and kept as files in the app's folder, one name per photo – «<day>_<MEAL>_<time>.jpg» – so no table is needed.
 */
object MealPhotos {
    /** Bumped on every change, so the screens read the folder again. */
    val version = MutableStateFlow(0)

    private fun dir(ctx: Context) = File(ctx.filesDir, "meals").apply { mkdirs() }

    /** The photos of one meal of one day, oldest first. */
    fun of(ctx: Context, day: Long, meal: Meal): List<File> =
        dir(ctx).listFiles { f -> f.name.startsWith("${day}_${meal.name}_") && f.name.endsWith(".jpg") }.orEmpty().sortedBy { it.name }

    /** A file for the camera to write the next photo into, and the address it is handed by. */
    fun next(ctx: Context, day: Long, meal: Meal): Pair<File, Uri> {
        val f = File(dir(ctx), "${day}_${meal.name}_${System.currentTimeMillis()}.jpg")
        return f to FileProvider.getUriForFile(ctx, ctx.packageName + ".shots", f)
    }

    /** After the camera: an empty file goes; a big one is turned upright and made smaller (1600 px), to keep the phone light. */
    fun settle(f: File, ok: Boolean) {
        if (!ok || !f.exists() || f.length() == 0L) { f.delete(); version.value++; return }
        runCatching {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, o)
            var k = 1
            while (maxOf(o.outWidth, o.outHeight) / (k * 2) >= 1600) k *= 2
            val b = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = k }) ?: return@runCatching
            val turn = when (ExifInterface(f.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f; ExifInterface.ORIENTATION_ROTATE_180 -> 180f; ExifInterface.ORIENTATION_ROTATE_270 -> 270f; else -> 0f
            }
            val up = if (turn != 0f) Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(turn) }, true) else b
            f.outputStream().use { up.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        }
        version.value++
    }

    fun delete(f: File) { f.delete(); version.value++ }

    /** A small picture for the list. */
    fun thumb(f: File, px: Int): Bitmap? = runCatching {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        var k = 1
        while (minOf(o.outWidth, o.outHeight) / (k * 2) >= px) k *= 2
        BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = k })
    }.getOrNull()
}
