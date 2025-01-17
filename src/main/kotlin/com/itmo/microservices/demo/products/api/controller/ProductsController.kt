package com.itmo.microservices.demo.products.api.controller



import com.itmo.microservices.demo.products.api.model.CatalogItemDto
import com.itmo.microservices.demo.products.api.model.ProductRequest
import com.itmo.microservices.demo.products.api.service.ProductsService
import com.itmo.microservices.demo.products.impl.entity.Product
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class ProductsController(private val productsService: ProductsService) {

    @RequestMapping("/items",method = [RequestMethod.GET])
    @Operation(
        summary = "Get product catalog / get all products",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = "Bad request", responseCode = "400", content = [Content()])
        ],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getProductCatalog(@AuthenticationPrincipal @RequestParam available: Boolean,
                          @Parameter(hidden = true) @AuthenticationPrincipal author: UserDetails?): List<Product>
    = productsService.getAllProducts(available, author)

    @PostMapping("/_internal/catalogItem")
    @Operation(
            summary = "Add product",
            responses = [
                ApiResponse(description = "OK", responseCode = "200"),
                ApiResponse(description = "Bad request", responseCode = "400", content = [Content()])
            ]
    )
    fun addProduct(@RequestBody productRequest: ProductRequest): CatalogItemDto =
            productsService.addProduct(productRequest)


    @PostMapping("/_internal/deleteAll")
    @Operation(
        summary = "Add product",
        responses = [
            ApiResponse(description = "OK", responseCode = "200"),
            ApiResponse(description = "Bad request", responseCode = "400", content = [Content()])
        ]
    )
    fun deleteAllProducts(): Unit =productsService.deleteAllProducts()



}