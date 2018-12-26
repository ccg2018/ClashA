package com.github.cgg.clasha

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun outJSON() {
        val result = JSONObject()
        result.put("versionCode", 4)
        result.put("versionName", "0.0.1-pre3")
        var download = JSONObject()
        //'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'

        download.put("armeabi-v7a", 10346696)
        download.put("arm64-v8a", 10346695)
        download.put("x86", 10346699)
        download.put("x86_64", 10346698)
        download.put("universal",10346697)
        result.put("info", download)
        result.put("apiUrl","https://api.github.com/repos/ccg2018/ClashA/releases/tags/0.0.1-pre3")
        println(result.toString())


    }
}
