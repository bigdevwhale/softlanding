package app.softlanding.work

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.softlanding.MainActivity
import app.softlanding.R
import app.softlanding.SoftApp
import app.softlanding.data.Nutr
import app.softlanding.domain.Days
import app.softlanding.domain.Norm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Today's calories and macros on the home screen (#4): eaten of the norm, protein, fat and carbs as bars,
 * fiber in a line. A tap opens the app. Redrawn whenever something is written down, and every half hour.
 */
class MacroWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch { try { refresh(context) } finally { pending.finish() } }
    }

    companion object {
        /** Draws every placed widget from the records. Safe to call when none is placed. */
        suspend fun refresh(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, MacroWidget::class.java))
            if (ids.isEmpty()) return
            val c = (app as SoftApp).container
            val s = c.settings.now()
            val today = Days.of(System.currentTimeMillis(), s.dayStart)
            val eaten = Nutr.sum(c.entries.day(today).map { it.n })
            val norm = Norm.of(s.profile, s.manualKcal, s.profile.weight)
            val g = app.getString(R.string.g)
            val v = RemoteViews(app.packageName, R.layout.widget_macros)
            v.setTextViewText(R.id.w_kcal, app.getString(R.string.w_kcal, eaten.kcal.roundToInt(), norm.kcal))
            v.setProgressBar(R.id.w_kcal_bar, norm.kcal.coerceAtLeast(1), eaten.kcal.roundToInt().coerceAtMost(norm.kcal), false)
            fun bar(text: Int, bar: Int, label: Int, have: Double, want: Int) {
                v.setTextViewText(text, "${app.getString(label)} ${have.roundToInt()}/$want $g")
                v.setProgressBar(bar, want.coerceAtLeast(1), have.roundToInt().coerceAtMost(want), false)
            }
            bar(R.id.w_p, R.id.w_p_bar, R.string.protein, eaten.p, norm.protein)
            bar(R.id.w_f, R.id.w_f_bar, R.string.fat, eaten.f, norm.fat)
            bar(R.id.w_c, R.id.w_c_bar, R.string.carbs, eaten.c, norm.carbs)
            v.setTextViewText(R.id.w_fiber, "${app.getString(R.string.fiber)} ${eaten.fb.roundToInt()}/${norm.fiber} $g")
            val open = PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            v.setOnClickPendingIntent(R.id.w_root, open)
            manager.updateAppWidget(ids, v)
        }
    }
}
