package com.example.propertyconsultancy.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream

object FileUtils {

    private const val TAG = "FileUtils"
    private const val MAX_IMAGE_SIZE = 800 // Reduced from 1024
    private const val COMPRESSION_QUALITY = 50 // Reduced from 70

    /**
     * Efficiently encodes a Uri to a Base64 string with optional image compression.
     */
    fun encodeUriToBase64(context: Context, uri: Uri): String? {
        if (uri.scheme?.startsWith("http") == true) return null
        
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: ""
        
        return try {
            if (mimeType.startsWith("image/")) {
                encodeImageToBase64(context, uri)
            } else {
                // For non-image files (like video), we read as is but with size caution
                encodeRawToBase64(context, uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding Uri to Base64: ${e.message}")
            null
        }
    }

    private fun encodeImageToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            
            // 1. Decode with inJustDecodeBounds to get dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            // 2. Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
            options.inJustDecodeBounds = false
            
            // 3. Decode the actual bitmap
            val compressedInputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(compressedInputStream, null, options)
            compressedInputStream.close()
            
            if (bitmap == null) return null
            
            // 4. Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
            val bytes = outputStream.toByteArray()
            bitmap.recycle() // Free memory
            
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Image compression error: ${e.message}")
            null
        }
    }

    private fun encodeRawToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            
            // Check size before reading everything into memory
            val fileSize = getFileSize(context, uri)
            if (fileSize > 50 * 1024 * 1024) { // Increased to 50MB limit for raw Base64 (videos)
                Log.w(TAG, "File too large for Base64 encoding: $fileSize bytes")
                return null
            }
            
            val bytes = inputStream.readBytes()
            inputStream.close()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Raw encoding error: ${e.message}")
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
