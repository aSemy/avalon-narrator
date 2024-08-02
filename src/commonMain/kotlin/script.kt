import Role.*
import Role.Companion.canSee
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


//enum class Role(
//  val knownByMerlin: Boolean = false,
//) {
//  Merlin,
//  Percival,
//  Morgana(knownByMerlin = true),
//  Oberon(knownByMerlin = true),
//  Mordred,
//  Assassin(knownByMerlin = true),
//}

enum class Role(val team: Team) {

  Merlin(team = Team.Good),
  Percival(team = Team.Good),
  Mordred(team = Team.Evil),
  Assassin(team = Team.Evil),
  Morgana(team = Team.Evil),
  Oberon(team = Team.Evil),
  ;

  enum class Team { Good, Evil }

  companion object {

    fun Role.canSee(other: Role): Boolean = other in viewableRoles()

    private fun Role.viewableRoles(): Set<Role> {
      return when (this) {
        Assassin -> setOf(Morgana, Mordred)
        Mordred  -> setOf(Morgana, Assassin)
        Morgana  -> setOf(Mordred, Assassin)
        Oberon   -> emptySet()
        Merlin   -> setOf(Morgana, Oberon, Assassin)
        Percival -> setOf(Merlin, Morgana)
      }
    }
  }
}

sealed interface ScriptElem {
  data class Text(val text: String) : ScriptElem
  data class Pause(val duration: Duration) : ScriptElem

  companion object {
    fun Text.fileName(): String = text.map { if (it.isLetterOrDigit()) it else "-" }.joinToString("")
  }
}

private fun MutableList<ScriptElem>.say(text: String) {
  this.add(ScriptElem.Text(text))
}

private fun MutableList<ScriptElem>.pause(duration: Duration) {
  this.add(ScriptElem.Pause(duration))
}

fun generateScript(
  roles: Set<Role>,
  enableMinionPriority: Boolean = false,
): List<ScriptElem> {
  val evilRoles = roles.filter { it.team == Team.Evil }

  return buildList {
    say("Okay, let's begin.")
    say("Everyone close your eyes.")
    pause(0.25.seconds)

    val minionsOfMordred =
      if (Oberon in roles) {
        "Mordred, Assassin, Morgana"
      } else {
        "Minions of Mordred"
      }

    if (!enableMinionPriority || evilRoles.isEmpty()) {
      if (Oberon in roles) {
        say("$minionsOfMordred, (but not Oberon), open your eyes and look around, so that you can identify your fellow agents of evil.")
      } else {
        say("$minionsOfMordred, open your eyes and look around, so that you can identify your fellow agents of evil.")
      }
      pause(5.seconds)
    } else {
      if (Mordred in roles) say("Mordred, raise *one* finger.")
      if (Assassin in roles) say("Assassin, raise *two* fingers.")
      if (Morgana in roles) say("Morgana, raise *three* fingers.")
      if (Oberon in roles) say("Oberon, keep your eyes closed and finger down.")
      pause(2.seconds)
    }

    if (enableMinionPriority) {
      say("Minions of Mordred, close your eyes, and put your fingers down.")
    } else {
      say("Minions of Mordred, close your eyes.")
    }
    say("All players should have their eyes closed.")
//    say("All players should have their eyes closed, with their hands in a closed fist in front of them.")

    val rolesKnownByMerlin = evilRoles.filter { Merlin.canSee(it) }.sorted()
    say("${rolesKnownByMerlin.joinToString(", ")}, extend your thumbs so that Merlin can identify you.")
    say("Merlin, open your eyes and see ${rolesKnownByMerlin.size} agents of evil.")
    pause(5.seconds) // big pause
    say("Merlin, close your eyes.")
    say("Minions of Mordred - put your thumbs down.")
    say("All players should have their eyes closed.")

//    pause(0.5.seconds)

    if (Percival in roles) {
      if (Morgana in roles) {
        say("Merlin and Morgana, extend your thumbs so that Percival may identify you.")
        say("Percival, open your eyes, and identify Merlin and Morgana.")
        pause(4.seconds)
        say("Percival, close your eyes.")
        say("Merlin and Morgana: put your thumbs down.")
      } else {
        say("Merlin, extend your thumb so that Percival may identify you.")
        say("Percival, open your eyes and identify Merlin.")
        pause(4.seconds)
        say("Percival, close your eyes.")
        say("Merlin, put your thumb down.")
      }
    }

    pause(0.5.seconds)

    say("Everyone, open your eyes. Let the game begin!")
  }
}
