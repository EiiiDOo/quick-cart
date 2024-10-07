package com.senseicoder.quickcart.core.global

object Constants {
    object Errors{
        const val UNKNOWN = "unknown error occurred"
        const val CUSTOMER_CREATE_FAILED = "customer creation failed"
    }

    sealed class SharedPrefs{
       object Settings{
           const val SETTINGS: String = "settings"

       }
    }

    object API{
        const val ADMIN = "https://android-alex-team5.myshopify.com/admin/api/2024-10/graphql.json"
        const val STORE_FRONT = "https://android-alex-team5.myshopify.com/api/2024-10/graphql.json"
        const val CUSTOMER_ID_PREFIX = "gid://shopify/Customer/"
        const val ORDER_ID_PREFIX = "gid://shopify/Order/"
    }

    const val USER_EMAIL: String = "userEmail"
    const val USER_DISPLAY_NAME: String = "userDisplayName"
    const val USER_TOKEN_DEFAULT: String = "unknown"
    const val USER_TOKEN: String = "userToken"
    const val USER_ID = "userId"
    const val USER_ID_DEFAULT = "unknown"
}