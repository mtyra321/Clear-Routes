package edu.byui.mynavigation

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import edu.byui.mynavigation.DailyForecast
import edu.byui.mynavigation.R

class WeatherDisplayAdapter(private val context: Context, private val imageModelArrayList: ArrayList<DailyForecast>) : BaseAdapter() {

    override fun getViewTypeCount(): Int {
        return count
    }

    override fun getItemViewType(position: Int): Int {

        return position
    }

    override fun getCount(): Int {
        return imageModelArrayList.size
    }

    override fun getItem(position: Int): Any {
        return imageModelArrayList[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val holder: ViewHolder

        if (convertView == null) {
            holder = ViewHolder()
            val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.weather_lv_item, null, true)

            holder.tvname = convertView!!.findViewById(R.id.name) as TextView
            holder.iv = convertView.findViewById(R.id.imgView) as ImageView
            holder.tvbody = convertView!!.findViewById(R.id.body) as TextView

            convertView.tag = holder
        } else {
            // the getTag returns the viewHolder object set as a tag to the view
            holder = convertView.tag as ViewHolder
        }

        holder.tvbody!!.setText(imageModelArrayList[position].toString())
        holder.tvname!!.setText(imageModelArrayList[position].day)
        holder.iv!!.setImageResource(imageModelArrayList[position].icon)



        return convertView
    }

    private inner class ViewHolder {

        var tvname: TextView? = null
        internal var iv: ImageView? = null
        var tvbody: TextView? = null

    }

}