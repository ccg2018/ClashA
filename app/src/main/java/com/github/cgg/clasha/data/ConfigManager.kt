package com.github.cgg.clasha.data

import android.database.sqlite.SQLiteCantOpenDatabaseException
import com.github.cgg.clasha.utils.printLog
import java.io.IOException
import java.sql.SQLException

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-03-11
 * @describe
 */
object ConfigManager {

    interface Listener {
        fun onAdd(profile: ProfileConfig)
        fun onRemove(profileId: Long)
        fun onCleared()
    }

    var listener: Listener? = null

    @Throws(SQLException::class)
    fun createProfileConfig(config: ProfileConfig = ProfileConfig()): ProfileConfig {
        config.id = 0
        config.order = PrivateDatabase.profileConfigDao.nextOrder() ?: 0
        config.id = PrivateDatabase.profileConfigDao.create(config)
        config.time = System.currentTimeMillis()
        listener?.onAdd(config)
        return config
    }

    @Throws(SQLException::class)
    fun getProfileConfig(id: Long): ProfileConfig? = try {
        PrivateDatabase.profileConfigDao[id]
    } catch (e: SQLiteCantOpenDatabaseException) {
        throw  IOException(e)
    } catch (e: SQLException) {
        printLog(e)
        null
    }


    @Throws(IOException::class)
    fun getAllProfileConfigs(): List<ProfileConfig>? = try {
        PrivateDatabase.profileConfigDao.list()
    } catch (ex: SQLiteCantOpenDatabaseException) {
        throw IOException(ex)
    } catch (ex: SQLException) {
        printLog(ex)
        null
    }

    @Throws(SQLException::class)
    fun delProfile(id: Long) {
        check(PrivateDatabase.profileConfigDao.delete(id) == 1)
        listener?.onRemove(id)
        //if (id in app.activeProfileIds && DataStore.directBootAware) DirectBoot.clean()
    }


    @Throws(SQLException::class)
    fun updateProfileConfig(profile: ProfileConfig) = check(PrivateDatabase.profileConfigDao.update(profile) == 1)
}