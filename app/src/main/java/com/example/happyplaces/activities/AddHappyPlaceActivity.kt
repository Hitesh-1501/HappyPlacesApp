package com.example.happyplaces.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.happyplaces.R
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.databinding.ActivityAddHappyPlaceBinding
import com.example.happyplaces.databinding.DialogCustomImageSelectionBinding
import com.example.happyplaces.models.HappyPlaceModel
import com.example.happyplaces.utils.GetAddresssFromLatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID


class AddHappyPlaceActivity : AppCompatActivity(),View.OnClickListener {
    private val cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var saveImageToInternalStorage: Uri? = null
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    //A variable for data model class in which we will receive the details to edit.)
    private var mHappyPlaceDetails: HappyPlaceModel? = null

    //Add a variable for FusedLocationProviderClient which is later used to get the current location.
    private lateinit var mFusedLocationClient : FusedLocationProviderClient


    private var binding: ActivityAddHappyPlaceBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setSupportActionBar(binding?.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressed()
        }

        //// Initialize the Fused location variable
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)



        /**
         *  This checks if the Google Places API is already initialized.
         *  Places.isInitialized() returns true if it has been initialized, and false otherwise.
         *  The exclamation mark (!) negates the result,
         *  meaning the condition becomes true if the API is not initialized.
         */
        if(!Places.isInitialized()){
            Places.initialize(this@AddHappyPlaceActivity,
                //his retrieves the Google Maps API key from the app's resources,
                // which is needed to authenticate requests to the Google Places API.
                resources.getString(R.string.google_maps_api_key))
        }


        // Assign the details to the variable of data model class which we have created above the details which we will receive through intent
        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            mHappyPlaceDetails = intent.getParcelableExtra(
                MainActivity.EXTRA_PLACE_DETAILS
            )
        }

        dateSetListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateView()

        }
        updateDateView()
        //Filling the existing details to the UI components to edit
        if (mHappyPlaceDetails != null) {
            supportActionBar?.title = "Edit Happy Place"
            binding?.etTitle?.setText(mHappyPlaceDetails!!.title)
            binding?.etDescription?.setText(mHappyPlaceDetails!!.description)
            binding?.etDate?.setText(mHappyPlaceDetails!!.date)
            binding?.etLocation?.setText(mHappyPlaceDetails!!.location)

            mLongitude = mHappyPlaceDetails!!.longitude
            mLatitude = mHappyPlaceDetails!!.latitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)
            binding?.ivPlaceImage?.setImageURI(saveImageToInternalStorage)

            binding?.btnSave?.text = "UPDATE"
        }

        binding?.etDate?.setOnClickListener(this)
        binding?.tvAddImage?.setOnClickListener(this)
        binding?.btnSave?.setOnClickListener(this)
        binding?.etLocation?.setOnClickListener(this)
        binding?.tvSelectCurrentLocation?.setOnClickListener(this)
    }

    private fun isLocationEnabled(): Boolean{
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    }
    /**
     * A function to request the current location. Using the fused location provider client.
     */

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestNewLocationData(){
       val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,0L).apply {
           setWaitForAccurateLocation(false)
           setMinUpdateIntervalMillis(1000)
           setMaxUpdateDelayMillis(0)
           setMaxUpdates(1)
       }.build()

       mFusedLocationClient.requestLocationUpdates(
           locationRequest,mLocationCallBack,Looper.myLooper()
       )
    }
    //Create a location callback object of fused location provider client where we will get the current location details
    private val mLocationCallBack = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult!!.lastLocation
            mLatitude = mLastLocation!!.latitude
            Log.e("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation!!.longitude
            Log.e("Current Longitude", "$mLongitude")
            val addressTask = GetAddresssFromLatLng(this@AddHappyPlaceActivity,mLatitude,mLongitude)
            addressTask.setAddressListener(object : GetAddresssFromLatLng.AddressListener{
                override fun onAddressFound(address: String) {
                    binding?.etLocation?.setText(address)
                }

                override fun onError() {
                    Log.e("Get Address::","Something went wrong")
                }
            })
            addressTask.getAddress()
        }
    }


    @SuppressLint("SuspiciousIndentation")
    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.et_date -> {
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            R.id.tv_add_image -> {
                val dialog = Dialog(this@AddHappyPlaceActivity)
                val binding: DialogCustomImageSelectionBinding =
                    DialogCustomImageSelectionBinding.inflate(layoutInflater)
                dialog.setContentView(binding.root)

                binding.tvCamera.setOnClickListener {
                    takePhotoFromCamera()
                    dialog.dismiss()
                }

                binding.tvGallery.setOnClickListener {
                    choosePhotoFromGallery()
                    dialog.dismiss()

                }
                dialog.show()
            }

            R.id.btn_save -> {
                when {
                    binding?.etTitle?.text.isNullOrEmpty() -> {
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "please enter a title", Toast.LENGTH_SHORT
                        ).show()
                    }

                    binding?.etDescription?.text.isNullOrEmpty() -> {
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "please enter a description", Toast.LENGTH_SHORT
                        ).show()
                    }

                    binding?.etLocation?.text.isNullOrEmpty() -> {
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "please enter a location", Toast.LENGTH_SHORT
                        ).show()
                    }

                    saveImageToInternalStorage == null -> {
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "please select an image", Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {
                        val happyPlaceModel = HappyPlaceModel(
                            //Changing the id if it is for edit.
                            if (mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,
                            binding?.etTitle?.text.toString(),
                            saveImageToInternalStorage.toString(),
                            binding?.etDescription?.text.toString(),
                            binding?.etDate?.text.toString(),
                            binding?.etLocation?.text.toString(),
                            mLatitude,
                            mLongitude

                        )

                        val dbHandler = DatabaseHandler(this)
                        // Call add or update details conditionally.
                        if (mHappyPlaceDetails == null) {
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)
                            if (addHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        } else {
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                            if (updateHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }
                    }
                }
            }
            R.id.et_location->{
                try {
                    // This is the list of the fields which has to be passed
                    /*This creates a list of fields (of type Place.Field) that will be returned by the autocomplete functionality.
                     These fields include:
                     Place.Field.ID: The unique ID of the selected place.
                     Place.Field.NAME: The name of the selected place.
                     Place.Field.LAT_LNG: The latitude and longitude coordinates of the selected place.
                     Place.Field.ADDRESS: The full address of the selected place.
                    */
                    val fields = listOf(
                        Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )
                    // Start the autocomplete intent with the unique request code
                    /*: This builds an intent using the Google Places Autocomplete API.
                     The Autocomplete.IntentBuilder creates an intent that launches an autocomplete activity where users can search for and select a place.
                     */
                    val intent =
                        Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN,fields).build(this@AddHappyPlaceActivity)
                        startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                }catch (e : Exception){
                    Log.e("Error",e.printStackTrace().toString())
                }
            }
            R.id.tv_select_current_location->{
                if(!isLocationEnabled()){
                    Toast.makeText(this,
                        "your location provider is turn off. please turn on location",
                        Toast.LENGTH_SHORT).show()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }else{
                    Dexter.withContext(this).withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ).withListener(object : MultiplePermissionsListener {
                        @RequiresApi(Build.VERSION_CODES.S)
                        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                           if(report!!.areAllPermissionsGranted()) {
                               requestNewLocationData()
                           }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            permissions: MutableList<PermissionRequest>,
                            token: PermissionToken
                        ) {
                            showRationalDialogForPermission()
                        }
                    }).onSameThread().check()
                }

            }
        }
    }

        private fun choosePhotoFromGallery() {
            Dexter.withContext(this@AddHappyPlaceActivity).withPermissions(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()) {
                        val galleryIntent = Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>,
                    token: PermissionToken
                ) {
                    showRationalDialogForPermission()
                }
            }).onSameThread().check()
        }

        private fun takePhotoFromCamera() {
            Dexter.withContext(this@AddHappyPlaceActivity).withPermissions(
                Manifest.permission.CAMERA,
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()) {
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>,
                    token: PermissionToken
                ) {
                    showRationalDialogForPermission()
                }
            }).onSameThread().check()
        }

        private fun showRationalDialogForPermission() {
            AlertDialog.Builder(this).setMessage(
                "" +
                        "it looks like you have turned off permission required" +
                        "for this feature it can be enabled under the" +
                        "Applications Settings"
            )
                .setPositiveButton("GO TO SETTINGS") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton("Cancle") { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }

        @Deprecated("Deprecated in Java")
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == GALLERY_REQUEST_CODE) {
                    if (data != null) {
                        val contentURI = data.data
                        try {
                            val selectedImageBitmap =
                                MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)

                            saveImageToInternalStorage =
                                saveImageToInternalStorage(selectedImageBitmap)

                            Log.e("Saved image", "path :: $saveImageToInternalStorage")

                            binding?.ivPlaceImage?.setImageBitmap(selectedImageBitmap)
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Toast.makeText(
                                this@AddHappyPlaceActivity,
                                "Failed to load the image from Gallery!", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else if (requestCode == CAMERA_REQUEST_CODE) {
                    val thumbNail: Bitmap = data!!.extras!!.get("data") as Bitmap

                    saveImageToInternalStorage = saveImageToInternalStorage(thumbNail)

                    Log.e("Saved image", "path :: $saveImageToInternalStorage")

                    binding?.ivPlaceImage?.setImageBitmap(thumbNail)

                }else if(requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE){
                    val place : Place = Autocomplete.getPlaceFromIntent(data!!)
                    binding?.etLocation?.setText(place.address)
                    mLatitude = place.latLng!!.latitude
                    mLongitude = place.latLng!!.longitude


                }
            }
        }



        private fun updateDateView() {
            val myFormat = "dd.MM.yyyy"
            val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
            binding?.etDate?.setText(sdf.format(cal.time).toString())

        }

        //  A function to save a copy of an image to internal storage for HappyPlaceApp to use.
        private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
            //Get the context wrapper instance
            val wrapper = ContextWrapper(applicationContext)
            // The bellow line return a directory in internal storage
            /**
             * The Mode Private here is
             * File creation mode: the default mode, where the created file can only
             * be accessed by the calling application (or all applications sharing the
             * same user ID).
             */
            var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
            // Create a file to save the image
            file = File(file, "${UUID.randomUUID()}.jpg")

            try {
                // Get the file output stream
                val stream: OutputStream = FileOutputStream(file)
                // Compress bitmap
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                stream.flush()
                stream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            // Return the saved image uri
            return Uri.parse(file.absolutePath)
        }

        companion object {
            private const val GALLERY_REQUEST_CODE = 1
            private const val CAMERA_REQUEST_CODE = 2
            private const val IMAGE_DIRECTORY = "HappyPlacesImages"
            private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
        }
    }
