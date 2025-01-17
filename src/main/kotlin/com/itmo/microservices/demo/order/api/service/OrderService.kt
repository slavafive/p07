package com.itmo.microservices.demo.order.api.service

import com.itmo.microservices.demo.order.api.model.BookingDto
import com.itmo.microservices.demo.order.api.model.OrderDto
import com.itmo.microservices.demo.order.api.model.OrderStatus
import com.itmo.microservices.demo.order.impl.entity.OrderItem
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

interface OrderService {
    fun getOrderById(orderId: UUID): OrderDto
    fun createOrder(userId: UserDetails): OrderDto
    fun addItemToOrder(orderId: UUID, productId: UUID, amount: Int)
    fun registerOrder(orderId: UUID): BookingDto
    fun setDeliveryTime(orderId: UUID, slotinSec: Int): BookingDto
    fun changeOrderStatus(orderId: UUID, status: OrderStatus)
    fun getOrderItemById(orderItemId: UUID): OrderItem
}