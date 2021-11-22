package com.itmo.microservices.demo.products.impl.entity

import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Product {
    @Id
    var id: UUID? = UUID.randomUUID()
    var title: String? = null
    var description: String? = null
    var price: Int? = 100
    var amount:Int?=null

    constructor()

    constructor(title: String? = null, description: String? = null,  price: Int? = null, amount:Int) {
        this.title = title
        this.description = description
        this.price = price
        this.amount=amount
    }

    override fun toString(): String {
        return "Product(id=$id, name=$title, description=$description, price=$price, amount=$amount)"
    }

}
