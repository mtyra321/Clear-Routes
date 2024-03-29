package edu.byui.mynavigation

import android.content.Context
import android.graphics.Color
import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject


/**
 * Created by Vishal on 10/20/2018.
 */
class PointsParser(mContext: Context, directionMode: String) :
    AsyncTask<String?, Int?, List<List<HashMap<String, String>>>?>() {
    var taskCallback: TaskLoadedCallback
    var directionMode = "driving"


    // Parsing the data in non-ui thread
    override fun doInBackground(vararg jsonData: String?): List<List<HashMap<String, String>>>? {
        val jObject: JSONObject
        var routes: List<List<HashMap<String, String>>>? = null
        try {
            jObject = JSONObject(jsonData[0])
            jsonData[0]?.let { Log.d("mylog", it) }
            val parser = DataParser()
            Log.d("mylog", parser.toString())

            // Starts parsing data
            routes = parser.parse(jObject)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return routes
    }

    // Executes in UI thread, after the parsing process
    override fun onPostExecute(result: List<List<HashMap<String, String>>>?) {
        var points: ArrayList<LatLng?>
        var lineOptions: PolylineOptions? = null
        // Traversing through all the routes
        for (i in result!!.indices) {
            points = ArrayList()
            lineOptions = PolylineOptions()
            // Fetching i-th route
            val path = result[i]
            // Fetching all the points in i-th route
            for (j in path.indices) {
                val point = path[j]
                val lat = point["lat"]!!.toDouble()
                val lng = point["lng"]!!.toDouble()
                val position = LatLng(lat, lng)
                points.add(position)
            }
            // Adding all the points in the route to LineOptions
            lineOptions.addAll(points)
            lineOptions.width(10f)
            lineOptions.color(Color.BLUE)
        }

        // Drawing polyline in the Google Map for the i-th route
        if (lineOptions != null) {
            //mMap.addPolyline(lineOptions);
            taskCallback.onTaskDone(lineOptions)
        } else {
            Log.d("mylog", "without Polylines drawn")
        }
    }

    init {
        taskCallback = mContext as TaskLoadedCallback
        this.directionMode = directionMode
    }
}