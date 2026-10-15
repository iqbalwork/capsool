package com.iqbalwork.capsule.platform.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/** Stops a list that keeps loading more items from looping forever. */
private const val MAX_PAGES = 60

/**
 * F-72: scrolls the app in front back to the top. Finds the largest visible scrollable view in
 * the active window and pages it backward until it cannot go further (spec §2.5.9).
 */
object ScrollToTop {

    fun run(service: AccessibilityService) {
        val root = service.rootInActiveWindow ?: return
        if (root.packageName == service.packageName) return
        val target = largestScrollable(root) ?: return
        repeat(MAX_PAGES) {
            if (!target.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) return
        }
    }

    private fun largestScrollable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var best: AccessibilityNodeInfo? = null
        var bestArea = 0
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        val bounds = Rect()
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isScrollable && node.isVisibleToUser) {
                node.getBoundsInScreen(bounds)
                val area = bounds.width() * bounds.height()
                if (area > bestArea) {
                    best = node
                    bestArea = area
                }
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::addLast)
        }
        return best
    }
}
