package com.guet.flexbox.litho.drawable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView.ScaleType
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.guet.flexbox.litho.transforms.FastBlur
import com.guet.flexbox.litho.transforms.ScaleTypes

class TransformGlideDrawable(
        private val context: Context
) : DrawableWrapper<Drawable>(NoOpDrawable()),
        Target<Drawable> by DelegateTarget() {
    private var width: Int = 0
    private var height: Int = 0

    fun unmount() {
        wrappedDrawable = NoOpDrawable()
    }

    fun bind(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun mount(
            model: Any,
            width: Int,
            height: Int,
            blurRadius: Float,
            blurSampling: Float,
            scaleType: ScaleType,
            lt: Float,
            rt: Float,
            rb: Float,
            lb: Float
    ) {
        bind(width, height)
        var request = Glide.with(context)
                .load(model)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
        var transforms: ArrayList<Transformation<Bitmap>>? = null
        val needRoundedCorners = lt != 0f || rb != 0f || lb != 0f || rt != 0f
        if (blurRadius <= 0 || blurSampling < 1) {
            transforms = ArrayList()
            transforms.add(FastBlur(blurRadius, blurSampling))
        }
        if (scaleType != ScaleType.FIT_XY || scaleType != ScaleType.MATRIX) {
            if (transforms == null) {
                transforms = ArrayList()
            }
            transforms.add(ScaleTypes(scaleType))
        }
        if (needRoundedCorners) {
            if (transforms == null) {
                transforms = ArrayList()
            }
            transforms.add(GranularRoundedCorners(
                    lt,
                    rt,
                    rb,
                    lb
            ))
        }
        if (!transforms.isNullOrEmpty()) {
            request = request.transform(*transforms.toTypedArray())
        }
        request.into(this)
    }

    override fun getSize(cb: SizeReadyCallback) {
        cb.onSizeReady(width, height)
    }

    override fun onResourceReady(
            resource: Drawable,
            transition: Transition<in Drawable>?
    ) {
        resource.bounds = bounds
        wrappedDrawable = resource
        invalidateSelf()
    }
}