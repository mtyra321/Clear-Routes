package edu.byui.mynavigation

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
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
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.tabs.TabLayout
import edu.byui.mynavigation.databinding.ActivityMapsBinding
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, TaskLoadedCallback {
//    , GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraIdleListener {

    private lateinit var map: GoogleMap
    private lateinit var geocoder: Geocoder
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
    private var polyline2: Polyline? = null

    // sets the departureTime to the current time
    private var departureTime = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    private var numberOfLines by Delegates.notNull<Int>()
    private var destinationList = ArrayList<String>()
    private var coords : MutableList<LatLng>? = mutableListOf( LatLng(-22.82,22.20), LatLng(-33.82,33.79), LatLng(-44.82,44.79))
    var adapter = TabAdapter(supportFragmentManager)
    private  var directionList: MutableList<String> = mutableListOf<String>("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13")
    private val sdf = SimpleDateFormat("hh:mm")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                //   OnClickTime()
//                binding.showTime.text = time
                binding.showTime.visibility = View.VISIBLE
                binding.timePicker.visibility = View.INVISIBLE


            }else if (binding.timePicker.visibility == View.INVISIBLE){
                binding.showTime.visibility = View.INVISIBLE
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
//        var geocoder = Geocoder(applicationContext)

        for (p in locStrings) {
            locDetail = geocoder.getFromLocationName(p,1)
            var coord_lat = locDetail[0].latitude
            var coord_lng = locDetail[0].longitude
            var coords = LatLng(coord_lat,coord_lng)
            locLatLngs.add(coords)
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
        originLoc = (destinationList[0]).replace(" ","")
//        val originCoords = placeMarkerAtString(originLoc)
        destinationList.removeAt(0)
        destinationLoc = (destinationList.last()).replace(" ","")
//        placeMarkerAtString(destinationLoc)
        destinationList.removeLast()
        val urlOriginLocStr = "origin=$originLoc"
        var destStr = "&destination=$destinationLoc"
        if (destinationList.size > 0) {
            for (w in destinationList) {
                if (w != "") {
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

        val deptTime = "&departure_time=$departureTime"
        Log.i("dept time", "departure time is : ${deptTime}")
        // set transportation mode
        val mode = "&mode=driving"
        val strApiKey = "&key=" + BuildConfig.MAPS_API_KEY
        // parameter string
        val parameters = "$origin$dest$deptTime$mode"
        // build url
        var fetch = FetchURL(this@MapsActivity).execute("https://maps.googleapis.com/maps/api/directions/json?$parameters$strApiKey")
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
        val markerOptions = MarkerOptions().position(location).title(titleStr)
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
//        currentPolyline = map.addPolyline((PolylineOptions) values[0]);
        currentPolyline = map.addPolyline(values[0] as PolylineOptions)
        Log.i("polyline", "onTaskDone: $currentPolyline")
        val linePoints = currentPolyline!!.points
        placeMarkerOnMap(linePoints[0])
        placeMarkerOnMap(linePoints.last())
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(linePoints[0], 8f))
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
        Log.i("jim", "Make directions visible")
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

    private fun addSingleTab(lat: Double, long: Double, location: Boolean){
        if(coords != null) {
            coords!!.add(LatLng(lat, long))
            val i = coords?.size?.minus(1)
            binding.tabLayout.apply {

                var mFragment = WeatherFragment(coords!![i!!], "city $i")
                val mBundle = Bundle()
                mBundle.putString("mText", "e")
                mFragment.arguments = mBundle
                adapter.addFrag(mFragment, "arguments $coords[i]")
                addTab(this.newTab().setText("city $i"))

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
                for (i in 0 until binding.tabLayout.tabCount) {
                   // (binding.tabLayout.getTabAt(i)?.view as LinearLayout).visibility = View.GONE

                }
            }
        }
        setupViewPager()
    }

    private fun setupTabLayout() {

        if(binding.tabLayout.tabCount > 1) {
            for (i in binding.tabLayout.tabCount - 1 downTo 0) {
                adapter.destroyItem(binding.viewPager, i, adapter.getItem(i))
                binding.tabLayout.removeTabAt(i)
            }
            adapter.removeAllFragments()
            adapter.notifyDataSetChanged();
            setupViewPager()
        }


        binding.tabLayout.apply {

            for (i in coords?.indices!!) {
                val mFragment = WeatherFragment(coords!![i], "city $i")
                val mBundle = Bundle()
                mBundle.putString("mText", "e")
                mFragment.arguments = mBundle
                adapter.addFrag(mFragment, "arguments $coords[i]")
                //addTab(this.newTab().setCustomView(R.layout.second_fragment))
                addTab(this.newTab().setText("city $i"))
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
            //   (binding.tabLayout.getTabAt(i)?.view as LinearLayout)

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

    fun getDirectionText() {
        val instructions = DataParser().instructions
        Log.d("instruct","Instructions: $instructions")
        for (i in instructions) {
            add_direction_Line(i)
        }
//        for(i in directionList){
//            add_direction_Line(i)
//        }

    }

    private fun saveData() {
        destinationList.clear()
        // this counts the no of child layout
        // inside the parent Linear layout
        val count = binding.stops.childCount
        var v: View?

        for (i in 0 until count) {
            v = binding.stops.getChildAt(i)
            val stopName: EditText = v.findViewById(R.id.et_name)
            // val experience: Spinner = v.findViewById(R.id.exp_spinner)

            // create an object of Language class
            val location = stopName.text.toString()

            // add the data to arraylist
            destinationList.add(location)

            // stopName.text.clear()
            v.findViewById<EditText>(R.id.et_name).text.clear()

        }

        //now that plan is available, make the weather tab available
        binding.weatherBtn.visibility = View.VISIBLE
        binding.directionBtn.visibility = View.VISIBLE

                setupTabLayout()
        setupViewPager()
        hideKeybord()
        getDirectionText()

        //relocate map
       // map.animateCamera(CameraUpdateFactory.newLatLngZoom(, 12f))



    }
    fun add_direction_Line( direction:String) {

        val directionView = TextView(this)

        directionView.text = direction
        binding.directions.addView(directionView, binding.directions.childCount)

        numberOfLines++
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
            // display format of time
            val msg = "$h : $min $am_pm"
            binding.showTime.text = msg
            binding.showTime.visibility = ViewGroup.VISIBLE
            binding.showTime.visibility = ViewGroup.VISIBLE
            sdf.timeZone = TimeZone.getTimeZone("UTC")

            val simpleDateFormat = SimpleDateFormat("yyyy-MMM-dd HH:mm:ss")
            simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val localDateFormat = SimpleDateFormat("yyyy-MMM-dd HH:mm:ss")
            val unix = localDateFormat.parse(simpleDateFormat.format(Date()))


            unix.hours = hour
            if(am_pm == "PM"){
                unix.hours += 12
            }
            Log.i("e", "hour is : ${unix.hours}")

            unix.minutes = minute
            Log.i("e", "gmt: ${unix.time}")
            departureTime = unix.time
        }
    }
}
