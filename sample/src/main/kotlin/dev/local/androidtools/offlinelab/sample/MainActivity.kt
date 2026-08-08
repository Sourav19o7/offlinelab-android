package dev.local.androidtools.offlinelab.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.local.androidtools.offlinelab.OfflineLab
import dev.local.androidtools.offlinelab.model.NetworkProfile
import dev.local.androidtools.offlinelab.model.SimulatedEvent
import dev.local.androidtools.offlinelab.model.SimulationOutcome
import dev.local.androidtools.offlinelab.sample.ui.theme.OfflineLabSampleTheme
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient.Builder()
        .addInterceptor(OfflineLab.interceptor())
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OfflineLabSampleTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    OfflineLabDemoScreen(client)
                }
            }
        }
    }
}

private val builtInProfiles = listOf(
    "Normal" to NetworkProfile.Normal,
    "Offline" to NetworkProfile.Offline,
    "Slow3G" to NetworkProfile.Slow3G,
    "HighLatency" to NetworkProfile.HighLatency,
    "RandomTimeout" to NetworkProfile.RandomTimeout,
    "Flaky" to NetworkProfile.Flaky,
    "ServerErrors" to NetworkProfile.ServerErrors,
    "RateLimited" to NetworkProfile.RateLimited,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineLabDemoScreen(client: OkHttpClient) {
    var activeProfileName by remember { mutableStateOf(OfflineLab.currentProfile()::class.simpleName.orEmpty()) }
    var lastResult by remember { mutableStateOf("No request made yet.") }
    var events by remember { mutableStateOf(OfflineLab.eventHistory()) }
    var customLatency by remember { mutableStateOf("1500") }
    var customFailureRate by remember { mutableStateOf("0.25") }

    DisposableEffect(Unit) {
        OfflineLab.setListener { events = OfflineLab.eventHistory() }
        onDispose { OfflineLab.setListener(null) }
    }

    fun applyProfile(name: String, profile: NetworkProfile) {
        OfflineLab.setProfile(profile)
        activeProfileName = name
    }

    fun performRequest() {
        val request = Request.Builder().url("https://example.com/").build()
        client.newCall(request).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    lastResult = "FAILED (likely simulated): ${e::class.simpleName}: ${e.message}"
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    val simulated = response.header("X-OfflineLab-Simulated") == "true"
                    lastResult = "${if (simulated) "SIMULATED" else "REAL"} response: ${response.code}"
                    response.close()
                }
            },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("OfflineLab Sample") }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DEBUG NETWORK TESTING TOOL — DO NOT SHIP ENABLED", color = Color(0xFFD32F2F), style = MaterialTheme.typography.labelLarge)
                    Text("Active profile: $activeProfileName", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text("Built-in profiles", style = MaterialTheme.typography.titleSmall)
            builtInProfiles.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { (name, profile) ->
                        Button(onClick = { applyProfile(name, profile) }, modifier = Modifier.weight(1f)) {
                            Text(name)
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Custom profile", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(value = customLatency, onValueChange = { customLatency = it }, label = { Text("Latency (ms)") })
                    OutlinedTextField(value = customFailureRate, onValueChange = { customFailureRate = it }, label = { Text("Failure rate (0.0-1.0)") })
                    Button(
                        onClick = {
                            applyProfile(
                                "Custom",
                                NetworkProfile.Custom(
                                    latencyMs = customLatency.toLongOrNull() ?: 0,
                                    failureRate = customFailureRate.toDoubleOrNull() ?: 0.0,
                                    httpErrorRate = 0.0,
                                    timeoutRate = 0.0,
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Apply custom profile") }
                }
            }

            Button(onClick = { applyProfile("Normal", NetworkProfile.Normal) }, modifier = Modifier.fillMaxWidth()) {
                Text("Reset to Normal")
            }

            Button(onClick = { performRequest() }, modifier = Modifier.fillMaxWidth()) {
                Text("Perform sample request")
            }

            Text(lastResult, style = MaterialTheme.typography.bodyMedium)

            Text("Event history (${events.size})", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(events.reversed()) { event -> EventRow(event) }
            }
        }
    }
}

@Composable
private fun EventRow(event: SimulatedEvent) {
    val label = when (event.outcome) {
        SimulationOutcome.PASSED_THROUGH -> "real"
        SimulationOutcome.OFFLINE_FAILURE -> "simulated offline"
        SimulationOutcome.TIMEOUT -> "simulated timeout"
        SimulationOutcome.HTTP_ERROR -> "simulated ${event.simulatedStatusCode}"
        SimulationOutcome.REAL_ERROR -> "real error"
    }
    Text("${event.method} ${event.url} — $label (${event.profileName})", style = MaterialTheme.typography.bodySmall)
}
