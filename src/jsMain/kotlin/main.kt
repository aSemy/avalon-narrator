import ScriptElem.Companion.fileName
import kotlin.time.Duration.Companion.seconds
import kotlinx.browser.document
import kotlinx.coroutines.*
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onClickFunction
import org.w3c.dom.Audio
import org.w3c.dom.HTMLInputElement
import web.audio.AudioContext
import web.events.EventHandler
import web.speech.SpeechSynthesisUtterance
import web.speech.speechSynthesis


fun speakText(text: String, onEnd: () -> Unit) {
  val utterance = SpeechSynthesisUtterance(text).apply {
    onend = EventHandler { onEnd() }
    lang = "en-GB"
    rate = 0.85f
  }
  speechSynthesis.speak(utterance)
}


suspend fun startScript(script: List<ScriptElem>, index: Int = 0) {
  if (index < script.size) {

//    (document.getElementById("progress")!! as HTMLElement)
//      .setAttribute(
//        "aria-valuenow",
//        ceil(((index * 2 - 1).toFloat() / (script.size.toFloat() * 2f)) * 50f).coerceAtMost(100f).toString()
//      )

    when (val elem = script[index]) {
      is ScriptElem.Pause -> {
        console.log("Pausing for ${elem.duration}...")
        delay(elem.duration)
        console.log("Finished pause")
        startScript(script, index + 1)
//        window.setTimeout({
//          startScript(script, index + 1)
//        }, elem.duration.inWholeMilliseconds.toInt())
      }

      is ScriptElem.Text  -> {
        Audio("./script/${elem.fileName()}.mp3").apply {
          console.log("Playing ${elem.fileName()}...")
          onended = {
            console.log("Finished playing ${elem.fileName()}")
            @Suppress("OPT_IN_USAGE")
            GlobalScope.launch {
              delay(0.5.seconds)
              startScript(script, index + 1)
            }
          }
          play().await()
        }
//        window.setTimeout({
//          speakText(elem.text) { startScript(script, index + 1) }
//        }, 0.5.seconds.inWholeMilliseconds.toInt())
      }
    }
  }
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun main() {
  val audioContext = AudioContext()

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

    button {
      +"Start Script"
      onClickFunction = {
        val selectedRoles = Role.entries.filter { role ->
          (document.getElementById(role.name) as HTMLInputElement).checked
        }
        val script = generateScript(selectedRoles.toSet())
        GlobalScope.launch {
          startScript(script)
        }
      }
    }

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
