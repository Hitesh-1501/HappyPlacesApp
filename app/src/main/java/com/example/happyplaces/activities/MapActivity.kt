package com.example.happyplaces.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.happyplaces.R
import com.example.happyplaces.databinding.ActivityMapBinding
import com.example.happyplaces.models.HappyPlaceModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity() , OnMapReadyCallback {
    var binding:ActivityMapBinding? = null
    private var mHappyPlaceDetail : HappyPlaceModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlaceDetail = intent.getParcelableExtra(
                MainActivity.EXTRA_PLACE_DETAILS
            )
        }

        if(mHappyPlaceDetail != null){
            setSupportActionBar(binding?.toolbarMap)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = mHappyPlaceDetail!!.title

            binding?.toolbarMap?.setNavigationOnClickListener {
                onBackPressed()
            }

            val supportMapFragment : SupportMapFragment =
                supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

            supportMapFragment.getMapAsync(this)

        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("HappyPlaceDetail", "Lat: ${mHappyPlaceDetail!!.latitude}, Lng: ${mHappyPlaceDetail!!.longitude}")
        val latlng = LatLng(
            mHappyPlaceDetail!!.latitude,
            mHappyPlaceDetail!!.longitude
        )
        googleMap.addMarker(MarkerOptions().position(latlng).title(mHappyPlaceDetail!!.location))
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(latlng, 10f)
        googleMap.animateCamera(newLatLngZoom)
        Log.e("Position", latlng.toString()) // Log LatLng values to ensure correctness

    }
}

