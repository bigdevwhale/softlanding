package app.softlanding.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.softlanding.R
import app.softlanding.container
import app.softlanding.domain.Days
import app.softlanding.domain.Norm
import app.softlanding.domain.Profile
import app.softlanding.ui.art.FlightScene
import app.softlanding.ui.art.SceneState
import app.softlanding.ui.components.Btn
import app.softlanding.ui.components.Kind
import app.softlanding.ui.settings.NormCard
import app.softlanding.ui.settings.ProfileFields
import app.softlanding.ui.theme.C
import app.softlanding.ui.theme.T
import kotlinx.coroutines.launch

@Composable
fun Onboarding(step0: Int = 0) {
    val ctx = LocalContext.current
    val c = ctx.container
    var step by remember { mutableIntStateOf(step0) }
    var p by remember { mutableStateOf(Profile()) }
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> c.remind(ok) }
    BackHandler(step > 0) { step-- }

    fun finish() {
        c.scope.launch {
            val today = Days.of(System.currentTimeMillis(), 4)
            c.settings.update { it.copy(onboarded = true, started = today, profile = p, seen = today - 1) }
            c.weigh(today, p.weight)
        }
        if (Build.VERSION.SDK_INT >= 33) notify.launch(Manifest.permission.POST_NOTIFICATIONS) else c.remind(true)
    }

    Column(Modifier.fillMaxSize().background(C.paper)) {
        if (step == 0) {
            Box(Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))) {
                FlightScene(SceneState(.62f, .35f, hour = 9), Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.app_name), style = T.Hero, modifier = Modifier.padding(top = 20.dp))
                Text(stringResource(R.string.ob_1), style = T.Body, modifier = Modifier.padding(top = 10.dp))
                Text(stringResource(R.string.ob_1b), style = T.Body.copy(color = C.inkSoft), modifier = Modifier.padding(top = 10.dp))
            }
        } else Column(Modifier.weight(1f).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(stringResource(if (step == 1) R.string.ob_2 else R.string.ob_3), style = T.Title, modifier = Modifier.padding(top = 20.dp))
            Text(stringResource(if (step == 1) R.string.ob_2b else R.string.ob_3b), style = T.Body.copy(color = C.inkSoft), modifier = Modifier.padding(top = 8.dp, bottom = 14.dp))
            if (step == 1) ProfileFields(p) { p = it }
            else {
                NormCard(Norm.of(p))
                Text(stringResource(R.string.ob_3c), style = T.Body, modifier = Modifier.padding(top = 14.dp))
                Text(stringResource(R.string.ob_3d), style = T.Small, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f)) {
                for (i in 0..2) Box(Modifier.padding(end = 6.dp).size(if (i == step) 22.dp else 8.dp, 8.dp).clip(CircleShape).background(if (i == step) C.red else C.lineD))
            }
            if (step > 0) Btn(stringResource(R.string.back), { step-- }, kind = Kind.GHOST, small = true)
            Btn(stringResource(if (step < 2) R.string.next else R.string.ob_start), { if (step < 2) step++ else finish() })
        }
    }
}
