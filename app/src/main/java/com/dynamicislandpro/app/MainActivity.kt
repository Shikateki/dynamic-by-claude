package com.dynamicislandpro.app

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dynamicislandpro.app.core.DynamicIslandEngine
import com.dynamicislandpro.app.core.PermissionManager
import com.dynamicislandpro.app.services.IslandService
import com.dynamicislandpro.app.ui.dashboard.DashboardPage
import com.dynamicislandpro.app.ui.permissions.PermissionPage
import com.dynamicislandpro.app.ui.preview.PreviewPage
import com.dynamicislandpro.app.ui.settings.AboutPage
import com.dynamicislandpro.app.ui.settings.AppearancePage
import com.dynamicislandpro.app.ui.settings.FeaturesPage
import com.dynamicislandpro.app.ui.toast
import com.dynamicislandpro.app.utils.PreferencesManager
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    val permissions by lazy { PermissionManager(this) }
    val prefs by lazy { PreferencesManager.get(this) }

    private var notificationCallback: ((Boolean) -> Unit)? = null
    private val notificationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationCallback?.invoke(granted)
            notificationCallback = null
        }

    private lateinit var tabs: TabLayout
    private lateinit var container: FrameLayout
    private lateinit var dashboard: DashboardPage
    private lateinit var preview: PreviewPage
    private lateinit var features: FeaturesPage
    private lateinit var appearance: AppearancePage
    private lateinit var permissionPage: PermissionPage
    private lateinit var about: AboutPage
    private lateinit var pages: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dashboard = DashboardPage(this)
        preview = PreviewPage(this)
        features = FeaturesPage(this)
        appearance = AppearancePage(this)
        permissionPage = PermissionPage(this)
        about = AboutPage(this)
        pages = listOf(dashboard.view, preview.view, features.view, appearance.view, permissionPage.view, about.view)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        tabs = TabLayout(this).apply {
            tabMode = TabLayout.MODE_SCROLLABLE
            setBackgroundColor(Color.BLACK)
        }
        container = FrameLayout(this)
        root.addView(tabs, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(container, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        listOf("Beranda", "Preview", "Fitur", "Tampilan", "Izin", "Tentang").forEach {
            tabs.addTab(tabs.newTab().setText(it))
        }
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = show(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        setContentView(root)
        // Android 15 memaksa edge-to-edge: beri ruang untuk status bar & navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        show(0)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { DynamicIslandEngine.serviceRunning.collect { dashboard.refresh() } }
                launch { DynamicIslandEngine.accessibilityConnected.collect { refreshStatus() } }
                launch {
                    prefs.settings.collect {
                        appearance.refresh()
                        preview.refresh()
                    }
                }
            }
        }
        Log.i(DynamicIslandApplication.TAG, "MainActivity created")
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun show(index: Int) {
        container.removeAllViews()
        container.addView(
            pages[index],
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        refreshStatus()
    }

    private fun refreshStatus() {
        dashboard.refresh()
        permissionPage.refresh()
    }

    fun toggleIsland() {
        if (DynamicIslandEngine.serviceRunning.value) {
            IslandService.stop(this)
            toast("Island dimatikan")
            return
        }
        if (!permissions.hasOverlay()) {
            toast("Izinkan \"Tampil di atas aplikasi lain\" dulu")
            launchSettings(permissions.overlayIntent())
            return
        }
        toast(if (IslandService.start(this)) "Island menyala" else "Gagal menyalakan service")
    }

    /** Buka halaman pengaturan; bila tidak tersedia di HyperOS, jatuh ke detail aplikasi. */
    fun launchSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (t: Exception) {
            Log.w(DynamicIslandApplication.TAG, "Settings intent failed: $intent", t)
            try {
                startActivity(permissions.appDetailsIntent())
            } catch (t2: Exception) {
                toast("Tidak bisa membuka pengaturan")
            }
        }
    }

    fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationCallback = onResult
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onResult(true)
        }
    }
}
