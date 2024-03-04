package com.hotellina.SouvenirScout.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FormProdValidatorViewModel : ViewModel(){
    val TAG : String = "FormProdValidatorViewModel"
    var counter_imagesuccess : Int = 0

    private val _form_locked : MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    var form_locked: LiveData<Boolean> = _form_locked

    private val _productName : MutableLiveData<String> = MutableLiveData<String>("")
    var productName: LiveData<String> = _productName

    private val _productDescr : MutableLiveData<String> = MutableLiveData<String>("")
    var productDescr: LiveData<String> = _productDescr

    private val _imageSuccess : MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    var imageSuccess: LiveData<Boolean> = _imageSuccess

    private val _productWeight : MutableLiveData<String> = MutableLiveData<String>("")
    var productWeight: LiveData<String> = _productWeight

    private val _productWeightMeasure : MutableLiveData<String> = MutableLiveData<String>("")
    var productWeightMeasure: LiveData<String> = _productWeightMeasure

    private val _productLength : MutableLiveData<String> = MutableLiveData<String>("")
    var productLength: LiveData<String> = _productLength

    private val _productWidth : MutableLiveData<String> = MutableLiveData<String>("")
    var productWidth: LiveData<String> = _productWidth

    private val _productHeight : MutableLiveData<String> = MutableLiveData<String>("")
    var productHeight: LiveData<String> = _productHeight

    private val _productimMeasure : MutableLiveData<String> = MutableLiveData<String>("")
    var productDimMeasure: LiveData<String> = _productimMeasure

    private val _productPrice : MutableLiveData<String> = MutableLiveData<String>("")
    var productPrice: LiveData<String> = _productPrice


    fun setForm_locked(bool: Boolean){
        _form_locked.value = bool
        Log.d(TAG, "++setForm_locked($bool)")
    }

    fun setproductPrice(str: String){
        _productPrice.value = str
        Log.d(TAG, "++setproductPrice($str)")
    }

    // Measure system
    fun setproductDimMeasure(str: String){
        _productimMeasure.value = str
        Log.d(TAG, "++setproductDimMeasure($str)")
    }
    // Measure system
    fun setproductWeightMeasure(str: String){
        _productWeightMeasure.value = str
        Log.d(TAG, "++setproductWeightMeasure($str)")
    }

    fun setproductHeight(str: String){
        _productHeight.value = str
        Log.d(TAG, "++setproductHeight($str)")
    }

    fun setproductWidth(str: String){
        _productWidth.value = str
        Log.d(TAG, "++setproductWidth($str)")
    }

    fun setproductLength(str: String){
        _productLength.value = str
        Log.d(TAG, "++setproductLength($str)")
    }

    fun setproductWeight(str: String){
        _productWeight.value = str
        Log.d(TAG, "++setproductWeight($str)")
    }

    fun setProductName(str: String){
        _productName.value = str
        //Log.d(TAG, "++setProductName($newProdName)")
    }

    fun setProductDescr(str: String){
        _productDescr.value = str
        //Log.d(TAG, "++setProductDescr($newProdDescr)")
    }

    fun setImageSuccessNewProd(){
        counter_imagesuccess++
        if (counter_imagesuccess >= 2)  _imageSuccess.value = true
        Log.d(TAG, "++setImageSuccess($counter_imagesuccess)")
    }

    fun setImageSuccess(){
         _imageSuccess.value = true
        Log.d(TAG, "++setImageSuccess()")
    }



    // we use apply below because it returns an instance of MediatorLiveData<Boolean>
    private val _valid : MediatorLiveData<Boolean> = MediatorLiveData<Boolean>().apply {
                                                            addSource(productName ,  {
                                                                val valid_internally = isFormValid(it, productDescr.value, imageSuccess.value!!)
                                                                value = valid_internally
                                                                //Log.d(TAG, it+ " "+ valid_internally.toString())
                                                            })
                                                            addSource(productDescr ,  {
                                                                val valid_internally = isFormValid(productName.value,it, imageSuccess.value!!)
                                                                value = valid_internally
                                                               // Log.d(TAG, it+ " "+ valid_internally.toString())
                                                            })
                                                            addSource(imageSuccess ,  {
                                                                val valid_internally = isFormValid(productName.value,productDescr.value!!,it)
                                                                value = valid_internally
                                                                //Log.d(TAG,   "$it "+ valid_internally.toString())
                                                            }) }

    var valid : LiveData<Boolean> = _valid

    fun isFormValid(productName: String?, productDescr: String?, imageSuccess: Boolean ): Boolean {
        val productNameCheck = (productName?.length!! >= 2) and ( productName?.length!! < 15)
        val productDescrCheck = (productDescr?.length!! > 15)
        val imageCheck = imageSuccess
        Log.d(TAG, "productNameCheck=$productNameCheck--productDescrCheck=$productDescrCheck--imageCheck=$imageCheck")

        return ( productNameCheck and
                productDescrCheck and
                imageCheck  )
    }



} // main class