package com.example.local_inter.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.example.local_inter.R
import com.google.android.material.button.MaterialButton
import kotlin.math.hypot

class HomeActiveFragment : Fragment(R.layout.fragment_home_active) {

    private lateinit var layerOrange: View
    private lateinit var layerLoading: View
    private lateinit var layerWhiteContainer: View
    private lateinit var layerContent: ConstraintLayout
    private lateinit var tvTitle: View
    private lateinit var btnStop: View

    private lateinit var operationBar: View
    private lateinit var btnOpPrimary: MaterialButton
    private lateinit var btnOpCancel: View

    private var revealCx: Int = 0
    private var revealCy: Int = 0
    private val buttonRadiusPx by lazy { 75 * resources.displayMetrics.density }

    private val initialConstraintSet = ConstraintSet()
    private var isDevicesExpanded = false
    private var isFilesExpanded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 基础绑定
        layerOrange = view.findViewById(R.id.layer_orange)
        layerLoading = view.findViewById(R.id.layer_loading)
        layerWhiteContainer = view.findViewById(R.id.layer_white_container)
        layerContent = view.findViewById<ConstraintLayout>(R.id.layer_content)
        tvTitle = view.findViewById(R.id.tv_title)
        btnStop = view.findViewById(R.id.btn_stop)

        operationBar = view.findViewById(R.id.operation_bar)
        btnOpPrimary = view.findViewById<MaterialButton>(R.id.btn_op_primary)
        btnOpCancel = view.findViewById(R.id.btn_op_cancel)

        val recyclerDevices = view.findViewById<RecyclerView>(R.id.recycler_devices)
        val recyclerFiles = view.findViewById<RecyclerView>(R.id.recycler_files)

        // 【修复】：显式声明 Lambda 类型，解决类型推断报错
        val onSelectionChanged: (String, Int) -> Unit = { type, count ->
            if (count > 0) showOperationBar(type) else hideOperationBar()
        }

        // 初始化适配器
        val deviceAdapter = MockAdapter("DEVICE", onSelectionChanged)
        deviceAdapter.setData(listOf("📱" to "iPhone 15", "💻" to "MacBook Pro", "🖥️" to "Windows PC"))
        recyclerDevices.layoutManager = LinearLayoutManager(context)
        recyclerDevices.adapter = deviceAdapter

        val fileAdapter = MockAdapter("FILE", onSelectionChanged)
        fileAdapter.setData(listOf("📄" to "论文_初稿.docx", "🎬" to "演示视频.mp4", "📂" to "项目源码.zip"))
        recyclerFiles.layoutManager = LinearLayoutManager(context)
        recyclerFiles.adapter = fileAdapter

        // 初始快照
        layerContent.post { initialConstraintSet.clone(layerContent) }

        // 点击逻辑
        view.findViewById<View>(R.id.card_devices).setOnClickListener { toggleCard(it, recyclerDevices, view.findViewById(R.id.card_files)) }
        view.findViewById<View>(R.id.card_files).setOnClickListener { toggleCard(it, recyclerFiles, view.findViewById(R.id.card_devices)) }
        btnStop.setOnClickListener { startReverseAnimation() }

        // 操作栏按钮处理
        btnOpCancel.setOnClickListener {
            deviceAdapter.clearSelection()
            fileAdapter.clearSelection()
            hideOperationBar()
        }
        btnOpPrimary.setOnClickListener {
            val selected = if (isDevicesExpanded) deviceAdapter.getSelected() else fileAdapter.getSelected()
            if (isDevicesExpanded) performSend(selected) else performSelect(selected)
            deviceAdapter.clearSelection()
            fileAdapter.clearSelection()
            hideOperationBar()
        }

        revealCx = arguments?.getInt("reveal_cx") ?: (resources.displayMetrics.widthPixels / 2)
        revealCy = arguments?.getInt("reveal_cy") ?: (resources.displayMetrics.heightPixels / 2)
        view.post { startStage1OrangeReveal() }
    }

    private fun toggleCard(targetCard: View, recyclerView: RecyclerView, otherCard: View) {
        val isExpanded = if (targetCard.id == R.id.card_devices) isDevicesExpanded else isFilesExpanded

        if (!isExpanded) {
            targetCard.elevation = 30f
            otherCard.visibility = View.GONE
            TransitionManager.beginDelayedTransition(layerContent, TransitionSet().addTransition(ChangeBounds()).setDuration(400))

            val cs = ConstraintSet(); cs.clone(layerContent)
            cs.connect(targetCard.id, ConstraintSet.TOP, R.id.tv_title, ConstraintSet.BOTTOM, 40)
            cs.connect(targetCard.id, ConstraintSet.BOTTOM, R.id.btn_stop, ConstraintSet.TOP, 40)
            cs.setVerticalBias(targetCard.id, 0f)
            cs.constrainHeight(targetCard.id, ConstraintSet.MATCH_CONSTRAINT)
            cs.applyTo(layerContent)

            recyclerView.post {
                val headerH = (80 * resources.displayMetrics.density).toInt()
                val availH = (btnStop.top - tvTitle.bottom) - 80 - headerH
                ValueAnimator.ofInt(0, if (availH > 0) availH else 800).apply {
                    addUpdateListener {
                        recyclerView.layoutParams.height = it.animatedValue as Int
                        recyclerView.requestLayout()
                    }
                    duration = 400
                    start()
                }
                recyclerView.visibility = View.VISIBLE
            }
        } else {
            // 根据要求：先关闭显示，再执行缩回动画
            recyclerView.visibility = View.GONE
            recyclerView.layoutParams.height = 0
            TransitionManager.beginDelayedTransition(layerContent, ChangeBounds().setDuration(400))
            initialConstraintSet.applyTo(layerContent)
            targetCard.elevation = 4f
            otherCard.visibility = View.VISIBLE
            hideOperationBar()
        }

        if (targetCard.id == R.id.card_devices) isDevicesExpanded = !isExpanded else isFilesExpanded = !isExpanded
    }

    private fun showOperationBar(type: String) {
        btnOpPrimary.text = if (type == "DEVICE") "发送" else "选中"
        if (operationBar.visibility == View.VISIBLE) return

        operationBar.visibility = View.VISIBLE
        operationBar.translationY = 300f
        operationBar.animate().translationY(0f).setDuration(300).start()
    }

    private fun hideOperationBar() {
        if (operationBar.visibility == View.GONE) return
        operationBar.animate().translationY(300f).setDuration(300).withEndAction {
            operationBar.visibility = View.GONE
        }.start()
    }

    private fun performSend(items: List<Pair<String, String>>) { android.util.Log.d("NAS", "发送至: $items") }
    private fun performSelect(items: List<Pair<String, String>>) { android.util.Log.d("NAS", "选中文件: $items") }

    private fun startStage1OrangeReveal() {
        val finalRadius = hypot(layerOrange.width.toDouble(), layerOrange.height.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(layerOrange, revealCx, revealCy, buttonRadiusPx, finalRadius)
        layerOrange.visibility = View.VISIBLE; anim.duration = 400
        anim.addListener(object : AnimatorListenerAdapter() { override fun onAnimationEnd(a: Animator) { startStage2Loading() } }); anim.start()
    }
    private fun startStage2Loading() {
        layerLoading.alpha = 0f; layerLoading.visibility = View.VISIBLE
        layerLoading.animate().alpha(1f).setDuration(300).withEndAction { layerLoading.postDelayed({ startStage3WhiteReveal() }, 2000) }.start()
    }
    private fun startStage3WhiteReveal() {
        val finalRadius = hypot(layerWhiteContainer.width.toDouble(), layerWhiteContainer.height.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(layerWhiteContainer, revealCx, revealCy, 0f, finalRadius)
        layerWhiteContainer.visibility = View.VISIBLE; anim.duration = 500
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(a: Animator) {
                layerOrange.visibility = View.GONE
                layerLoading.visibility = View.GONE
                layerContent.visibility = View.VISIBLE
                layerContent.alpha = 1f
            }
        }); anim.start()
    }
    private fun startReverseAnimation() {
        layerOrange.visibility = View.VISIBLE
        val maxR = hypot(layerWhiteContainer.width.toDouble(), layerWhiteContainer.height.toDouble()).toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(layerWhiteContainer, revealCx, revealCy, maxR, 0f)
        anim.duration = 400; anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(a: Animator) {
                layerWhiteContainer.visibility = View.INVISIBLE
                val anim2 = ViewAnimationUtils.createCircularReveal(layerOrange, revealCx, revealCy, maxR, buttonRadiusPx)
                anim2.duration = 400; anim2.addListener(object : AnimatorListenerAdapter() { override fun onAnimationEnd(a: Animator) { layerOrange.findNavController().popBackStack() } }); anim2.start()
            }
        }); anim.start()
    }
}

class MockAdapter(private val type: String, private val onSelect: (String, Int) -> Unit) : RecyclerView.Adapter<MockAdapter.VH>() {
    private var data = listOf<Pair<String, String>>()
    private val selected = mutableSetOf<Int>()
    fun setData(d: List<Pair<String, String>>) { data = d; notifyDataSetChanged() }
    fun clearSelection() { selected.clear(); notifyDataSetChanged(); onSelect(type, 0) }
    fun getSelected() = data.filterIndexed { i, _ -> selected.contains(i) }
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val root = v.findViewById<View>(R.id.item_root)
        val icon = v.findViewById<TextView>(R.id.item_icon)
        val name = v.findViewById<TextView>(R.id.item_name)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_list_card, p, false))
    override fun onBindViewHolder(h: VH, p: Int) {
        h.icon.text = data[p].first; h.name.text = data[p].second
        if (selected.contains(p)) {
            h.root.setBackgroundColor(Color.parseColor("#4080FF"))
            h.name.setTextColor(Color.WHITE)
        } else {
            h.root.setBackgroundColor(Color.TRANSPARENT)
            h.name.setTextColor(Color.BLACK)
        }
        h.root.setOnClickListener {
            if (selected.contains(p)) selected.remove(p) else selected.add(p)
            notifyItemChanged(p)
            onSelect(type, selected.size)
        }
    }
    override fun getItemCount() = data.size
}