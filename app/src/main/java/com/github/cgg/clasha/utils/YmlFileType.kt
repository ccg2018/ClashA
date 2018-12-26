package com.github.cgg.clasha.utils

import com.github.cgg.clasha.R
import me.rosuh.filepicker.filetype.FileType

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-01-03
 * @describe
 */
class YmlFileType : FileType{
    override val fileIconResId: Int
        get() = R.drawable.ic_config_yml
    override val fileType: String
        get() = "Yml"


    override fun verify(fileName: String): Boolean {
        /**
         * 使用 endWith 是不可靠的，因为文件名有可能是以格式结尾，但是没有 . 符号
         * 比如 文件名仅为：example_png
         */
        val isHasSuffix = fileName.contains(".")
        if (!isHasSuffix){
            // 如果没有 . 符号，即是没有文件后缀
            return false
        }
        val suffix = fileName.substring(fileName.lastIndexOf(".") + 1)
        return when (suffix){
            "yml"-> {
                true
            }
            else -> {
                false
            }
        }
    }
}