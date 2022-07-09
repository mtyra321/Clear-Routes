package edu.byui.mynavigation

import org.json.JSONObject
import org.json.JSONArray
import com.google.android.gms.maps.model.LatLng
import org.json.JSONException
import java.lang.Exception
//import java.util.ArrayList
//import java.util.HashMap
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * This code parses the data to retrieve the polylines to draw the route on the map
 * Created by Vishal on 10/20/2018.
 */
class DataParser {
//    val legDurations: MutableList<Int> = ArrayList() // in seconds
//    val legDistances: MutableList<Int> = ArrayList() // in meters
//    val legEndLocs: MutableList<JSONObject> = ArrayList() // [{lat: val, lng: val},{lat: val,lng: val}]
//    val instructions: MutableList<String> = ArrayList() // ["turn left","turn right", "merge",...]
    fun parse(jObject: JSONObject): MutableList<List<HashMap<String, String>>> {
        val routes: MutableList<List<HashMap<String, String>>> = ArrayList()
        val jRoutes: JSONArray
        var jLegs: JSONArray
        var jSteps: JSONArray
        try {
            jRoutes = jObject.getJSONArray("routes")
            /** Traversing all routes  */
            for (i in 0 until jRoutes.length()) {
                jLegs = (jRoutes[i] as JSONObject).getJSONArray("legs")
                val path: MutableList<HashMap<String, String>> = ArrayList()
//                val path: MutableList<*> = ArrayList<Any>()
//                val path: MutableList = mutableListOf<HashMap>()
                /** Traversing all legs  */
                for (j in 0 until jLegs.length()) {
                    /** add each leg duration, distance, and end_location to arrays */
//                    val legDuration = ((jLegs[j] as JSONObject)["duration"] as JSONObject)["value"] as Int
//                    legDurations.add(legDuration) // [31653,11799,70097] (seconds)
//                    val legDistance = ((jLegs[j] as JSONObject)["distance"] as JSONObject)["value"] as Int
//                    legDistances.add(legDistance) // [932311,349512,2137682] (meters)
//                    val legEndLoc = ((jLegs[j] as JSONObject)["end_location"] as JSONObject)
//                    legEndLocs.add(legEndLoc) // [{"lat": 34.052, "lng": -118.243},{"lat":35.4675,"lng":-97.5164077},...]
                    jSteps = (jLegs[j] as JSONObject).getJSONArray("steps")
                    /** Traversing all steps  */
                    for (k in 0 until jSteps.length()) {
                        /** Put instruction steps into an array */
//                        var instruct = (jSteps[k] as JSONObject)["html_instructions"] as String
//                        instructions.add(instruct)
                        var polyline = ""
                        polyline =
                            ((jSteps[k] as JSONObject)["polyline"] as JSONObject)["points"] as String
                        val list = decodePoly(polyline)
                        /** Traversing all points  */
                        for (l in list.indices) {
                            val hm = HashMap<String, String>()
                            hm["lat"] = java.lang.Double.toString(list[l].latitude)
                            hm["lng"] = java.lang.Double.toString(list[l].longitude)
                            path.add(hm)
                        }
                    }
                    routes.add(path)
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: Exception) {
        }
        return routes
    }

    /**
     * Method to decode polyline points
     * Courtesy : https://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
     */
    private fun decodePoly(encoded: String): List<LatLng> {
        val poly: MutableList<LatLng> = ArrayList()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        return poly
    }
}