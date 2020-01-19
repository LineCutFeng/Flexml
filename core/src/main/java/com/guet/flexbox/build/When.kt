package com.guet.flexbox.build

import com.guet.flexbox.HostingContext
import com.guet.flexbox.TemplateNode
import com.guet.flexbox.el.ELContext

object When : Declaration() {

    override val attributeInfoSet: AttributeInfoSet
        get() = emptyMap()

    override fun onBuild(
            bindings: BuildUtils,
            attrs: AttributeSet,
            children: List<TemplateNode>,
            factory: Factory?,
            pageContext: HostingContext,
            data: ELContext,
            upperVisibility: Boolean,
            other: Any
    ): List<Child> {
        var elseItem: TemplateNode? = null
        if (children.isNullOrEmpty()) {
            return emptyList()
        }
        for (item in children) {
            if (item.type == "case") {
                val itemAttrs = item.attrs
                if (itemAttrs != null && If.onBind(
                                itemAttrs,
                                pageContext,
                                data
                        )["test"] == true) {
                    return item.children?.map {
                        bindings.build(
                                it,
                                pageContext,
                                data,
                                upperVisibility,
                                other
                        )
                    }?.flatten() ?: emptyList()
                }
            } else if (item.type == "else" && elseItem == null) {
                elseItem = item
            }
        }
        return elseItem?.children?.map {
            bindings.build(
                    it,
                    pageContext,
                    data,
                    upperVisibility,
                    other
            )
        }?.flatten() ?: emptyList()
    }

}