package com.iqbalwork.capsule

import android.app.Application

class CapsoolApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}
