package com.github.cgg.clasha.widget

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.github.cgg.clasha.R

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-03-17
 * @describe
 */
class EditTextDialog : DialogFragment() {
    companion object {
        private const val TAG = "EditTextDialog"

        private const val EXTRA_TITLE = "title"
        private const val EXTRA_HINT = "hint"
        private const val EXTRA_MULTILINE = "multiline"
        private const val EXTRA_HAS_ONNEUTRAL = "neutral"

        private const val EXTRA_TEXT = "text"
        private const val EXTRA_TAG = "tag"


        fun newInstance(
            title: String? = null,
            hint: String? = null,
            text: String? = null,
            tag: String? = null,
            isMultiline: Boolean = false,
            hasOnNeutral: Boolean = false
        ): EditTextDialog {
            val dialog = EditTextDialog()
            val args = Bundle().apply {
                putString(EXTRA_TITLE, title)
                putString(EXTRA_HINT, hint)
                putString(EXTRA_TEXT, text)
                putString(EXTRA_TAG, tag)
                putBoolean(EXTRA_MULTILINE, isMultiline)
                putBoolean(EXTRA_HAS_ONNEUTRAL, hasOnNeutral)
            }
            dialog.arguments = args
            return dialog
        }
    }

    lateinit var editText: EditText
    var onOk: ((editText: EditText, tag: String) -> Unit)? = null
    var onCancel: (() -> Unit)? = null
    var onNeutral: ((editText: EditText, tag: String) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments?.getString(EXTRA_TITLE)
        val hint = arguments?.getString(EXTRA_HINT)
        val text: String? = arguments?.getString(EXTRA_TEXT)
        val tag: String = arguments?.getString(EXTRA_TAG) ?: ""
        val hasOnNeutral = arguments?.getBoolean(EXTRA_HAS_ONNEUTRAL) ?: false
        val isMiltiline = arguments?.getBoolean(EXTRA_MULTILINE) ?: false

        // StackOverflowError
        // val view = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val view = activity!!.layoutInflater.inflate(R.layout.dialog_edit_text, null)

        editText = view.findViewById(R.id.editText)
        editText.hint = hint

        if (isMiltiline) {
            editText.minLines = 3
            editText.inputType =
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        if (text != null) {
            // editText.setText(text)
            // editText.setSelection(text.length)
            editText.append(text)
        }

        val builder = AlertDialog.Builder(context!!)
            .setTitle(title)
            .setView(view)

            .setPositiveButton(android.R.string.ok) { _, _ ->
                onOk?.invoke(editText, tag)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                onCancel?.invoke()
            }
        if (hasOnNeutral) {
            builder.setNeutralButton(R.string.dialog_delete) { _, _ ->
                onNeutral?.invoke(editText, tag)
            }
        }
        val dialog = builder.create()

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        return dialog
    }

}