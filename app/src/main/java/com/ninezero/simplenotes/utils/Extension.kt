package com.ninezero.simplenotes.utils

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS
import android.widget.ImageView
import android.widget.Toast
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.FutureTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

fun View.hideKeyboard() = (context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
    .hideSoftInputFromWindow(windowToken, HIDE_NOT_ALWAYS)

fun Context.shortToast(message: String?) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Context.loadHiRezThumbnail(
    uri: Uri?,
    image: ImageView
) = Glide.with(this)
    .load(uri)
    .override(500, 500)
    .diskCacheStrategy(DiskCacheStrategy.ALL)
    .thumbnail(0.1f)
    .transition(DrawableTransitionOptions.withCrossFade(200))
    .into(image)

suspend fun Context.asyncImageLoader(uri: Uri?, image: ImageView, job: CoroutineScope) {
    val bitmap = job.async(Dispatchers.IO, CoroutineStart.DEFAULT) {
        val futureTarget: FutureTarget<Bitmap> = Glide.with(this@asyncImageLoader)
            .asBitmap()
            .load(uri)
            .submit(1500, 1500)
        return@async futureTarget.get()
    }
    try {
        Glide.with(this)
            .load(bitmap.await())
            .thumbnail(0.01f)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .transition(DrawableTransitionOptions.withCrossFade(200))
            .into(image)
    } catch (e: IllegalArgumentException) {
        Log.e("asyncImageLoader", e.stackTraceToString())
    }
}

fun View.doOnApplyWindowInsets(windowInsetsListener: (insetView: View, windowInsets: WindowInsetsCompat,
                                                      initialPadding: Insets, initialMargins: Insets) -> Unit) {
    val initialPadding = Insets.of(paddingLeft, paddingTop, paddingRight, paddingBottom)
    val initialMargins = Insets.of(marginLeft, marginTop, marginRight, marginBottom)

    ViewCompat.setOnApplyWindowInsetsListener(this) { insetView, windowInsets ->
        windowInsets.also {
            windowInsetsListener(insetView, windowInsets, initialPadding, initialMargins)
        }
    }

    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            v.requestApplyInsets()
        }

        override fun onViewDetachedFromWindow(v: View) = Unit
    })

    if (isAttachedToWindow) {
        requestApplyInsets()
    }
}