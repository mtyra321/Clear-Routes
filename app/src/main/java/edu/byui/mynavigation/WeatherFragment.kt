package edu.byui.mynavigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.LatLng
import edu.byui.mynavigation.DailyForecast
import edu.byui.mynavigation.ImageModel
import edu.byui.mynavigation.WeatherDisplayAdapter
import edu.byui.mynavigation.BuildConfig
import edu.byui.mynavigation.databinding.WeatherFragmentBinding
import edu.byui.mynavigation.R


class WeatherFragment(coords: LatLng, cityName: String) : Fragment(){


    private lateinit var binding: WeatherFragmentBinding
    private lateinit var args :String
    private var weatherDisplayAdapter: WeatherDisplayAdapter? = null
    private var imageModelArrayList: ArrayList<ImageModel>? = null

    private var theRequestQueue: RequestQueue? = null
    var label: TextView? = null
    var cityNameLabel: TextView? = null
    private var currentTempUnit: String? = null
    private val lat = coords.latitude
    private val lon = coords.longitude
    private val cityName = cityName
    private val baseUrl = "https://api.openweathermap.org/data/2.5/"
    private val apiKey = BuildConfig.WEATHER_API_KEY
    private val oneCallUrl = "https://api.openweathermap.org/data/3.0/onecall?"
    private lateinit var  listItems: ArrayList<DailyForecast>
    private var imageView: ImageView? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = WeatherFragmentBinding.inflate(layoutInflater)
        binding.convert.setOnClickListener {
            Log.i("setufsd", "onclick")
            binding.convert.text = convertTemp()
        }



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupData()

        theRequestQueue = Volley.newRequestQueue(view.context)
        getDailyResult()

    }

    private fun setupData() {
        binding.label.text = "e"
    }


//    fun LoadImageFromWebOperations(url: String?): Drawable {
//
//            val inSt: InputStream = URL(url).getContent() as InputStream
//           // Drawable.createFromStream(inSt, "src name")
//        return Drawable.createFromStream(inSt, "src name")
//    }



    fun convertTemp(): String{
        var temps = "E"
            Log.i("t", "List is not null")
            for (i in listItems) {
                if (i.degrees == "F") {
                    i.convertToCelsius()
                    temps = "C"
                } else {
                    i.convertToFahrenheit()
                    temps = "F"

                }
            }
          //  activity?.runOnUiThread(java.lang.Runnable {
                val listView = view?.findViewById<ListView>(R.id.ForecastList)

//                val adapter = view?.let { ArrayAdapter(it.context, android.R.layout.simple_list_item_1, listItems) }
//                if (listView != null) {
//                    listView.adapter = adapter
//                }
        weatherDisplayAdapter = context?.let { WeatherDisplayAdapter(it, listItems!!) }
        listView!!.adapter = weatherDisplayAdapter


        //  })

        return temps
    }

    fun getDailyResult() {
        cityNameLabel = view?.findViewById(R.id.cityName)

        cityNameLabel?.text = cityName
        val weatherUrl = oneCallUrl + "lat=${lat}&lon=${lon}&units=imperial&appid=${apiKey}"
        //val url = "https://blah.fdsa.com"
        label = view?.findViewById(R.id.label)
        label?.text = "$lat $lon"

        // Request a string response from the provided URL.
        val request = JsonObjectRequest(
            Request.Method.GET, weatherUrl, null,
            { response ->
                val jsonDailyArray = response.getJSONArray("daily")
                  listItems = ArrayList<DailyForecast>(jsonDailyArray.length())
                //    Log.i("e",  " " +jsonDailyArray.length()  )
                for (i in 0 until  jsonDailyArray.length()) {
                  //  Log.i("s", "in loop")
                    var d = DailyForecast()

                    d.max = jsonDailyArray.getJSONObject(i).getJSONObject("temp").getDouble("max")
                    d.min = jsonDailyArray.getJSONObject(i).getJSONObject("temp").getDouble("min")
                    d.dateTime = jsonDailyArray.getJSONObject(i).getLong("dt")
                    d.iconId = "icon" +jsonDailyArray.getJSONObject(i).getJSONArray("weather").getJSONObject(0).getString("icon")
                    d.degrees = "F"
                    if(d.iconId == "icon01d"){
                        d.icon = R.drawable.icon01d
                    } else if(d.iconId == "icon01n"){
                        d.icon = R.drawable.icon01n
                    } else if(d.iconId == "icon02d"){
                        d.icon = R.drawable.icon02d
                    } else if(d.iconId == "icon02n"){
                        d.icon = R.drawable.icon02n
                    } else if(d.iconId == "icon03d"){
                        d.icon = R.drawable.icon03d
                    } else if(d.iconId == "icon03n"){
                        d.icon = R.drawable.icon03n
                    } else if(d.iconId == "icon04d"){
                        d.icon = R.drawable.icon04d
                    } else if(d.iconId == "icon04n"){
                        d.icon = R.drawable.icon04n
                    } else if(d.iconId == "icon09d"){
                        d.icon = R.drawable.icon09d
                    } else if(d.iconId == "icon09n"){
                        d.icon = R.drawable.icon09n
                    } else if(d.iconId == "icon10d"){
                        d.icon = R.drawable.icon10d
                    } else if(d.iconId == "icon10n"){
                        d.icon = R.drawable.icon10n
                    } else if(d.iconId == "icon11d"){
                        d.icon = R.drawable.icon11d
                    } else if(d.iconId == "icon11n"){
                        d.icon = R.drawable.icon11n
                    } else if(d.iconId == "icon13d"){
                        d.icon = R.drawable.icon13d
                    } else if(d.iconId == "icon13n"){
                        d.icon = R.drawable.icon13n
                    } else if(d.iconId == "icon50d"){
                        d.icon = R.drawable.icon50d
                    } else if(d.iconId == "icon50n"){
                        d.icon = R.drawable.icon50n
                    }


                    d.weatherDesc = jsonDailyArray.getJSONObject(i).getJSONArray("weather").getJSONObject(0).getString("main")
                   // Log.i("esldakjshd", "List items length" + listItems.size)
                    listItems.add(d)

                }
//                val customAdapter = CustomAdapter(this, arrayList)
//                if (listView != null) {
//                    listView.setAdapter(customAdapter)
//                }
                val listView = view?.findViewById<ListView>(R.id.ForecastList)
//                val adapter = view?.let { ArrayAdapter(it.context, android.R.layout.simple_list_item_1, listItems) }
//                if (listView != null) {
//                    listView.adapter = adapter
//                }
                weatherDisplayAdapter = context?.let { WeatherDisplayAdapter(it, listItems!!) }
                listView!!.adapter = weatherDisplayAdapter
            },
            { _ ->
                //println("Oops! $error")
            }
        )
        theRequestQueue?.add(request) ?: println("Opps! Couldn't create a queue.")

    }
}
