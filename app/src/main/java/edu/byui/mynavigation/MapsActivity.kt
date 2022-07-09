package edu.byui.mynavigation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewManager
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.tabs.TabLayout
import edu.byui.mynavigation.databinding.ActivityMapsBinding
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.properties.Delegates


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, TaskLoadedCallback {
//    , GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraIdleListener {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false
//    private lateinit var currentAddress: TextView
    private var destinationLat = 0.0
    private var destinationLng = 0.0
    private var origin:  MarkerOptions? = null
    private var destination: MarkerOptions? = null
    private var originLoc = ""
    private var destinationLoc = ""
    private var originLat = 0.0
    private var originLong = 0.0
    private var currentPolyline: Polyline? = null
    // sets the departureTime to the current time
    private var departureTime = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    //if R.something doesn't work, remove the .R import
    private var numberOfLines by Delegates.notNull<Int>()
    private var destinationList = ArrayList<String>()
    private var coords : MutableList<LatLng>? = mutableListOf( LatLng(-22.82,22.20), LatLng(-33.82,33.79), LatLng(-44.82,44.79))
    var adapter = TabAdapter(supportFragmentManager)

//    var dataParser = DataParser()
//    private var distances = legDistances
//    private var stringEndPoints: List<String> = listOf("Rexburg", "Lincoln")
//    private var latLngEndPoints: List<LatLng> = listOf<LatLng>((43.826,-111.789),(40.813,-96.707))


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

//    private fun searchArea() {
//        val tfOriginLocation = findViewById<EditText>(R.id.et_name)
//        val originLocation = tfOriginLocation.text.toString().replace(" ","")
//
//        var dept_time = 1657877400
//        var dest = "Kansas City, KS"
//        var destList = mutableListOf<String>("Omaha, NE", "Nebraska City, NE", "Auburn, NE") // build from form data
//        val defaultOrigin = originLocation //LatLng(43.81418,-111.783066)
////        val defaultDestination = LatLng(43.810898,-111.77808)
////        if ((origin!!.position == null) or (destination!!.position == null))
//        FetchURL(this@MapsActivity).execute(getDirectionsUrl(defaultOrigin, dest, destList, dept_time))
//
//        // FetchURL(this@MapsActivity).execute(getDirectionsUrl(origin?.position ?: defaultOrigin,
//        //            destination?.position ?: defaultDestination
//        //        ))
//
//    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
//        currentAddress = findViewById(R.id.tvAdd)
        setUpMap()
    }

    private fun parseDestinationList() {
        var waypointStr = ""
        var urlDestStr = ""
//        var addressList= ArrayList<Address>()
        originLoc = (destinationList[0]).replace(" ","")
        destinationList.removeAt(0)
        destinationLoc = (destinationList.last()).replace(" ","")
        destinationList.removeLast()
        val urlOriginLocStr = "origin=$originLoc"
        var destStr = "&destination=$destinationLoc"
        if (destinationList.size > 0) {
            for (w in destinationList) {
                if (w != "") {
//                    val geocoder = Geocoder(applicationContext)
//                    val address = geocoder.getFromLocationName(w,1)
//                    addressList.add(address)
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
            urlDestStr = "&alternatives=true$destStr"
        }
        getDirections(urlOriginLocStr,urlDestStr)
    }


//    private fun parseDestinations(destination: String,waypointList: MutableList<String>): String {
////        var addressList: List<Address>? = null
//        var dest = destination.replace(" ","")
//        var destStr = "&destination=$dest"
//        var waypointStr = ""
//        var urlDestStr = ""
//
//        if (waypointList.size > 0) {
//            for (w in waypointList) {
//                if (w != "") {
//                    var wPoint = w.replace(" ", "")
//                    waypointStr = if (waypointStr == "") {
//                        "&waypoints=$wPoint"
//                    } else {
//                        "$waypointStr|$wPoint"
//                    }
//                }
//            }
//            urlDestStr = destStr + waypointStr
//            } else {
//                urlDestStr = "&alternatives=true$destStr"
//            }
//        return urlDestStr
//    }

//        for (d in waypointList) {
//            if (d != "") {
//                var wPoint = d.replace(" ", "")
//                // more than one destination = waypoints
//                waypointStr = if (waypointList.size > 1) {
//                    if (waypointStr == "") {
//                        "&waypoints=$wPoint"
//                    } else {
//                        "$waypointStr|$wPoint"
//                    }
//                    // only one destination = destination and set alternatives to true (can only use alternatives if no waypoints)
//                } else {
//                    "&alternatives=true&destination=$wPoint"
//                }
//            }
//        }
//        val urlDestStr = destStr + waypointStr


    // call this function when form is submitted w/ trip info
    private fun getDirections(origin: String, dest: String) {
        // if bad weather => &traffic_model=pessimistic otherwise &traffic_model=best_guess

        val deptTime = "&departure_time=$departureTime"
        // set transportation mode
        val mode = "&mode=driving"
        val strApiKey = "&key=" + BuildConfig.MAPS_API_KEY
        // parameter string
        val parameters = "$origin$dest$deptTime$mode"
        // build url
        FetchURL(this@MapsActivity).execute("https://maps.googleapis.com/maps/api/directions/json?$parameters$strApiKey")
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

    // use this tutorial, step 6 to help convert to passing in a list for multiple markers
    // https://developers.google.com/codelabs/maps-platform/maps-platform-101-android#5
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
    }

    fun makeWeatherVisible(view : View){
        if(binding.weather.visibility == View.VISIBLE){
            binding.weather.visibility = View.INVISIBLE

        }else if (binding.weather.visibility == View.INVISIBLE){
            binding.weather.visibility = View.VISIBLE
            binding.planLayout.visibility = View.INVISIBLE

        }

    }
    fun makeNewPlanVisible(view: View){
        if(binding.planLayout.visibility == View.VISIBLE){
            binding.planLayout.visibility = View.INVISIBLE

        }else if (binding.planLayout.visibility == View.INVISIBLE){
            binding.planLayout.visibility = View.VISIBLE
            binding.weather.visibility = View.INVISIBLE

        }

    }

    fun makeDirectionsVisible(view:View){

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
        setupTabLayout()
        setupViewPager()
        hideKeybord()

        //relocate map
       // map.animateCamera(CameraUpdateFactory.newLatLngZoom(, 12f))



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

}
