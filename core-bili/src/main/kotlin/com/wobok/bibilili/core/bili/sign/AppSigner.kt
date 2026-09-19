package com.wobok.bibilili.core.bili.sign

/**
 * APP 端 appkey/appsec 签名：`sign = md5(按 key 升序的 query + appsec)`。
 *
 * 本项目只在极少数没有 Web 对应物的接口上用得到（例如取演职人员的
 * `pgc/view/v2/app/season`）。登录一律走 Web 扫码，**不使用 TV 端 appkey**：
 * 某组 appkey 换来的 access_token 换一组 appkey 调用会返回 `-663`，
 * 会把整个接口层锁死在那套体系里。
 */
data class AppKeyPair(val appKey: String, val appSec: String) {
    companion object {
        /** 云视听小电视（TV 版）。登录不用它，仅在明确需要 TV 片源时才考虑。 */
        val TV = AppKeyPair("4409e2ce8ffd12b8", "59b43e04ad6965f34319062b478f83dd")
    }
}

object AppSigner {
    fun sign(params: Map<String, String>, keys: AppKeyPair): Map<String, String> {
        val withAppKey = params + ("appkey" to keys.appKey)
        val query = UrlCodec.sortedQuery(withAppKey)
        return withAppKey + ("sign" to Md5.hex(query + keys.appSec))
    }
}
