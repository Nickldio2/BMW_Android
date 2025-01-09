package com.example.mapboxtest

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.tripdata.maneuver.api.MapboxLaneIconsApi
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.maneuver.model.LaneIconResources
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.model.DistanceRemainingFormatter
import com.mapbox.navigation.tripdata.progress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.tripdata.progress.model.TimeRemainingFormatter
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.components.maneuver.view.MapboxLaneGuidance
import com.mapbox.navigation.ui.components.maneuver.view.MapboxPrimaryManeuver
import com.mapbox.navigation.ui.components.maneuver.view.MapboxStepDistance
import com.mapbox.navigation.ui.components.maneuver.view.MapboxTurnIconManeuver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.Math.toRadians
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CancellationException
import kotlin.coroutines.resumeWithException
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

interface OnSuggestionClickListener {
    fun onSuggestionClicked(latLng: LatLng, locationName: String, address: String)
}

class NavigationUI : Fragment(R.layout.navigation_ui) {

    private lateinit var placesClient: PlacesClient
    private lateinit var searchEditText: EditText
    private lateinit var searchFrame: FrameLayout
    private lateinit var suggestionsContainer: FrameLayout
    private lateinit var searchIcon: ImageView
    private lateinit var currentLocation: LatLng
    private var searchDebounce: Job? = null
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var suggestionClicked = false
    private lateinit var mapFragment: MapFragment
    private lateinit var turnIconManeuver: MapboxTurnIconManeuver
    private lateinit var nextManeuver: MapboxPrimaryManeuver
    private lateinit var stepDistanceManeuver: MapboxStepDistance
    lateinit var arrivalText: TextView
    private lateinit var remainingDurationText: TextView
    private lateinit var remainingKmText: TextView
    private lateinit var cancelTripButton: CardView
    private lateinit var laneGuidanceContainer: LinearLayout
    private lateinit var tripProgressBar: ProgressBar
    private lateinit var tripProgressIcon: ImageView
    private var totalDistance : Double = 0.0

    private val tripProgressFormatter: TripProgressUpdateFormatter by lazy {
        val distanceFormatterOptions = DistanceFormatterOptions.Builder(this.requireContext()).build()
        TripProgressUpdateFormatter.Builder(this.requireContext())
            .distanceRemainingFormatter(DistanceRemainingFormatter(distanceFormatterOptions))
            .timeRemainingFormatter(TimeRemainingFormatter(this.requireContext()))
            .estimatedTimeToArrivalFormatter(EstimatedTimeToArrivalFormatter(this.requireContext()))
            .build()
    }

    private val tripProgressApi: MapboxTripProgressApi by lazy {
        MapboxTripProgressApi(tripProgressFormatter)
    }

    private val distanceFormatter: DistanceFormatterOptions by lazy {
        DistanceFormatterOptions.Builder(this.requireContext())
            .unitType(UnitType.METRIC)
            .roundingIncrement(50)
            .build()
    }

    private val maneuverApi: MapboxManeuverApi by lazy {
        MapboxManeuverApi(MapboxDistanceFormatter(distanceFormatter))
    }

    private val laneIconsApi: MapboxLaneIconsApi by lazy {
        MapboxLaneIconsApi(LaneIconResources.Builder().build())
    }

    val routeProgressObserver = RouteProgressObserver { routeProgress ->
        mapFragment.routeLineApi.updateWithRouteProgress(routeProgress) { result ->
            val updatedManeuverArrow = mapFragment.routeArrow.addUpcomingManeuverArrow(routeProgress)
            mapFragment.routeArrowView.renderManeuverUpdate(mapFragment.mapStyle, updatedManeuverArrow)
            mapFragment.routeLineView.renderRouteLineUpdate(mapFragment.mapStyle, result)

            val maneuversResult = maneuverApi.getManeuvers(routeProgress)
            val tripProgressUpdateValue = tripProgressApi.getTripProgress(routeProgress)

            val km = tripProgressUpdateValue.distanceRemaining

            val formattedDistance = when {
                km >= 10000 -> String.format(Locale.getDefault(), "%.0f km", km / 1000)
                km >= 1000 -> String.format(Locale.getDefault(), "%.1f km", km / 1000)
                else -> String.format(Locale.getDefault(), "%d m", (km / 50).toInt() * 50)
            }

            val timeRemaining = tripProgressUpdateValue.totalTimeRemaining
            val totalMinutes = timeRemaining.toInt() / 60
            val hoursRemaining = totalMinutes / 60
            val minutesRemaining = totalMinutes % 60

            val formattedRemaining = if (hoursRemaining > 0) {
                String.format(Locale.getDefault(), "%d:%02d h", hoursRemaining, minutesRemaining)
            } else {
                String.format(Locale.getDefault(), "%d min", minutesRemaining)
            }

            val eta = tripProgressUpdateValue.estimatedTimeToArrival
            val etaDate = Date(eta)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeFormat.timeZone = TimeZone.getTimeZone("Europe/Berlin")

            val formattedTime = timeFormat.format(etaDate)

            if (maneuversResult.isValue) {

                val maneuvers = maneuversResult.value
                val firstManeuver = maneuvers?.firstOrNull()

                firstManeuver?.let { turnIconManeuver.renderPrimaryTurnIcon(it.primary) }
                firstManeuver?.let { stepDistanceManeuver.renderDistanceRemaining(it.stepDistance) }
                firstManeuver?.let { nextManeuver.renderManeuver(it.primary, null) }

                laneGuidanceContainer.removeAllViews()

                // Add lane icons dynamically
                firstManeuver?.laneGuidance?.allLanes?.forEach { laneIndicator ->
                    val laneIcon = laneIconsApi.getTurnLane(laneIndicator)
                    laneIcon.let {
                        val laneView = MapboxLaneGuidance(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(70,64)
                        }
                        laneView.renderLane(it, ContextThemeWrapper(requireContext(), R.style.CustomLaneGuidance))
                        laneGuidanceContainer.addView(laneView)
                    }
                }

                if (remainingKmText.text == "null") {
                    totalDistance = tripProgressUpdateValue.distanceRemaining
                    tripProgressBar.setMax(totalDistance.roundToInt())
                    Log.e("MapboxLog: NavigationUI", "totalDistance: " + totalDistance.roundToInt())
                }

                val tripProgressInKm = totalDistance - km
                tripProgressBar.progress = tripProgressInKm.roundToInt()
                Log.e("MapboxLog: NavigationUI", "progress: " + tripProgressInKm.roundToInt())

                val progressRatio: Float = (tripProgressInKm / totalDistance).toFloat()
                val progressBarWidth = tripProgressBar.width
                val iconOffset = (progressBarWidth * progressRatio).toInt()
                tripProgressIcon.translationX = iconOffset.toFloat()

                arrivalText.text = formattedTime
                remainingDurationText.text = formattedRemaining
                remainingKmText.text = formattedDistance

            }

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.navigation_ui, container, false)

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        val tabButtonRecent = view.findViewById<FrameLayout>(R.id.tabButton_recent)
        val tabButtonContacts = view.findViewById<FrameLayout>(R.id.tabButton_contacts)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment {
                return if (position == 0) FirstFragment() else SecondFragment()
            }
        }

        tabButtonRecent.setOnClickListener {
            viewPager.currentItem = 0
        }

        tabButtonContacts.setOnClickListener {
            viewPager.currentItem = 1
        }

        viewPager.currentItem = 0

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 0) {
                    tabButtonRecent.setBackgroundResource(R.drawable.tab_selected)
                    tabButtonContacts.setBackgroundResource(R.drawable.tab_unselected)
                } else {
                    tabButtonContacts.setBackgroundResource(R.drawable.tab_selected)
                    tabButtonRecent.setBackgroundResource(R.drawable.tab_unselected)
                }
            }
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as MainActivity).setupUI(view)

        mapFragment = parentFragmentManager.findFragmentById(R.id.fragment_map_container) as MapFragment

        placesClient = Places.createClient(requireContext())
        searchEditText = view.findViewById(R.id.searchEditText)
        searchFrame = view.findViewById(R.id.searchFrame)
        suggestionsContainer = view.findViewById(R.id.suggestionsContainer)
        searchIcon = view.findViewById(R.id.searchIcon)
        turnIconManeuver = view.findViewById(R.id.turnIconManeuver)
        nextManeuver = view.findViewById(R.id.nextManeuver)
        stepDistanceManeuver = view.findViewById(R.id.stepDistanceManeuver)
        arrivalText = view.findViewById(R.id.tripArrivalView)
        remainingDurationText = view.findViewById(R.id.tripRemainingView)
        remainingKmText = view.findViewById(R.id.kmView)
        cancelTripButton = view.findViewById(R.id.cancelButton)
        laneGuidanceContainer = view.findViewById(R.id.laneGuidanceContainer)
        tripProgressBar = view.findViewById(R.id.tripProgressBar)
        tripProgressIcon = view.findViewById(R.id.progressIcon)

        sharedViewModel.currentLocation.observe(viewLifecycleOwner) { location ->
            currentLocation = location
        }

        sharedViewModel.clearFocusEvent.observe(viewLifecycleOwner) {
            if (searchEditText.hasFocus()){
                searchEditText.clearFocus()
            }
        }


        placesClient = Places.createClient(requireContext())
        val suggestionsRecyclerView = view.findViewById<RecyclerView>(R.id.suggestionsRecyclerView)

        // Setup RecyclerView with an adapter
        val suggestionsAdapter = SuggestionsAdapter(object : OnSuggestionClickListener {
            override fun onSuggestionClicked(latLng: LatLng, locationName: String, address: String) {
                suggestionClicked = true
                searchEditText.clearFocus()
                sharedViewModel.selectLocation(latLng)
                sharedViewModel.selectAddress(locationName, address)
            }
        })
        suggestionsRecyclerView.adapter = suggestionsAdapter
        suggestionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val customItemDecoration = CustomItemDecoration(requireContext(), R.drawable.custom_divider)
        suggestionsRecyclerView.addItemDecoration(customItemDecoration)

        cancelTripButton.setOnClickListener{onCancelTripButtonClicked()}

        searchEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {

                searchIcon.visibility = View.GONE

                val customAnimation = CustomAnimation()

                customAnimation.hideContentSearch(requireContext())

                lifecycleScope.launch {
                    val query = searchEditText.text.toString().trim()
                    if (query.isNotEmpty()) {
                        fetchPredictions(query, suggestionsAdapter)
                    } else {
                        suggestionsAdapter.submitList(emptyList())
                        animateHeightChange(suggestionsContainer, 10, false)
                    }
                }
            } else {
                if (!suggestionClicked){
                    animateHeightChange(suggestionsContainer,10, true)
                }else{
                    suggestionClicked = false
                }
            }
        }

        // Listen for text changes in the search bar
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchDebounce?.cancel()
                searchDebounce = lifecycleScope.launch {
                    delay(350) // Debounce time in milliseconds
                    val query = s.toString().trim()
                    if (query.isNotEmpty()) {
                        fetchPredictions(query, suggestionsAdapter)
                    } else {
                        suggestionsAdapter.submitList(emptyList())
                        animateHeightChange(suggestionsContainer, 10, false)
                    }
                }
            }
        })

    }

    private fun onCancelTripButtonClicked() {
        mapFragment.cancelNavigation()
    }

    private fun calculateDynamicHeight(itemCount: Int): Int {
        val itemHeight = 54
        return itemCount * (itemHeight) + 48
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6372.8 // In kilometers
        val l1 = toRadians(lat1)
        val l2 = toRadians(lat2)
        val dl = toRadians(lat2 - lat1)
        val dr = toRadians(lon2 - lon1)
        return 2 * r * asin(sqrt(sin(dl / 2) * sin(dl / 2) + cos(l1) * cos(l2) * sin(dr / 2) * sin(dr / 2)))
    }

    private fun createSquareBounds(center: LatLng): RectangularBounds {
        // Approximate "distance" in degrees latitude (very rough approximation)
        val latChange = 15000.0 / 111320.0

        // Approximate "distance" in degrees longitude (rough approximation)
        val lonChange = 15000.0 / (cos(toRadians(center.latitude)) * 111320.0)

        val southWest = LatLng(center.latitude - latChange, center.longitude - lonChange)
        val northEast = LatLng(center.latitude + latChange, center.longitude + lonChange)

        return RectangularBounds.newInstance(southWest, northEast)
    }

    private suspend fun fetchPlaceDetails(placeId: String): LatLng? {
        val placeFields = listOf(Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)
        return try {
            val response = placesClient.fetchPlace(request).await()
            response.place.latLng
        } catch (e: Exception) {
            Log.e("MapboxLog: NavigationUI", "Error fetching place details: $e")
            null
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T> Task<T>.await(): T {
        if (isComplete) {
            val e = exception
            if (e == null) {
                if (isCanceled) {
                    throw CancellationException("Task $this was cancelled normally.")
                } else {
                    return result
                }
            } else {
                throw e
            }
        }

        return suspendCancellableCoroutine { cont ->
            addOnCompleteListener {
                val e = it.exception
                if (e == null) {
                    if (it.isCanceled) {
                        cont.cancel()
                    } else {
                        cont.resume(it.result) {}
                    }
                } else {
                    cont.resumeWithException(e)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchPredictions(query: String, adapter: SuggestionsAdapter) {
        val token = AutocompleteSessionToken.newInstance()

        currentLocation.let { nonNullCurrentLocation ->
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(query)
                .setOrigin(nonNullCurrentLocation)
                .setLocationBias(createSquareBounds(nonNullCurrentLocation))
                .build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    lifecycleScope.launch {
                        val suggestionsWithDistances = response.autocompletePredictions.take(4).mapNotNull { prediction ->
                            val placeId = prediction.placeId
                            fetchPlaceDetails(placeId)?.let { latLng ->
                                val distance = haversine(
                                    nonNullCurrentLocation.latitude,
                                    nonNullCurrentLocation.longitude,
                                    latLng.latitude,
                                    latLng.longitude
                                ).toInt() // Convert to Int for simplicity
                                val fullText = prediction.getFullText(null).toString()
                                val formattedText = removeCountryFromSuggestion(fullText)
                                SuggestionsAdapter.SuggestionItem(formattedText, "$distance km", distance, latLng)
                            }
                        }

                        // Sort the list of suggestions by distance
                        val sortedSuggestions = suggestionsWithDistances.sortedBy { it.distanceValue }
                        adapter.submitList(sortedSuggestions)

                        val newHeight = calculateDynamicHeight(sortedSuggestions.size)
                        val currentHeight = suggestionsContainer.height

                        if (currentHeight != newHeight) {
                            if (newHeight == 0) {
                                animateHeightChange(suggestionsContainer, 10, false)
                            }else{
                                animateHeightChange(suggestionsContainer, newHeight, false)
                            }
                        }
                    }
                }
        }
    }

    private fun animateHeightChange(targetView: View, endHeight: Int, collapseEditBox: Boolean) {
        val startHeight = suggestionsContainer.height
        val animator = ValueAnimator.ofInt(startHeight, endHeight)

        animator.addUpdateListener { valueAnimator ->
            val newHeight = valueAnimator.animatedValue as Int
            val layoutParams = targetView.layoutParams
            layoutParams.height = newHeight
            targetView.layoutParams = layoutParams
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (collapseEditBox){
                    searchEditText.text.clear()
                    searchIcon.visibility = View.VISIBLE
                }
            }
        })
        animator.duration = 350
        animator.start()
    }

    private fun removeCountryFromSuggestion(fullText: String): String {
        val parts = fullText.split(", ")
        // Remove the last part which is assumed to be the country
        return parts.dropLast(1).joinToString(", ")
    }

}

class SuggestionsAdapter(private val clickListener: OnSuggestionClickListener) : ListAdapter<SuggestionsAdapter.SuggestionItem, SuggestionsAdapter.SuggestionViewHolder>(SuggestionDiffCallback()) {

    data class SuggestionItem(
        val text: String,
        val distance: String, // Displayed distance
        val distanceValue: Int, // Actual distance value for sorting
        val latLng: LatLng // Geographical coordinates of the suggestion
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }

    class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewLocationName: TextView = itemView.findViewById(R.id.textViewLocationName)
        private val textViewAddress: TextView = itemView.findViewById(R.id.textViewAddress)
        private val textViewDistance: TextView = itemView.findViewById(R.id.textViewDistance)

        fun bind(item: SuggestionItem, clickListener: OnSuggestionClickListener) {
            val locationParts = item.text.split(", ")
            textViewLocationName.text = locationParts.firstOrNull() ?: ""
            textViewAddress.text = locationParts.drop(1).joinToString(", ")
            textViewDistance.text = item.distance
            itemView.setOnClickListener { clickListener.onSuggestionClicked(item.latLng, textViewLocationName.text.toString(), textViewAddress.text.toString()
            ) }
        }
    }

    class SuggestionDiffCallback : DiffUtil.ItemCallback<SuggestionItem>() {
        override fun areItemsTheSame(oldItem: SuggestionItem, newItem: SuggestionItem): Boolean {
            return oldItem.text == newItem.text
        }

        override fun areContentsTheSame(oldItem: SuggestionItem, newItem: SuggestionItem): Boolean {
            return oldItem == newItem
        }
    }
}

class CustomItemDecoration(context: Context, resId: Int) : RecyclerView.ItemDecoration() {

    private val divider: Drawable? = ContextCompat.getDrawable(context, resId)

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        val childCount = parent.childCount
        for (i in 0 until childCount - 1) { // Ensure the divider is not drawn after the last item
            val child = parent.getChildAt(i)

            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + (divider?.intrinsicHeight ?: 0)

            divider?.setBounds(left, top, right, bottom)
            divider?.draw(c)
        }
    }
}

class FirstFragment : Fragment() {

    private lateinit var recentRecyclerView: RecyclerView
    private lateinit var topDestinationsAdapter: RecentDestinations

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.tab_recent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recentRecyclerView = view.findViewById(R.id.recentRecyclerView)

        // Get the top destinations
        val topDestinations = getTopDestinations(requireContext())

        // Initialize the adapter
        topDestinationsAdapter = RecentDestinations(topDestinations)

        // Set up the RecyclerView
        recentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recentRecyclerView.adapter = topDestinationsAdapter

    }
}

class SecondFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.tab_contacts, container, false)
    }
}
