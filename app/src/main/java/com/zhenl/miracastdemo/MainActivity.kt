package com.zhenl.miracastdemo

import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuItemCompat
import androidx.lifecycle.Observer
import androidx.mediarouter.app.MediaRouteActionProvider
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.miui.wifidisplay.WifiDisplayAdmin
import com.zhenl.miracastdemo.player.Player
import com.zhenl.miracastdemo.player.PlaylistItem

class MainActivity : AppCompatActivity() {

    private var mediaRouter: MediaRouter? = null
    private var mSelector: MediaRouteSelector? = null
    private var mSelectedRoute: MediaRouter.RouteInfo? = null
    private var mPlayer: Player? = null
    private var uri: Uri? = null

    private lateinit var adapter: ArrayAdapter<WifiDisplay>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WifiDisplayManager.attach(applicationContext)

        // Get the media router service.
        mediaRouter = MediaRouter.getInstance(this)
        // Create a route selector for the type of routes your app supports.
        mSelector = MediaRouteSelector.Builder()
            // These are the framework-supported intents
            .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
            .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
            .build()

        adapter = ArrayAdapter(this, R.layout.item_wifi_display, R.id.tv)
        findViewById<ListView>(R.id.lv)?.let {
            it.adapter = adapter
            it.setOnItemClickListener { parent, view, position, id ->
                adapter.getItem(position)?.let { display ->
                    WifiDisplayAdmin.getInstance().connectWifiDisplay(display.deviceAddress)
                }
            }
        }

        WifiDisplayManager.displays.observe(this, Observer {
            adapter.clear()
            adapter.addAll(it)
        })

        fileSearch()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sample_media_router_menu, menu)

        // Attach the MediaRouteSelector to the menu item
        val mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item)
        val mediaRouteActionProvider =
            MenuItemCompat.getActionProvider(mediaRouteMenuItem) as MediaRouteActionProvider

        // Attach the MediaRouteSelector that you built in onCreate()
        mSelector?.also(mediaRouteActionProvider::setRouteSelector)

        return true
    }

    override fun onStart() {
        WifiDisplayManager.startScan()
        mediaRouter?.addCallback(
            mSelector!!,
            mediaRouterCallback,
            MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
        )
        super.onStart()
    }

    override fun onStop() {
        WifiDisplayManager.stopScan()
        mediaRouter?.removeCallback(mediaRouterCallback)
        super.onStop()
    }

    private val mediaRouterCallback = object : MediaRouter.Callback() {
        override fun onRouteSelected(
            router: MediaRouter,
            route: MediaRouter.RouteInfo,
            reason: Int
        ) {
            if (route.supportsControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)) {
                Log.d("onRouteSelected", "remote playback device ${route.name}")
            } else {
                Log.d("onRouteSelected", "secondary output device ${route.name}")
                mSelectedRoute = route
                mPlayer = Player.create(applicationContext, route)
                mPlayer?.updatePresentation()
            }
        }

        override fun onRouteAdded(router: MediaRouter?, route: MediaRouter.RouteInfo?) {
            Log.d("onRouteAdded", route?.name ?: "unknown")
        }

        override fun onRouteChanged(router: MediaRouter?, route: MediaRouter.RouteInfo?) {
            Log.d("onRouteChanged", route?.toString() ?: "unknown")
            if (route == mSelectedRoute) {
                WifiDisplayManager.displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).forEach {
                    if (it.name == route?.name) {
                        mPlayer?.updatePresentation(it)
                        mPlayer?.play(PlaylistItem(null, null, uri, null, null))
                    }
                }
            }
        }
    }

    private fun fileSearch() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null || resultCode != RESULT_OK) return
        if (requestCode == 101) {
            uri = data.data
        }
    }

}