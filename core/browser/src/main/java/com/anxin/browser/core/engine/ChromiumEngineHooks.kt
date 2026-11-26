package com.anxin.browser.core.engine

/**
 * 可选内核（ChromiumEngine）的运行时钩子。
 *
 * 主工程在关闭 `-PuseChromiumKernel` 时不编译 ChromiumEngine，因此 AppGraph
 * 通过反射创建内核实例，只依赖本接口，避免编译期强引用可选类。
 */
interface ChromiumEngineHooks {
    fun setVisitListener(listener: (title: String, url: String) -> Unit)
    fun setPageScriptProvider(provider: (url: String) -> List<String>)
}
