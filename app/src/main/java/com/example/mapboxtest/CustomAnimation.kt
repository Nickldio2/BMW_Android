package com.example.mapboxtest

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity

class CustomAnimation {

    fun switchContent(context: Context, finalCallback: () -> Unit) {
        val activity = context as AppCompatActivity

        // Get references to the views
        val suggestionsContainer = activity.findViewById<View>(R.id.suggestionsContainer)
        val searchBar = activity.findViewById<View>(R.id.searchBar)
        val homeButton = activity.findViewById<View>(R.id.homeButton)
        val gasButton = activity.findViewById<View>(R.id.gasButton)
        val foodButton = activity.findViewById<View>(R.id.foodButton)
        val historyContainer = activity.findViewById<View>(R.id.historyContainer)
        val tabButtonRecent = activity.findViewById<View>(R.id.tabButton_recent)
        val tabButtonContacts = activity.findViewById<View>(R.id.tabButton_contacts)

        val maneuverView = activity.findViewById<View>(R.id.maneuverView)
        val tripInfoView = activity.findViewById<View>(R.id.tripInfoView)
        val cancelButton = activity.findViewById<View>(R.id.cancelButton)

        val viewsGroup1 = listOf(suggestionsContainer, searchBar, homeButton, gasButton, foodButton, historyContainer, tabButtonRecent, tabButtonContacts)
        val viewsGroup2 = listOf(maneuverView, tripInfoView, cancelButton)

        if (viewsGroup1.any { it.visibility == VISIBLE }) {
            fadeOutViews({
                fadeInViews(finalCallback, *viewsGroup2.toTypedArray())
            }, *viewsGroup1.toTypedArray())
        } else {
            fadeOutViews({
                fadeInViews(finalCallback, *viewsGroup1.toTypedArray())
            }, *viewsGroup2.toTypedArray())
        }
    }

    fun animateIcons(context: Context) {

        val activity = context as AppCompatActivity

        val btnSearch = activity.findViewById<View>(R.id.btnSearch)
        val btnGas = activity.findViewById<View>(R.id.btnGas)
        val btnFood = activity.findViewById<View>(R.id.btnFood)

        if (btnSearch.visibility == GONE) {

            btnSearch.animate().cancel()
            btnGas.animate().cancel()
            btnFood.animate().cancel()

            // Set visibility to VISIBLE and animate to alpha 1
            btnSearch.visibility = VISIBLE
            btnSearch.animate().alpha(1f).setDuration(350).setListener(null)
            btnGas.visibility = VISIBLE
            btnGas.animate().alpha(1f).setDuration(350).setListener(null)
            btnFood.visibility = VISIBLE
            btnFood.animate().alpha(1f).setDuration(350).setListener(null)

        } else {

            btnSearch.animate().cancel()
            btnGas.animate().cancel()
            btnFood.animate().cancel()

            btnSearch.animate()
                .alpha(0f)
                .setDuration(350)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        btnSearch.visibility = GONE
                    }
                })

            btnGas.animate()
                .alpha(0f)
                .setDuration(350)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        btnGas.visibility = GONE
                    }
                })

            btnFood.animate()
                .alpha(0f)
                .setDuration(350)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        btnFood.visibility = GONE
                    }
                })
        }
    }

    fun hideContentSearch(context: Context) {
        val activity = context as AppCompatActivity

        // Get references to the views
        val homeButton = activity.findViewById<View>(R.id.homeButton)
        val gasButton = activity.findViewById<View>(R.id.gasButton)
        val foodButton = activity.findViewById<View>(R.id.foodButton)
        val historyContainer = activity.findViewById<View>(R.id.historyContainer)
        val tabButtonRecent = activity.findViewById<View>(R.id.tabButton_recent)
        val tabButtonContacts = activity.findViewById<View>(R.id.tabButton_contacts)

        val viewsGroup = listOf(homeButton, gasButton, foodButton, historyContainer, tabButtonRecent, tabButtonContacts)

        fadeOutViews({viewsGroup.toTypedArray()})
    }

    private fun fadeOutViews(callback: () -> Unit, vararg views: View) {
        val totalViews = views.size
        var animationsEnded = 0

        for (view in views) {
            view.alpha = 1f
            view.visibility = VISIBLE
            view.animate()
                .alpha(0f)
                .setDuration(500)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = GONE
                        animationsEnded++
                        if (animationsEnded == totalViews) {
                            callback()
                        }
                    }
                })
                .start()
        }
    }

    private fun fadeInViews(callback: () -> Unit, vararg views: View) {
        val totalViews = views.size
        var animationsEnded = 0

        for (view in views) {
            view.alpha = 0f
            view.visibility = VISIBLE
            view.animate()
                .alpha(1f)
                .setDuration(500)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        animationsEnded++
                        if (animationsEnded == totalViews) {
                            callback()
                        }
                    }
                })
                .start()
        }
    }
}
