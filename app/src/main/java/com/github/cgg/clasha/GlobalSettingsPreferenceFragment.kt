package com.github.cgg.clasha

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.widget.EditText
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.github.cgg.clasha.bg.BaseService
import com.github.cgg.clasha.data.DataStore
import com.github.cgg.clasha.utils.Key
import com.github.cgg.clasha.utils.getGson
import com.github.cgg.clasha.utils.remove
import com.github.cgg.clasha.widget.EditTextDialog
import com.takisoft.preferencex.AutoSummaryEditTextPreference
import com.takisoft.preferencex.PreferenceFragmentCompat
import com.takisoft.preferencex.SimpleMenuPreference
import com.takisoft.preferencex.SwitchPreferenceCompat
import org.json.JSONArray
import org.json.JSONObject


/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-03-15
 * @describe
 */
class GlobalSettingsPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = DataStore.publicStore
        DataStore.initGlobal()
        addPreferencesFromResource(R.xml.pref_global)


        val allowLan = findPreference<SwitchPreferenceCompat>(Key.allowLan)?.apply {
            this.isEnabled = DataStore.allowLan
        }
        val ipv6Enable = findPreference<SwitchPreferenceCompat>(Key.ipv6)?.apply {
            this.isEnabled = DataStore.ipv6Enable
        }
        val bypassPrivateNetwork = findPreference<SwitchPreferenceCompat>(Key.KEY_BYPASS_PRIVATE_NETWORK)?.apply {
            this.isEnabled = DataStore.isBypassPrivateNetwork
        }
        val portApi = findPreference<AutoSummaryEditTextPreference>(Key.portApi)
        val serviceMode = findPreference<SimpleMenuPreference>(Key.serviceMode)
        val portHttpProxy = findPreference<AutoSummaryEditTextPreference>(Key.portHttpProxy)
        val portProxy = findPreference<AutoSummaryEditTextPreference>(Key.portProxy)
        val portLocalDns = findPreference<AutoSummaryEditTextPreference>(Key.portLocalDns)
        val dnsMode = findPreference<SimpleMenuPreference>(Key.dnsMode)
        val clashLoglevel = findPreference<SimpleMenuPreference>(Key.clashLoglevel)

        val serviceModeValue = DataStore.serviceMode
        val meteredPr = findPreference<SwitchPreference>(Key.metered)?.apply {
            if (Build.VERSION.SDK_INT >= 28) isEnabled = serviceModeValue == Key.modeVpn else remove()
        }

        /*val onServiceModeChange = Preference.OnPreferenceChangeListener { preference, newValue ->
            val (enabledLocalDns, enabledTransproxy) = when (newValue as String?) {
                Key.modeProxy -> Pair(false, false)
                Key.modeVpn -> Pair(true, false)
                Key.modeTransproxy -> Pair(true, true)
                else -> throw IllegalArgumentException("newValue: $newValue")
            }
            true
        }*/

        val listener: (BaseService.State) -> Unit = {
            if (it == BaseService.State.Stopped) {
                serviceMode?.isEnabled = true
                portHttpProxy?.isEnabled = true
                portProxy?.isEnabled = true
                portLocalDns?.isEnabled = true
                allowLan?.isEnabled = true
                ipv6Enable?.isEnabled = true
                portApi?.isEnabled = true
                clashLoglevel?.isEnabled = true
                dnsMode?.isEnabled = true
                meteredPr?.isEnabled = true
                bypassPrivateNetwork?.isEnabled = true
            } else {
                serviceMode?.isEnabled = false
                portHttpProxy?.isEnabled = false
                portProxy?.isEnabled = false
                portLocalDns?.isEnabled = false
                allowLan?.isEnabled = false
                ipv6Enable?.isEnabled = false
                portApi?.isEnabled = false
                clashLoglevel?.isEnabled = false
                dnsMode?.isEnabled = false
                meteredPr?.isEnabled = false
                bypassPrivateNetwork?.isEnabled = false
            }
        }

        portLocalDns?.onPreferenceChangeListener = object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
                val dnsJSONobject = JSONObject(DataStore.dnsConfig)
                dnsJSONobject.put("listen", if (DataStore.allowLan) "0.0.0.0:$newValue" else "127.0.0.1:$newValue")
                DataStore.dnsConfig = dnsJSONobject.toString()
                return true
            }
        }

        dnsMode?.onPreferenceChangeListener = object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
                val dnsJSONObject = JSONObject(DataStore.dnsConfig)
                dnsJSONObject.put("enhanced-mode", newValue)
                DataStore.dnsConfig = dnsJSONObject.toString()
                return true
            }
        }

        ipv6Enable?.onPreferenceChangeListener = object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
                val dnsJSONobject = JSONObject(DataStore.dnsConfig)
                dnsJSONobject.put("ipv6", newValue)
                DataStore.dnsConfig = dnsJSONobject.toString()
                return true
            }
        }

        listener((activity as MainActivity).state)
        MainActivity.stateListener = listener

        loadDnsPreferenes()

    }

    private val dnsJSON by lazy {
        JSONObject(DataStore.dnsConfig)
    }

    private fun loadDnsPreferenes() {
        val context = preferenceManager.context

        val dnsCustomNameserver = findPreference<PreferenceCategory>("dnsCustomNameserver")
        val dnsCustomFallback = findPreference<PreferenceCategory>("dnsCustomFallback")


        val debug = findPreference<Preference>("pref_debug")
        debug?.setOnPreferenceClickListener {
            DataStore.publicStore.remove(Key.dnsConfig)
            ToastUtils.showShort("删除dnsConfig数据库数据")
            LogUtils.w("allow Lan is ${DataStore.allowLan}")
            true
        }

        //load dnsconfig
        val namserverJSONArray = dnsJSON.optJSONArray("nameserver")
        val fallbackJSONArray = dnsJSON.optJSONArray("fallback")

        for (n in 0 until namserverJSONArray.length()) {
            val dns = namserverJSONArray.optString(n)
            val preference = Preference(context)
            preference.title = "Nameserver DNS"
            preference.summary = dns
            preference.setDefaultValue(dns)
            val dialog = EditTextDialog.newInstance(
                title = preference.title.toString(), hint = "", text = dns, tag = dns,
                isMultiline = false, hasOnNeutral = true
            )
            dialog.onOk = { it, tag ->
                if (TextUtils.isEmpty(it.text)) {
                    ToastUtils.showShort(R.string.not_empty)
                } else {
                    saveDns("nameserver", namserverJSONArray, preference, tag, it.text)
                }
            }

            dialog.onNeutral = { editText, tag ->
                removeDns(dnsCustomNameserver, "nameserver", namserverJSONArray, tag, preference)
            }
            preference.setOnPreferenceClickListener {
                dialog.show(
                    fragmentManager ?: (activity as MainActivity).supportFragmentManager,
                    preference.title.toString()
                )
                true
            }
            dnsCustomNameserver?.addPreference(preference)
        }

        for (n in 0 until fallbackJSONArray.length()) {
            val dns = fallbackJSONArray.optString(n)
            val preference = Preference(context)
            preference.title = "Fallback DNS"
            preference.summary = dns
            preference.setDefaultValue(dns)
            val dialog = EditTextDialog.newInstance(
                title = preference.title.toString(), hint = "if input is empty that remove it", text = dns, tag = dns,
                isMultiline = false, hasOnNeutral = true
            )
            dialog.onOk = { it, tag ->
                if (TextUtils.isEmpty(it.text)) {
                    ToastUtils.showShort(R.string.not_empty)
                } else {
                    saveDns("fallback", fallbackJSONArray, preference, tag, it.text)
                }
            }

            dialog.onNeutral = { editText, tag ->
                removeDns(dnsCustomFallback, "fallback", fallbackJSONArray, tag, preference)
            }

            preference.setOnPreferenceClickListener {
                dialog.show(
                    fragmentManager ?: (activity as MainActivity).supportFragmentManager,
                    preference.title.toString()
                )
                true
            }
            dnsCustomFallback?.addPreference(preference)
        }


        val namserverAdd = findPreference<Preference>("pref_dns_nameserver_add")
        val fallbackAdd = findPreference<Preference>("pref_dns_fallback_add")

        namserverAdd?.setOnPreferenceClickListener {
            val addDialog = bindEditDialog(
                title = "add Nameserver DNS", hint = "1.1.1.1 or tls://goole.dns",
                isMultiline = false, onOk = { it, tag ->
                    if (!TextUtils.isEmpty(it.text)) {
                        val preference = Preference(context)

                        preference.title = "Nameserver DNS"
                        preference.summary = it.text
                        preference.setDefaultValue(it.text)
                        preference.setOnPreferenceClickListener {
                            val dialog = bindEditDialog(
                                title = preference.title.toString(),
                                hint = "if input is empty that remove it",
                                text = preference.summary.toString(),
                                tag = preference.summary.toString(),
                                isMultiline = false,
                                hasOnNeutral = true,
                                onOk = { it2, tag2 ->
                                    if (TextUtils.isEmpty(it2.text)) {
                                        ToastUtils.showShort(R.string.not_empty)
                                    } else {
                                        saveDns("nameserver", namserverJSONArray, preference, tag2, it2.text)
                                    }
                                },
                                onNeutral = { _, tag ->
                                    removeDns(dnsCustomNameserver, "nameserver", namserverJSONArray, tag, preference)
                                }
                            )

                            dialog.show(
                                fragmentManager ?: (activity as MainActivity).supportFragmentManager,
                                preference.title.toString()
                            )
                            true
                        }
                        dnsCustomNameserver?.addPreference(preference)

                        namserverJSONArray.put(it.text.toString())
                        dnsJSON.put("nameserver", namserverJSONArray)
                        DataStore.dnsConfig = dnsJSON.toString()
                        LogUtils.w(getGson().toJson(JSONObject(DataStore.dnsConfig)))
                    }
                }
            )

            addDialog.show(fragmentManager ?: (activity as MainActivity).supportFragmentManager, "addNsDns")
            true
        }
        fallbackAdd?.setOnPreferenceClickListener {
            val addDialog = bindEditDialog(
                title = "add Fallback DNS", hint = "1.1.1.1 or tls://goole.dns",
                isMultiline = false, onOk = { it, tag ->
                    if (!TextUtils.isEmpty(it.text)) {
                        val preference = Preference(context)

                        preference.title = "Fallback DNS"
                        preference.summary = it.text
                        preference.setDefaultValue(it.text)
                        preference.setOnPreferenceClickListener {
                            val dialog = bindEditDialog(
                                title = preference.title.toString(),
                                hint = "if input is empty that remove it",
                                text = preference.summary.toString(),
                                tag = preference.summary.toString(),
                                isMultiline = false,
                                hasOnNeutral = true,
                                onOk = { it2, tag2 ->
                                    if (TextUtils.isEmpty(it2.text)) {
                                        ToastUtils.showShort(R.string.not_empty)
                                    } else {
                                        saveDns("fallback", fallbackJSONArray, preference, tag2, it2.text)
                                    }
                                },
                                onNeutral = { _, tag ->
                                    removeDns(dnsCustomFallback, "fallback", fallbackJSONArray, tag, preference)
                                }
                            )
                            dialog.show(
                                fragmentManager ?: (activity as MainActivity).supportFragmentManager,
                                preference.title.toString()
                            )
                            true
                        }
                        dnsCustomFallback?.addPreference(preference)
                        fallbackJSONArray.put(it.text.toString())
                        dnsJSON.put("fallback", fallbackJSONArray)
                        DataStore.dnsConfig = dnsJSON.toString()
                        LogUtils.w(getGson().toJson(JSONObject(DataStore.dnsConfig)))
                    }
                }
            )

            addDialog.show(fragmentManager ?: (activity as MainActivity).supportFragmentManager, "addFbDns")
            true
        }

    }

    private fun bindEditDialog(
        title: String? = null,
        hint: String? = null,
        text: String? = null,
        tag: String? = null,
        isMultiline: Boolean = false,
        hasOnNeutral: Boolean = false,
        onOk: ((editText: EditText, tag: String) -> Unit)? = null,
        onNeutral: ((editText: EditText, tag: String) -> Unit)? = null
    ): EditTextDialog {
        val dialog = EditTextDialog.newInstance(
            title = title, hint = hint, text = text, tag = tag,
            isMultiline = isMultiline, hasOnNeutral = hasOnNeutral
        )
        dialog.onOk = onOk
        dialog.onNeutral = onNeutral
        return dialog
    }

    private fun removeDns(
        preferenceCategory: PreferenceCategory?,
        jsonArrayName: String,
        jsonArray: JSONArray,
        value: String,
        preference: Preference
    ) {
        preferenceCategory?.removePreference(preference)
        //循环
        for (i in 0 until jsonArray.length()) {
            if (value == jsonArray.get(i)) {
                jsonArray.remove(i)
                break
            }
        }
        dnsJSON.put(jsonArrayName, jsonArray)
        DataStore.dnsConfig = dnsJSON.toString()
        LogUtils.w(getGson().toJson(JSONObject(DataStore.dnsConfig)))
    }

    private fun saveDns(
        jsonArrayName: String,
        jsonArray: JSONArray,
        preference: Preference,
        value: String,
        text: Editable
    ) {
        //循环
        for (i in 0 until jsonArray.length()) {
            if (value == jsonArray.get(i)) {
                jsonArray.put(i, text.toString())
                break
            }
        }
        preference.summary = text
        dnsJSON.put(jsonArrayName, jsonArray)
        DataStore.dnsConfig = dnsJSON.toString()
        LogUtils.w(getGson().toJson(JSONObject(DataStore.dnsConfig)))
    }
}