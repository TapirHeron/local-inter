package com.example.local_inter.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.local_inter.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton

class HomeUnstartedFragment : Fragment(R.layout.fragment_home_unstarted) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnToggle = view.findViewById<MaterialButton>(R.id.btn_toggle)

        val bottomSheetLayout = view.findViewById<View>(R.id.bottom_sheet_layout)
        val bottomSheetHeader = view.findViewById<View>(R.id.bottom_sheet_header)
        val btnOpenSettings = view.findViewById<View>(R.id.btn_open_settings) // 找到新的设置按钮

        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)

        // 确保初始状态是隐藏的
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // 1. 点击主界面底部的“⚙️ 设置”按钮时 -> 展开面板
        btnOpenSettings.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        // 2. 点击面板上的“⬇️ 收起设置”时 -> 隐藏面板
        bottomSheetHeader.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        btnToggle.setOnClickListener {
            // 1. 动态获取按钮在屏幕上的中心坐标 (X, Y)
            val cx = (btnToggle.left + btnToggle.right) / 2
            val cy = (btnToggle.top + btnToggle.bottom) / 2

            // 【新增】：动态获取按钮当前的实际像素半径 (宽度的一半)
            val actualRadius = btnToggle.width / 2

            // 2. 将坐标和半径一起打包
            val bundle = Bundle().apply {
                putInt("reveal_cx", cx)
                putInt("reveal_cy", cy)
                putInt("reveal_radius", actualRadius) // 把真实半径传给下个页面
            }

            // 3. 执行跳转
            findNavController().navigate(R.id.action_unstarted_to_active, bundle)
        }
    }
}