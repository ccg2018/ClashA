package com.github.cgg.clasha.data

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.room.*
import com.github.cgg.clasha.utils.Key
import kotlinx.android.parcel.Parcelize
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-05-28
 * @describe
 */
@Entity
@Parcelize
data class LogMessage constructor(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var content: String? = "",//rule:127 -> host,dns host -> ip
    var originContent: String? = "",
    var logType: String? = "",//DNS/rule
    var time: Long,
    var currentRunDate: Date,
    var profileId: Long
) : Parcelable, Serializable {

    @androidx.room.Dao
    interface Dao {
        @Insert
        fun create(value: LogMessage): Long

        @Query("SELECT * FROM `LogMessage` WHERE `profileId` = :id")
        operator fun get(id: Long): List<LogMessage>

        @Query("SELECT * FROM `LogMessage` WHERE `profileId` = :id ORDER BY id DESC limit :page,${Key.LOG_PAGESIZE} ")
        fun getByPage(id: Long, page: Int): List<LogMessage>

        @Query("DELETE FROM `LogMessage`")
        fun removeAll()
    }

    fun getDateFormatted(): String {
        return formatter.format(Date(time)).toString()
    }

    fun isRuleType(): Boolean {
        return Key.LOG_TYPE_RULE == logType
    }

    fun isOtherType(): Boolean {
        return Key.LOG_TYPE_OTHER == logType
    }

    companion object {
        val formatter = SimpleDateFormat("HH:mm:ss.SSS")
    }
}
