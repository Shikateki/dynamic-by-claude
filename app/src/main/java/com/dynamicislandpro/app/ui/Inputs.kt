package com.dynamicislandpro.app.ui

import android.content.Context
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

fun inputField(ctx: Context, hint: String, numeric: Boolean = false, initial: String = ""): EditText =
    EditText(ctx).apply {
        this.hint = hint
        setText(initial)
        setTextColor(Ui.TEXT)
        setHintTextColor(Ui.MUTED)
        textSize = 15f
        inputType = if (numeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
        setSingleLine(true)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
