package edu.byui.mynavigation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.tabs.TabLayout
import edu.byui.mynavigation.databinding.ActivityMapsBinding
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.properties.Delegates


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, TaskLoadedCallback {
//    , GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraIdleListener {

    private lateinit var map: GoogleMap
    private lateinit var geocoder: Geocoder
    private var theRequestQueue: RequestQueue? = null

    //    private var pp = PointsParser()
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false
    private var originLoc = ""
    private var destinationLoc = ""
    private var originLat = 0.0
    private var originLong = 0.0
    private var currentPolyline: Polyline? = null

    // sets the departureTime to the current time
    private var departureTime = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    private var numberOfLines by Delegates.notNull<Int>()
    private var destinationList = ArrayList<String>()
    private lateinit var coords : MutableList<LatLng>
    var adapter = TabAdapter(supportFragmentManager)
    private  var directionList: MutableList<String> = mutableListOf<String>("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13")
    private val sdf = SimpleDateFormat("hh:mm")
    private val baseUrl = "https://api.openweathermap.org/data/2.5/weather?"
    private var totalTimeOfTrip = 0
    private  var unix: Date = Date()

    private var depart: Pair<Int, Int> = Pair(unix.hours,unix.minutes)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        theRequestQueue = Volley.newRequestQueue(this)

        //setContentView(R.layout.activity_maps)
        setContentView(binding.root)
        numberOfLines = 0
        setUpDestinations()
        binding.addLineBtn.setOnClickListener {
            Add_Line()
        }
        binding.getData.setOnClickListener {
            saveData()
            makeNewPlanVisible(it)
            parseDestinationList() // separates out locations, calls url creator, creates api call
        }
        binding.buttonDate.setOnClickListener{
            if(binding.timePicker.visibility == View.VISIBLE){
           //     binding.showTime.visibility = View.VISIBLE
                binding.timePicker.visibility = View.INVISIBLE


            }else if (binding.timePicker.visibility == View.INVISIBLE){
           //     binding.showTime.visibility = View.INVISIBLE
                binding.timePicker.visibility = View.VISIBLE

            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // update lastLocation with new location & update map
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation!!
                placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
            }
        }
        createLocationRequest()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        geocoder = Geocoder(this)
        setUpMap()
    }


    private fun getLocCoords(locStrings: ArrayList<String>): ArrayList<LatLng> {
        // accepts a list of string locations, returns a list of LatLng values
        var locDetail: MutableList<Address>?
        var locLatLngs: ArrayList<LatLng> = ArrayList<LatLng>()
        destinationList.add(0 , "current location")
        locLatLngs.add(0, LatLng(lastLocation.latitude, lastLocation.longitude))
        for (p in locStrings) {
            if(p != "current location") {
                locDetail = geocoder.getFromLocationName(p, 1)
                val coord_lat = locDetail[0].latitude
                val coord_lng = locDetail[0].longitude
                val coords = LatLng(coord_lat, coord_lng)
                locLatLngs.add(coords)
            }
        }
        return locLatLngs
    }

//    private fun originDestMarkers() {
//        pp.points[0]?.let { placeMarkerOnMap(it) }
//        pp.points.last()?.let { placeMarkerOnMap(it) }
//    }
//    private fun placeMarkerAtString(locString: String): LatLng {
//        // accepts a list of string locations, returns a list of LatLng values
//        var locDetail: MutableList<Address>?
//
//        locDetail = geocoder.getFromLocationName(locString,1)
//        var coord_lat = locDetail[0].latitude
//        var coord_lng = locDetail[0].longitude
//        var coord = LatLng(coord_lat,coord_lng)
//        placeMarkerOnMap(coord)
//        return coord
//    }

    private fun parseDestinationList() {
        var waypointStr = ""
        var urlDestStr = ""
//        var addressList= ArrayList<Address>()
        originLoc = (destinationList[1]).replace(" ","")
//        val originCoords = placeMarkerAtString(originLoc)
        destinationList.removeAt(1)
        destinationLoc = (destinationList.last()).replace(" ","")
//        placeMarkerAtString(destinationLoc)
        destinationList.removeLast()
        val urlOriginLocStr = "origin=$originLoc"
        var destStr = "&destination=$destinationLoc"
        if (destinationList.size > 1) {
            for (w in destinationList) {
                if (w != "current location") {
                    var wPoint = w.replace(" ", "")
                    waypointStr = if (waypointStr == "") {
                        "&waypoints=$wPoint"
                    } else {
                        "$waypointStr|$wPoint"
                    }
                }
            }
            urlDestStr = destStr + waypointStr
        } else {
            urlDestStr = destStr
//            urlDestStr = "&alternatives=true$destStr"
        }
        getDirections(urlOriginLocStr,urlDestStr)
//        map.animateCamera(CameraUpdateFactory.newLatLngZoom(originCoords, 8f))

    }

    // call this function when form is submitted w/ trip info
    private fun getDirections(origin: String, dest: String) {
        // if bad weather => &traffic_model=pessimistic otherwise &traffic_model=best_guess
        totalTimeOfTrip = 0
        val deptTime = "&departure_time=$departureTime"
        // set transportation mode
        val mode = "&mode=driving"
        val strApiKey = "&key=" + BuildConfig.MAPS_API_KEY
        // parameter string
        val parameters = "$origin$dest$deptTime$mode"
        // build url
        var fetch = FetchURL(this@MapsActivity).execute("https://maps.googleapis.com/maps/api/directions/json?$parameters$strApiKey")

        val j = JSONObject(fetch.get())
        var legs = (j.getJSONArray("routes")[0] as JSONObject ).getJSONArray("legs")
        for(i in 0 until legs.length()) {
            var steps = legs.getJSONObject(i).getJSONArray("steps")
            var legTime = legs.getJSONObject(i).getJSONObject("duration").getInt("value")
            totalTimeOfTrip += legTime
            for (j in 0 until steps.length()) {
                var stepInstructions = steps.getJSONObject(j).getString("html_instructions")

                add_direction_Line(html2text(stepInstructions))
            }
        }
        var hoursLength = totalTimeOfTrip / 3600
        var minutesLength = (totalTimeOfTrip % 3600) / 60
        binding.totalTime.text = "Total Time: ${hoursLength}:${minutesLength}"
        var hours = depart.first + hoursLength
        var minutes = depart.second + minutesLength
        if(minutes >= 60){
            minutes -= 60
            hours ++
        }
        var arriveAmPm = "AM"
        while (hours > 24) {
            hours -= 24
        }

        if(hours  == 12){
            arriveAmPm = "PM"
        }
        else if (hours > 12){

            hours -=12
            arriveAmPm = "PM"
        }
         binding.arriveTime.text = "Arrival Time: "+ String.format("%02d:%02d ${arriveAmPm}", hours, minutes)
        var departAmPm = "AM"
        var departHours = depart.first
        var departMinutes = depart.second
        if(departHours  == 12){
            departAmPm = "PM"
        }
        else if (departHours > 12){
            departHours -=12
            departAmPm = "PM"
        }
        binding.departTime.text = "Departure Time: "+ String.format("%02d:%02d ${departAmPm}", departHours, departMinutes)

    }

    @SuppressLint("MissingPermission")
    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // enables my-location layer
            map.isMyLocationEnabled = true
            // gets most recent location available
            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                // to be safe, ensure not null, move to user's location
                if (location != null) {
                    lastLocation = location
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    placeMarkerOnMap(currentLatLng) // change parameter to origin or destination
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))

                    originLat = location.latitude
                    originLong = location.longitude
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
    }

    private fun placeMarkerOnMap(location: LatLng) {
        val titleStr = getAddress(location)
        val markerOptions = MarkerOptions().position(location).title(titleStr).snippet("Departure Time: ")
        map.addMarker(markerOptions)?.showInfoWindow()
    }

    private fun placeCustomMarkerOnMap(location: LatLng, iconId: Int) {
        val titleStr = getAddress(location)
        val markerOptions = MarkerOptions().position(location).title(titleStr).icon(BitmapDescriptorFactory.fromResource(iconId))
        map.addMarker(markerOptions)

    }

    private fun getAddress(latLng: LatLng): String {
        // 1
        val geocoder = Geocoder(this)
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""

        try {
            // 2
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            // 3
            if (null != addresses && !addresses.isEmpty()) {
                address = addresses[0]
                for (i in 0 until address.maxAddressLineIndex) {
                    addressText += if (i == 0) address.getAddressLine(i) else "\n" + address.getAddressLine(
                        i
                    )
                }
            }
        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }

        return addressText
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = Priority.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        // check location settings
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        // if success initiate location request
        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            // failure asks user to enable location services
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(
                        this@MapsActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }


    // start update request if result_OK
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
    }
    //  stop location update request
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // restart location update request
    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }

    override fun onTaskDone(vararg values: Any?) {
        if (currentPolyline != null) {
            currentPolyline!!.remove()
        }
        currentPolyline = map.addPolyline(values[0] as PolylineOptions)
        val linePoints = currentPolyline!!.points
        placeMarkerOnMap(linePoints[0])
        placeMarkerOnMap(linePoints.last())
        createIconMapMarkers(linePoints)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(linePoints[0], 8f))
    }

     fun createIconMapMarkers(linePoints: MutableList<LatLng>) {
        getWeatherRouteIcon(linePoints[(linePoints.size*.75).toInt()])
        getWeatherRouteIcon(linePoints[(linePoints.size*.50).toInt()])
        getWeatherRouteIcon(linePoints[(linePoints.size*.25).toInt()])
}

    fun makeWeatherVisible(view : View){
        if(binding.weather.visibility == View.VISIBLE){
            binding.weather.visibility = View.INVISIBLE

        }else if (binding.weather.visibility == View.INVISIBLE){
            binding.weather.visibility = View.VISIBLE
            binding.planLayout.visibility = View.INVISIBLE
            binding.directionsLayout.visibility = View.INVISIBLE
        }

    }
    fun makeNewPlanVisible(view: View){
        if(binding.planLayout.visibility == View.VISIBLE){
            binding.planLayout.visibility = View.INVISIBLE

        }else if (binding.planLayout.visibility == View.INVISIBLE){
            map.clear()
            binding.planLayout.visibility = View.VISIBLE
            binding.weather.visibility = View.INVISIBLE
            OnClickTime()
            binding.directionsLayout.visibility = View.INVISIBLE
        }
    }

    fun makeDirectionsVisible(view:View){
        if(binding.directionsLayout.visibility == View.VISIBLE){
            binding.directionsLayout.visibility = View.INVISIBLE

        }else if (binding.directionsLayout.visibility == View.INVISIBLE){
            binding.directionsLayout.visibility = View.VISIBLE
            binding.planLayout.visibility = View.INVISIBLE
            binding.weather.visibility = View.INVISIBLE

        }
    }

    private fun setupViewPager() {
        this.binding.viewPager.setAdapter(adapter);

    }

    private fun setupTabLayout() {

        if(binding.tabLayout.tabCount > 1) {
            for (i in binding.tabLayout.tabCount - 1 downTo 1) {
                adapter.destroyItem(binding.viewPager, i, adapter.getItem(i))
                binding.tabLayout.removeTabAt(i)
            }
            adapter.removeAllFragments()
            adapter.notifyDataSetChanged();
            setupViewPager()
        }

        binding.tabLayout.apply {
            for (i in coords.indices) {
                val mFragment = WeatherFragment(coords[i], destinationList[i])
                val mBundle = Bundle()
               // mBundle.putString("mText", "e")
                mFragment.arguments = mBundle
                adapter.addFrag(mFragment, "arguments $coords[i]")
                addTab(this.newTab().setText(destinationList[i]))
            }

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.position?.let {
                        binding.viewPager.currentItem = it
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                }
            })
        }
        for (i in 0 until binding.tabLayout.tabCount) {
            (binding.tabLayout.getTabAt(i)?.view as LinearLayout).visibility = View.GONE
        }
    }

    fun setUpDestinations(){
        val inflater = LayoutInflater.from(this).inflate(R.layout.row_stop, null)
        inflater.findViewById<EditText>(R.id.et_name).hint = "Enter Origin"
        inflater.findViewById<Button>(R.id.xBtn).visibility = View.INVISIBLE

        binding.stops.addView(inflater, binding.stops.childCount)
        val inflater2 = LayoutInflater.from(this).inflate(R.layout.row_stop, null)
        inflater2.findViewById<EditText>(R.id.et_name).hint = "Enter Destination"
        inflater2.findViewById<Button>(R.id.xBtn).visibility = View.INVISIBLE
        binding.stops.addView(inflater2, binding.stops.childCount)
        numberOfLines+=2
    }

    fun Add_Line() {

        val inflater = LayoutInflater.from(this).inflate(R.layout.row_stop, null)
        inflater.findViewById<Button>(R.id.xBtn).setOnClickListener {
            deleteLine(inflater)
        }
        binding.stops.addView(inflater, binding.stops.childCount-1)
        numberOfLines++
    }

    fun deleteLine(view:View){
        (view.getParent() as ViewManager).removeView(view)
    }


    private fun saveData() {
        destinationList.clear()
        binding.directions.removeAllViews()
        val count = binding.stops.childCount
        var v: View?
        for (i in 0 until count) {
            v = binding.stops.getChildAt(i)
            val stopName: EditText = v.findViewById(R.id.et_name)
            val location = stopName.text.toString()
            destinationList.add(location)
            v.findViewById<EditText>(R.id.et_name).text.clear()
        }
        binding.weatherBtn.visibility = View.VISIBLE
        binding.directionBtn.visibility = View.VISIBLE
        coords = getLocCoords(destinationList)

        setupTabLayout()
        setupViewPager()
        hideKeybord()
    }
    fun add_direction_Line( direction:String) {
        val directionView = TextView(this)

        directionView.text = direction
        directionView.setTextColor(resources.getColor( R.color.white))
        directionView.setTextSize(20F)
        binding.directions.addView(directionView, binding.directions.childCount)

    }
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2

    }
    fun hideKeybord() {
        val view = this.currentFocus
        if (view != null) {
            val hideKey = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            hideKey.hideSoftInputFromWindow(view.windowToken, 0)
        } else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun OnClickTime() {

        binding.timePicker.setOnTimeChangedListener { _, hour, minute -> var hour = hour
            var am_pm = ""
            depart = Pair(hour,minute)

            // AM_PM decider logic
            when {hour == 0 -> { hour += 12
                am_pm = "AM"
            }
                hour == 12 -> am_pm = "PM"
                hour > 12 -> { hour -= 12
                    am_pm = "PM"
                }
                else -> am_pm = "AM"
            }

            val h = if (hour < 10) "0$hour" else hour
            val min = if (minute < 10) "0$minute" else minute

            sdf.timeZone = TimeZone.getTimeZone("UTC")

            val simpleDateFormat = SimpleDateFormat("yyyy-MMM-dd HH:mm:ss")
            simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val localDateFormat = SimpleDateFormat("yyyy-MMM-dd HH:mm:ss")
            unix = localDateFormat.parse(simpleDateFormat.format(Date()))

            unix.hours = hour
            if(am_pm == "PM"){
                unix.hours += 12
            }
            unix.minutes = minute
            departureTime = unix.time
            binding.departTime.text = "Departure Time: "+ String.format("%02d:%02d ${am_pm}", hour, minute)
        }
    }
    fun getWeatherRouteIcon(coord: LatLng){
        val weatherUrl = baseUrl + "lat=${coord.latitude}&lon=${coord.longitude}&units=imperial&appid=${BuildConfig.WEATHER_API_KEY}"
        var icon = 0

        // Request a string response from the provided URL.
        val request = JsonObjectRequest(
            Request.Method.GET, weatherUrl, null,
            { response ->
                val jsonDailyArray = response.getJSONArray("weather")
                for (i in 0 until  jsonDailyArray.length()) {

                    val iconId = "icon" +jsonDailyArray.getJSONObject(0).getString("icon")
                    if(iconId == "icon01d"){
                        icon = R.drawable.icon01d
                    } else if(iconId == "icon01n"){
                        icon = R.drawable.icon01n
                    } else if(iconId == "icon02d"){
                        icon = R.drawable.icon02d
                    } else if(iconId == "icon02n"){
                        icon = R.drawable.icon02n
                    } else if(iconId == "icon03d"){
                        icon = R.drawable.icon03d
                    } else if(iconId == "icon03n"){
                        icon = R.drawable.icon03n
                    } else if(iconId == "icon04d"){
                        icon = R.drawable.icon04d
                    } else if(iconId == "icon04n"){
                        icon = R.drawable.icon04n
                    } else if(iconId == "icon09d"){
                        icon = R.drawable.icon09d
                    } else if(iconId == "icon09n"){
                        icon = R.drawable.icon09n
                    } else if(iconId == "icon10d"){
                        icon = R.drawable.icon10d
                    } else if(iconId == "icon10n"){
                        icon = R.drawable.icon10n
                    } else if(iconId == "icon11d"){
                        icon = R.drawable.icon11d
                    } else if(iconId == "icon11n"){
                        icon = R.drawable.icon11n
                    } else if(iconId == "icon13d"){
                        icon = R.drawable.icon13d
                    } else if(iconId == "icon13n"){
                        icon = R.drawable.icon13n
                    } else if(iconId == "icon50d"){
                        icon = R.drawable.icon50d
                    } else if(iconId == "icon50n"){
                        icon = R.drawable.icon50n
                    }
                    placeCustomMarkerOnMap(coord, icon)
                   }

            },
            { _ ->
                //println("Oops! $error")
            }
        )
        theRequestQueue?.add(request) ?: println("Opps! Couldn't create a queue.")
    }

    fun html2text(html: String): String {
        return Html.fromHtml(html).toString()

    }

}


