package com.stockpyme.app.ui.comun

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Abre el menú de compartir de Android con un texto (por ejemplo, un pedido a un proveedor). */
fun compartirTexto(context: Context, texto: String, asunto: String, email: String? = null) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, asunto)
        putExtra(Intent.EXTRA_TEXT, texto)
        if (email != null) putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
    }
    context.startActivity(Intent.createChooser(intent, asunto))
}

/**
 * Guarda [bytes] en un archivo temporal de la app y abre el menú de compartir con él
 * (correo, Drive, WhatsApp...). Se comparte a través de un FileProvider: las demás apps
 * solo pueden leer ese archivo, no el resto de la carpeta de la app.
 */
fun compartirArchivo(context: Context, bytes: ByteArray, nombre: String, tipo: String = "text/csv") {
    val carpeta = File(context.cacheDir, "exportaciones").apply { mkdirs() }
    carpeta.listFiles()?.forEach { it.delete() } // solo se conserva la última exportación
    val archivo = File(carpeta, nombre).apply { writeBytes(bytes) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
    val intent = Intent(Intent.ACTION_SEND).apply {
        this.type = tipo
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, nombre)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, nombre))
}
