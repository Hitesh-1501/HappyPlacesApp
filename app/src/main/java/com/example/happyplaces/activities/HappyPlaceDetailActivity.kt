package com.example.happyplaces.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.happyplaces.R
import com.example.happyplaces.databinding.ActivityHappyPlaceDetailBinding
import com.example.happyplaces.models.HappyPlaceModel

class HappyPlaceDetailActivity : AppCompatActivity() {
    private var binding : ActivityHappyPlaceDetailBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHappyPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        var happyPlaceDetailModel : HappyPlaceModel? = null
        // After launching the activity we will check and get the Serializable  and parcelable data class with the details in it and set it to UI components.
        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            // get the Parcelable data model class with the details in it
            happyPlaceDetailModel = intent.getParcelableExtra(
                MainActivity.EXTRA_PLACE_DETAILS
            )
        }

        if(happyPlaceDetailModel != null){
            setSupportActionBar(binding?.toolbarHappyPlaceDetail)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = happyPlaceDetailModel.title

            binding?.toolbarHappyPlaceDetail?.setNavigationOnClickListener {
                onBackPressed()
            }

            binding?.ivPlaceImage?.setImageURI(Uri.parse(happyPlaceDetailModel.image))
            binding?.tvDescription?.text = happyPlaceDetailModel.description
            binding?.tvLocation?.text = happyPlaceDetailModel.location

            binding?.btnViewOnMap?.setOnClickListener {
                val intent = Intent(this,MapActivity::class.java)
                intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS,happyPlaceDetailModel)
                startActivity(intent)
            }
        }

    }
}