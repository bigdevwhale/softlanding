package app.softlanding.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import app.softlanding.BuildConfig
import app.softlanding.R
import app.softlanding.container
import app.softlanding.domain.Activity
import app.softlanding.domain.Norm
import app.softlanding.domain.Profile
import app.softlanding.domain.Sex
import app.softlanding.ui.Home
import app.softlanding.ui.Nav
import app.softlanding.ui.add.NumField
import app.softlanding.ui.components.Btn
import app.softlanding.ui.components.Card
import app.softlanding.ui.components.Chip
import app.softlanding.ui.components.Flow
import app.softlanding.ui.components.Kind
import app.softlanding.ui.components.Page
import app.softlanding.ui.components.Row2
import app.softlanding.ui.components.Section
import app.softlanding.ui.components.kcal
import app.softlanding.ui.components.one
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T
import java.io.File

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "softlanding"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
}

private fun num(s: String) = s.replace(',', '.').toDoubleOrNull()

/** Who is flying: the fields of a profile, shared with the welcome. */
@Composable
fun ProfileFields(p: Profile, onChange: (Profile) -> Unit) {
    var age by remember { mutableStateOf(p.age.toString()) }
    var height by remember { mutableStateOf(p.height.toString()) }
    var weight by remember { mutableStateOf(one(p.weight).replace('.', ',')) }
    var goal by remember { mutableStateOf(one(p.goal).replace('.', ',')) }
    fun push(a: String = age, hh: String = height, w: String = weight, g: String = goal) {
        onChange(p.copy(age = a.toIntOrNull()?.coerceIn(12, 100) ?: p.age, height = hh.toIntOrNull()?.coerceIn(120, 230) ?: p.height,
            weight = num(w)?.coerceIn(30.0, 300.0) ?: p.weight, goal = num(g)?.coerceIn(30.0, 300.0) ?: p.goal))
    }
    Flow {
        Chip(stringResource(R.string.sex_f), p.sex == Sex.F, { onChange(p.copy(sex = Sex.F)) })
        Chip(stringResource(R.string.sex_m), p.sex == Sex.M, { onChange(p.copy(sex = Sex.M)) })
    }
    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumField(age, { age = it; push(a = it) }, stringResource(R.string.p_age), Modifier.weight(1f))
        NumField(height, { height = it; push(hh = it) }, stringResource(R.string.p_height), Modifier.weight(1f))
    }
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumField(weight, { weight = it; push(w = it) }, stringResource(R.string.p_weight), Modifier.weight(1f))
        NumField(goal, { goal = it; push(g = it) }, stringResource(R.string.p_goal), Modifier.weight(1f))
    }
    Text(stringResource(R.string.p_activity), style = T.Small, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for ((a, l, d) in listOf(
            Triple(Activity.SITTING, R.string.act_sitting, R.string.act_sitting_d),
            Triple(Activity.LIGHT, R.string.act_light, R.string.act_light_d),
            Triple(Activity.ACTIVE, R.string.act_active, R.string.act_active_d),
            Triple(Activity.SPORT, R.string.act_sport, R.string.act_sport_d),
        )) Card(Modifier.fillMaxWidth(), onClick = { onChange(p.copy(activity = a)) }, color = if (p.activity == a) C.redL else C.card,
            border = if (p.activity == a) C.red else C.line, pad = 12.dp) {
            Text(stringResource(l), style = T.Strong)
            Text(stringResource(d), style = T.Small)
        }
    }
    if (p.direction != 0) {
        Text(stringResource(if (p.direction < 0) R.string.p_pace_lose else R.string.p_pace_gain), style = T.Small, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        Flow {
            for (v in listOf(.25, .5, .75)) Chip(stringResource(R.string.p_pace_v, v.toString().replace('.', ',')), p.pace == v, { onChange(p.copy(pace = v)) })
        }
    }
}

@Composable
fun NormCard(n: Norm, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), color = C.meadowL, border = C.meadowL) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(kcal(n.kcal.toDouble()), style = T.Hero.copy(color = C.meadowD))
            Text(" " + stringResource(R.string.kcal_day), style = T.Body.copy(color = C.meadowD), modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(stringResource(R.string.norm_meadow, kcal(n.lo.toDouble()), kcal(n.hi.toDouble())), style = T.Body)
        Text(stringResource(R.string.norm_macros, n.protein, n.fat, n.carbs), style = T.Small, modifier = Modifier.padding(top = 4.dp))
        if (n.tdee > 0) Text(stringResource(R.string.norm_tdee, kcal(n.bmr.toDouble()), kcal(n.tdee.toDouble())), style = T.Small, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun SettingsPage(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val s = h.s
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    var profile by remember { mutableStateOf(s.profile) }
    var manual by remember { mutableStateOf(if (s.manualKcal > 0) s.manualKcal.toString() else "") }
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> c.remind(ok) }
    val norm = Norm.of(profile, manual.toIntOrNull() ?: 0)
    val dirty = profile != s.profile || (manual.toIntOrNull() ?: 0) != s.manualKcal

    Page(stringResource(R.string.settings), nav::close) {
        Section(stringResource(R.string.set_norm))
        NormCard(norm)
        Section(stringResource(R.string.set_profile))
        ProfileFields(profile) { profile = it }
        NumField(manual, { manual = it.filter(Char::isDigit).take(4) }, stringResource(R.string.p_manual), Modifier.fillMaxWidth().padding(top = 12.dp))
        Text(stringResource(R.string.p_manual_hint), style = T.Small, modifier = Modifier.padding(top = 4.dp))
        if (dirty) Btn(stringResource(R.string.save), {
            val m = manual.toIntOrNull()?.takeIf { it in 800..6000 } ?: 0
            c.write { settings.update { it.copy(profile = profile, manualKcal = m) } }
            if (profile.weight != s.profile.weight) c.write { weigh(h.today, profile.weight) }
        }, Modifier.fillMaxWidth().padding(top = 12.dp))

        Section(stringResource(R.string.set_day_start))
        Text(stringResource(R.string.set_day_start_hint), style = T.Small)
        Flow(Modifier.padding(top = 8.dp)) {
            for (hr in listOf(0, 2, 3, 4, 5)) Chip("%02d:00".format(hr), s.dayStart == hr, { c.write { settings.update { it.copy(dayStart = hr) } } })
        }

        Section(stringResource(R.string.set_remind))
        Text(stringResource(R.string.set_remind_hint), style = T.Small)
        Flow(Modifier.padding(top = 8.dp)) {
            Chip(stringResource(R.string.on), s.remind, {
                if (Build.VERSION.SDK_INT >= 33) notify.launch(Manifest.permission.POST_NOTIFICATIONS) else c.remind(true)
            })
            Chip(stringResource(R.string.off), !s.remind, { c.remind(false) })
        }

        Section(stringResource(R.string.set_online))
        Text(stringResource(R.string.set_online_hint), style = T.Small)
        Flow(Modifier.padding(top = 8.dp)) {
            Chip(stringResource(R.string.on), s.online, { c.write { settings.update { it.copy(online = true) } } })
            Chip(stringResource(R.string.off), !s.online, { c.write { settings.update { it.copy(online = false) } } })
        }

        Section(stringResource(R.string.set_export))
        Text(stringResource(R.string.set_export_hint), style = T.Small)
        Btn(stringResource(R.string.set_export_btn), {
            val f = File(File(ctx.cacheDir, "shots").apply { mkdirs() }, "softlanding.csv")
            f.writeText(buildString {
                appendLine("date,meal,name,grams,kcal,protein,fat,carbs")
                fun n(d: Double) = String.format(java.util.Locale.ROOT, "%.1f", d)
                for (e in h.entries) appendLine(listOf(app.softlanding.domain.Days.date(e.day), e.meal.name.lowercase(), "\"" + e.name.replace("\"", "\"\"") + "\"",
                    n(e.grams), n(e.kcal), n(e.p), n(e.f), n(e.c)).joinToString(","))
            })
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".shots", f)
            runCatching {
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }, Modifier.padding(top = 8.dp), kind = Kind.SECONDARY, small = true)

        Section(stringResource(R.string.set_lang))
        val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
        Flow {
            for ((tag, l) in listOf("" to R.string.lang_system, "ru" to R.string.lang_ru, "en" to R.string.lang_en))
                Chip(stringResource(l), cur == tag, { AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)) })
        }

        Section(stringResource(R.string.community))
        Text(stringResource(R.string.community_text), style = T.Small)
        for ((label, sub, url) in listOf(
            Triple(R.string.community_bug, R.string.community_bug_sub, Community.BUG),
            Triple(R.string.community_idea, R.string.community_idea_sub, Community.IDEA),
            Triple(R.string.community_follow, R.string.community_follow_sub, Community.FOLLOW),
            Triple(R.string.community_chat, R.string.community_chat_sub, Community.CHAT),
        )) Card(Modifier.fillMaxWidth().padding(top = 8.dp), onClick = { open(url) }, pad = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(stringResource(label), style = T.Strong); Text(stringResource(sub), style = T.Small) }
                Text("›", style = T.Heading.copy(color = C.red))
            }
        }

        Section(stringResource(R.string.about))
        Text(stringResource(R.string.about_text, BuildConfig.VERSION_NAME), style = T.Small)
        Row2(stringResource(R.string.about_foods), h.foods.size.toString(), Modifier.padding(top = 6.dp))
        Btn(stringResource(R.string.source), { open("https://github.com/bigdevwhale/softlanding") }, Modifier.padding(top = 8.dp), kind = Kind.SECONDARY, small = true)
    }
}
