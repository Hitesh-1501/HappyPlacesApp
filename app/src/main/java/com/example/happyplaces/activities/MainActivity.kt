package com.example.happyplaces.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.adapters.HappyPlaceAdapter
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.databinding.ActivityMainBinding
import com.example.happyplaces.models.HappyPlaceModel
import com.example.happyplaces.utils.SwipeToEditCallback
import com.happyplaces.utils.SwipeToDeleteCallback


class MainActivity : AppCompatActivity() {
    private var binding:ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding  = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.fabAddHappyPlace?.setOnClickListener {
            val intent = Intent(this, AddHappyPlaceActivity::class.java)
            startActivityForResult(intent, ADD_PLACE_ACTIVITY_REQUEST_CODE)
        }
        getHappyPlacesListFromLocalDB()
    }

    private fun setupHappyPlacesRecyclerView(
        happyPlacesList : ArrayList<HappyPlaceModel>){
        binding?.rvHappyPlacesList?.layoutManager = LinearLayoutManager(this)
        binding?.rvHappyPlacesList?.setHasFixedSize(true)
        val placesAdapter = HappyPlaceAdapter(this,happyPlacesList)
        binding?.rvHappyPlacesList?.adapter = placesAdapter
        //Bind the onclickListener with adapter onClick function
        placesAdapter.setOnClickListener(object : HappyPlaceAdapter.OnClickListener{
            override fun onClick(position: Int, model: HappyPlaceModel) {
                val intent = Intent(this@MainActivity,HappyPlaceDetailActivity::class.java)
                //Pass the HappyPlaceDetails data model class to the detail activity
                intent.putExtra(EXTRA_PLACE_DETAILS,model)
                startActivity(intent)
            }
        })
        // Bind the edit feature class to recyclerview
        val editSwipeHandler = object : SwipeToEditCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Call the adapter function when it is swiped
                val adapter = binding?.rvHappyPlacesList?.adapter as HappyPlaceAdapter
                adapter.notifyEditItem(this@MainActivity,viewHolder.adapterPosition,
                    ADD_PLACE_ACTIVITY_REQUEST_CODE)
            }
        }
        val editTouchHelper = ItemTouchHelper(editSwipeHandler)
        editTouchHelper.attachToRecyclerView(binding?.rvHappyPlacesList)
        //Bind the delete feature class to recyclerview
        val deleteSwipeHandler = object : SwipeToDeleteCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Call the adapter function when it is swiped
                val adapter = binding?.rvHappyPlacesList?.adapter as HappyPlaceAdapter
                adapter.removeAt(viewHolder.adapterPosition)
                getHappyPlacesListFromLocalDB()  // Gets the latest list from the local database after item being delete from it.
            }
        }
        val deleteTouchHelper = ItemTouchHelper(deleteSwipeHandler)
        deleteTouchHelper.attachToRecyclerView(binding?.rvHappyPlacesList)
    }

    private fun getHappyPlacesListFromLocalDB(){
        val dbHandler = DatabaseHandler(this)
        val getHappyPlacesList : ArrayList<HappyPlaceModel> = dbHandler.getHappyPlacesList()

        if(getHappyPlacesList.size > 0){
           binding?.rvHappyPlacesList?.visibility = View.VISIBLE
           binding?.tvNoRecordsAvailable?.visibility = View.GONE
           setupHappyPlacesRecyclerView(getHappyPlacesList)
        }else{
            binding?.rvHappyPlacesList?.visibility = View.GONE
            binding?.tvNoRecordsAvailable?.visibility = View.VISIBLE
        }
    }

    // TODO(Step 4 : It is called when the activity which launched with the request code and expecting a result from the launched activity.)
    // START
    // Call Back method  to get the Message form other Activity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // check if the request code is same as what is passed  here it is 'ADD_PLACE_ACTIVITY_REQUEST_CODE'
        if (requestCode == ADD_PLACE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                getHappyPlacesListFromLocalDB()
            }else{
                Log.e("Activity", "Cancelled or Back Pressed")
            }
        }
    }
    // END


    companion object{
        var ADD_PLACE_ACTIVITY_REQUEST_CODE = 1
        var EXTRA_PLACE_DETAILS = "extra_place_details"
    }
}