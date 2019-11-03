package com.github.cgg.clasha.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import com.blankj.utilcode.util.LogUtils
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.github.cgg.clasha.data.LogMessage.Companion.formatter
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import org.yaml.snakeyaml.Yaml
import java.io.FileInputStream
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-03-11
 * @describe
 */
@Entity
@Parcelize
data class ProfileConfig constructor(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var configName: String? = "",
    var url: String? = "",
    var proxyItems: String? = "",
    var proxyGroupItems: String? = "",
    var ruleItems: String? = "",
    var order: Long = 0,
    var origin: String? = "",
    private var itemType: Int = 0,
    var selector: String? = "",
    var time: Long = 0

) : MultiItemEntity, Parcelable, Serializable {
    override fun getItemType(): Int {
        return itemType
    }

    companion object {

        val DEFAULT = 0
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm")

        fun findProfileConfig(
            data: String?,
            profileConfig: ProfileConfig? = null,
            configName: String? = "Config-${Date().time}",
            url: String? = null
        ): ProfileConfig {
            var yaml = Yaml()
            var config = profileConfig ?: ProfileConfig()

            config.url = url
            config.configName = configName
            //remove too lagre
            //config.origin = data

            //yaml to json
            var jsonObject = JSONObject(yaml.load<Map<String, Objects>>(data))
            //get proxyItems
            config.proxyItems = jsonObject.optJSONArray("Proxy").toString(4)
            //get proxyGroupItems
            config.proxyGroupItems = jsonObject.optJSONArray("Proxy Group").toString(4)
            //get ruleItems
            config.ruleItems = jsonObject.optJSONArray("Rule").toString(4)
            return config
        }
    }

    @androidx.room.Dao
    interface Dao {

        @Query("SELECT * FROM `ProfileConfig` WHERE `id` = :id")
        operator fun get(id: Long): ProfileConfig?

        @Insert
        fun create(value: ProfileConfig): Long

        @Update
        fun update(value: ProfileConfig): Int

        @Query("SELECT MAX(`order`) + 1 FROM `ProfileConfig`")
        fun nextOrder(): Long?

        @Query("SELECT * FROM `ProfileConfig` ORDER BY `order`")
        fun list(): List<ProfileConfig>

        @Query("DELETE FROM `ProfileConfig` WHERE `id` = :id")
        fun delete(id: Long): Int
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("port", DataStore.portHttpProxy)
        put("socks-port", DataStore.portProxy)
        put("allow-lan", DataStore.allowLan)
        put("mode", DataStore.clashMode)
        put("log-level", DataStore.clashLoglevel)
        put(
            "external-controller",
            if (DataStore.allowLan) ("0.0.0.0:${DataStore.portApi}") else ("127.0.0.1:${DataStore.portApi}")
        )

        put("dns", JSONObject(DataStore.dnsConfig))
        LogUtils.w(this)

        put("Proxy", JSONArray(proxyItems))
        put("Proxy Group", JSONArray(proxyGroupItems))
        put("Rule", JSONArray(ruleItems))
    }

    fun getDateFormatted(): String {
        return formatter.format(Date(time)).toString()
    }


}