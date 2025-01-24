package com.example.filmverse.Adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.filmverse.Activities.GenreFilmActivity
import com.example.filmverse.Domian.GenreSolo
import com.example.filmverse.R

class CategoryListAdapter(private val items: ArrayList<GenreSolo>) : RecyclerView.Adapter<CategoryListAdapter.ViewHolder>() {
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val inflate: View = LayoutInflater.from(parent.context).inflate(R.layout.viewholder_category, parent, false)
        return ViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.titleTxt.text = items[position].genre

        holder.itemView.setOnClickListener {
            val intent = Intent(context, GenreFilmActivity::class.java).apply {
                putExtra("GENRE", items[position].genre)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTxt: TextView = itemView.findViewById(R.id.Titletxt)
    }
}