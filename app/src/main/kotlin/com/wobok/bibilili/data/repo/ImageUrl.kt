package com.wobok.bibilili.data.repo

/**
 * 统一把图片地址修成 https。
 *
 * B 站各个接口给封面的写法并不一致：搜索给 `//i0.hdslb.com/...` 的协议相对写法，
 * 排行榜和历史里混着 `http://i0.hdslb.com/...`。而 App 的
 * `network_security_config` 是禁明文的，于是 http 那一批**静悄悄地全部加载失败**，
 * 表现就是「有的封面出得来有的出不来」。图床本身支持 https，直接升级即可，
 * 比为了看封面把明文放开安全得多。
 */
fun String.asHttps(): String = when {
    isBlank() -> this
    startsWith("//") -> "https:$this"
    startsWith("http://") -> "https://" + substring(7)
    else -> this
}
