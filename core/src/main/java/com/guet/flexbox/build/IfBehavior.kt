package com.guet.flexbox.build

import com.facebook.litho.Component
import com.guet.flexbox.NodeInfo

internal object IfBehavior : Behavior() {
    override fun onApply(
            c: BuildContext,
            attrs: Map<String, String>,
            children: List<NodeInfo>,
            upperVisibility: Int
    ): List<Component> {
        return if (c.requestValue("test", attrs)) {
            return children.map {
                c.createFromElement(it, upperVisibility)
            }.flatten()
        } else {
            emptyList()
        }
    }
}