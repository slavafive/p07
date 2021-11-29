package com.itmo.microservices.demo.order.impl.entity

import java.util.*
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class OrderItem {
    @Id
    @GeneratedValue
    var id: UUID? = null
    var title: String? = null
    var price: Int? = null

    constructor()

    constructor(title: String?, price: Int?) {
        this.title = title
        this.price = price
    }

    override fun toString(): String {
        return "OrderItem(id=$id, title=$title, price=$price)"
    }

}