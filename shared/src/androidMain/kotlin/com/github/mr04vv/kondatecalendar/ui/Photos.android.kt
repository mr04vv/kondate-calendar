package com.github.mr04vv.kondatecalendar.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

private const val PHOTO_DIR = "photos"
private const val PHOTO_MAX_PX = 1280

/** Decodes [path] downsampled to about [PHOTO_MAX_PX] and upright per its EXIF orientation. */
actual fun loadPhoto(path: String): ImageBitmap? {
    val file = File(path)
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= PHOTO_MAX_PX) sample *= 2
    val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return null
    val degrees = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    val upright = if (degrees == 0f) {
        bitmap
    } else {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
    }
    return upright.asImageBitmap()
}

actual fun deletePhoto(path: String) {
    File(path).delete()
}

@Composable
actual fun PhotoSection(photoPath: String?, onChange: (String?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingCameraFile
        if (saved && file != null) onChange(file.path) else file?.delete()
        pendingCameraFile = null
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    onChange(copyIntoPhotos(context, uri).path)
                } catch (e: IOException) {
                    Toast.makeText(context, "写真を読み込めませんでした: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SectionTitle("写真")
    photoPath?.let { rememberPhoto(it) }?.let { photo ->
        Image(
            photo,
            contentDescription = "料理の写真",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(160.dp).clip(RoundedCornerShape(16.dp)),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            val file = newPhotoFile(context)
            pendingCameraFile = file
            try {
                takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            } catch (e: ActivityNotFoundException) {
                pendingCameraFile = null
                Toast.makeText(context, "カメラアプリが見つかりません", Toast.LENGTH_SHORT).show()
            }
        }) { Text("カメラで撮る") }
        OutlinedButton(onClick = {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) { Text("ギャラリーから選ぶ") }
    }
    if (photoPath != null) {
        TextButton(onClick = { onChange(null) }) { Text("写真を外す") }
    }
}

private fun newPhotoFile(context: Context): File =
    File(context.filesDir, PHOTO_DIR).apply { mkdirs() }.let { File(it, "${UUID.randomUUID()}.jpg") }

// ponytail: a photo picked or taken but never saved stays in filesDir; sweep unreferenced files if storage matters.
private suspend fun copyIntoPhotos(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
    val file = newPhotoFile(context)
    val input = context.contentResolver.openInputStream(uri) ?: throw IOException("cannot open $uri")
    input.use { source -> file.outputStream().use { source.copyTo(it) } }
    file
}
