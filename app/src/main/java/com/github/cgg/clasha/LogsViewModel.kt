package com.github.cgg.clasha

import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.cgg.clasha.data.LogMessage


/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-06-09
 * @describe
 */
class LogsViewModel : ViewModel() {
    val title = ObservableField<String>()
    var data = MutableLiveData<List<LogMessage>>()

    fun setData(list: List<LogMessage>) {
        data.postValue(list)
    }
}