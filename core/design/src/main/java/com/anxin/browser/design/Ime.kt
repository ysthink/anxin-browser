package com.anxin.browser.design

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.composed
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 点按文本框时兜底显式拉起输入法。
 *
 * 真机（Redmi K40 / MIUI + 微信键盘 WeType）实测：键盘被返回键收起后，点文本框
 * 的第一次 showSoftInput 请求偶发被静默丢弃（MIUI 在输入法收起时会重建 Activity
 * 窗口，新窗口的输入会话未重绑，IMS 侧直接忽略 show 请求），表现为「点击不弹
 * 键盘，再点一次才弹」。
 *
 * 处理：点按抬起时立刻普通请求一次；300ms / 800ms 后若仍未弹出，先 restartInput
 * 强制重绑输入会话再请求。正常输入中（键盘已弹出）不会走到重试，组合输入不受影响。
 *
 * 该修饰符**不消费事件**，只观察「按下→抬起」，文本框自身的点按/光标/选区行为
 * 不受影响。
 */
fun Modifier.imeTap(): Modifier = composed {
    val view = LocalView.current
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val up = waitForUpOrCancellation()
            if (up != null) {
                fun imeVisible(): Boolean =
                    androidx.core.view.ViewCompat.getRootWindowInsets(view)
                        ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true

                fun plainShow() {
                    val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                        as? InputMethodManager
                    imm?.showSoftInput(view, 0)
                }

                // 输入法未弹出时才重绑会话重试；正常输入中（键盘已弹出）不做任何
                // 干预，避免打断组合输入。
                fun rebindAndShow() {
                    if (imeVisible()) return
                    val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                        as? InputMethodManager
                    if (imm != null) {
                        imm.restartInput(view)
                        imm.showSoftInput(view, 0)
                    }
                }

                view.post { plainShow() }
                view.postDelayed({ rebindAndShow() }, 300)
                view.postDelayed({ rebindAndShow() }, 800)
            }
        }
    }
}
