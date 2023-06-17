package com.ph03nix_x.capacityinfo.interfaces

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.queryPurchaseHistory
import com.ph03nix_x.capacityinfo.PREMIUM_ID
import com.ph03nix_x.capacityinfo.R
import com.ph03nix_x.capacityinfo.activities.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Ph03niX-X on 04.12.2021
 * Ph03niX-X@outlook.com
 */

@SuppressLint("StaticFieldLeak")
interface PremiumInterface: PurchasesUpdatedListener {

    companion object {

        private var mProductDetailsList: List<ProductDetails>? = null

        var premiumContext: Context? = null
        var premiumActivity: Activity? = null
        var billingClient: BillingClient? = null

        var isPremium = false
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {

        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                CoroutineScope(Dispatchers.Default).launch {
                    handlePurchase(purchase)
                }
            }
        } else if (billingResult.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED) {
            isPremium = true
            MainActivity.instance?.toolbar?.menu?.findItem(R.id.premium)?.isVisible = false
            MainActivity.instance?.toolbar?.menu?.findItem(R.id.history_premium)?.isVisible = false
            MainActivity.instance?.toolbar?.menu?.findItem(R.id.clear_history)?.isVisible = true
        }
    }

    fun initiateBilling(isPurchasePremium: Boolean = false) {

        billingClient = BillingClient.newBuilder(premiumContext!!)
            .setListener(purchasesUpdatedListener()).enablePendingPurchases().build()

        if (billingClient?.connectionState == BillingClient.ConnectionState.DISCONNECTED)
            startConnection(isPurchasePremium)

    }

    private fun startConnection(isPurchasePremium: Boolean) {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    if(isPurchasePremium) purchasePremium()
                    else {
                        querySkuDetails()
                        checkPremium()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun purchasesUpdatedListener() = PurchasesUpdatedListener { _, purchases ->
        if (purchases != null) {
            for (purchase in purchases) {
                CoroutineScope(Dispatchers.Default).launch {
                    handlePurchase(purchase)
                }
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                withContext(Dispatchers.IO) {
                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams.build()) {
                        if (it.responseCode == BillingResponseCode.OK) {
                            isPremium = true
                            Toast.makeText(
                                premiumContext, R.string.premium_features_unlocked,
                                Toast.LENGTH_LONG
                            ).show()
                            MainActivity.instance?.toolbar?.menu?.findItem(R.id.premium)
                                ?.isVisible = false
                            MainActivity.instance?.toolbar?.menu?.findItem(R.id.history_premium)
                                ?.isVisible =
                                false
                            MainActivity.instance?.toolbar?.menu?.findItem(R.id.clear_history)
                                ?.isVisible = true
                        }
                    }
                }
            } else {
                isPremium = true
                Toast.makeText(premiumContext, R.string.premium_features_unlocked,
                    Toast.LENGTH_LONG).show()
                MainActivity.instance?.toolbar?.menu?.findItem(R.id.premium)?.isVisible = false
                MainActivity.instance?.toolbar?.menu?.findItem(R.id.history_premium)?.isVisible =
                    false
                MainActivity.instance?.toolbar?.menu?.findItem(R.id.clear_history)?.isVisible = true
            }

        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            isPremium = true
            Toast.makeText(premiumContext, R.string.premium_features_unlocked, Toast.LENGTH_LONG)
                .show()
            MainActivity.instance?.toolbar?.menu?.findItem(R.id.premium)?.isVisible = false
            MainActivity.instance?.toolbar?.menu?.findItem(R.id.history_premium)?.isVisible = false
            MainActivity.instance?.toolbar?.menu?.findItem(R.id.clear_history)?.isVisible = true
        }
    }

    private fun querySkuDetails() {
        val productList = mutableListOf(QueryProductDetailsParams.Product.newBuilder().apply {
            setProductId(PREMIUM_ID)
            setProductType(ProductType.INAPP)
        }.build())

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(productList).build()

        billingClient?.queryProductDetailsAsync(queryProductDetailsParams) { billingResult,
                                                                             productDetailsList ->

            if (billingResult.responseCode == BillingResponseCode.OK)
                mProductDetailsList = productDetailsList
        }
    }

    fun purchasePremium() {

        if(!mProductDetailsList.isNullOrEmpty()) {
            val productDetailsParamsList = listOf(BillingFlowParams.ProductDetailsParams
                .newBuilder().setProductDetails(mProductDetailsList!![0]).build())

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            billingClient?.launchBillingFlow(premiumActivity!!, billingFlowParams)
        }
    }

    fun getOrderId(): String? {
//        if(premiumContext != null && BillingProcessor.isIabServiceAvailable(premiumContext))
//            billingProcessor = BillingProcessor.newBillingProcessor(premiumContext,
//                GOOGLE_PLAY_LICENSE_KEY, this)
//        if(billingProcessor?.isInitialized != true) billingProcessor?.initialize()
//        if(isPremium()) return billingProcessor?.getPurchaseInfo(premium.test)?.purchaseData?.orderId
        return null
    }

    fun checkPremium() {

        val params = QueryPurchaseHistoryParams.newBuilder()
            .setProductType(ProductType.INAPP)

        CoroutineScope(Dispatchers.Main).launch {

            val purchaseHistoryResult = billingClient?.queryPurchaseHistory(params.build())

            val purchaseHistoryRecordList = purchaseHistoryResult?.purchaseHistoryRecordList

            isPremium = !purchaseHistoryRecordList.isNullOrEmpty()

            MainActivity.instance?.toolbar?.menu?.findItem(R.id.premium)?.isVisible = !isPremium
            MainActivity.instance?.toolbar?.menu?.findItem(R.id.history_premium)?.isVisible =
                !isPremium
            MainActivity.instance?.toolbar?.menu?.findItem(R.id.clear_history)?.isVisible =
                isPremium
        }
    }
}