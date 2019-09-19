package com.github.cgg.clasha.net

import android.content.Context
import com.yanzhenjie.andserver.annotation.Config
import com.yanzhenjie.andserver.framework.config.Multipart
import com.yanzhenjie.andserver.framework.config.WebConfig
import com.yanzhenjie.andserver.framework.website.AssetsWebsite
import com.yanzhenjie.andserver.framework.website.Website
import java.io.File

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-09-19
 * @describe
 */
@Config
class HTTPConfig : WebConfig {

    override fun onConfig(context: Context, delegate: WebConfig.Delegate) {
        delegate.addWebsite(AssetsWebsite(context, "/clash") as Website?)
        delegate.setMultipart(
            Multipart.newBuilder()
                .allFileMaxSize((1024 * 1024 * 20).toLong()) // 20M
                .fileMaxSize((1024 * 1024 * 5).toLong()) // 5M
                .maxInMemorySize(1024 * 10) // 1024 * 10 bytes
                .uploadTempDir(File(context.cacheDir, "_server_upload_cache_"))
                .build()
        )
    }
}