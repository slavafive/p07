package com.itmo.microservices.demo.order.impl.service

import com.google.common.eventbus.EventBus
import com.itmo.microservices.commonlib.annotations.InjectEventLogger
import com.itmo.microservices.commonlib.logging.EventLogger
import com.itmo.microservices.demo.common.exception.NotFoundException
import com.itmo.microservices.demo.order.api.model.BusketModel
import com.itmo.microservices.demo.order.api.messaging.BusketCreatedEvent
import com.itmo.microservices.demo.order.api.messaging.BusketDeletedEvent
import com.itmo.microservices.demo.order.api.messaging.BusketUpdatedEvent
import com.itmo.microservices.demo.order.api.model.ProductType
import com.itmo.microservices.demo.order.api.service.BusketService
import com.itmo.microservices.demo.order.impl.entity.Busket
import com.itmo.microservices.demo.order.impl.entity.OrderProduct
import com.itmo.microservices.demo.order.impl.repository.BusketRepository
import com.itmo.microservices.demo.order.impl.repository.OrderProductRepository
import com.itmo.microservices.demo.order.impl.util.toModel
import com.itmo.microservices.demo.order.impl.logging.BusketNotableEvents
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.ResponseStatus
import java.lang.RuntimeException
import java.util.*

@Service
@Suppress("UnstableApiUsage")
class BusketServiceImpl(private val productRepository: OrderProductRepository,
                        private val busketRepository: BusketRepository,
                        private val eventBus: EventBus
) : BusketService {

    @InjectEventLogger
    lateinit var eventLogger: EventLogger

    private fun createProductMock() {
        var product = OrderProduct(
            name = "Галоши",
            description = "Крутые галоши",
            country = "Русские",
            price = 300.0,
            sale = null,
            type = ProductType.CLOTHES
        )
        productRepository.save(product)
        eventLogger.info(
            BusketNotableEvents.I_PRODUCT_CREATED,
            product
        )
    }

    override fun allBuskets(): List<BusketModel> {
        createProductMock()
        eventLogger.info(BusketNotableEvents.I_ALL_BUSKETS_GOT)
        return busketRepository.findAll().map { it.toModel() }
    }

    override fun createBusket(busket: BusketModel, author: UserDetails): BusketModel {
        val products = busket.products.mapNotNull { productRepository.findById(it).orElse(null) }.toMutableList()
        val busket = Busket(
            username = author.username,
            products = products
        )
        busketRepository.save(busket)
        eventBus.post(BusketCreatedEvent(busket.toModel()))
        eventLogger.info(
            BusketNotableEvents.I_BUSKET_CREATED,
            busket
        )
        return busket.toModel()
    }

    override fun getBusketById(busketId: UUID): BusketModel {
        val busket = busketRepository.findByIdOrNull(busketId) ?: throw NotFoundException("Busket $busketId not found")
        eventLogger.info(BusketNotableEvents.I_BUSKET_GOT, busket)
        return busket.toModel()
    }

    override fun deleteBusketById(busketId: UUID): BusketModel {
        val busket = busketRepository.findByIdOrNull(busketId) ?: throw NotFoundException("Busket $busketId not found")
        busketRepository.delete(busket)
        eventBus.post(BusketDeletedEvent(busket.toModel()))
        eventLogger.info(BusketNotableEvents.I_BUSKET_DELETED, busket)
        return busket.toModel()
    }

    override fun addProductToBusket(busketId: UUID, productId: UUID): BusketModel {
        val busket = busketRepository.findByIdOrNull(busketId) ?: throw NotFoundException("Busket $busketId not found")
        val product = productRepository.findByIdOrNull(productId) ?: throw NotFoundException("Product $productId not found")
        busket.products?.add(product)
        busketRepository.save(busket)
        eventLogger.info(BusketNotableEvents.I_BUSKET_UPDATED, busket)
        eventBus.post(BusketUpdatedEvent(busket.toModel()))
        return busket.toModel()
    }

    override fun deleteProductFromBusket(busketId: UUID, productId: UUID): BusketModel? {
        val busket = busketRepository.findByIdOrNull(busketId) ?: throw NotFoundException("Busket $busketId not found")
        productRepository.findByIdOrNull(productId) ?: throw NotFoundException("Product $productId not found")
        val id = busket.products?.indexOfFirst { it.id == productId } ?: -1
        if (id == -1) {
            return null
        }

        busket.products?.removeAt(id)
        busketRepository.save(busket)
        eventLogger.info(BusketNotableEvents.I_BUSKET_UPDATED, busket)
        eventBus.post(BusketUpdatedEvent(busket.toModel()))
        return busket.toModel()
    }
}