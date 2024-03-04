package com.hotellina.SouvenirScout.administration

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.hotellina.SouvenirScout.MainActivity
import com.hotellina.SouvenirScout.R

import com.hotellina.SouvenirScout.dialogs.DeleteProductDialogFragment
import com.hotellina.SouvenirScout.entities.CurrentOperation
import com.hotellina.SouvenirScout.entities.EntMeasure
import com.hotellina.SouvenirScout.entities.LxWxH
import com.hotellina.SouvenirScout.entities.Products
import com.hotellina.SouvenirScout.entities.Settings
import com.hotellina.SouvenirScout.entities.Vendors
import com.hotellina.SouvenirScout.viewmodels.FormProdValidatorViewModel
import com.hotellina.SouvenirScout.viewmodels.SouvenirViewModel
import java.io.ByteArrayOutputStream
import java.util.Base64

// Tutorials
//
// https://developer.android.com/guide/fragments/appbar#fragment-inflate

// Tutorial on country color picker
// https://www.droidcon.com/2022/03/01/the-big-form-with-jetpack-compose/
// https://gist.github.com/hkawii/453f905474acade1bcc1993b92ca914e

// For how to handle access to a state in a composable Listen
// to https://www.youtube.com/watch?v=3oXBnM6fZj0

// On Form Validation
// https://gist.github.com/weverb2/f92e276c55c24c8f63214cf736aac911

class Vendor_Product_Edit_FR : Fragment() {

    val TAG : String = "*Vendor_Product_Edit_FR"
    var productPositionInList : Int? = null
    var product_list : ArrayList<Products>? = null
    var product : Products? = null
    var original_product : Products? = null
    var editedByUser : Boolean = false
    private val souvenirViewModel: SouvenirViewModel by activityViewModels()
    lateinit var activity: MainActivity
    lateinit var vendorProfileCurrentlyIn : Vendors
    private var encodedBtmp: String? = null
    private val formProdValidatorViewModel: FormProdValidatorViewModel by viewModels()
    var currencyValue : String? = null

    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Log.d(TAG,"+onCreateView")
        CurrentOperation.name =  "Vendor_Product_Edit_FR"
        Log.d(TAG, "CurrentOperation.name=${CurrentOperation.name}")

        activity = (getActivity() as MainActivity)
        productPositionInList = requireArguments()!!.getInt("productPositionInList")
        product_list = souvenirViewModel.vendor_auth_products.value
        Log.d(TAG, "+onCreateView productPositionInList=$productPositionInList ")
        Log.d(TAG, "+onCreateView product_list=$product_list size=${product_list?.size}")

        vendorProfileCurrentlyIn = souvenirViewModel.vendor_profile_for_login.value!!
        val currencyIndex = vendorProfileCurrentlyIn.currency!!
        currencyValue =  getResources().getStringArray(R.array.currency)[currencyIndex]

        val view = ComposeView(requireContext())
        val newProduct:Boolean = (productPositionInList == -1)

        if ( newProduct ) {
            newProductInit(view)
        } else {
            //means we are editing an existing product
            editExistingProductInit(view)
        }
        return view
    }


    @OptIn(ExperimentalMaterial3Api::class)
    private fun editExistingProductInit(view : ComposeView?=null) {
        product = product_list!![productPositionInList!!]
        Log.d(TAG, "+onCreateView productPositionInList=$productPositionInList")
        Log.d(TAG, "+onCreateView" + product.toString())
        original_product = product

        val setProdNameFunction : (String) -> Unit = {formProdValidatorViewModel.setProductName(it)}
        val setProdDescrFunction : (String) -> Unit = {formProdValidatorViewModel.setProductDescr(it)}
        val setImageSuccessFunction : (AsyncImagePainter.State) -> Unit = { formProdValidatorViewModel.setImageSuccess() }

        val prodDim = product!!.dimensions!!
        val L =  LxWxHfromDBtoSeparateObjects(prodDim)?.lenght
        val W =  LxWxHfromDBtoSeparateObjects(prodDim)?.width
        val H =  LxWxHfromDBtoSeparateObjects(prodDim)?.height

        with (formProdValidatorViewModel){
            with(product!!) {
                setProductName(product_name!!)
                setProductDescr(description!!)
                setproductWeight(weight.toString())
                setproductWeightMeasure(weight_unit.toString())
                setproductLength(L.toString())
                setproductWidth(W.toString())
                setproductHeight(H.toString())
                setproductDimMeasure(dimensions_unit.toString())
                setproductPrice(price.toString())
            }
        }

        view?.apply{
            setContent {
                with (formProdValidatorViewModel) {
                    val prodNameState by productName.observeAsState()
                    val prodDescrState by productDescr.observeAsState()
                    val prodWeightState by productWeight.observeAsState()
                    val prodWeightMeasureState by productWeightMeasure.observeAsState()
                    val prodLengthState by productLength.observeAsState()
                    val prodWidthState by productWidth.observeAsState()
                    val prodHeightState by productHeight.observeAsState()
                    val prodDimMeasureState by productDimMeasure.observeAsState()
                    val prodPriceState by productPrice.observeAsState()
                    val isFormValidState by valid.observeAsState(false)

                    layoutModifyProduct(
                        prodName = prodNameState!!,
                        onProdNameChanged = setProdNameFunction,
                        prodDescr = prodDescrState ?: "",
                        onProdDescrChanged = setProdDescrFunction,
                        onImageSuccessChanged = setImageSuccessFunction,
                        prodWeight = prodWeightState ?: "",
                        prodWeightMeasure = prodWeightMeasureState!!,
                        prodLength = prodLengthState ?: "",
                        prodWidth = prodWidthState ?: "",
                        prodHeight = prodHeightState ?: "",
                        prodDimMeasure = prodDimMeasureState!!,
                        prodPrice = prodPriceState ?: "",
                        isFormValid = isFormValidState ?: false,
                    )
                }
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    private fun newProductInit(view : ComposeView? = null ) {
        Log.d(TAG, "+onCreateView new product")
        val setProdNameFunction : (String) -> Unit = {formProdValidatorViewModel.setProductName(it)}
        val setProdDescrFunction : (String) -> Unit = {formProdValidatorViewModel.setProductDescr(it)}
        val setImageSuccessNewProdFunction : (AsyncImagePainter.State) -> Unit = { formProdValidatorViewModel.setImageSuccessNewProd() }

        view?.apply{
            setContent {
                with (formProdValidatorViewModel) {
                    val prodNameState by productName.observeAsState()
                    val prodDescrState by productDescr.observeAsState()
                    val prodWeightState   by productWeight.observeAsState()
                    val prodWeightMeasureState  by productWeightMeasure.observeAsState("0")
                    val prodLengthState  by productLength.observeAsState()
                    val prodWidthState   by productWidth.observeAsState()
                    val prodHeightState  by productHeight.observeAsState()
                    val prodDimMeasureState   by productDimMeasure.observeAsState("0")
                    val prodPriceState  by productPrice.observeAsState()
                    val isFormValidState by valid.observeAsState()

                    layoutNewProduct(prodName = prodNameState  ?: "",
                        onProdNameChanged = setProdNameFunction ,
                        prodDescr = prodDescrState  ?: "",
                        onProdDescrChanged = setProdDescrFunction,
                        onImageSuccessChanged = setImageSuccessNewProdFunction,
                        prodWeight =prodWeightState  ?: "",
                        prodWeightMeasure =prodWeightMeasureState ,
                        prodLength =prodLengthState  ?: "",
                        prodWidth =prodWidthState  ?: "",
                        prodHeight = prodHeightState  ?: "",
                        prodDimMeasure =prodDimMeasureState ,
                        prodPrice =prodPriceState  ?: "",
                        isFormValid = isFormValidState ?: false  )
                }
            }
        }
    }

    @ExperimentalMaterial3Api
    @Preview(showBackground = false)
    @Composable
    fun layoutNewProduct(prodName :String="",
                         onProdNameChanged: (String) -> Unit = {},
                         prodDescr : String="",
                         onProdDescrChanged: (String) -> Unit = {},
                         onImageSuccessChanged: (AsyncImagePainter.State) -> Unit={},
                         prodWeight : String="",
                         prodWeightMeasure : String="",
                         prodLength : String="",
                         prodWidth : String="",
                         prodHeight : String="",
                         prodDimMeasure : String="",
                         prodPrice : String="",
                         isFormValid: Boolean = false,
                         ) {
        val locked by  formProdValidatorViewModel.form_locked.observeAsState()
        Scaffold(
            topBar = {
                TopAppBar(title = {Text("New Product")},
                    actions = {
                        IconButton(
                            onClick = {
                            Log.d(TAG, "Save button pressed")
                            val vendor_id = souvenirViewModel.vendor_profile_for_login.value!!.id
                             val new_product =  Products(vendor_id = vendor_id,
                                                        product_name = prodName,
                                                        description =  prodDescr,
                                                        price =  if (prodPrice.isEmpty()) null else prodPrice.toDouble(),
                                                        weight =  if (prodWeight.isEmpty()) null else prodWeight.toInt(),
                                                        dimensions ="${prodLength}x${prodWidth}x${prodHeight}",
                                                        weight_unit = if (prodWeightMeasure.isEmpty()) 0 else prodWeightMeasure.toInt(),
                                                        dimensions_unit = if (prodDimMeasure.isEmpty()) 0 else prodDimMeasure.toInt(),
                                                        product_picture_BASE64= encodedBtmp
                                                         )

                            Log.d(TAG, "new_product=${new_product}")
                            souvenirViewModel.try_create_product(new_product!!)
                            souvenirViewModel.product_created_response.observe(viewLifecycleOwner) {
                                if (it)  {
                                    showToast("Product created", Toast.LENGTH_SHORT)
                                    formProdValidatorViewModel.setForm_locked(true)
                                }
                            }
                                            },
                            enabled = isFormValid and !locked!!) {
                            Icon(
                                painterResource(id = R.drawable.baseline_done_24),
                                contentDescription = "Save"
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { back_actions() }) {
                            Icon(
                                painterResource(id = R.drawable.baseline_arrow_back_24),
                                contentDescription = "Remove the product"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            )
            {
                name_and_Picture(prodName,onProdNameChanged,onImageSuccessChanged)
                product_description(prodDescr,onProdDescrChanged)
                product_details()
            }
        }
    }


    @ExperimentalMaterial3Api
    //@Preview(showBackground = false)
    @Composable
    fun layoutModifyProduct(prodName :String="",
                         onProdNameChanged: (String) -> Unit = {},
                         prodDescr : String="",
                         onProdDescrChanged: (String) -> Unit = {},
                         onImageSuccessChanged: (AsyncImagePainter.State) -> Unit={},
                         prodWeight : String?,
                         prodWeightMeasure : String="",
                         prodLength : String="",
                         prodWidth : String="",
                         prodHeight : String="",
                         prodDimMeasure : String="",
                         prodPrice : String?="",
                         isFormValid: Boolean = false,
                            ) {
        val locked by  formProdValidatorViewModel.form_locked.observeAsState()

        Scaffold(
            topBar = {
                TopAppBar(title = {Text("Modify product")},
                    actions = {
                        IconButton(onClick = {
                                                Log.d(TAG, "Remove product")
                                                DeleteProductDialogFragment().show( parentFragmentManager,"Delete_product_Dialog")
                                             },
                                    enabled = !locked!!
                            ) {
                            Icon(
                                painterResource(id = R.drawable.baseline_delete_24),
                                contentDescription = "Remove the product"
                            )
                        }
                        IconButton(onClick = {
                            Log.d(TAG, "Save button pressed")

                            val vendor_id = souvenirViewModel.vendor_profile_for_login.value!!.id
                            Log.d(TAG, "vendor_id=$vendor_id")
                            val product_modified =  Products( id = original_product!!.id,
                                product_name = prodName,
                                description =  prodDescr,
                                price =  if (prodPrice.equals("null") or prodPrice.isNullOrEmpty()) null else prodPrice!!.toDouble(),
                                vendor_id = vendor_id,
                                weight =  if (prodWeight.equals("null") or prodWeight.isNullOrEmpty() ) null else prodWeight!!.toDouble().toInt(),
                                dimensions ="${prodLength}x${prodWidth}x${prodHeight}",
                                weight_unit = if (prodWeightMeasure.isEmpty()) 0 else prodWeightMeasure.toInt(),
                                dimensions_unit = if (prodDimMeasure.isEmpty()) 0 else prodDimMeasure.toInt(),
                                product_picture_BASE64= encodedBtmp
                            )

                            Log.d(TAG, "product_modified=${product_modified.toString()}")
                            souvenirViewModel.try_update_product(product_modified!!,productPositionInList!!)
                            souvenirViewModel.product_updated_response.observe(viewLifecycleOwner,product_updated_response_OBSERVER)

                        },
                            enabled = isFormValid) {
                            Icon(
                                painterResource(id = R.drawable.baseline_done_24),
                                contentDescription = "Save"
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { back_actions() }) {
                            Icon(
                                painterResource(id = R.drawable.baseline_arrow_back_24),
                                contentDescription = "Go back to the list"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            )
            {
                val image_final_url = Settings.IMAGES_BASE_URL + vendorProfileCurrentlyIn.id + "/" +
                        "products" +  "/" + product!!.id  + ".jpg"
                name_and_Picture(prodName,onProdNameChanged,onImageSuccessChanged,image_final_url)
                product_description(prodDescr,onProdDescrChanged)
                product_details(weight = prodWeight, weightMeasure = prodWeightMeasure.toInt(),
                                prodDimMeasure = prodDimMeasure.toInt(),
                                L=prodLength, W= prodWidth , H=prodHeight,
                                price = prodPrice!!)
            }
        }
    }

    @Composable
    fun name_and_Picture(productName : String,
                         onProdNameChanged: (String) -> Unit,
                         onImageSuccess: (AsyncImagePainter.State) -> Unit,
                         image_final_url : String =""){
        val locked by  formProdValidatorViewModel.form_locked.observeAsState()

        Row(
            modifier = Modifier
                .background(color = colorResource(R.color.light_blue))
                .padding(10.dp),
        )
        {
            val imageUri: MutableState<String> = rememberSaveable { mutableStateOf(image_final_url)}
            val painter = rememberAsyncImagePainter( model = imageUri.value.ifEmpty { R.drawable.noimage150x150 },
                                                                    onSuccess = onImageSuccess, )
            val launcher =
                            rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent(),
                                                              onResult = { uri: Uri? ->
                                                                  if (uri != null ) {
                                                                      imageUri.value = uri.toString()
                                                                      val orignal_bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri)
                                                                      Log.d(TAG, "Image processing original_width=${orignal_bitmap.width} + original_height=${orignal_bitmap.height}")

                                                                      val cropped_bitmap = cropCenter(orignal_bitmap)!!
                                                                      Log.d(TAG, "Image processing cropped_width=${cropped_bitmap.width} + cropped_height=${cropped_bitmap.height}")

                                                                      val scaled_bitmap : Bitmap= cropped_bitmap.scale(150,150,true)
                                                                      Log.d(TAG, "Image processing scaled_width=${scaled_bitmap.width} + scaled_height=${scaled_bitmap.height}")
                                                                      encodedBtmp  = encodeToBase64(scaled_bitmap, Bitmap.CompressFormat.JPEG)
                                                                      Log.d(TAG, "URI image = $uri")
                                                                      editedByUser = true
                                                                  } else
                                                                      Log.d(TAG, "not a valid URI for Image")
                                                              })

            var product_name_error by remember { mutableStateOf(false) }

            Image(
                painter =  painter,
                contentDescription = "Choose the image",
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
                    .clickable {  if (!locked!! )
                                        launcher.launch("image/*")
                    }
            )
            BasicTextField(
                // for inputs
                modifier = Modifier
                    .align(Alignment.Top)
                    .widthIn(10.dp, 10.dp),
                value = "*",
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp),
                readOnly = true,
                onValueChange = {},
            )
            TextField( // for inputs
                value = productName ,
                //modifier = Modifier.border(  width = 1.dp ),
                isError = product_name_error,
                singleLine = true,
                onValueChange =  onProdNameChanged,
                label = { Text("Product name*") },
                enabled = !locked!!
            )
        }
    }

    @Composable
    fun product_description(productDescr : String,
                            onProdDescrChanged: (String) -> Unit ,){
        val locked by  formProdValidatorViewModel.form_locked.observeAsState()
        Row(
            modifier = Modifier
                .background(color = colorResource(R.color.light_blue))
                .padding(10.dp),
        )
        {
            TextField( // for inputs
                modifier = Modifier.fillMaxWidth(),
                value = productDescr,
                onValueChange = onProdDescrChanged,
                textStyle = TextStyle(fontSize =  15.sp ),
                maxLines = 4,
                label = { Text("Product description*") },
                enabled = !locked!!
            )
        }
    }

    @Composable
    fun product_details(weight:String? = "", weightMeasure: Int = 0,
                        prodDimMeasure: Int = 0,
                        L:String = "",
                        W:String = "",
                        H:String = "",
                        price:String ="",
    ){
        weight_show(weight,weightMeasure)
        dimensions_show(L=L,W=W,H=H,
                        prodDimMeasure =prodDimMeasure)
        price_show(price)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun weight_show(weight:String? = "",weightMeasure:Int=0){
        val locked by  formProdValidatorViewModel.form_locked.observeAsState()
        Row(
            modifier = Modifier
                .background(color = colorResource(R.color.light_blue))
                .padding(10.dp)
                .fillMaxWidth(),
        )
        {
            var text: String by remember { mutableStateOf(( if (weight.equals("null")) "" else weight!!  )) }
//            val pattern = remember { Regex("^\\d+\$") }
            val pattern = remember { Regex("^(\\s*|\\d+)\$") }
            TextField( // for inputs
                modifier = Modifier.widthIn(10.dp,90.dp), //.width(80.dp)
                value = text,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number ),
                onValueChange = { if (it.matches(pattern)) {text = it
                                        formProdValidatorViewModel.setproductWeight(it)}
                                },
                label = { Text("Weight") },
                enabled = !locked!!
            ) //
            Spacer(modifier = Modifier.width(10.dp))
            //DropdownMenuBoxMeasure(R.array.weight_metric)
            DropdownMenuBoxMeasure(metrOrImper(EntMeasure.weight,0.intToBoolean()),
                                    { it: String -> formProdValidatorViewModel.setproductWeightMeasure(it) },
                                        weightMeasure,)
        } //
    }

    @Composable
    fun dimensions_show(L:String = "",
                        W:String = "",
                        H:String = "",
                        prodDimMeasure: Int = 0){
        val locked by  formProdValidatorViewModel.form_locked.observeAsState()
        Row(modifier = Modifier
            .background(color = colorResource(R.color.light_blue))
            .padding(10.dp)
            .fillMaxWidth())
        {
            var textL by remember { mutableStateOf(L) }
            TextField( // for inputs
                modifier = Modifier.widthIn(30.dp,60.dp),                     //.width(80.dp)
                value = textL,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number ),
                onValueChange = { textL = it
                                    formProdValidatorViewModel.setproductLength(it) },
                label = { Text("L") },
                enabled = !locked!!,
            ) //
            var textW by remember { mutableStateOf(W) }
            TextField( // for inputs
                modifier = Modifier.widthIn(30.dp,60.dp),
                value = textW,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number ),
                onValueChange = { textW = it
                                    formProdValidatorViewModel.setproductWidth(it)
                                },
                label = { Text("W") },
                enabled = !locked!!,
            ) //
            var textH by remember { mutableStateOf(H) }
            TextField( // for inputs
                modifier = Modifier.widthIn(30.dp,60.dp),
                value = textH,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number ),
                onValueChange = { textH = it
                                    formProdValidatorViewModel.setproductHeight(it)
                },
                label = { Text("H") },
                enabled = !locked!!,
            ) //
            Spacer(modifier = Modifier.width(10.dp))
            DropdownMenuBoxMeasure(metrOrImper(EntMeasure.dimensions,0.intToBoolean()),
                {formProdValidatorViewModel.setproductDimMeasure(it)},
                prodDimMeasure)
        } //
    }
    @Preview
    @Composable
    fun price_show(price:String? = ""){
        val locked by  formProdValidatorViewModel.form_locked.observeAsState()
        Row(modifier = Modifier
            .background(color = colorResource(R.color.light_blue))
            .padding(10.dp)
            .fillMaxWidth())
        {
            var text_price by remember { mutableStateOf( ( if (price.equals("null")) "" else price!! ) )}
            TextField( // for inputs
                modifier = Modifier.widthIn(30.dp,90.dp),
                value = text_price,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number ),
                onValueChange = { text_price = it
                    formProdValidatorViewModel.setproductPrice(it)
                },
                label = { Text("Price") },
                enabled =!locked!!,
            )

            Spacer(modifier = Modifier.width(10.dp))

            var text_currency by remember { mutableStateOf(currencyValue?:"€") }
            BasicTextField( modifier = Modifier
                .widthIn(30.dp, 90.dp)
                .align(Alignment.CenterVertically),
                            value = text_currency,
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 16.sp),
                            readOnly = true,
                            onValueChange = { text_currency = it },
                         ) //


        } //
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DropdownMenuBoxMeasure(arrayRes: Int, onValueChange: (String) -> Unit,selected_index:Int = 0) {
        val context = LocalContext.current
        var array_items: Array<String> = context.resources.getStringArray(arrayRes)
        var expanded by remember { mutableStateOf(false) }
        var selectedText by remember { mutableStateOf(array_items[selected_index]) };
        val locked by  formProdValidatorViewModel.form_locked.observeAsState()

        Box(
            modifier = Modifier.widthIn(130.dp,130.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    if (!locked!!) expanded = !expanded
                },
            ) {
                TextField(
                    value = selectedText,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(),
                    enabled = !locked!!
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {

                    array_items.forEachIndexed { index, item ->
                        DropdownMenuItem(
                            text = { Text(text = item) },
                            onClick = {
                                selectedText = item
                                expanded = false
                                Log.i(TAG,"Selected item ${item.toString()} ")
                                onValueChange(index.toString())  // viewmodel
                            },
                           // enabled = !locked!!
                        )
                    }
                }
            }
        }
    }

    fun cropCenter(bmp: Bitmap): Bitmap? {
        val dimension = Math.min(bmp.width, bmp.height)
        return ThumbnailUtils.extractThumbnail(bmp, dimension, dimension)
    }

    fun showToast(s: String, lengthShort: Int) {
        val toast = Toast.makeText(requireContext(), s, lengthShort)
        toast.setGravity(Gravity.CENTER,0,0)
        toast.show()
    }

    fun back_actions(){
        Log.d(TAG, "Going back.....")
        original_product=null
        activity.remove_vendor_edit_product_fragment()
    }

    val product_updated_response_OBSERVER : Observer<Boolean> = Observer<Boolean>{
        if (it != null) {
            Log.d(TAG, "OBSERVED product_updated_response=$it")
            if ((it)) {
                editedByUser = false
                showToast("Product modified", Toast.LENGTH_SHORT)
                //formProdValidatorViewModel.setForm_locked(true)
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
                showToast("Product deleted", Toast.LENGTH_SHORT)
                formProdValidatorViewModel.setForm_locked(true)
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

 //    private fun clearFocusAndHideKeyboard() {
//        // hide the keyboard
//        val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        inputMethodManager.hideSoftInputFromWindow(requireView().windowToken, 0)
//        // clear focus
//        binding.root.clearFocus()
//    }

    fun LxWxHfromDBtoSeparateObjects(str:String?): LxWxH? {
        if (str != null ) {
            val fistX = str.indexOf("x")
            val lastX = str.lastIndexOf("x")
            val length = str.substring(0, fistX)
            val width = str.substring(fistX + 1, lastX)
            val height = str.substring(lastX + 1, str.length)
            return LxWxH(length,width,height)
        } else return null

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun metrOrImper(m : EntMeasure, imperial: Boolean):Int{
        if (m == EntMeasure.weight )
            if ( imperial )  return R.array.weight_imp
            else                return R.array.weight_metr
        if (m == EntMeasure.dimensions )
            if (imperial)   return R.array.dimension_imp
            else               return R.array.dimension_metr
        return 0
    }

    private fun Int.intToBoolean(): Boolean {
        if (this == 1)  return true
        else            return false
    }

    private fun Boolean.booleanToInt(): Int {
        if (this)  return 1
        else return 0
    }

     override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG,"+onDestroyView CurrentOperation.name=${CurrentOperation.name}")
    }


    override fun onPause() {
        super.onPause()
        Log.d(TAG,"+onPause")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG,"+onResume  CurrentOperation=${CurrentOperation.name}")
    }

} // main