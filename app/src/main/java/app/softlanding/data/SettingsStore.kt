package app.softlanding.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.softlanding.domain.Activity
import app.softlanding.domain.Profile
import app.softlanding.domain.Sex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** The flight day the journey began on. */
    val started: Long = 0,
    val profile: Profile = Profile(),
    /** A norm set by hand; 0 means it is worked out from the profile. */
    val manualKcal: Int = 0,
    /** The hour a new day begins: a late snack before it still belongs to yesterday. */
    val dayStart: Int = 4,
    /** The envelope pattern chosen in the hangar. */
    val envelope: Int = 0,
    /** Evening reminder while the balloon is still in the air with nothing logged. */
    val remind: Boolean = true,
    /** The online catalogue (Open Food Facts) may be asked about barcodes and names. */
    val online: Boolean = true,
    /** The last plate chosen on the scales, kept for the next weighing. */
    val plate: Long = 0,
    /** The last flight day whose landing has been shown; later ones get their ceremony on the next open. */
    val seen: Long = 0,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        started = this[STARTED] ?: 0L,
        profile = Profile(
            sex = runCatching { Sex.valueOf(this[SEX] ?: "") }.getOrDefault(Sex.F),
            age = this[AGE] ?: 30,
            height = this[HEIGHT] ?: 168,
            weight = this[WEIGHT] ?: 70.0,
            goal = this[GOAL] ?: 65.0,
            activity = runCatching { Activity.valueOf(this[ACT] ?: "") }.getOrDefault(Activity.LIGHT),
            pace = this[PACE] ?: 0.5,
        ),
        manualKcal = this[MANUAL] ?: 0,
        dayStart = this[DAY_START] ?: 4,
        envelope = this[ENVELOPE] ?: 0,
        remind = this[REMIND] ?: true,
        online = this[ONLINE] ?: true,
        plate = this[PLATE] ?: 0L,
        seen = this[SEEN] ?: 0L,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[STARTED] = s.started
            it[SEX] = s.profile.sex.name
            it[AGE] = s.profile.age
            it[HEIGHT] = s.profile.height
            it[WEIGHT] = s.profile.weight
            it[GOAL] = s.profile.goal
            it[ACT] = s.profile.activity.name
            it[PACE] = s.profile.pace
            it[MANUAL] = s.manualKcal
            it[DAY_START] = s.dayStart
            it[ENVELOPE] = s.envelope
            it[REMIND] = s.remind
            it[ONLINE] = s.online
            it[PLATE] = s.plate
            it[SEEN] = s.seen
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val STARTED = longPreferencesKey("started")
        val SEX = stringPreferencesKey("sex")
        val AGE = intPreferencesKey("age")
        val HEIGHT = intPreferencesKey("height")
        val WEIGHT = doublePreferencesKey("weight")
        val GOAL = doublePreferencesKey("goal")
        val ACT = stringPreferencesKey("activity")
        val PACE = doublePreferencesKey("pace")
        val MANUAL = intPreferencesKey("manual")
        val DAY_START = intPreferencesKey("day_start")
        val ENVELOPE = intPreferencesKey("envelope")
        val REMIND = booleanPreferencesKey("remind")
        val ONLINE = booleanPreferencesKey("online")
        val PLATE = longPreferencesKey("plate")
        val SEEN = longPreferencesKey("seen")
    }
}
