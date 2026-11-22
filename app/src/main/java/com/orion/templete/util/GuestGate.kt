package com.orion.templete.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.orion.templete.MainActivity

/**
 * Guest browsing: anyone can look at art without an account. The first tap on a
 * write action (like, comment, follow, chat) asks them to sign up instead.
 */
object GuestGate {

    fun isGuest(context: Context): Boolean =
        SecureStorage(context).getToken().isNullOrBlank()

    /** Returns true when the caller is a guest (and the sign-up prompt was shown). */
    fun check(context: Context): Boolean {
        if (!isGuest(context)) return false
        Toast.makeText(
            context,
            "Create an account to like, comment, follow and chat",
            Toast.LENGTH_LONG
        ).show()
        context.startActivity(Intent(context, MainActivity::class.java).apply {
            putExtra("force_signup", true)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
        return true
    }
}
