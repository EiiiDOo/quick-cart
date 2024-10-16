package com.senseicoder.quickcart.features.main.ui.product_details

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isNotEmpty
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.senseicoder.quickcart.R
import com.senseicoder.quickcart.core.dialogs.ConfirmationDialog
import com.senseicoder.quickcart.core.global.Constants
import com.senseicoder.quickcart.core.global.NetworkUtils
import com.senseicoder.quickcart.core.global.dpToPx
import com.senseicoder.quickcart.core.global.lightenColor
import com.senseicoder.quickcart.core.global.showSnackbar
import com.senseicoder.quickcart.core.global.toColor
import com.senseicoder.quickcart.core.global.toTwoDecimalPlaces
import com.senseicoder.quickcart.core.model.ReviewDTO
import com.senseicoder.quickcart.core.model.graph_product.OptionValues
import com.senseicoder.quickcart.core.model.graph_product.ProductDTO
import com.senseicoder.quickcart.core.model.graph_product.Variant
import com.senseicoder.quickcart.core.network.StorefrontHandlerImpl
import com.senseicoder.quickcart.core.network.currency.CurrencyRemoteImpl
import com.senseicoder.quickcart.core.repos.cart.CartRepoImpl
import com.senseicoder.quickcart.core.repos.favorite.FavoriteRepoImpl
import com.senseicoder.quickcart.core.repos.currency.CurrencyRepoImpl
import com.senseicoder.quickcart.core.repos.product.ProductsRepo
import com.senseicoder.quickcart.core.services.SharedPrefsService
import com.senseicoder.quickcart.core.wrappers.ApiState
import com.senseicoder.quickcart.databinding.FragmentProductDetailsBinding
import com.senseicoder.quickcart.features.main.ui.favorite.viewmodel.FavoriteViewModel
import com.senseicoder.quickcart.features.main.ui.favorite.viewmodel.FavoriteViewModelFactory
import com.senseicoder.quickcart.features.main.ui.main_activity.MainActivity
import com.senseicoder.quickcart.features.main.ui.main_activity.viewmodels.MainActivityViewModel
import com.senseicoder.quickcart.features.main.ui.main_activity.viewmodels.MainActivityViewModelFactory
import com.senseicoder.quickcart.features.main.ui.product_details.adapters.ProductDetailsPagerAdapter
import com.senseicoder.quickcart.features.main.ui.product_details.viewmodel.ProductDetailsViewModel
import com.senseicoder.quickcart.features.main.ui.product_details.viewmodel.ProductDetailsViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class ProductDetailsFragment : Fragment() {


    private lateinit var pagerAdapter: ProductDetailsPagerAdapter
    private lateinit var binding: FragmentProductDetailsBinding
    private lateinit var productDetailsViewModel: ProductDetailsViewModel
    private lateinit var favoriteViewModel: FavoriteViewModel

    private val reviews: List<ReviewDTO> = listOf(
        ReviewDTO(name = "Kareem", description = "Quality is not as advertised.", rating = 2.5),
        ReviewDTO(name = "Dina", description = "Very happy with my purchase.", rating = 4.5),
        ReviewDTO(name = "Ali", description = "Not bad.", rating = 3.0),
        ReviewDTO(name = "Layla", description = "Absolutely love it! Great buy.", rating = 5.0),
        ReviewDTO(
            name = "Tamer",
            description = "Product arrived in terrible condition.",
            rating = 1.0
        ),
        ReviewDTO(name = "Adel", description = "Not worth the price.", rating = 2.0),
        ReviewDTO(name = "Salma", description = "Exceeded my expectations!", rating = 5.0),
        ReviewDTO(name = "Ibrahim", description = "Wouldn't recommend.", rating = 2.0),
        ReviewDTO(name = "Reem", description = "Good, but shipping took too long.", rating = 3.5),
    )
    private lateinit var currency: String
    private var selectedAmount: Int = 0

    //for animating pager
    private lateinit var handler: Handler
    private val animationRunnable: Runnable = object : Runnable {
        override fun run() {
            val totalPages = binding.productDetailsImagesPager.adapter?.itemCount ?: 0

            // Move to the next page or reset to the first page if at the end
            if (currentPage == totalPages - 1) {
                currentPage = 0
            } else {
                currentPage++
            }

            binding.productDetailsImagesPager.setCurrentItem(currentPage, true)

            // Repeat this runnable every `swipeInterval` milliseconds
            handler.postDelayed(this, swipeInterval)
        }
    }
    private val swipeInterval: Long = 3000 // 3 seconds
    private var currentPage = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductDetailsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val productDetailsFactory = ProductDetailsViewModelFactory(
            CartRepoImpl.getInstance(
                StorefrontHandlerImpl,
                SharedPrefsService
            ),
            ProductsRepo.getInstance()
        )
        val favoriteDetailsFactory = FavoriteViewModelFactory(
            FavoriteRepoImpl.getInstance()
        )
        productDetailsViewModel = ViewModelProvider(this, productDetailsFactory)[ProductDetailsViewModel::class.java]
        favoriteViewModel = ViewModelProvider(this, favoriteDetailsFactory)[FavoriteViewModel::class.java]
        val firebaseUserId = SharedPrefsService.getSharedPrefString(Constants.FIREBASE_USER_ID, Constants.FIREBASE_USER_ID_DEFAULT)

        productDetailsViewModel.getProductDetails(ViewModelProvider(requireActivity())[MainActivityViewModel::class.java].currentProductId.value)
        favoriteViewModel.checkIfFavorite(firebaseUserId,ViewModelProvider(requireActivity())[MainActivityViewModel::class.java].currentProductId.value)
        subscribeToObservables()
        productDetailsViewModel.getProductDetails(
            ViewModelProvider(
                requireActivity(),
                MainActivityViewModelFactory(
                    CurrencyRepoImpl(
                        CurrencyRemoteImpl
                    )
                )
            )[MainActivityViewModel::class.java].currentProductId.value
        )
        handler = Handler(Looper.getMainLooper())
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as MainActivity).hideBottomNavBar()
    }

    private fun subscribeToObservables() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED){
                productDetailsViewModel.product.collect { response->
                    binding.apply {
                        when (response) {
                            ApiState.Loading, ApiState.Init -> {
                                showLoadingGroup()
                                successProductDetails.visibility = View.GONE
                            }

                            is ApiState.Success -> {
                                hideLoadingGroup()
                                successProductDetails.visibility = View.VISIBLE
                                val product = response.data
                                binding.apply {
                                    titleProductDetails.text = product.title
                                    descriptionProductDetails.text = product.description
                                    stockProductDetails.text = "${requireContext().getString(R.string.in_stock)}${product.totalInventory}"
                                    currentSelectedQuantityProductDetails.text = "0"
                                    currency = product.currency
                                    priceVarianceProductDetails.text = getPriceVariance(product)
                                    cartPrice.text = "${getString(R.string.price_text)}0.0 $currency"
                                    reviewCountProductDetails.text = "${reviews.size} Reviews"
                                    updateSizesGroup(product.variants, product.options.first { it.name == "Size" }.values)
                                    updateColorsGroup(product.variants, product.options.first { it.name == "Color" }.values)
                                    // Initialize the adapter
                                    pagerAdapter = ProductDetailsPagerAdapter(
                                        product.images
                                    )
                                    productDetailsImagesPager.adapter = pagerAdapter
                                    // Set the adapter to ViewPager2
                                    binding.productDetailsImagesPager.adapter = pagerAdapter
                                    // Link the ViewPager2 with the DotsIndicator
                                    binding.productDetailsDotsIndicator.setViewPager2(binding.productDetailsImagesPager)
                                    binding.successProductDetails.visibility = View.VISIBLE
                                    startAutoSwipe()
                                }
                            }

                            is ApiState.Failure -> {
                                hideLoadingGroup()
                                this@ProductDetailsFragment.showSnackbar(response.msg)
                            }
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED){
                productDetailsViewModel.selectedProduct.collectLatest { selectedProducts->
                    binding.apply {
                        when(selectedProducts){
                            ProductDetailsViewModel.ProductState.Init -> {
                                Log.d(TAG, "subscribeToObservables: Init")
                            }

                            is ProductDetailsViewModel.ProductState.MultiSelected -> {
                                selectedAmount = 0
                                val variant = selectedProducts.data.first.first()
                                Log.d(TAG, "subscribeToObservables: MultiSelected\n${selectedProducts.data.first}")
                                variant.let {
                                    if(variant.quantityAvailable.toInt() != 0)
                                    {
                                        enableButtons()
                                        addToCartBtnProductDetails.isEnabled = false
                                        decreaseQuantityBtnProductDetails.isEnabled = false
                                        stockProductDetails.setTextColor(ColorStateList.valueOf(requireContext().getColor(R.color.black)))
                                    }
                                    else{
                                        stockProductDetails.setTextColor(ColorStateList.valueOf(requireContext().getColor(R.color.red)))
                                    }
                                    stockProductDetails.text = "${requireContext().getString(R.string.in_stock)}${it.quantityAvailable}"
                                    currentSelectedQuantityProductDetails.text = "0"
                                    cartPrice.text = "${getString(R.string.price_text)}0.0 $currency"
                                    /*pagerAdapter.updateList(listOf(variant.image))
                                    binding.productDetailsDotsIndicator.setViewPager2(binding.productDetailsImagesPager)*/
                                    increaseQuantityBtnProductDetails.setOnClickListener{ _->
                                        selectedAmount++
                                        if(selectedAmount == variant.quantityAvailable.toInt()){
                                            increaseQuantityBtnProductDetails.isEnabled = false
                                        }
                                        decreaseQuantityBtnProductDetails.isEnabled = true
                                        addToCartBtnProductDetails.isEnabled = true
                                        currentSelectedQuantityProductDetails.text = selectedAmount.toString()
                                        val newPrice = (it.price.amount.toDouble() * selectedAmount).toTwoDecimalPlaces()
                                        cartPrice.text = "${getString(R.string.price_text)}$newPrice $currency"
                                    }
                                    decreaseQuantityBtnProductDetails.setOnClickListener{ _->
                                        selectedAmount--
                                        if(selectedAmount == 0){
                                            decreaseQuantityBtnProductDetails.isEnabled = false
                                            addToCartBtnProductDetails.isEnabled = false
                                        }
                                        increaseQuantityBtnProductDetails.isEnabled = true
                                        currentSelectedQuantityProductDetails.text = selectedAmount.toString()
                                        val newPrice = (it.price.amount.toDouble() * selectedAmount).toTwoDecimalPlaces()
                                        cartPrice.text = "${getString(R.string.price_text)}$newPrice $currency"
                                    }
                                    addToCartBtnProductDetails.setOnClickListener {
                                        if(NetworkUtils.isConnected(requireContext())){
                                            if(SharedPrefsService.getSharedPrefString(Constants.USER_TOKEN, Constants.USER_TOKEN_DEFAULT) != Constants.USER_TOKEN_DEFAULT){
                                                productDetailsViewModel.addProductToCart(selectedAmount, selectedProducts.data.first, ViewModelProvider(requireActivity())[MainActivityViewModel::class.java].currentUser.value)
                                            }else{
                                                Toast.makeText(
                                                    requireContext(),
                                                    getString(R.string.permission_denied),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            }else{
                                                showSnackbar(getString(R.string.no_internet_connection))
                                        }
                                    }
                                    // Link the ViewPager2 with the DotsIndicator
//                                    binding.productDetailsDotsIndicator.setViewPager2(binding.productDetailsImagesPager)
                                }
                            }
                            is ProductDetailsViewModel.ProductState.SingleSelected -> {
                                Log.d(TAG, "subscribeToObservables: SingleSelected, ${selectedProducts.data.second}\n${selectedProducts.data.first}")
                                when(selectedProducts.data.second){
                                    ProductDetailsViewModel.SelectedBy.Color -> {
                                        updateSizesGroup(selectedProducts.data.first, (productDetailsViewModel.product.value as ApiState.Success).data.options.first { it.name == "Size" }.values)
                                    }
                                    ProductDetailsViewModel.SelectedBy.Size -> {
                                        updateColorsGroup(selectedProducts.data.first, (productDetailsViewModel.product.value as ApiState.Success).data.options.first { it.name == "Color" }.values)
                                    }
                                }
                            }
                            is ProductDetailsViewModel.ProductState.Unselected -> {
                                Log.d(TAG, "subscribeToObservables: Unselected")
                                disableButtons()
                                selectedAmount = 0
                                currentSelectedQuantityProductDetails.text = "0"
                                cartPrice.text = "${getString(R.string.price_text)}0.0 $currency"
                                pagerAdapter.updateList((productDetailsViewModel.product.value as ApiState.Success<ProductDTO>).data.images)
                                when(selectedProducts.data.second){
                                    ProductDetailsViewModel.SelectedBy.Color -> {
                                        updateColorsGroup(selectedProducts.data.first, (productDetailsViewModel.product.value as ApiState.Success).data.options.first { it.name == "Color" }.values)
                                    }
                                    ProductDetailsViewModel.SelectedBy.Size -> {
                                        updateSizesGroup(selectedProducts.data.first, (productDetailsViewModel.product.value as ApiState.Success).data.options.first { it.name == "Size" }.values)
                                    }
                                }
                            }

                            ProductDetailsViewModel.ProductState.Reset -> {
                                val product = (productDetailsViewModel.product.value as ApiState.Success).data
                                binding.apply {
                                    titleProductDetails.text = product.title
                                    descriptionProductDetails.text = product.description
                                    stockProductDetails.text = "${requireContext().getString(R.string.in_stock)}${product.totalInventory}"
                                    currentSelectedQuantityProductDetails.text = "0"
                                    currency = product.currency
                                    reviewCountProductDetails.text = "${reviews.size} Reviews"
                                    cartPrice.text = "${getString(R.string.price_text)}0.0 $currency"
                                    updateSizesGroup(product.variants, product.options.first { it.name == "Size" }.values)
                                    updateColorsGroup(product.variants, product.options.first { it.name == "Color" }.values)
                                    // Initialize the adapter
                                    pagerAdapter = ProductDetailsPagerAdapter(
                                        product.images
                                    )
                                    pagerAdapter.updateList(product.images)
                                    // Link the ViewPager2 with the DotsIndicator
                                    binding.productDetailsDotsIndicator.setViewPager2(binding.productDetailsImagesPager)
                                    binding.successProductDetails.visibility = View.VISIBLE
                                    startAutoSwipe()
                                }
                            }
                        }
                    }
                    enableAllChipGroups()
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED){
                productDetailsViewModel.addingToCart.collect {
                    when(it){
                        ApiState.Init -> {

                        }
                        ApiState.Loading -> {
                            disableButtons()
                            disableAllChipGroups()
                        }
                        is ApiState.Success -> {
                            this@ProductDetailsFragment.showSnackbar(getString(R.string.product_added_successfully))
                            delay(1.5.seconds)
                            findNavController().navigateUp()
                        }
                        is ApiState.Failure -> {
                            enableButtons()
                            enableAllChipGroups()
                            this@ProductDetailsFragment.showSnackbar(getString(R.string.product_added_unsuccessfully))
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED){
                favoriteViewModel.isFavorite.collect { response ->
                    binding.apply {
                        when(response){
                            ApiState.Init, ApiState.Loading  -> {
                                favoriteLayoutProductDetails.isClickable = false
                            }
                            is ApiState.Success -> {
                                favoriteLayoutProductDetails.isEnabled = true
                                favoriteLayoutProductDetails.icon = AppCompatResources.getDrawable(requireContext(), if(response.data) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)
                                binding.favoriteLayoutProductDetails.setOnClickListener{
                                   setupFavoriteOnClickListener(response.data)
                                }
                            }
                            is ApiState.Failure -> {
                                favoriteLayoutProductDetails.isEnabled = true
                                showSnackbar(response.msg)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getPriceVariance(product: ProductDTO): CharSequence {
        val priceMinimum = product.priceRange.minVariantPrice.amount
        val priceMaximum = product.priceRange.maxVariantPrice.amount
        val priceMinimumEmpty = priceMinimum.isBlank()
        val priceMaximumEmpty = priceMaximum.isBlank()
        val price = product.let {
            Log.d(TAG, "bind: minimum: ${priceMinimum}, maximum: ${priceMaximum}\n priceMinimumEmpty: $priceMinimumEmpty, priceMaximumEmpty: $priceMaximumEmpty")
            if ((!priceMinimumEmpty && !priceMaximumEmpty) && (priceMinimum != priceMaximum)) {
                "${getString(R.string.price_variance)}${priceMinimum} - ${priceMaximum} $currency"
            }else{
                if(!priceMinimumEmpty){
                    "${getString(R.string.price_text)}${priceMinimum} $currency"
                }else if (!priceMaximumEmpty){
                    "${getString(R.string.price_text)}${priceMaximum} $currency"
                }else{
                    "Unknown Price"
                }
            }
        }
        return price
    }

    private fun setupFavoriteOnClickListener(data: Boolean) {
        if(NetworkUtils.isConnected(requireContext())){
            if(SharedPrefsService.getSharedPrefString(Constants.USER_TOKEN, Constants.USER_TOKEN_DEFAULT) != Constants.USER_TOKEN_DEFAULT){
                if(data) ConfirmationDialog(requireActivity(), null){
                    favoriteViewModel.removeFromFavorite((productDetailsViewModel.product.value as ApiState.Success).data)
                }.apply {
                    this.message = getString(R.string.remove_from_favorite_confirmation)
                    showDialog()
                } else favoriteViewModel.addToFavorite((productDetailsViewModel.product.value as ApiState.Success).data)
            }else{
                Toast.makeText(
                    requireContext(),
                    getString(R.string.permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }else{
            showSnackbar(getString(R.string.no_internet_connection))
        }
    }

    private fun disableButtons(){
        binding.apply {
            addToCartBtnProductDetails.isEnabled = false
            favoriteLayoutProductDetails.isEnabled = false
            increaseQuantityBtnProductDetails.isEnabled = false
            decreaseQuantityBtnProductDetails.isEnabled = false
        }
    }

    private fun enableButtons(){
        binding.apply {
            addToCartBtnProductDetails.isEnabled = true
            increaseQuantityBtnProductDetails.isEnabled = true
            decreaseQuantityBtnProductDetails.isEnabled = true
            favoriteLayoutProductDetails.isEnabled = true
        }
    }

    private fun updateColorsGroup(variants: List<Variant>, optionValues: List<OptionValues>){
        if(binding.colorsChipGroupProductDetails.isNotEmpty()){
            binding.colorsChipGroupProductDetails.removeAllViews()
        }
        for(optionValue in optionValues){
            addChipToColorsGroup(optionValue, variants.filter { variant -> variant.selectedOptions.firstOrNull{it.name == "Color"} != null })
        }
    }

    private fun updateSizesGroup(variants: List<Variant>, optionValues: List<OptionValues>) {
        if (binding.sizesChipGroupProductDetails.isNotEmpty()) {
            binding.sizesChipGroupProductDetails.removeAllViews()
        }
        for (optionValue in optionValues) {
            addChipToSizesGroup(optionValue, variants.filter { variant -> variant.selectedOptions.firstOrNull{it.name == "Size"} != null })
        }
    }

    private fun addChipToColorsGroup(optionValues: OptionValues, variants: List<Variant>) {
        val chipGroup = binding.colorsChipGroupProductDetails
        val backgroundColor = optionValues.name.toColor()
        val chip = Chip(chipGroup.context).apply {
            setTextColor(requireContext().getColor(R.color.transparent))
            chipBackgroundColor =
                ColorStateList.valueOf(backgroundColor)
            chipIcon = AppCompatResources.getDrawable(context, R.drawable.ic_check)
            isChipIconVisible = false
            layoutParams = ViewGroup.LayoutParams(48.dpToPx(requireContext()), 48.dpToPx(requireContext()))
            rippleColor = ColorStateList.valueOf(backgroundColor.lightenColor())
            chipStrokeColor = ColorStateList.valueOf(backgroundColor)
            text = optionValues.name
            setTextColor(ColorStateList.valueOf(requireContext().getColor(R.color.transparent)))
            this.isCheckable = true
            setOnCheckedChangeListener { chip, isChecked ->
                Log.d(TAG, "setOnCheckedChangeListener: $isChecked")
                isChipIconVisible = isChecked
                if(chip.isPressed){
                    val chipId = binding.sizesChipGroupProductDetails.checkedChipId
                    disableAllChipGroups()
                    productDetailsViewModel.setCurrentSelectedProduct(
                        variants,
                        isChecked,
                        ProductDetailsViewModel.SelectedBy.Color,
                        binding.sizesChipGroupProductDetails.findViewById<Chip>(chipId)?.text,
                        optionValues.name,
                    )
                }
            }
        }
        chipGroup.addView(chip)
    }

    private fun addChipToSizesGroup(optionValues: OptionValues, variants: List<Variant>) {
        val chipGroup = binding.sizesChipGroupProductDetails
        val chip = Chip(chipGroup.context).apply {
            setTextColor(requireContext().getColor(R.color.primary))
            chipBackgroundColor =
                ColorStateList.valueOf(requireContext().getColor(R.color.white))
            chipIcon = AppCompatResources.getDrawable(context, R.drawable.ic_check)
            isChipIconVisible = false
            checkedIconTint = ColorStateList.valueOf(requireContext().getColor(R.color.primary))
            rippleColor = ColorStateList.valueOf(requireContext().getColor(R.color.primary))
            text = optionValues.name
            this.isCheckable = true
            setOnCheckedChangeListener { chip, isChecked ->
                Log.d(TAG, "setOnCheckedChangeListener: $isChecked")
                isChipIconVisible = isChecked
                if(chip.isPressed){
                    val chipId = binding.colorsChipGroupProductDetails.checkedChipId
                    disableAllChipGroups()
                    productDetailsViewModel.setCurrentSelectedProduct(
                        variants,
                        isChecked,
                        ProductDetailsViewModel.SelectedBy.Size,
                        binding.colorsChipGroupProductDetails.findViewById<Chip>(chipId)?.text,
                        optionValues.name
                    )
                }
            }
        }
        chipGroup.addView(chip)
    }

    private fun updateButtonText(price: String) {
        // Button title
        val buttonText = "Add To Cart"

        // Combine title and price into a single string
        val fullText = "$buttonText    |   $$price"

        // Create a SpannableString
        val spannable = SpannableString(fullText)

        // Set spans for the price
        val priceStart = fullText.indexOf("$") // Find the start index of the price
        val priceEnd = fullText.length // End of the string

        // Style the price (change color to yellow and make it bold)
        spannable.setSpan(
            ForegroundColorSpan(requireContext().getColor(R.color.primary)), // Change color to yellow
            priceStart,
            priceEnd,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.setSpan(
            StyleSpan(Typeface.BOLD), // Make the price bold
            priceStart,
            priceEnd,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Set the spannable text to the MaterialButton
//        binding.addToCartBtnProductDetails.text = spannable
    }

    private fun disableAllChipGroups(){
        disableChipGroup(binding.sizesChipGroupProductDetails)
        disableChipGroup(binding.colorsChipGroupProductDetails)
    }

    private fun enableAllChipGroups(){
        enableChipGroup(binding.sizesChipGroupProductDetails)
        enableChipGroup(binding.colorsChipGroupProductDetails)
    }

    private fun enableChipGroup(chipGroup: ChipGroup){
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            chip.isEnabled = true
        }
    }

    private fun disableChipGroup(chipGroup: ChipGroup){
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            chip.isEnabled = false
        }
    }

    private fun hideLoadingGroup(){
        binding.shimmerProductDetails.stopShimmer()
        binding.loadingProductDetails.visibility = View.GONE
    }

    private fun showLoadingGroup(){
        binding.shimmerProductDetails.startShimmer()
        binding.loadingProductDetails.visibility = View.VISIBLE
    }

    private fun startAutoSwipe() {
        /*handler.removeCallbacks(animationRunnable)
        handler.postDelayed(animationRunnable, swipeInterval)*/
    }

    companion object {
        private const val TAG = "ProductDetailsFragment"
    }
}