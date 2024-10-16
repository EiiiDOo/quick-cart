package com.senseicoder.quickcart.core.network.interfaces


import com.senseicoder.quickcart.core.entity.order.Order
import com.senseicoder.quickcart.core.wrappers.ApiState
import com.senseicoder.quickcart.core.entity.product.ProductDetails
import com.admin.adapter.CreateAddressMutation_ResponseAdapter
import com.apollographql.apollo.api.ApolloResponse
import com.senseicoder.quickcart.core.model.ProductOfCart
import com.storefront.AddProductToCartMutation
import com.storefront.CartLinesUpdateMutation
import com.storefront.CreateCartMutation
import com.storefront.CreateCustomerAccessTokenMutation
import com.storefront.CreateCustomerMutation
import com.storefront.GetProductByIdQuery
import com.storefront.CustomerAddressesQuery
import com.storefront.CustomerDefaultAddressUpdateMutation
import com.storefront.GetCartDetailsQuery
import com.storefront.RemoveProductFromCartMutation
import com.storefront.SearchQuery
import com.storefront.type.MailingAddressInput
import kotlinx.coroutines.flow.Flow

interface StorefrontHandler {
    suspend fun loginUser(
        email: String,
        password: String
    ): Flow<CreateCustomerAccessTokenMutation.CustomerAccessToken>

    fun createCustomer(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Flow<CreateCustomerMutation.Customer>


    //getting orders
    fun getCustomerOrders(token: String): Flow<ApiState<List<Order>>>

    //SHOPPING CART NEEDED

    suspend fun removeProductFromCart(cartId: String, lineId: String): Flow<RemoveProductFromCartMutation.CartLinesRemove?>

    suspend fun getProductsCart(cartId: String): Flow<ApolloResponse<GetCartDetailsQuery.Data>>

    suspend fun updateQuantityOfProduct(cartId: String, lineId: String,quantity:Int): Flow<CartLinesUpdateMutation.Lines?>

    //DETAILS NEEDED

    suspend fun getProductDetailsById(id: String): Flow<GetProductByIdQuery.Product?>
    //ADDRESS NEEDED

    suspend fun getCustomerAddresses(token: String): Flow<CustomerAddressesQuery.Customer>

    suspend fun updateCustomerAddress(address : MailingAddressInput, token:String , id :String,):Flow<String?>

    suspend fun deleteAddress(id :String,token:String):Flow<String?>

    suspend fun createAddress(
        customerAddress: MailingAddressInput,
        token: String
    ): Flow<String?>

    suspend fun updateDefaultAddress(token:String,id :String):Flow<CustomerDefaultAddressUpdateMutation.CustomerDefaultAddressUpdate>?

    suspend fun createCart(email: String): Flow<CreateCartMutation.Cart>

    suspend fun addToCartById(
        cartId: String,
        quantity: Int,
        variantId: String
    ): Flow<AddProductToCartMutation.CartLinesAdd>

    suspend fun getProductsByQuery(query: String): Flow<SearchQuery.Search>
}