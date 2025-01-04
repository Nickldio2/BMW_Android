package com.example.mapboxtest

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.maps.model.LatLng
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapLoadedCallback
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck3D
import com.mapbox.maps.plugin.ModelScaleMode
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.api.RoutesRenderedCallback
import com.mapbox.navigation.ui.maps.route.line.api.RoutesRenderedResult
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources

@OptIn(MapboxExperimental::class, ExperimentalPreviewMapboxNavigationAPI::class)
@SuppressLint("MissingPermission", "ClickableViewAccessibility")
class MapFragment : Fragment(R.layout.map_fragment) {

    lateinit var mapView: MapView
    private lateinit var blackScreen: View
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    lateinit var routeLineApi: MapboxRouteLineApi
    lateinit var routeLineView: MapboxRouteLineView
    private lateinit var routeLineApiOptions: MapboxRouteLineApiOptions
    private lateinit var routeLineViewOptions: MapboxRouteLineViewOptions
    private lateinit var routeLineColorResources: RouteLineColorResources
    lateinit var routeArrow: MapboxRouteArrowApi
    private lateinit var routeArrowOptions: RouteArrowOptions
    lateinit var routeArrowView: MapboxRouteArrowView
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var cameraState: String
    private lateinit var navigationObservers: NavigationObservers
    private lateinit var navigationUI: NavigationUI
    private lateinit var locationObserver: CustomLocationObserver
    private lateinit var lastLocation: Location
    private var selectedDestinationName: String? = null
    private var selectedDestinationAddress: String? = null
    private var selectedDestinationPoint: Point? = null
    var currentDestinationName: String? = null
    var currentDestinationAddress: String? = null
    var currentDestinationPoint: Point? = null
    private var pointAnnotation: PointAnnotation? = null
    private var isMapPrepared = false
    private var defaultZoom: Double = 18.7
    private var defaultPitch: Double = 25.0
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private var quickStart = true
    private var mapReady = false
    private lateinit var btnRecenter: ImageButton
    private lateinit var btnStartNavigation: Button
    private lateinit var btnCancel: Button
    private lateinit var btnStartNavToMarker: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBarCardView: CardView
    private lateinit var touchListener: View.OnTouchListener
    private lateinit var customPuck3D: LocationPuck3D
    private var progressAnimator: ObjectAnimator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var startAnimationRunnable: Runnable? = null
    private var isAnimationCanceled = false
    private lateinit var navigationLocationProvider: NavigationLocationProvider
    private lateinit var userSpeed: TextView
    lateinit var mapStyle: Style
    private var isAnimationStarted = false

    private val requestLocationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {

            MapboxNavigationProvider.retrieve().registerLocationObserver(locationObserver.locationObserver)
            MapboxNavigationProvider.retrieve().registerArrivalObserver(navigationObservers.arrivalObserver)
            mapView.location.addOnIndicatorPositionChangedListener(navigationObservers.onPositionChangedListener)
            Log.e("MapboxLog: MapFragment", "Starting empty trip session")
            MapboxNavigationProvider.retrieve().startTripSession()
            setupNavigationCameraAndViewport()

        }
    }

    fun handleNewLocation(location: Location, speed: Double, keyPoints: List<Location>) {

        if (keyPoints.isEmpty()) {

            navigationLocationProvider.changePosition(
                location = location
            )

        }else{

            navigationLocationProvider.changePosition(
                location = location,
                keyPoints = keyPoints
            )

        }

        val speedKmh = (speed.times(3.6)).toInt()
        var newPitch = defaultPitch + speed

        if (newPitch > 58.0) {
            newPitch = 58.0
        }

        userSpeed.text = getString(R.string.speed_format, speedKmh)

        lastLocation = location

        val latLng = LatLng(location.latitude, location.longitude)
        sharedViewModel.updateCurrentLocation(latLng)

        viewportDataSource.onLocationChanged(location)
        viewportDataSource.followingZoomPropertyOverride(defaultZoom - (speed * 0.015))
        viewportDataSource.followingPitchPropertyOverride(newPitch)
        viewportDataSource.evaluate()

        if (!isMapPrepared) {
            isMapPrepared = true
            prepareMapUnderBlackScreen()
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.map_fragment, container, false)

        if (!MapboxNavigationApp.isSetup()) {
            MapboxNavigationApp.setup {
                NavigationOptions.Builder(requireContext())
                    .build()
            }
            MapboxNavigationApp.attach(this)
        }

        return view
    }

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapInitOptions = MapInitOptions(
            context = requireContext(),
            antialiasingSampleCount = 4 // Set the antialiasing sample count
        )

        mapView = MapView(requireContext(), mapInitOptions).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        navigationObservers = NavigationObservers(mapFragment = this)
        navigationUI = parentFragmentManager.findFragmentById(R.id.fragment_directions_container) as NavigationUI

        locationObserver = CustomLocationObserver(this)

        val container = view.findViewById<ViewGroup>(R.id.mapContainer)
        container.addView(mapView)

        mapView.setMaximumFps(60)

        blackScreen = activity?.findViewById(R.id.blackScreen)!!
        userSpeed = view.findViewById(R.id.userSpeed)

        routeLineColorResources = RouteLineColorResources.Builder()
            .routeLineTraveledColor(Color.parseColor("#546A76"))
            .build()

        routeLineApiOptions = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .build()

        routeLineApi = MapboxRouteLineApi(routeLineApiOptions)

        routeLineViewOptions = MapboxRouteLineViewOptions.Builder(requireContext())
            .routeLineColorResources(routeLineColorResources)
            .routeLineBelowLayerId("road-label-navigation")
            .displaySoftGradientForTraffic(true)
            .build()

        routeLineView = MapboxRouteLineView(routeLineViewOptions)

        routeArrow = MapboxRouteArrowApi()
        routeArrowOptions = RouteArrowOptions.Builder(requireContext()).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)

        pointAnnotationManager = mapView.annotations.createPointAnnotationManager()

        // Disable scale bar, logo and attribution
        mapView.scalebar.enabled = false
        mapView.compass.enabled = false
        mapView.logo.enabled = false
        mapView.attribution.enabled = false

        mapView.location.puckBearingEnabled = true

        mapView.gestures.doubleTouchToZoomOutEnabled = false
        mapView.gestures.doubleTapToZoomInEnabled = false
        mapView.gestures.scrollDecelerationEnabled = false
        mapView.gestures.quickZoomEnabled = false

        disableMapInteractions()

        btnRecenter = view.findViewById(R.id.btnRecenter)
        btnStartNavigation = view.findViewById(R.id.btnStartNavigation)
        btnCancel = view.findViewById(R.id.btnResetNavigation)
        btnStartNavToMarker = view.findViewById(R.id.btnStartNavToMarker)
        progressBarCardView = view.findViewById(R.id.zoomProgressBarCardView)
        progressBar = view.findViewById(R.id.zoomProgressBar)
        progressBar.max = 500
        progressBar.progress = 0

        touchListener = View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isAnimationCanceled) {
                        Log.e("MapboxLog: MapFragment", "Canceling animation")
                        isAnimationCanceled = true
                        cancelDelayProgressBarAnimation {
                            Log.d("MapboxLog: MapFragment", "short click")
                            CustomAnimation().animateIcons(requireContext())
                        }
                        cancelProgressBarAnimation()
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    Log.e("MapboxLog: MapFragment", "Touch detected")
                    isAnimationCanceled = false
                    delayProgressBarAnimation()
                }
            }
            false
        }

        val styleCustom = "mapbox://styles/nickldio/clxhczg2t007m01qwctc7d8gv"

        mapView.mapboxMap.loadStyle(styleCustom){ style ->
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.red_marker)
            style.addImage("red_marker", bitmap)
            mapStyle = style
        }

        navigationLocationProvider = NavigationLocationProvider()

        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)

    }

    private fun registerInputListeners() {

        mapView.setOnTouchListener(touchListener)

        val mapLoadedCallback = MapLoadedCallback {
           Log.e("MapboxLog: MapFragment", "Map loaded")
        }

        mapView.mapboxMap.subscribeMapLoaded(mapLoadedCallback)

        mapView.gestures.addOnMapClickListener { point ->
            sharedViewModel.clearFocus()
            if (cameraState == "IDLE") {
                if (pointAnnotation == null) {
                    val pointAnnotationOptions = PointAnnotationOptions()
                        .withPoint(point)
                        .withIconImage("red_marker")
                        .withIconSize(0.5)
                        .withIconOffset(listOf(0.0, -34.0)) //offset on y axis to better visually align icon shape with actual clicked location
                    pointAnnotation = pointAnnotationManager.create(pointAnnotationOptions)
                } else {
                    val annotationCardView = requireView().findViewById<CardView>(R.id.previewCardView)
                    activity?.runOnUiThread {
                        annotationCardView.visibility = GONE // Make the Annotation CardView visible
                        pointAnnotation?.let {
                            it.point = point
                            pointAnnotationManager.update(it)
                        }
                    }
                }
                val cameraOptions = CameraOptions.Builder()
                    .center(point)
                    .padding(EdgeInsets(80.0,80.0,0.0,80.0))
                    .zoom(17.0)
                    .pitch(0.0)
                    .build()
                val animationOptions = MapAnimationOptions.mapAnimationOptions {
                    duration(1000) // Duration of the animation in milliseconds
                }
                mapView.mapboxMap.flyTo(
                    cameraOptions = cameraOptions,
                    animationOptions = animationOptions
                )
                clearRoute()
                btnStartNavToMarker.visibility = VISIBLE
                true
            } else {
                false
            }
        }

        pointAnnotationManager.addClickListener { annotation ->
            if (annotation == pointAnnotation) {
                pointAnnotationManager.delete(annotation)
                pointAnnotation = null
                clearRoute()
                btnStartNavToMarker.visibility = View.INVISIBLE
                true
            } else {
                false
            }
        }

        btnRecenter.setOnClickListener {
            viewportDataSource.followingZoomPropertyOverride(defaultZoom)
            viewportDataSource.followingPitchPropertyOverride(defaultPitch)
            navigationCamera.requestNavigationCameraToFollowing()
        }

        btnStartNavigation.setOnClickListener {
            startNavigation()
            sharedViewModel.clearFocus()

        }

        btnCancel.setOnClickListener {
            MapboxNavigationProvider.retrieve().unregisterRoutesObserver(navigationObservers.routesObserver)
            MapboxNavigationProvider.retrieve().unregisterRouteProgressObserver(navigationUI.routeProgressObserver)
            clearRoute()
        }

        btnStartNavToMarker.setOnClickListener {
            val userLocationPoint = Point.fromLngLat(lastLocation.longitude, lastLocation.latitude)
            fetchRoute(userLocationPoint, pointAnnotation!!.point)
            btnStartNavToMarker.visibility = View.INVISIBLE
        }

        sharedViewModel.selectedLocation.observe(viewLifecycleOwner) { suggestionLatLng ->
            lastLocation.let { location ->
                val userLocationPoint = Point.fromLngLat(location.longitude, location.latitude)
                val suggestionPoint = Point.fromLngLat(suggestionLatLng.longitude, suggestionLatLng.latitude)
                selectedDestinationPoint = suggestionPoint
                fetchRoute(userLocationPoint, suggestionPoint)
                pointAnnotationManager.delete(pointAnnotationManager.annotations)
                pointAnnotation = null
            }
        }

        sharedViewModel.selectedLocationName.observe(viewLifecycleOwner) {
            selectedDestinationName = it
        }

        sharedViewModel.selectedAddress.observe(viewLifecycleOwner) {
            selectedDestinationAddress = it
        }

    }

    private fun delayProgressBarAnimation() {
        isAnimationStarted = false
        startAnimationRunnable = Runnable {
            isAnimationStarted = true
            startProgressBarAnimation {
                Log.e("MapboxLog: MapFragment", "Long touch detected")
                if (cameraState == "FOLLOWING") {
                    val stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                        .maxDuration(600L)
                        .build()
                    val frameTransitionOptions = NavigationCameraTransitionOptions.Builder()
                        .maxDuration(600L)
                        .build()
                    navigationCamera.requestNavigationCameraToOverview(
                        stateTransitionOptions,
                        frameTransitionOptions
                    )
                    mapView.setOnTouchListener(null)
                }
            }
        }
        handler.postDelayed(startAnimationRunnable!!, 200)
    }


    private fun cancelDelayProgressBarAnimation(onCancel: (() -> Unit)? = null) {
        if (!isAnimationStarted) {
            startAnimationRunnable?.let { handler.removeCallbacks(it) }
            startAnimationRunnable = null
            onCancel?.invoke()
        }
    }

    private fun startProgressBarAnimation(callback: () -> Unit) {
        Log.e("MapboxLog: MapFragment", "Starting progress bar")
        showProgressBar()
        progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 500).apply {
            duration = 500
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isAnimationCanceled) {
                        Log.e("MapboxLog: MapFragment", "Animation finished")
                        callback()
                        hideProgressBar()
                    }
                }
                override fun onAnimationCancel(animation: Animator) {
                    Log.e("MapboxLog: MapFragment", "Animation canceled")
                    hideProgressBar()
                }
            })
            start()
        }
    }

    private fun cancelProgressBarAnimation() {
        progressAnimator?.cancel()
    }

    private fun showProgressBar() {
        val finalY = 32f * resources.displayMetrics.density
        val animator = ObjectAnimator.ofFloat(progressBarCardView, "y", finalY)
        animator.duration = 200
        animator.start()
    }

    private fun hideProgressBar() {
        val finalY = -32f * resources.displayMetrics.density
        val animator = ObjectAnimator.ofFloat(progressBarCardView, "y", finalY)
        animator.duration = 200
        animator.start()
    }

    private fun animateCardViewShrink() {

        val mainCardView = requireActivity().findViewById<CardView>(R.id.CardView_main)
        val topCardView = requireActivity().findViewById<FrameLayout>(R.id.CardView_top)
        val bottomCardView = requireActivity().findViewById<CardView>(R.id.CardView_bottom)

        // Measure the width of the screen
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // Calculate the new width for the map card view (70% of the screen width)
        val newMapWidth = (screenWidth * 70) / 100

        // Create an animation to change the layout parameters of the map card view
        val animation = ValueAnimator.ofInt(mainCardView.width, newMapWidth)
        animation.addUpdateListener { animator ->
            val layoutParams = mainCardView.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.width = animator.animatedValue as Int
            mainCardView.layoutParams = layoutParams
        }

        animation.duration = 750 // duration of the animation in milliseconds
        animation.start()

        // Slide in the directions and media card views from the right
        topCardView.translationX = topCardView.width.toFloat()
        bottomCardView.translationX = bottomCardView.width.toFloat()

        topCardView.animate()
            .translationX(0f)
            .setDuration(750)
            .start()

        bottomCardView.animate()
            .translationX(0f)
            .setDuration(750)
            .start()

    }

    private fun overviewRoute(route: NavigationRoute) {
        viewportDataSource.onRouteChanged(route)
        viewportDataSource.clearOverviewOverrides()
        viewportDataSource.overviewPadding = EdgeInsets(100.0, 100.0, 100.0, 100.0) // Adjust this padding as needed
        viewportDataSource.evaluate()
        navigationCamera.requestNavigationCameraToOverview()
    }

    private val routesRenderedCallback = object : RoutesRenderedCallback {
        override fun onRoutesRendered(result: RoutesRenderedResult) {
            Log.e("MapboxLog: MapFragment", "Route rendered")
        }
    }

    private fun setupNavigationCameraAndViewport() {

        viewportDataSource = MapboxNavigationViewportDataSource(mapView.mapboxMap)

        // Initialize the navigation camera
        navigationCamera = NavigationCamera(
            mapView.mapboxMap,
            mapView.camera,
            viewportDataSource
        )

        navigationCamera.registerNavigationCameraStateChangeObserver(navigationObservers.navigationCameraStateObserver)

        viewportDataSource.options.overviewFrameOptions.pitchUpdatesAllowed = true
        viewportDataSource.options.overviewFrameOptions.bearingUpdatesAllowed = true
        viewportDataSource.options.followingFrameOptions.pitchUpdatesAllowed = true
        viewportDataSource.options.followingFrameOptions.zoomUpdatesAllowed = true
        viewportDataSource.followingPadding = EdgeInsets(0.0, 0.0, 200.0, 0.0)
        viewportDataSource.followingZoomPropertyOverride(defaultZoom)
        viewportDataSource.followingPitchPropertyOverride(defaultPitch)
        viewportDataSource.overviewPitchPropertyOverride(0.0)
        viewportDataSource.overviewZoomPropertyOverride(16.0)

        navigationCamera.requestNavigationCameraToIdle()

    }

    private fun fetchRoute(origin: Point, destination: Point) {

        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(listOf(origin, destination))
            .build()

        MapboxNavigationProvider.retrieve().requestRoutes(
            routeOptions,
            object : NavigationRouterCallback {
                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                    if (routes.isNotEmpty()) {
                        val firstRoute = routes.first()
                        val durationInSeconds = firstRoute.directionsRoute.duration()
                        val distanceInMeters = firstRoute.directionsRoute.distance()
                        val routeLines = routes.map { NavigationRouteLine(it, null) }
                        MapboxNavigationProvider.retrieve().setNavigationRoutes(routes)
                        Log.e("MapboxLog: MapFragment", "Setting route lines")
                        routeLineApi.setNavigationRouteLines(routeLines) { result ->
                            mapView.mapboxMap.getStyle { style ->
                                routeLineView.renderRouteDrawData(style, result,
                                    mapView.mapboxMap, routesRenderedCallback)
                                displayRouteDetails(durationInSeconds, distanceInMeters)
                                overviewRoute(firstRoute)
                            }
                        }
                    }
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    // Handle failure
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                    // Handle cancellation
                }
            }
        )
    }

    private fun displayRouteDetails(durationInSeconds: Double, distanceInMeters: Double?) {
        val durationInMinutes = (durationInSeconds / 60).toInt()
        val distanceInKilometers = distanceInMeters?.div(1000.0)?.let { Math.round(it).toInt() }

        val durationText = getString(R.string.duration_preview, durationInMinutes)
        val distanceText = getString(R.string.distance_preview, distanceInKilometers ?: 0)

        val annotationCardView = view?.findViewById<CardView>(R.id.previewCardView)
        val durationTextView = view?.findViewById<TextView>(R.id.tvRouteDuration)
        val distanceTextView = view?.findViewById<TextView>(R.id.tvRouteDistance)
        activity?.runOnUiThread {
            durationTextView?.text = durationText
            distanceTextView?.text = distanceText
            annotationCardView?.visibility = VISIBLE // Make the CardView visible
        }
    }

    private fun switchCardViewContent(navigating: Boolean) {
        if (navigating) {
            CustomAnimation().switchContent(requireContext()) {
                MapboxNavigationProvider.retrieve().registerRoutesObserver(navigationObservers.routesObserver)
                MapboxNavigationProvider.retrieve().registerRouteProgressObserver(navigationUI.routeProgressObserver)
            }
        } else {
            CustomAnimation().switchContent(requireContext()) {
                MapboxNavigationProvider.retrieve().unregisterRoutesObserver(navigationObservers.routesObserver)
                MapboxNavigationProvider.retrieve().unregisterRouteProgressObserver(navigationUI.routeProgressObserver)
                navigationUI.arrivalText.text = getString(R.string.null_string)
            }
        }
    }

    private fun startNavigation() {
        // Check if there are current routes available
        val currentRoutes = routeLineApi.getNavigationRoutes()
        if (currentRoutes.isNotEmpty()) {
            Log.e("MapboxLog: MapFragment", "Starting navigation")
            routeLineApi.setNavigationRoutes(currentRoutes) { value ->
                value.fold(
                    {
                        // Handle error
                    },
                    {
                        switchCardViewContent(true)
                        navigationCamera.requestNavigationCameraToFollowing()

                        val annotationCardView = view?.findViewById<CardView>(R.id.previewCardView)
                        annotationCardView?.visibility = GONE

                        currentDestinationName= selectedDestinationName
                        currentDestinationAddress = selectedDestinationAddress
                        currentDestinationPoint = selectedDestinationPoint
                        selectedDestinationName = null
                        selectedDestinationAddress = null
                        selectedDestinationPoint = null

                    }
                )
            }
        }
    }

    fun cancelNavigation() {
        // Stop the trip session
        switchCardViewContent(false)

        // Clear route lines and route arrows
        routeLineApi.clearRouteLine { result ->
            mapView.mapboxMap.getStyle { style ->
                routeLineView.renderClearRouteLineValue(style, result)
                val clearArrowsValue = routeArrow.clearArrows()
                routeArrowView.render(style, clearArrowsValue)
                Log.e("MapboxLog: MapFragment", "Navigation cancelled and route lines cleared")
            }

            val annotationCardView = view?.findViewById<CardView>(R.id.previewCardView)
            annotationCardView?.visibility = GONE

            // Clear destination info
            currentDestinationName = null
            currentDestinationAddress = null
            currentDestinationPoint = null
        }
    }


    fun handleNavigationCameraStateChange(state: NavigationCameraState) {
        Log.e("MapboxLog: MapFragment", "NavigationCameraState changed to: $state")
        cameraState = state.toString()
        when (state) {
            NavigationCameraState.IDLE -> {}
            NavigationCameraState.FOLLOWING -> {
                mapView.setOnTouchListener(touchListener)
            }
            NavigationCameraState.OVERVIEW -> {
                enableMapInteractions()
                btnRecenter.visibility = VISIBLE
                mapView.location.apply {
                    locationPuck = createDefault2DPuck()
                }
            }
            NavigationCameraState.TRANSITION_TO_FOLLOWING -> {
                disableMapInteractions()
                btnRecenter.visibility = GONE
                mapView.location.apply {
                    locationPuck = customPuck3D
                }
            }
            NavigationCameraState.TRANSITION_TO_OVERVIEW -> {}
        }
    }

    private fun clearRoute() {
        routeLineApi.clearRouteLine { result ->
            mapView.mapboxMap.getStyle { style ->
                routeLineView.renderClearRouteLineValue(style, result)
                routeArrow.clearArrows()
            }
        }
    }

    private fun prepareMapUnderBlackScreen() {

        val cameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(-41.0, 44.0))
            .zoom(0.7)
            .pitch(0.0)
            .bearing(0.0)
            .build()

        mapView.mapboxMap.setCamera(cameraOptions)

        if (quickStart){
            getCurrentLocationAndAnimateMap(1000, true)
        }else{
            Handler(Looper.getMainLooper()).postDelayed({

                // First zoom in over 7000ms to render stuff
                getCurrentLocationAndAnimateMap(7000, false)

                // Start delay of zoom duration + short buffer (7000ms + 500ms)
                Handler(Looper.getMainLooper()).postDelayed({

                    // After ensuring the animation is done, reset to globe view
                    mapView.mapboxMap.setCamera(cameraOptions)

                    // Wait 500ms to give the globe time to load layers and prevent the user from seeing the textures snap onto it
                    Handler(Looper.getMainLooper()).postDelayed({

                        // Release the black screen (fade-out)
                        blackScreen.animate()
                            .alpha(0f) // Fade to transparent
                            .setDuration(1000) // Duration of the fade
                            .withEndAction {
                                // After the fade-out is complete, make the view completely gone
                                blackScreen.visibility = GONE
                            }

                        // Start delay of 3000ms before starting "real" zoom in animation
                        Handler(Looper.getMainLooper()).postDelayed({

                            // Zoom in to location
                            getCurrentLocationAndAnimateMap(12000, true)

                        }, 3000) // duration for user to see the globe

                    }, 500) // Wait duration for globe to render

                }, 7500) // Wait duration for the hidden zoom-in to play

            }, 1500) // Wait duration to render first globe view behind black screen
        }

    }

    private fun getCurrentLocationAndAnimateMap(
        animationDuration: Long,
        applyLocationPuckAfterAnimation: Boolean
    ) {
        lastLocation.let { location ->
            val point = Point.fromLngLat(location.longitude, location.latitude)
            animateMapToPoint(
                point,
                animationDuration,
                applyLocationPuckAfterAnimation,
            )
        }
    }

    private fun animateMapToPoint(
        point: Point,
        animationDuration: Long,
        applyLocationPuckAfterAnimation: Boolean,
    ) {
        if (applyLocationPuckAfterAnimation){
            val cameraOptions = CameraOptions.Builder()
                .center(point)
                .zoom(defaultZoom)
                .pitch(0.0)
                .bearing(0.0)
                .build()

            val animationOptions = MapAnimationOptions.mapAnimationOptions {
                duration(animationDuration) // Use the passed duration
            }

            mapView.mapboxMap.flyTo(
                cameraOptions = cameraOptions,
                animationOptions = animationOptions,
                animatorListener = object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                        // Animation started
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        // After animation end, set up components
                        setupBaseMapComponents(applyLocationPuckAfterAnimation)
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        // Animation canceled
                    }

                    override fun onAnimationRepeat(animation: Animator) {
                        // Animation repeated
                    }
                }
            )

        } else {

            val cameraOptions = CameraOptions.Builder()
                .center(point)
                .zoom(17.0)
                .pitch(0.0)
                .bearing(0.0)
                .build()

            val animationOptions = MapAnimationOptions.mapAnimationOptions {
                duration(animationDuration) // Use the passed duration
            }

            mapView.mapboxMap.flyTo(
                cameraOptions = cameraOptions,
                animationOptions = animationOptions,
                animatorListener = object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}

                    override fun onAnimationEnd(animation: Animator) {}

                    override fun onAnimationCancel(animation: Animator) {}

                    override fun onAnimationRepeat(animation: Animator) {}

                }
            )

        }

    }

    private fun setupBaseMapComponents(applyLocationPuckAfterAnimation: Boolean) {
        if (applyLocationPuckAfterAnimation) {

            customPuck3D = LocationPuck3D(
                modelUri = "asset://bmw_e90.glb",
                modelCastShadows = true,
                modelReceiveShadows = true,
                modelEmissiveStrength = 1.0f,
                modelEmissiveStrengthExpression = null,
                modelScale = listOf(0.017f, 0.017f, 0.017f),
                modelScaleMode = ModelScaleMode.MAP
            )

            mapView.mapboxMap.getStyle {

                mapView.location.apply {
                    this.setLocationProvider(navigationLocationProvider)
                    this.locationPuck = customPuck3D
                    this.puckBearing = PuckBearing.COURSE
                    this.enabled = true
                    this.layerAbove = "road-label-navigation"
                }

            }

/*            mapView.mapboxMap.getStyle { style ->

                val layers = style.styleLayers

                for (layer in layers) {
                    val layerId = layer.id
                    Log.d("MapboxLog: MapFragment", "Layer ID: $layerId")
                }

            }*/

            Handler(Looper.getMainLooper()).postDelayed({
                animateCardViewShrink()
            }, 2000)

            Handler(Looper.getMainLooper()).postDelayed({
                registerInputListeners()
                navigationCamera.requestNavigationCameraToFollowing()
                mapReady = true
            }, 3600)
        }
    }

    private fun disableMapInteractions() {
        mapView.gestures.scrollEnabled = false
        mapView.gestures.pinchToZoomEnabled = false
        mapView.gestures.rotateEnabled = false
        mapView.gestures.pitchEnabled = false
    }

    private fun enableMapInteractions() {
        mapView.gestures.scrollEnabled = true
        mapView.gestures.pinchToZoomEnabled = true
        mapView.gestures.rotateEnabled = true
        mapView.gestures.pitchEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        routeLineApi.cancel()
        routeLineView.cancel()
        navigationObservers.cleanup()
        MapboxNavigationProvider.retrieve().stopTripSession()
        MapboxNavigationProvider.retrieve().unregisterLocationObserver(locationObserver.locationObserver)
        MapboxNavigationProvider.retrieve().unregisterArrivalObserver(navigationObservers.arrivalObserver)
        MapboxNavigationProvider.retrieve().unregisterRoutesObserver(navigationObservers.routesObserver)
        MapboxNavigationProvider.retrieve().unregisterRouteProgressObserver(navigationUI.routeProgressObserver)
        mapView.location.removeOnIndicatorPositionChangedListener(navigationObservers.onPositionChangedListener)
        navigationCamera.unregisterNavigationCameraStateChangeObserver(navigationObservers.navigationCameraStateObserver)
        mapView.setOnTouchListener(null)
    }

}