@file:OptIn(ExperimentalSerializationApi::class)

import ScriptElem.Companion.fileName
import com.google.cloud.texttospeech.v1.*
import java.nio.file.Path
import kotlin.io.path.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream


fun main(args: Array<String>) {

  val outputDir = args.firstOrNull()?.let(::Path) ?: error("Output dir missing")

  outputDir.createDirectories()

  val map =
    getAllSubsets(Role.entries)
      .flatMap { generateScript(it, enableMinionPriority = false) }
      .filterIsInstance<ScriptElem.Text>()
      .distinctBy { it.text }
      .associate {
        val id = it.fileName()
        val file = generate(script = it, outputDir = outputDir)
        id to file.relativeTo(outputDir).invariantSeparatorsPathString
      }

  val idFile = outputDir.resolve("id.json")
  json.encodeToStream(map, idFile.outputStream())
}

private val json = Json {
  prettyPrint = true
  prettyPrintIndent = "  "
}


private fun generate(
  script: ScriptElem.Text,
  outputName: String = script.fileName(),
  outputDir: Path,
): Path {

  TextToSpeechClient.create().use { textToSpeechClient ->
    // Set the text input to be synthesized
    val input = SynthesisInput {
      text = script.text
    }

    val voice = VoiceSelectionParams {
      languageCode = "en-GB"
      ssmlGender = SsmlVoiceGender.FEMALE
      name = "en-GB-Studio-C"
    }

    // Select the type of audio file you want returned
    val audioConfig = AudioConfig {
      audioEncoding = AudioEncoding.MP3
      speakingRate = 0.9
    }

    // Perform the text-to-speech request on the text input with
    // the selected voice parameters and audio file type
    val response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig)

    // Get the audio contents from the response
    val file = outputDir.resolve("$outputName.mp3")

    file.outputStream().use { out ->
      out.write(response.audioContent.toByteArray())
      println("Audio content written to file \"$outputName.mp3\"")
    }

    return file
  }
}


private fun SynthesisInput(build: SynthesisInput.Builder.() -> Unit): SynthesisInput =
  SynthesisInput.newBuilder().apply(build).build()


private fun AudioConfig(build: AudioConfig.Builder.() -> Unit): AudioConfig =
  AudioConfig.newBuilder().apply(build).build()


private fun VoiceSelectionParams(build: VoiceSelectionParams.Builder.() -> Unit): VoiceSelectionParams =
  VoiceSelectionParams.newBuilder().apply(build).build()


private fun <T> getAllSubsets(input: List<T>): List<Set<T>> {
  val allMasks = 1 shl input.size
  return (0..<allMasks).map { mask ->
    buildSet {
      for (i in input.indices) {
        if ((mask and (1 shl i)) > 0) {
          add(input[i])
        }
      }
    }
  }
}
