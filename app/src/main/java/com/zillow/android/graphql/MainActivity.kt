package com.zillow.android.graphql

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.normalized.CacheControl
import com.apollographql.apollo.exception.ApolloException
import kotlinx.android.synthetic.main.listing_card.view.*
import okhttp3.OkHttpClient
import java.util.*

class MainActivity : AppCompatActivity() {

    val TAG = "QL"

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

    private val apolloClient: ApolloClient = ApolloClient.builder()
            .serverUrl( "http://172.27.10.138:3000/graphql" )
            .okHttpClient( okHttpClient)
            .build()

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //findViewById(R.id.button1).setOnClickListener { runQuery() }
        val swiper = findViewById(R.id.swipe_refresh) as SwipeRefreshLayout
        swiper.isEnabled = true
        swiper.setOnRefreshListener {
            Log.d(TAG, "refresh")
            runQuery()
            swiper.isRefreshing = false
        }

        recyclerView = findViewById(R.id.recyclerView) as RecyclerView
    }

    override fun onResume() {
        super.onResume()
        runQuery()
    }

    fun runQuery() {
        Log.d(TAG, "run query")
        var rentalsQuery = RentalsQuery.builder()
                .query("status:open")
                .limit(10)
                .page(1)
                .featured_count(2)
                .build()
        var rentalsCall = apolloClient
                .query(rentalsQuery)
                .cacheControl(CacheControl.NETWORK_FIRST)
        rentalsCall.enqueue(object: ApolloCall.Callback<RentalsQuery.Data>() {
            override fun onResponse(response: Response<RentalsQuery.Data>) {
                Log.d(TAG, response.data().toString())
                var listings = response.data()?.rental_search()?.listings() ?: LinkedList<RentalsQuery.Listing>()
                runOnUiThread { createAdapter(listings) }
            }
            override fun onFailure(e: ApolloException) {
                Log.e(TAG, e.message, e)
            }
        })
    }

    fun createAdapter(listings: List<RentalsQuery.Listing>) {
        for ( l : RentalsQuery.Listing in listings )
        {
            Log.d(TAG, l.id() + l.address()?.street() )
        }
        val adapter = ListingsAdapter(listings)
        recyclerView.adapter = adapter
    }

    private class ListingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(listing: RentalsQuery.Listing) = with(itemView) {
            listing_address.text = listing.address()?.unit() + " " + listing.address()?.street()
            listing_price.text = "$%,d".format(listing.price())
        }
    }

    private class ListingsAdapter(var listings: List<RentalsQuery.Listing>) : RecyclerView.Adapter<ListingViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ListingViewHolder {
            val view: View = LayoutInflater.from(parent?.context).inflate(R.layout.listing_card, parent, false)
            return ListingViewHolder(view)
        }

        override fun onBindViewHolder(holder: ListingViewHolder?, position: Int) {
            holder?.bind(listings.get(position))
        }

        override fun getItemCount(): Int {
            return listings.count()
        }
    }
}
