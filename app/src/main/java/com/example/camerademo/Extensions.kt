@file:JvmName("ExtensionUtils")
package com.example.camerademo

import android.app.Activity
import android.content.ContextWrapper
import android.view.View

val View.activity: Activity?
    get() {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return null
    }

