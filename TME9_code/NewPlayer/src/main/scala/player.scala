
package upmc.akka.culto

import math._

import javax.sound.midi._
import javax.sound.midi.ShortMessage._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import akka.actor.{Props, Actor, ActorRef, ActorSystem}

abstract class ObjetMusical
case class Note (pitch:Int, vol:Int, dur:Int) extends ObjetMusical
case class Chord (date:Int, notes:List[Note]) extends ObjetMusical
case class Voix (id:Int, chords:List[Chord]) extends ObjetMusical
case class MidiNote (pitch:Int, vel:Int, dur:Int, at:Int) 
case class Chordseq (chords:List[Chord]) extends ObjetMusical

object player {

val system = ActorSystem("Player")
val info = MidiSystem.getMidiDeviceInfo().filter(_.getName == "Gervill").headOption
// or "SimpleSynth virtual input" or "Gervill"
val device = info.map(MidiSystem.getMidiDevice).getOrElse {
    println("[ERROR] Could not find Gervill synthesizer.")
      sys.exit(1)
  }

val rcvr = device.getReceiver()



/////////////////////////////////////////////////
def note_on (pitch:Int, vel:Int, chan:Int): Unit = {
    val msg = new ShortMessage
    msg.setMessage(NOTE_ON, chan, pitch, vel)
    rcvr.send(msg, -1)
}

def note_off (pitch:Int, chan:Int): Unit = {
    val msg = new ShortMessage
    msg.setMessage(NOTE_ON, chan, pitch, 0)
    rcvr.send(msg, -1)
}



//////////////////////////////////////////////////

class Intrument (chan :Int) extends Actor{
  def receive = {
    case MidiNote(p,v, d, at) => {
      system.scheduler.scheduleOnce ((at) milliseconds) (note_on (p,v,chan))
      system.scheduler.scheduleOnce ((at+d) milliseconds) (note_off (p,chan))
    }
    case "kill" =>
      println ("bye")
      device.close()
  }
}

//////////////////////////////////////////////////

class PlayerActor () extends Actor{
  var Instruments: List[ActorRef] = List(
    context.actorOf(Props(new Intrument(0)), "Instrument1"),
    context.actorOf(Props(new Intrument(1)), "Instrument2"),
    context.actorOf(Props(new Intrument(2)), "Instrument3"),
    context.actorOf(Props(new Intrument(3)), "Instrument4"))

  def receive = {
    case Voix (num,chords) => {
      println("\n\n====Test receive voix ====\n\n")
      chords.foreach (chord => {
      val date = chord.date
      val notes = chord.notes
      notes.foreach (note => Instruments(num) ! MidiNote(note.pitch, note.vol, note.dur, date))
      })
    }
    case Chordseq (l) => {
      println("\n\n====Test receive Chordseq====\n\n")
      l.foreach (chord => {
      val date = chord.date
      val notes = chord.notes
      notes.foreach (note => Instruments(0) ! MidiNote(note.pitch, note.vol, note.dur, date))
      })
    }
 
    case "kill" => {
      println ("bye")
      device.close()
    }

    case s : String =>
      println ("recoit message :" + s)
  }
}

  
//////////////////////////////////////////////////

def main(args: Array[String]): Unit = {
    device.open()
    var msgch = new ShortMessage
    msgch.setMessage(PROGRAM_CHANGE, 0, 0)
    rcvr.send(msgch, -1)
    msgch.setMessage(PROGRAM_CHANGE,  12, 1)
    rcvr.send(msgch, -1)
    msgch.setMessage(PROGRAM_CHANGE,  56, 2)
    rcvr.send(msgch, -1)
    msgch.setMessage(PROGRAM_CHANGE,  68, 3)
    rcvr.send(msgch, -1)

    val player = system.actorOf(Props[PlayerActor], "PlayerActor")
    println ("control c pour quitter ")
 }
}

