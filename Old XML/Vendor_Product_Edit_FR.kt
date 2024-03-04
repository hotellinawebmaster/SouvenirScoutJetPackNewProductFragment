package com.hotellina.SouvenirScout.administration

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hotellina.SouvenirScout.MainActivity
import com.hotellina.SouvenirScout.R
import com.hotellina.SouvenirScout.databinding.FragmentVendorProductEditBinding
import com.hotellina.SouvenirScout.dialogs.DeleteProductDialogFragment
import com.hotellina.SouvenirScout.entities.CurrentOperation
import com.hotellina.SouvenirScout.entities.EntityMeasure
import com.hotellina.SouvenirScout.entities.LxWxH
import com.hotellina.SouvenirScout.entities.Products
import com.hotellina.SouvenirScout.entities.Settings
import com.hotellina.SouvenirScout.entities.Vendors
import com.hotellina.SouvenirScout.viewmodels.SouvenirViewModel
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.text.DecimalFormat
import java.util.Base64


// Tutorial by Google
// https://developer.android.com/guide/fragments/appbar#fragment-inflate

class Vendor_Product_Edit_FR : Fragment() {
    // souvenirViewModel.vendor_auth_products and souvenirViewModel.vendor_profile_for_login
    // are the structures in the viewmodel to watch out for

    private lateinit var binding:  FragmentVendorProductEditBinding
    private val souvenirViewModel: SouvenirViewModel by activityViewModels()
    lateinit var activity: MainActivity

    val TAG : String = "*Vendor_Product_Edit_FR"
    var productPositionInList : Int? = null
    var product_list : ArrayList<Products>? = null
    var product : Products? = null
    var original_product : Products? = null
    var spinnerWatcher : SpinnerWatcher = SpinnerWatcher()
    var textWatcher : TextWatcher = TextWatcher()
    var editedByUser : Boolean = false
    var imageListener : ProductImageListener = ProductImageListener()
    var is_this_a_new_product : Boolean? = null
    lateinit var vendorProfileCurrentlyIn : Vendors

    lateinit var activityResultLauncher : ActivityResultLauncher<Intent>
    private var encodedBtmp: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Log.d(TAG,"+onCreateView")

        CurrentOperation.name =  "Vendor_Product_Edit_FR"
        Log.d(TAG, "CurrentOperation.name=${CurrentOperation.name}")

        binding = FragmentVendorProductEditBinding.inflate(inflater, container, false)
        val view = binding.root
        activity = (getActivity() as MainActivity)
        // sets the fragmentTag
        // values initialization
        productPositionInList = requireArguments()!!.getInt("productPositionInList")
        product_list = souvenirViewModel.vendor_auth_products.value
        Log.d(TAG, "+onCreateView productPositionInList=$productPositionInList ")
        Log.d(TAG, "+onCreateView product_list=$product_list size=${product_list?.size} ")

        is_this_a_new_product = productPositionInList == -1 // boolean condition for a new product

        if (productPositionInList == -1 ){ // means it is a new product
            Log.d(TAG, "+onCreateView new product")
            initViewsNewProduct()
            initListenersNewProduct()
        }else { //means we are editing an existing product
            product = product_list!![productPositionInList!!]
            Log.d(TAG, "+onCreateView productPositionInList=$productPositionInList")
            Log.d(TAG, "+onCreateView" + product.toString())
            original_product = product
            initViews()
            initListeners()
        }

        // for image upload
        binding.productImageEditXml.setOnClickListener(imageListener)
        activityResultLauncher  = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(), ActivityResultCallback<ActivityResult>(){
                // it = ActivityResult
                if (it.resultCode == Activity.RESULT_OK){
                    val data_intent = it.data
                    val uri : Uri = data_intent!!.data!!
                    try {
                        val orignal_bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri)
                        Log.d(TAG, "Image processing original_width=${orignal_bitmap.width} + original_height=${orignal_bitmap.height}")

                        val cropped_bitmap = cropCenter(orignal_bitmap)!!
                        Log.d(TAG, "Image processing cropped_width=${cropped_bitmap.width} + cropped_height=${cropped_bitmap.height}")

                        val scaled_bitmap : Bitmap= cropped_bitmap.scale(150,150,true)
                        Log.d(TAG, "Image processing scaled_width=${scaled_bitmap.width} + scaled_height=${scaled_bitmap.height}")

                        setImageInGUI(scaled_bitmap)
                        encodedBtmp  = encodeToBase64(scaled_bitmap, Bitmap.CompressFormat.JPEG)
                        if (checkFormData()) saveOrDiscardBtnsEnabled(true)
                        else saveOrDiscardBtnsEnabled(false)
                        //encodedBtmpSMALL = encodeToBase64(smallImage, Bitmap.CompressFormat.JPEG)
                        editedByUser = true
                        Log.d(TAG, "URI image = $uri")
                    } catch (e: FileNotFoundException) {
                        Log.d(TAG, "Image not found on the phone $e")
                    }
                    catch (e: IOException){ Log.d(TAG, "IO error for the image $e ")}
                } else{
                    Log.d(TAG, "Image intent not ok")}
            }
        )
        return view
    }

    inner class ProductImageListener :  View.OnClickListener {
        override fun onClick(v: View?) {
            val upload_image_intent : Intent =Intent()
            upload_image_intent.setType("image/*")
            upload_image_intent.setAction(Intent.ACTION_PICK)
            upload_image_intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(upload_image_intent)
        }
    }


    fun cropCenter(bmp: Bitmap): Bitmap? {
        val dimension = Math.min(bmp.width, bmp.height)
        return ThumbnailUtils.extractThumbnail(bmp, dimension, dimension)
    }

    fun showToast(s: String, lengthShort: Int) {
        // Toast.LENGTH_SHORT
        val text = s
        val duration = lengthShort
        val toast = Toast.makeText(requireContext(), text, duration)
        //toast.view
        toast.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initViewsNewProduct() {
        Log.d(TAG, "+initViews")
        encodedBtmp = null

        vendorProfileCurrentlyIn = souvenirViewModel.vendor_profile_for_login.value!!
        binding.productEditContextualToolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
        binding.productTitle.isEnabled = true
        binding.productDescriptionXml.isEnabled = true
        binding.productDescriptionXmlTextInputEditText.imeOptions=EditorInfo.IME_ACTION_DONE
        binding.productDescriptionXmlTextInputEditText.setRawInputType(InputType.TYPE_CLASS_TEXT);

        binding.price.isEnabled = true
        binding.weight.isEnabled = true
        binding.lValues.isEnabled = true
        //binding.contextualToolbar.menu.findItem(R.id.product_edit_action_remove).setEnabled(true);

        binding.productTitle.editText?.setText("")
        binding.productDescriptionXml.editText?.setText("")
        binding.price.editText?.setText("")
        binding.weight.editText?.setText( "")

        val dim = LxWxHfromDBtoSeparateObjects("0x0x0")
        binding.lValues.editText?.setText("")
        binding.wValues.editText?.setText("")
        binding.hValues.editText?.setText("")

        // Spinner
        val weight_adapter =
            ArrayAdapter.createFromResource( this.requireContext(),metricOrImperial(
                EntityMeasure.weight,0.intToBoolean()), android.R.layout.simple_spinner_item)

        val dimensions_adapter =  ArrayAdapter.createFromResource( this.requireContext(),metricOrImperial(
            EntityMeasure.dimensions, 0.intToBoolean()), android.R.layout.simple_spinner_item)

        weight_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dimensions_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerWeight.adapter     = weight_adapter
        binding.spinnerDimensions.adapter = dimensions_adapter
        binding.spinnerWeight.setSelection(0,false)
        binding.spinnerDimensions.setSelection(0,false)
        val curr = vendorProfileCurrentlyIn.currency!!
        binding.currencyUnit.text =  getResources().getStringArray(R.array.currency)[curr]

        // image
        binding.productImageEditXml.setImageResource(R.drawable.noimage150x150)

        // toolbar, set buttons to disabled
        saveOrDiscardBtnsEnabled(false)
        removeBtnEnabledAndVisible(false,false)
        editedByUser = false
    }

    private fun removeBtnEnabledAndVisible(enabled: Boolean, visible:Boolean) {
        if (enabled) {
            binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_remove).isEnabled = true
            binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_remove).icon!!.alpha= 255
        }
        else {
            binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_remove).isEnabled = false
            binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_remove).icon!!.alpha= 100
        }

        if (visible) binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_remove).isVisible = true
        else binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_remove).isVisible = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initViews() {
        val decimalFormat = DecimalFormat("0.#####")

        Log.d(TAG, "+initViews")
        val vendorProfileCurrentlyIn = souvenirViewModel.vendor_profile_for_login.value!!
        binding.productEditContextualToolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
        binding.productTitle.isEnabled = true
        binding.productDescriptionXml.isEnabled = true
        binding.productDescriptionXmlTextInputEditText.imeOptions=EditorInfo.IME_ACTION_DONE
        binding.productDescriptionXmlTextInputEditText.setRawInputType(InputType.TYPE_CLASS_TEXT);

        binding.price.isEnabled   = true
        binding.weight.isEnabled  = true
        binding.lValues.isEnabled = true

        binding.productTitle.editText?.setText( product!!.product_name )
        binding.productDescriptionXml.editText?.setText( product!!.description )

        if (product!!.price != null ) binding.price.editText?.setText( decimalFormat.format(java.lang.Double.valueOf(product!!.price!!)))
        else binding.price.editText?.setText("")

        if (product!!.weight != null ) binding.weight.editText?.setText( product!!.weight!!.toString() )
        else binding.price.editText?.setText("")

        val dim = LxWxHfromDBtoSeparateObjects(product!!.dimensions!!)
        binding.lValues.editText?.setText(dim.lenght )
        binding.wValues.editText?.setText(dim.width )
        binding.hValues.editText?.setText(dim.height )

        // Spinner
        val weight_adapter =
            ArrayAdapter.createFromResource( this.requireContext(),metricOrImperial(
                EntityMeasure.weight,souvenirViewModel.vendor_profile_for_login.value!!.store_measure_system!!.intToBoolean()), android.R.layout.simple_spinner_item)

        val dimensions_adapter =  ArrayAdapter.createFromResource( this.requireContext(),metricOrImperial(
            EntityMeasure.dimensions, souvenirViewModel.vendor_profile_for_login.value!!.store_measure_system!!.intToBoolean()), android.R.layout.simple_spinner_item)

        weight_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dimensions_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerWeight.adapter     = weight_adapter
        binding.spinnerDimensions.adapter = dimensions_adapter
        binding.spinnerWeight.setSelection(product!!.weight_unit!!.toInt(),false)
        binding.spinnerDimensions.setSelection(product!!.dimensions_unit!!.toInt(),false)
        val curr = vendorProfileCurrentlyIn.currency!!
        binding.currencyUnit.text =  getResources().getStringArray(R.array.currency)[curr]

        // image
       // val image_final_url = Settings.PATHS.PRODUCT_IMAGES_BASE_URL +  product!!.id + ".jpg"
    //    Picasso.get().load(image_final_url).memoryPolicy(MemoryPolicy.NO_CACHE).into(binding.productImageEditXml);

        val image_final_url = Settings.IMAGES_BASE_URL + vendorProfileCurrentlyIn.id + "/" +
                "products" +  "/" + product!!.id  + ".jpg"

        Glide.with(this).
            load(image_final_url).
            diskCacheStrategy(DiskCacheStrategy.NONE).
            skipMemoryCache(true).
            into(binding.productImageEditXml);

        // toolbar, set buttons to disabled
        saveOrDiscardBtnsEnabled(false)
        removeBtnEnabledAndVisible(true,true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initListeners() {
        binding.productTitle.editText?.addTextChangedListener(textWatcher)
        binding.productDescriptionXml.editText?.addTextChangedListener(textWatcher)
        binding.price.editText?.addTextChangedListener(textWatcher)
        binding.weight.editText?.addTextChangedListener(textWatcher)
        binding.lValues.editText?.addTextChangedListener(textWatcher)
        binding.hValues.editText?.addTextChangedListener(textWatcher)
        binding.wValues.editText?.addTextChangedListener(textWatcher)
        binding.spinnerDimensions.setOnItemSelectedListener(spinnerWatcher)
        binding.spinnerWeight.setOnItemSelectedListener(spinnerWatcher)
        binding.productEditContextualToolbar.setNavigationOnClickListener{ view ->
            back_actions()
        }
        binding.productEditContextualToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.product_edit_discard -> {
                    Log.d(TAG, "R.id.product_edit_discard_product_save")
                    removeListeners()
                    if ( is_this_a_new_product!! ) initViewsNewProduct()
                    else initViews()
                    clearFocusAndHideKeyboard()
                    editedByUser = false
                    initListeners()
                    true
                }
                R.id.product_edit_save -> {
                    Log.d(TAG, "R.id.product_edit_action_save")
                    clearFocusAndHideKeyboard()
                    saveOrDiscardBtnsEnabled(false)
                    val product_modified = getProductFromForm(original_product!!)
                    Log.d(TAG, "product_modified=${product_modified.toString()}")
                    souvenirViewModel.try_update_product(product_modified!!,productPositionInList!!)
                    souvenirViewModel.product_updated_response.observe(viewLifecycleOwner,product_updated_response_OBSERVER)
                    true
                }
                R.id.product_edit_remove -> {
                    Log.d(TAG, "R.id.product_edit_action_remove product")
                    DeleteProductDialogFragment().show( parentFragmentManager,"Delete_product_Dialog")
                    true
                }
                else -> false
            }
        }
        backButtonPressed()
    }

    fun back_actions(){
        Log.d(TAG, "Going back.....")
        original_product=null
        activity.remove_vendor_edit_product_fragment()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initListenersNewProduct() {
        binding.productTitle.editText?.addTextChangedListener(textWatcher)
        binding.productDescriptionXml.editText?.addTextChangedListener(textWatcher)
        binding.price.editText?.addTextChangedListener(textWatcher)
        binding.weight.editText?.addTextChangedListener(textWatcher)
        binding.lValues.editText?.addTextChangedListener(textWatcher)
        binding.hValues.editText?.addTextChangedListener(textWatcher)
        binding.wValues.editText?.addTextChangedListener(textWatcher)
        binding.spinnerDimensions.setOnItemSelectedListener(spinnerWatcher)
        binding.spinnerWeight.setOnItemSelectedListener(spinnerWatcher)
        binding.productEditContextualToolbar.setNavigationOnClickListener { view ->
            back_actions()
        }
        binding.productEditContextualToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.product_edit_discard -> {
                    Log.d(TAG, "R.id.product_edit_discard_product_save")
                    removeListeners()
                    initViewsNewProduct()
                    clearFocusAndHideKeyboard()
                    editedByUser = false
                    initListenersNewProduct()
                    true
                }
                R.id.product_edit_save -> {
                    Log.d(TAG, "R.id.product_edit_action_save")
                    val vendor_id = souvenirViewModel.vendor_profile_for_login.value!!.id
                    val new_product = getProductFromForm(Products(vendor_id = vendor_id ))
                    Log.d(TAG, "product_modified=${new_product.toString()}")
                    souvenirViewModel.try_create_product(new_product!!)
                    souvenirViewModel.product_created_response.observe(viewLifecycleOwner) {
                        if (it)  {
                            //product_list = souvenirViewModel.vendor_auth_products.value!!
                            //Log.d(TAG, "product_list=$product_list")
                            // productPositionInList = product_list!!.size -1
                            // original_product = product_list!![product_list!!.size]
                            saveOrDiscardBtnsEnabled(false)
                            removeListeners()
                            freezeForm()
                            showToast("Product created", Toast.LENGTH_SHORT)
                        }
                    }
                    true
                }
                R.id.product_edit_remove -> {
                    Log.d(TAG, "R.id.product_edit_action_remove product")
//                    souvenirViewModel.try_delete_product(product_modified!!,productPositionInList!!)
//                    souvenirViewModel.product_deleted_response.observe(viewLifecycleOwner) {
//                        if (it) {
//                            Log.d(TAG,"+onCreateView A product was just removed")
//                            binding.productTitle.isEnabled = false
//                            binding.productDescriptionXml.isEnabled = false
//                            binding.price.isEnabled = false
//                            binding.weight.isEnabled = false
//                            binding.lValues.isEnabled = false
//                            binding.contextualToolbar.menu.findItem(R.id.product_edit_action_remove).setEnabled(false);
//                        }
//                    }
                    false
                }
                else -> false
            }
        }
        backButtonPressed()
    }

    private fun backButtonPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Whatever you want
                // when back pressed
                Log.d(TAG, "Back buttonn pressed in fragment")
                back_actions()
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    val product_updated_response_OBSERVER : Observer<Boolean> = Observer<Boolean>{
        if (it != null) {
            val wasMenuEnabled = binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_save).isEnabled
            Log.d(TAG, "OBSERVED product_updated_response=$it")
            // if ((it) && (wasMenuEnabled)) {
            if ((it)) {
                editedByUser = false
                showToast("Product modified", Toast.LENGTH_SHORT)
                souvenirViewModel.product_updated_response.removeObservers(viewLifecycleOwner)
                souvenirViewModel.product_updated_response_RESET()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun confirmDeleteFromDialog() {
        souvenirViewModel.try_delete_product(original_product!!,productPositionInList!!)
        souvenirViewModel.product_deleted_response.observe(viewLifecycleOwner) {
            if (it) {
                Log.d(TAG,"+onCreateView A product was just removed")
                freezeForm()
                removeBtnEnabledAndVisible(false,true)
                showToast("Product deleted", Toast.LENGTH_SHORT)
            }
        }
    }

    fun cancelDeleteFromDialog() {
        Log.d(TAG, "no action on cancel delete")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun encodeToBase64(image: Bitmap, compressFormat: Bitmap.CompressFormat?, quality: Int=100): String? {
        val byteArrayOS = ByteArrayOutputStream()
        image.compress(compressFormat!!, quality, byteArrayOS)
        return  Base64.getEncoder().encodeToString(byteArrayOS.toByteArray())
    }

    fun setImageInGUI(bmp: Bitmap){
        binding.productImageEditXml.alpha = 1f
        binding.productImageEditXml.setImageBitmap(bmp)
    }

    private fun getProductFromForm(tempProd: Products): Products {
        with(binding) {
            with(tempProd) {
                product_name = productTitle.editText?.text.toString()
                description = productDescriptionXml.editText?.text.toString()
                price = if (binding.price.editText?.text?.toString() != "")
                    binding.price.editText?.text.toString().toDouble() else null

                weight = if (binding.weight.editText?.text.toString() != "")
                    binding.weight.editText?.text.toString().toInt() else null
                dimensions =
                    "${lValues.editText?.text.toString()}x${wValues.editText?.text.toString()}x${hValues.editText?.text.toString()}"
                weight_unit = spinnerWeight.selectedItemPosition
                dimensions_unit = spinnerDimensions.selectedItemPosition
                product_picture_BASE64= encodedBtmp
            }
        }
        return tempProd
    }

    private fun clearFocusAndHideKeyboard() {
        // hide the keyboard
        val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(requireView().windowToken, 0)
        // clear focus
        binding.root.clearFocus()
    }

    fun LxWxHfromDBtoSeparateObjects(str:String): LxWxH {
        val fistX = str.indexOf("x")
        val lastX = str.lastIndexOf("x")
        val length = str.substring(0,fistX)
        val width =  str.substring(fistX +1, lastX)
        val height = str.substring(lastX + 1, str.length)
        return LxWxH(length,width,height)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun metricOrImperial(m : EntityMeasure, impOrMetric: Boolean):Int{
        //val impOrMetric = souvenirViewModel.vendor_profile_for_login.value!!.store_measure_system!!.intToBoolean()
        //Log.d(TAG,"+mOrI impOrMetric=$impOrMetric")
        if (m == EntityMeasure.weight )
            if ( impOrMetric )  return R.array.weight_imperial
            else                return R.array.weight_metric
        if (m == EntityMeasure.dimensions )
            if (impOrMetric)   return R.array.dimension_imperial
            else               return R.array.dimension_metric
        return 0
    }

    private fun Int.intToBoolean(): Boolean {
        if (this == 1)   return true
        else  return false
    }

    private fun Boolean.booleanToInt(): Int {
        if (this)  return 1
        else return 0
    }

    fun freezeForm(){
        with(binding){
            productTitleTextInputEditText.isEnabled = false
            productDescriptionXmlTextInputEditText.isEnabled = false
            weightTextInputEditText.isEnabled = false
            lValuesTextInputEditText.isEnabled = false
            wValuesTextInputEditText.isEnabled = false
            hValuesTextInputEditText.isEnabled = false
            priceTextInputEditText.isEnabled = false
            spinnerDimensions.isEnabled = false
            spinnerWeight.isEnabled = false
            productImageEditXml.alpha = 0.30f
            binding.productImageEditXml.setOnClickListener(null)
        }
    }

    private fun removeListeners(){
        //texts
        binding.productTitle.editText?.removeTextChangedListener(textWatcher)
        binding.productDescriptionXml.editText?.removeTextChangedListener(textWatcher)
        binding.price.editText?.removeTextChangedListener(textWatcher)
        binding.weight.editText?.removeTextChangedListener(textWatcher)
        // dimensions
        binding.lValues.editText?.removeTextChangedListener(textWatcher)
        binding.wValues.editText?.removeTextChangedListener(textWatcher)
        binding.hValues.editText?.removeTextChangedListener(textWatcher)

        // spinners
        binding.spinnerDimensions.setOnItemSelectedListener(null)
        binding.spinnerWeight.setOnItemSelectedListener(null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveOrDiscardBtnsEnabled(b:Boolean):Unit{
        Log.d(TAG,"+saveOrDiscardDialogEnabled(b=$b)++++ BEFORE")
//        Log.d(TAG, "--------BEFORE------" )
//        Log.d(TAG, "DISCARD enabled=" + binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_discard).isEnabled)
//        Log.d(TAG, "SAVE enabled=" +binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_save).isEnabled )
        if (b) {
            binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_save).isEnabled = true
            binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_save).icon!!.alpha=255
            binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_discard).isEnabled = true
            binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_discard).icon!!.alpha=255
        }
        else {
            binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_save).isEnabled = false
            binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_save).icon!!.alpha=100
            binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_discard).isEnabled = false
            binding.productEditContextualToolbar.menu.findItem(R.id.product_edit_discard).icon!!.alpha=100
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG,"+onDestroyView CurrentOperation.name=${CurrentOperation.name}")
    }

    inner class SpinnerWatcher : AdapterView.OnItemSelectedListener {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long,
        ) {
            val item = parent!!.getItemAtPosition(position)
            Log.d(TAG,"view=${(view!!.parent as View).tag} + item=$item")
            if (!editedByUser) {
                editedByUser = true
                saveOrDiscardBtnsEnabled(true)
            }
        }
        override fun onNothingSelected(parent: AdapterView<*>?) { }
    }

    inner class TextWatcher : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int)  {}
        @RequiresApi(Build.VERSION_CODES.O)
        override fun afterTextChanged(s: Editable?) {
            Log.d(TAG,"s=$s")
            editedByUser = true
            if (checkFormData()) saveOrDiscardBtnsEnabled(true)
            else saveOrDiscardBtnsEnabled(false)
        }
    }

    private fun checkFormData():Boolean {
        Log.d(TAG,"+checkFormData  binding.productTitle.editText?.text.toString()=${binding.productTitle.editText?.text.toString()}")
         if ( binding.productTitle.editText?.text.toString() != "" &&
             binding.productDescriptionXml.editText?.text.toString() != "" &&
             binding.productImageEditXml.drawable.constantState != requireContext().getResources().getDrawable(
                 R.drawable.noimage150x150
             ).constantState
        ) return true
        else return false
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG,"+onPause")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG,"+onResume  CurrentOperation=${CurrentOperation.name}")
    }

}

