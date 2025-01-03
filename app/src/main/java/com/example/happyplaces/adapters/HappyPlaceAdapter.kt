package com.example.happyplaces.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.activities.AddHappyPlaceActivity
import com.example.happyplaces.activities.MainActivity
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.databinding.ItemHappyPlaceBinding
import com.example.happyplaces.models.HappyPlaceModel

open class HappyPlaceAdapter(
    private var context: Context,
    private var list : ArrayList<HappyPlaceModel>
):RecyclerView.Adapter<HappyPlaceAdapter.ViewHolder>(){
    // Add a variable for onClickListener interface
    private var onClickListener : OnClickListener? = null

    class ViewHolder(binding: ItemHappyPlaceBinding) : RecyclerView.ViewHolder(binding.root){
        val iv = binding.ivPlaceImage
        val tvtitle = binding.tvTitle
        val tvdesctiption = binding.tvDescription
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemHappyPlaceBinding.inflate(LayoutInflater.from(context),parent,false)
        )
    }

    override fun onBindViewHolder(holder:ViewHolder, position: Int) {
        val model = list[position]
        holder.iv.setImageURI(Uri.parse(model.image))
        holder.tvtitle.text = model.title
        holder.tvdesctiption.text = model.description
        //Finally add an onclickListener to the item
        holder.itemView.setOnClickListener{
            if(onClickListener != null){
                onClickListener!!.onClick(position,model)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
    /**
     * A function to bind the onclickListener.
     */

    fun setOnClickListener(onClickListener : OnClickListener){
        this.onClickListener = onClickListener
    }
    //Create an interface for onclickListener
    interface OnClickListener{
        fun onClick(position: Int,model: HappyPlaceModel)
    }
     //  A function to edit the added happy place detail and pass the existing details through intent.
    fun notifyEditItem(activity:Activity, position: Int,reqestCode: Int){
        val intent = Intent(context,AddHappyPlaceActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS,list[position])
        activity.startActivityForResult(intent,reqestCode)// Activity is started with requestCode
        notifyItemChanged(position) // Notify any registered observers that the item at position has changed.
    }
    // A function to delete the added happy place detail from the local storage.
    fun removeAt(position: Int){
        val dbHandler = DatabaseHandler(context)
        val isDelete = dbHandler.deleteHappyPlace(list[position])
        if(isDelete > 0){
            list.removeAt(position)
            notifyItemRemoved(position)
        }
    }

}