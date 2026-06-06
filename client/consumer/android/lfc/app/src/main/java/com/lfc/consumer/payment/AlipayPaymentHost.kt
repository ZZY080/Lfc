package com.lfc.consumer.payment

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * 持有当前前台 Activity，供支付宝 SDK 调起支付。
 * 在 MainActivity.onResume / onPause 中绑定/解绑。
 */
object AlipayPaymentHost {
    private var activityRef: WeakReference<Activity>? = null

    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (activityRef?.get() === activity) {
            activityRef = null
        }
    }

    fun getActivity(): Activity? = activityRef?.get()
}
