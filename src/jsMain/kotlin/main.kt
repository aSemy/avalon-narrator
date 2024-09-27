import ScriptElem.Companion.fileName
import kotlin.collections.set
import kotlin.time.Duration.Companion.seconds
import kotlinx.browser.document
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onClickFunction
import kotlinx.html.org.w3c.dom.events.Event
import org.w3c.dom.HTMLInputElement
import web.audio.AudioBuffer
import web.audio.AudioContext
import web.http.fetch


//suspend fun startScript(script: List<ScriptElem>, index: Int = 0): Unit = coroutineScope {
//  if (index < script.size) {
//
////    (document.getElementById("progress")!! as HTMLElement)
////      .setAttribute(
////        "aria-valuenow",
////        ceil(((index * 2 - 1).toFloat() / (script.size.toFloat() * 2f)) * 50f).coerceAtMost(100f).toString()
////      )
//
//    when (val elem = script[index]) {
//      is ScriptElem.Pause -> {
//        console.log("Pausing for ${elem.duration}...")
//        delay(elem.duration)
//        console.log("Finished pause")
//        startScript(script, index + 1)
//      }
//
//      is ScriptElem.Text  -> {
//        Audio("./script/${elem.fileName()}.mp3").apply {
//          console.log("Playing ${elem.fileName()}...")
//          onended = {
//            console.log("Finished playing ${elem.fileName()}")
//            launch {
//              delay(0.5.seconds)
//              startScript(script, index + 1)
//            }
//          }
//          play().await()
//        }
//      }
//    }
//  }
//}

private val scriptPlayer by lazy { ScriptPlayer() }

suspend fun main(): Unit = coroutineScope {

  val root = document.getElementById("root") ?: error("missing root")

  root.append {
    div(classes = "btn-group-vertical") {
      attributes["role"] = "group"

      Role.entries.forEach { role ->
        input(type = InputType.checkBox, classes = "btn-check") {
          id = role.name
          attributes["autocomplete"] = "off"
          attributes["checked"] = "true"
          onClickFunction = {

            val selectedRoles = Role.entries.filter { role ->
              (document.getElementById(role.name) as HTMLInputElement).checked
            }.toSet()

            val script = generateScript(selectedRoles).filterIsInstance<ScriptElem.Text>()
              .joinToString("\n") { it.text }

            document.getElementById("script-text")
              ?.innerHTML = script
          }
        }
        label(classes = "btn btn-outline-primary") {
          htmlFor = role.name
          +role.name
        }
      }
    }

    val playButton = button {
      role = "switch"
      attributes["data-playing"] = "false"
      attributes["aria-checked"] = "false"
      +"Play/Pause"

//      onClickFunction = {
//        val selectedRoles = Role.entries.filter { role ->
//          (document.getElementById(role.name) as HTMLInputElement).checked
//        }
//        val script = generateScript(selectedRoles.toSet())
//        launch {
//          startScript(script)
//        }
//      }
    }

//    val playButton = document.querySelector("button")!!
    playButton.addEventListener(
      type = "click",
      { e: Event ->
        console.log("Play/Pause clicked $e")

        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch {
          val selectedRoles = Role.entries.filter { role ->
            (document.getElementById(role.name) as HTMLInputElement).checked
          }
          scriptPlayer.play(selectedRoles.toSet())
        }

//        // Check if context is in suspended state (autoplay policy)
//        if (audioContext.state == web.audio.AudioContextState.suspended) {
//          launch {
//            audioContext.resume()
//          }
//        }

        // Play or pause track depending on state

//        if (!playButton.dataset["playing"].toBoolean()) {
//          //audioElement.play()
//          playButton.dataset["playing"] = "true"
//        } else {
//          //audioElement.pause()
//          playButton.dataset["playing"] = "false";
//        }

      },
      options = false
    )

    div(classes = "script-container") {
      p {
        id = "script-text"

        val selectedRoles = Role.entries.filter { role ->
          (document.getElementById(role.name) as HTMLInputElement).checked
        }.toSet()

        val script = generateScript(selectedRoles).filterIsInstance<ScriptElem.Text>()
          .joinToString("\n") { it.text }

        text(script)
      }
    }
//
//    div(classes = "progress") {
//      id = "progress"
//      attributes["role"] = "progressbar"
//      attributes["aria-valuenow"] = "prg"
//      attributes["aria-valuemin"] = "0"
//      attributes["aria-valuemax"] = "100"
//      attributes["style.width.%"] = "prg"
//      div(classes = "progress-bar progress-bar-striped progress-bar-animated") {
////        id = "progress-bar"
//        style = "width: 50%"
//      }
//    }
  }
}


private class ScriptPlayer {
  private val audioContext by lazy { AudioContext() }

  private val audioBuffers = mutableMapOf<String, AudioBuffer>()

  suspend fun play(roles: Set<Role>) {
    console.log("ScriptPlay.play $roles")

    val script = generateScript(roles)

    script.filterIsInstance<ScriptElem.Text>().forEach {
      audioBuffers.getOrPut(it.fileName()) {
        fetchAudioBuffer(it)
      }
    }

    script.forEach { elem ->
      when (elem) {
        is ScriptElem.Pause -> delay(elem.duration)
        is ScriptElem.Text  -> {
          val audioBuffer = audioBuffers[elem.fileName()] ?: error("missing audio for $elem")
          val source = audioContext.createBufferSource()
          source.buffer = audioBuffer
          source.connect(audioContext.destination)
          source.start(0.0)

          // Wait for the current track to finish
          delay(audioBuffer.duration.seconds)
          // Add a bit of delay between audio tracks
          delay(0.7.seconds)
        }
      }
    }
  }

  suspend fun stop() {
    audioContext.suspend()
  }

  private suspend fun fetchAudioBuffer(text: ScriptElem.Text): AudioBuffer {
    val url = "./script/${text.fileName()}.mp3?raw=true"
    console.log("Fetching audio buffer from $url...")
    val response = fetch(url)
    console.log("   response: $response ${response.status} ${response.statusText}")
    val arrayBuffer = response.arrayBuffer()
    console.log("   arrayBuffer: $arrayBuffer")
    val audioData = audioContext.decodeAudioData(arrayBuffer)
    console.log("Fetched audio buffer $audioData from $url")
    return audioData
  }
}
