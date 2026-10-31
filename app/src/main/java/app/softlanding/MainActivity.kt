package app.softlanding

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.softlanding.data.Settings
import app.softlanding.ui.SoftRoot
import app.softlanding.ui.theme.SoftTheme

class MainActivity : AppCompatActivity() {
    private var action by mutableStateOf<Pair<String, Long>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.light(0x00000000, 0x00000000))
        if (savedInstanceState == null) take(intent)
        shortcuts()
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val seeding by container.seeding.collectAsStateWithLifecycle()
            // Wait for settings and the food table so the welcome doesn't flash for someone already here.
            if (!seeding) settings?.let { s -> SoftTheme { SoftRoot(s, action = action) } }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    /** A launcher shortcut or the evening note asks to go straight to adding or scanning. */
    private fun take(i: Intent?) {
        val a = when {
            i?.action == ACTION_SCAN -> SCAN
            i?.action == ACTION_ADD -> ADD
            else -> i?.getStringExtra(EXTRA)
        }
        // A new value each time, so the same shortcut twice still opens.
        if (a != null) action = a to System.nanoTime()
    }

    private fun shortcuts() {
        fun sc(id: String, label: Int, act: String) = ShortcutInfoCompat.Builder(this, id).setShortLabel(getString(label))
            .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
            .setIntent(Intent(this, MainActivity::class.java).setAction(act)).build()
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(this, listOf(sc("scan", R.string.sc_scan, ACTION_SCAN), sc("add", R.string.sc_add, ACTION_ADD))) }
    }

    companion object {
        const val EXTRA = "go"
        const val ADD = "add"
        const val SCAN = "scan"
        const val ACTION_ADD = "app.softlanding.ADD"
        const val ACTION_SCAN = "app.softlanding.SCAN"
    }
}
