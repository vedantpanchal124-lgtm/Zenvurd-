package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.PurpleTertiary
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.sin

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        VoiceStudioScreen()
      }
    }
  }
}

enum class VoicePreset(val label: String, val speed: Float, val pitch: Float, val volume: Float) {
  DEFAULT("Default", 1.0f, 1.0f, 0.8f),
  NARRATOR("Narrator", 0.85f, 0.9f, 0.9f),
  ENERGETIC("Energetic", 1.3f, 1.15f, 1.0f),
  CALM("Calm", 0.75f, 0.95f, 0.7f),
  PODCAST("Podcast", 1.05f, 1.0f, 0.95f)
}

enum class PlaybackState {
  STOPPED, PLAYING, PAUSED
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceStudioScreen() {
  val context = LocalContext.current
  var textInput by remember {
    mutableStateOf(
      "Welcome to Voice Studio! Type or paste any text into this large textarea. " +
        "You can explore available speech voices, experiment with preset vocal personas like Narrator or Podcast, " +
        "and fine-tune speed, pitch, and volume enhancement sliders to craft the perfect audio narration."
    )
  }

  // TTS Engine & Voices
  var tts by remember { mutableStateOf<TextToSpeech?>(null) }
  var isTtsReady by remember { mutableStateOf(false) }
  var availableVoices by remember { mutableStateOf<List<Voice>>(emptyList()) }
  var selectedVoice by remember { mutableStateOf<Voice?>(null) }
  var voiceDropdownExpanded by remember { mutableStateOf(false) }

  // Playback & Tracking
  var playbackState by remember { mutableStateOf(PlaybackState.STOPPED) }
  var currentSpeechOffset by remember { mutableIntStateOf(0) }

  // Enhancement Sliders
  var speedSlider by remember { mutableFloatStateOf(1.0f) }
  var pitchSlider by remember { mutableFloatStateOf(1.0f) }
  var volumeSlider by remember { mutableFloatStateOf(0.8f) } // 0.0 to 1.0
  var activePreset by remember { mutableStateOf(VoicePreset.DEFAULT) }

  // Initialize TextToSpeech
  DisposableEffect(context) {
    val listener = object : UtteranceProgressListener() {
      override fun onStart(utteranceId: String?) {
        playbackState = PlaybackState.PLAYING
      }

      override fun onDone(utteranceId: String?) {
        playbackState = PlaybackState.STOPPED
        currentSpeechOffset = 0
      }

      override fun onError(utteranceId: String?) {
        playbackState = PlaybackState.STOPPED
      }

      override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
        currentSpeechOffset = start
      }
    }

    var ttsInstance: TextToSpeech? = null
    ttsInstance = TextToSpeech(context) { status ->
      if (status == TextToSpeech.SUCCESS) {
        ttsInstance?.setOnUtteranceProgressListener(listener)
        isTtsReady = true

        val voices = try {
          ttsInstance?.voices?.toList()
            ?.filter { !it.isNetworkConnectionRequired && !it.name.contains("local", ignoreCase = true).not() }
            ?.sortedBy { it.locale.displayName }
            ?: emptyList()
        } catch (e: Exception) {
          emptyList()
        }

        val fallbackVoices = if (voices.isEmpty()) {
          try { ttsInstance?.voices?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }
        } else voices

        availableVoices = fallbackVoices
        selectedVoice = fallbackVoices.firstOrNull { it.locale.language == Locale.getDefault().language }
          ?: fallbackVoices.firstOrNull()
      } else {
        Log.e("VoiceStudio", "TTS Initialization Failed")
      }
    }
    tts = ttsInstance

    onDispose {
      ttsInstance?.stop()
      ttsInstance?.shutdown()
    }
  }

  // Update TTS parameters whenever sliders or selected voice change
  LaunchedEffect(selectedVoice, speedSlider, pitchSlider) {
    if (isTtsReady && tts != null) {
      try {
        selectedVoice?.let { tts?.voice = it }
        tts?.setSpeechRate(speedSlider)
        tts?.setPitch(pitchSlider)
      } catch (e: Exception) {
        Log.e("VoiceStudio", "Failed to update TTS settings: ${e.message}")
      }
    }
  }

  fun startPlayback(fromOffset: Int = 0) {
    if (!isTtsReady || tts == null) {
      Toast.makeText(context, "Speech engine initializing...", Toast.LENGTH_SHORT).show()
      return
    }
    if (textInput.isBlank()) {
      Toast.makeText(context, "Please enter some text to read.", Toast.LENGTH_SHORT).show()
      return
    }

    val textToSpeak = if (fromOffset > 0 && fromOffset < textInput.length) {
      textInput.substring(fromOffset)
    } else {
      currentSpeechOffset = 0
      textInput
    }

    val params = Bundle().apply {
      putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeSlider)
    }

    playbackState = PlaybackState.PLAYING
    tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "VoiceStudioUtterance")
  }

  fun pausePlayback() {
    if (playbackState == PlaybackState.PLAYING) {
      tts?.stop()
      playbackState = PlaybackState.PAUSED
    }
  }

  fun stopPlayback() {
    tts?.stop()
    playbackState = PlaybackState.STOPPED
    currentSpeechOffset = 0
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.RecordVoiceOver,
              contentDescription = "App Icon",
              tint = IndigoPrimary,
              modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Voice Studio",
              fontWeight = FontWeight.Bold,
              fontSize = 22.sp,
              color = TextPrimary
            )
          }
        },
        actions = {
          IconButton(onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
              val pasted = clip.getItemAt(0).text?.toString()
              if (!pasted.isNullOrBlank()) {
                textInput = pasted
                Toast.makeText(context, "Text pasted from clipboard", Toast.LENGTH_SHORT).show()
              }
            } else {
              Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
          }) {
            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = IndigoPrimary)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = NavySurface,
          titleContentColor = TextPrimary
        )
      )
    },
    containerColor = NavyBackground
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

      // 1. WAVEFORM ANIMATION (Visualizer)
      WaveformVisualizerCard(playbackState = playbackState)

      // 2. TEXT INPUT
      Card(
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "1. TEXT INPUT",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = IndigoPrimary,
              letterSpacing = 1.sp
            )
            Text(
              text = "${textInput.length} chars",
              fontSize = 12.sp,
              color = TextSecondary
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = textInput,
            onValueChange = {
              textInput = it
              if (playbackState != PlaybackState.STOPPED) {
                stopPlayback()
              }
            },
            placeholder = { Text("Type or paste text here...", color = TextSecondary) },
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp)
              .testTag("text_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = IndigoPrimary,
              unfocusedBorderColor = Color(0xFF334155),
              focusedContainerColor = NavySurface,
              unfocusedContainerColor = NavySurface,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              cursorColor = IndigoPrimary
            ),
            shape = RoundedCornerShape(12.dp)
          )
        }
      }

      // 3. VOICE SELECTOR
      Card(
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "2. VOICE SELECTOR",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = IndigoPrimary,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(8.dp))

          Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
              color = NavySurface,
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { voiceDropdownExpanded = true }
                .testTag("voice_selector")
            ) {
              Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = "Voice",
                    tint = PurpleTertiary,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    val voiceDisplayName = selectedVoice?.let { voice ->
                      val localeName = try { voice.locale.displayName } catch (e: Exception) { voice.locale.toString() }
                      "$localeName (${voice.name.substringAfterLast("-").take(15)})"
                    } ?: if (isTtsReady) "Default System Voice" else "Loading Voices..."

                    Text(
                      text = voiceDisplayName,
                      color = TextPrimary,
                      fontWeight = FontWeight.Medium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${availableVoices.size} voices available via Speech Engine",
                      color = TextSecondary,
                      fontSize = 11.sp
                    )
                  }
                }
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = "Expand",
                  tint = TextSecondary
                )
              }
            }

            DropdownMenu(
              expanded = voiceDropdownExpanded,
              onDismissRequest = { voiceDropdownExpanded = false },
              modifier = Modifier
                .background(NavySurface)
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                .height(300.dp)
            ) {
              if (availableVoices.isEmpty()) {
                DropdownMenuItem(
                  text = { Text("No voices detected", color = TextSecondary) },
                  onClick = { voiceDropdownExpanded = false }
                )
              } else {
                availableVoices.forEach { voice ->
                  val isSelected = voice.name == selectedVoice?.name
                  DropdownMenuItem(
                    text = {
                      Column {
                        Text(
                          text = try { voice.locale.displayName } catch (e: Exception) { voice.locale.toString() },
                          color = if (isSelected) IndigoPrimary else TextPrimary,
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                          text = voice.name,
                          color = TextSecondary,
                          fontSize = 11.sp
                        )
                      }
                    },
                    onClick = {
                      selectedVoice = voice
                      voiceDropdownExpanded = false
                      if (playbackState == PlaybackState.PLAYING) {
                        stopPlayback()
                        startPlayback()
                      }
                    }
                  )
                }
              }
            }
          }
        }
      }

      // 4. VOICE PRESETS
      Card(
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "3. VOICE PRESETS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = IndigoPrimary,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(12.dp))

          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            VoicePreset.entries.forEach { preset ->
              val isCurrent = activePreset == preset
              val btnBg = if (isCurrent) IndigoPrimary else NavySurfaceVariant
              val btnText = if (isCurrent) Color(0xFF111827) else TextPrimary

              Surface(
                color = btnBg,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                  .clickable {
                    activePreset = preset
                    speedSlider = preset.speed
                    pitchSlider = preset.pitch
                    volumeSlider = preset.volume
                  }
                  .padding(1.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = preset.label,
                    tint = btnText,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = preset.label,
                    color = btnText,
                    fontSize = 13.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                  )
                }
              }
            }
          }
        }
      }

      // 5. ENHANCEMENT SLIDERS
      Card(
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "4. ENHANCEMENT SLIDERS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = IndigoPrimary,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(16.dp))

          // Speed Slider
          SliderControlRow(
            icon = Icons.Default.Speed,
            title = "Speed",
            valueText = String.format(Locale.US, "%.2fx", speedSlider),
            value = speedSlider,
            valueRange = 0.5f..2.0f,
            onValueChange = { speedSlider = it }
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Pitch Slider
          SliderControlRow(
            icon = Icons.Default.GraphicEq,
            title = "Pitch",
            valueText = String.format(Locale.US, "%.2f", pitchSlider),
            value = pitchSlider,
            valueRange = 0.5f..2.0f,
            onValueChange = { pitchSlider = it }
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Volume Slider
          SliderControlRow(
            icon = Icons.Default.VolumeUp,
            title = "Volume",
            valueText = "${(volumeSlider * 100).toInt()}%",
            value = volumeSlider,
            valueRange = 0.0f..1.0f,
            onValueChange = { volumeSlider = it }
          )
        }
      }

      // 6. PLAYBACK BUTTONS
      Card(
        colors = CardDefaults.cardColors(
          containerColor = Color(0xFF1E1B4B) // Rich deep indigo base
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "5. PLAYBACK CONTROLS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = IndigoPrimary,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // PLAY BUTTON
            Button(
              onClick = {
                if (playbackState == PlaybackState.PAUSED) {
                  startPlayback(fromOffset = currentSpeechOffset)
                } else {
                  startPlayback(fromOffset = 0)
                }
              },
              enabled = playbackState != PlaybackState.PLAYING,
              colors = ButtonDefaults.buttonColors(
                containerColor = IndigoPrimary,
                contentColor = Color(0xFF111827),
                disabledContainerColor = NavySurfaceVariant,
                disabledContentColor = TextSecondary
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1.3f)
                .height(52.dp)
                .testTag("play_button")
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(24.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Play", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // PAUSE / RESUME BUTTON
            FilledTonalButton(
              onClick = {
                if (playbackState == PlaybackState.PLAYING) {
                  pausePlayback()
                } else if (playbackState == PlaybackState.PAUSED) {
                  startPlayback(fromOffset = currentSpeechOffset)
                }
              },
              enabled = playbackState == PlaybackState.PLAYING || playbackState == PlaybackState.PAUSED,
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = PurpleTertiary.copy(alpha = 0.2f),
                contentColor = PurpleTertiary,
                disabledContainerColor = NavySurfaceVariant.copy(alpha = 0.5f),
                disabledContentColor = TextSecondary.copy(alpha = 0.5f)
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("pause_button")
            ) {
              Icon(
                imageVector = if (playbackState == PlaybackState.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = "Pause/Resume"
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (playbackState == PlaybackState.PAUSED) "Resume" else "Pause")
            }

            // STOP BUTTON
            FilledTonalButton(
              onClick = { stopPlayback() },
              enabled = playbackState != PlaybackState.STOPPED,
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                contentColor = Color(0xFFF87171),
                disabledContainerColor = NavySurfaceVariant.copy(alpha = 0.5f),
                disabledContentColor = TextSecondary.copy(alpha = 0.5f)
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("stop_button")
            ) {
              Icon(Icons.Default.Stop, contentDescription = "Stop")
              Spacer(modifier = Modifier.width(4.dp))
              Text("Stop")
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun SliderControlRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  valueText: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit
) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = title, tint = TealAccent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
      }
      Surface(
        color = NavySurfaceVariant,
        shape = RoundedCornerShape(6.dp)
      ) {
        Text(
          text = valueText,
          color = IndigoPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
      }
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = valueRange,
      colors = SliderDefaults.colors(
        thumbColor = IndigoPrimary,
        activeTrackColor = IndigoPrimary,
        inactiveTrackColor = NavySurfaceVariant
      )
    )
  }
}

@Composable
fun WaveformVisualizerCard(playbackState: PlaybackState) {
  val isPlaying = playbackState == PlaybackState.PLAYING

  val infiniteTransition = rememberInfiniteTransition(label = "waveform")
  val phase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2 * Math.PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "phase"
  )

  Card(
    colors = CardDefaults.cardColors(containerColor = NavyCard),
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isPlaying) IndigoPrimary else Color(0xFF334155)
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "6. WAVEFORM ANIMATION",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = IndigoPrimary,
          letterSpacing = 1.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(
                when (playbackState) {
                  PlaybackState.PLAYING -> TealAccent
                  PlaybackState.PAUSED -> Color(0xFFFBBF24)
                  PlaybackState.STOPPED -> TextSecondary
                }
              )
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = playbackState.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = when (playbackState) {
              PlaybackState.PLAYING -> TealAccent
              PlaybackState.PAUSED -> Color(0xFFFBBF24)
              PlaybackState.STOPPED -> TextSecondary
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Waveform bars
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(64.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val barCount = 28
        for (i in 0 until barCount) {
          val normalizedI = i.toFloat() / barCount
          val wave1 = sin((normalizedI * 4 * Math.PI + phase).toDouble()).toFloat()
          val wave2 = sin((normalizedI * 2 * Math.PI - phase * 1.5f).toDouble()).toFloat()
          val combined = (wave1 + wave2) / 2f

          val targetHeightDp = if (isPlaying) {
            12.dp + ((combined + 1f) / 2f * 48).dp
          } else if (playbackState == PlaybackState.PAUSED) {
            12.dp + ((sin((normalizedI * Math.PI).toDouble()).toFloat()) * 16).dp
          } else {
            6.dp
          }

          Box(
            modifier = Modifier
              .width(5.dp)
              .height(targetHeightDp)
              .clip(RoundedCornerShape(2.5.dp))
              .background(
                Brush.verticalGradient(
                  colors = if (isPlaying) {
                    listOf(PurpleTertiary, IndigoPrimary, TealAccent)
                  } else {
                    listOf(TextSecondary.copy(alpha = 0.3f), TextSecondary.copy(alpha = 0.5f))
                  }
                )
              )
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}
