package com.guet.flexbox.litho.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.facebook.litho.*
import com.facebook.litho.annotations.*
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaJustify
import com.guet.flexbox.ConcurrentUtils
import com.guet.flexbox.Orientation
import com.guet.flexbox.litho.LayoutThreadHandler
import com.guet.flexbox.litho.toPx
import java.lang.ref.WeakReference


@MountSpec(isPureRender = true, hasChildLithoViews = true)
object BannerSpec {

    @PropDefault
    val timeSpan = 3000L

    @get:JvmName(name = "getIsCircular")
    @PropDefault
    val isCircular: Boolean = true

    @PropDefault
    val indicatorHeightPx: Int = 5.toPx()

    @PropDefault
    val orientation = Orientation.HORIZONTAL

    @PropDefault
    val indicatorSelectedColor: Int = Color.WHITE

    @PropDefault
    val indicatorUnselectedColor: Int = Color.GRAY

    @PropDefault
    val indicatorEnable: Boolean = true

    @OnCreateMountContent
    fun onCreateMountContent(c: Context): LithoBannerView {
        return LithoBannerView(c)
    }

    @OnMount
    fun onMount(
            c: ComponentContext,
            view: LithoBannerView,
            @Prop(optional = true) orientation: Orientation,
            @Prop(optional = true) isCircular: Boolean,
            @Prop(optional = true) indicatorHeightPx: Int,
            @Prop(optional = true) indicatorSelectedColor: Int,
            @Prop(optional = true) indicatorUnselectedColor: Int,
            @Prop(optional = true) indicatorEnable: Boolean,
            @Prop(optional = true, varArg = "child") children: List<Component>?
    ) {
        if (!children.isNullOrEmpty()) {
            view.viewPager.adapter = BannerAdapter(
                    c,
                    isCircular,
                    children
            )
            view.viewPager.currentItem = children.size * 100
        }
        if (orientation == Orientation.HORIZONTAL) {
            view.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        } else {
            view.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        }
        view.indicatorEnable = indicatorEnable
        view.indicatorHeightPx = indicatorHeightPx
        view.indicatorSelectedColor = indicatorSelectedColor
        view.indicatorUnselectedColor = indicatorUnselectedColor
    }

    @OnUnmount
    fun onUnmount(
            c: ComponentContext,
            view: LithoBannerView
    ) {
        view.viewPager.adapter = null
    }

    @OnBind
    fun onBind(
            c: ComponentContext,
            host: LithoBannerView,
            @Prop(optional = true) timeSpan: Long,
            @Prop(optional = true, varArg = "child") children: List<Component>?
    ) {
        val rect = Rect(0, 0, host.measuredWidth, host.measuredWidth)
        listOf(
                listOf(host.indicators),
                (0 until host.viewPager.childCount).mapNotNull {
                    host.viewPager.getChildAt(it) as? LithoView
                }
        ).flatten().forEach {
            it.performIncrementalMount(rect, false)
        }
        if (timeSpan > 0) {
            val token = CarouselRunnable(
                    host.viewPager,
                    timeSpan
            )
            host.token = token
            ConcurrentUtils.mainThreadHandler
                    .postDelayed(token, timeSpan)
        }
    }

    @OnUnbind
    fun onUnbind(
            c: ComponentContext,
            host: LithoBannerView
    ) {
        host.token?.let {
            ConcurrentUtils.mainThreadHandler
                    .removeCallbacks(it)
        }
    }

}

class CarouselRunnable(
        host: ViewPager2,
        private val timeSpan: Long
) : WeakReference<ViewPager2>(host), Runnable {
    override fun run() {
        get()?.let {
            it.setCurrentItem(it.currentItem + 1, true)
            ConcurrentUtils.mainThreadHandler
                    .postDelayed(this, timeSpan)
        }
    }
}

private class LithoViewHolder(
        c: ComponentContext,
        val lithoView: LithoView = LithoView(c).apply {
            layoutParams = ViewGroup.LayoutParams(-1, -1)
            componentTree = ComponentTree.create(c)
                    .layoutThreadHandler(LayoutThreadHandler)
                    .isReconciliationEnabled(false)
                    .build()
        }
) : RecyclerView.ViewHolder(lithoView)

private class BannerAdapter(
        private val c: ComponentContext,
        private val isCircular: Boolean,
        private val components: List<Component>
) : RecyclerView.Adapter<LithoViewHolder>() {

    fun getNormalizedPosition(position: Int): Int {
        return if (isCircular)
            position % components.size
        else
            position
    }

    val realCount: Int
        get() = components.size

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): LithoViewHolder {
        return LithoViewHolder(c)
    }

    override fun getItemCount(): Int {
        return if (isCircular) {
            Int.MAX_VALUE
        } else {
            components.size
        }
    }

    override fun onViewRecycled(holder: LithoViewHolder) {
        holder.lithoView.unmountAllItems()
        holder.lithoView.componentTree = null
    }

    override fun onBindViewHolder(holder: LithoViewHolder, position: Int) {
        val p = getNormalizedPosition(position)
        holder.lithoView.setComponentAsync(components[p])
    }
}

class LithoBannerView(context: Context) : FrameLayout(context), HasLithoViewChildren {

    val viewPager: ViewPager2 = ViewPager2(context)

    var token: CarouselRunnable? = null

    val indicators = LithoView(context)

    var indicatorHeightPx: Int = BannerSpec.indicatorHeightPx

    var indicatorSelectedColor: Int = BannerSpec.indicatorSelectedColor

    var indicatorUnselectedColor: Int = BannerSpec.indicatorUnselectedColor

    var indicatorEnable: Boolean = BannerSpec.indicatorEnable

    private val callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            val adapter = viewPager.adapter
            if (adapter !is BannerAdapter) {
                return
            }
            val realPosition = adapter.getNormalizedPosition(position)
            val indicatorPx = 5.toPx()
            val c = indicators.componentContext
            val outline = CornerOutlineProvider(indicatorPx)
            indicators.setComponentAsync(Row.create(c)
                    .justifyContent(YogaJustify.CENTER)
                    .alignItems(YogaAlign.FLEX_END)
                    .child(Row.create(c)
                            .marginPx(YogaEdge.BOTTOM, indicatorHeightPx)
                            .apply {
                                (0 until adapter.realCount).forEach { index ->
                                    child(Row.create(c)
                                            .widthPx(indicatorPx)
                                            .heightPx(indicatorPx)
                                            .marginPx(YogaEdge.LEFT, indicatorPx / 2)
                                            .marginPx(YogaEdge.RIGHT, indicatorPx / 2)
                                            .outlineProvider(outline)
                                            .clipToOutline(true)
                                            .apply {
                                                if (index == realPosition) {
                                                    backgroundColor(indicatorSelectedColor)
                                                } else {
                                                    backgroundColor(indicatorUnselectedColor)
                                                }
                                            }
                                    )
                                }
                            })
                    .build())
        }
    }

    init {
        addView(viewPager, LayoutParams(-1, -1))
        addView(indicators, LayoutParams(-1, -1))
        viewPager.registerOnPageChangeCallback(callback)
    }

    override fun obtainLithoViewChildren(lithoViews: MutableList<LithoView>) {
        lithoViews.add(indicators)
        for (i in 0..viewPager.childCount) {
            val child: View = viewPager.getChildAt(i)
            if (child is LithoView) {
                lithoViews.add(child)
            }
        }
    }

}