package com.example.local_inter.ui

// AnimationExtensions.kt (一个完全独立的文件，不污染核心逻辑)
import android.graphics.Color
import android.view.View
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.transition.MaterialContainerTransform

fun Fragment.setupContainerTransform(viewToTransform: View, sharedElementName: String) {
    // 配置转场动画参数
    val transform = MaterialContainerTransform().apply {
        // 设置动画持续时间 (300ms 左右比较自然)
        duration = 300L
        // 设置动画起始和结束的背景色（这里都设为白色，避免扩散过程中背景闪动）
        startView = viewToTransform
        // 设置插值器（这里使用速度与半径成正比的加速/减速效果，符合物理规律）
        interpolator = FastOutSlowInInterpolator()
        // 设置容器颜色
        containerColor = Color.WHITE
        scrimColor = Color.TRANSPARENT // 设置遮罩颜色为透明
    }

    // 将此动画设置为该 Fragment 的进入转场动画
    enterTransition = transform
}