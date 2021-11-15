package com.itmo.microservices.demo.order.impl.logging

import com.itmo.microservices.commonlib.logging.NotableEvent
import com.itmo.microservices.demo.order.api.model.BusketModel
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

enum class BusketNotableEvents(private val template: String) : NotableEvent {
    I_ALL_BUSKETS_GOT("All buskets got"),
    I_BUSKET_GOT("Busket got: {}"),
    I_BUSKET_CREATED("Busket created: {}"),
    I_PRODUCT_CREATED("Product created: {}"),
    I_BUSKET_DELETED("Busket deleted: {}"),
    I_BUSKET_UPDATED("Busket updated: {}");

    override fun getTemplate(): String {
        return template
    }

    override fun getName(): String {
        return name
    }
}