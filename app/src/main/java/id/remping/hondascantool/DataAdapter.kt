package id.remping.hondascantool

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DataAdapter(var data: List<Pair<String,String>>) : RecyclerView.Adapter<DataAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val key: TextView = view.findViewById(R.id.key)
        val value: TextView = view.findViewById(R.id.value)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_data, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.key.text = data[position].first
        holder.value.text = data[position].second
    }

    override fun getItemCount(): Int = data.size

    fun update(newData: List<Pair<String,String>>) {
        data = newData
        notifyDataSetChanged()
    }
}
