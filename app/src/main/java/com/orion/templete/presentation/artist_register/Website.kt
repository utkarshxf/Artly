package com.orion.templete.presentation.artist_register

// People type "mysite.com"; a link without a scheme can't be opened, so save it as https://mysite.com
fun normalizeWebsite(input: String?): String? {
    val url = input?.trim().orEmpty()
    if (url.isEmpty()) return null
    return if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) url
    else "https://$url"
}
