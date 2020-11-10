package upmc.akka.culto

import akka.actor.{Actor, ActorRef, ActorSystem, Props}


abstract class ObjetMusical
case class Note (pitch:Int, vol:Int, dur:Int) extends ObjetMusical
case class Chord (date:Int, notes:List[Note]) extends ObjetMusical
case class Chordseq (chords:List[Chord]) extends ObjetMusical
case class Voix (id:Int, chords:List[Chord]) extends ObjetMusical
case class MidiNote (pitch:Int, vel:Int, dur:Int, at:Int)

sealed trait VoicerSystem
case class Play(sequence: Chordseq) extends VoicerSystem
case class Work(sequence: List[Chord]) extends VoicerSystem
case class Result(sequence: List[Voix]) extends VoicerSystem
case class ChordMusic(sequence: List[Voix]) extends VoicerSystem

case class PlayVoice(num : Int, chords:List[Chord])
case class PlayNormal(chordseq:Chordseq)

// Worker do their work
class Worker extends Actor {
  def receive = {
    case Work(value: List[Chord]) =>
      sender ! Result( calculate (value) )
  }

  /**
    * calculate, get voice list from chord list
    * @param obj list of chord
    * @return list of voice
    */
  def calculate(obj: List[Chord]): List[Voix] = {
    List(
      getVoiceListFromChordList(0, obj),
      getVoiceListFromChordList(1, obj),
      getVoiceListFromChordList(2, obj),
      getVoiceListFromChordList(3, obj)
    )
  }

  /**
    * get voice from chord list
    * @param idVoice
    * @param list
    * @return voice
    */
  def getVoiceListFromChordList(idVoice: Int, list: List[Chord]): Voix = {
    var returnListChord: List[Chord] = Nil
    for (chord <- list)
      chord match {
        case Chord(date, notes) => {
          if (idVoice < notes.size){
            returnListChord = Chord(date, List(notes(idVoice)))::returnListChord
          }
        }
      }

    Voix(idVoice, returnListChord)
  }
}

class Master(nbWorkers: Int, nbElement: Int, nbVoice: Int, listener: ActorRef) extends Actor {
  var workers: List[ActorRef] = Nil
  var voices: List[Voix] = Nil

  var voice0: Voix = Voix(0, List())
  var voice1: Voix = Voix(1, List())
  var voice2: Voix = Voix(2, List())
  var voice3: Voix = Voix(3, List())

  var nbMessage: Int = 0
  var nbResults: Int = 0
  var indexWorker: Int = 0

  for (i <- (0 to nbWorkers).reverse)
    workers = context.actorOf(Props[Worker], name = "workers" + i) :: workers

  def receive = {
    case Play(value: Chordseq) =>
      value match {
        case Chordseq(l) => {
          nbMessage = l.size / nbElement
          println("====> Number of Chord : " + nbMessage)
          for(i <- 1 to  nbMessage){
            workers(indexWorker % nbWorkers) ! Work(l.slice(0 + indexWorker*nbElement, nbElement + indexWorker * nbElement))
            indexWorker = indexWorker + 1
          }

          workers(indexWorker % nbWorkers) ! Work(l.slice(indexWorker * nbElement, l.size))
        }
      }

    case Result(value: List[Voix]) => {
      for (music <- value) {
        music match {
          case Voix(id, list) => {
            id match {
              case 0 => voice0 = appendChordToVoice(voice0, list)
              case 1 => voice1 = appendChordToVoice(voice1, list)
              case 2 => voice2 = appendChordToVoice(voice2, list)
              case 3 => voice3 = appendChordToVoice(voice3, list)
            }
          }
        }
      }

      nbResults = nbResults + 1
      if (nbResults == nbMessage + 1) {
        listener ! ChordMusic(List(voice0, voice1, voice2, voice3))
      context.stop(self)
      }
    }

    def appendChordToVoice(v: Voix, chords: List[Chord]): Voix = {
      v match {
        case Voix(id, list) =>
          Voix(id, list:::chords)
      }
    }
  }
}

/**
  * Listener
  */
class Listener extends Actor {
  val remote = context.actorSelection("akka.tcp://Player@127.0.0.1:6005/user/PlayerActor")
  def receive = {
    // Play chord music
    case ChordMusic(value: List[Voix]) => {
      for (voice <- value) {
        println(voice)
        remote ! voice
      }
    }
  }
}

/* MAIN */
object PlayCantate extends App {
  //Question 1
  val cantate  = Chordseq ( List (Chord (0 , List (Note (64, 100, 1000)))
    , Chord (1000 , List (Note (66, 100, 1000)))
    , Chord (2000 , List (Note (64, 100, 1000)))
    , Chord (3000 , List (Note (67, 100, 1000)))
    , Chord (4000 , List (Note (65, 100, 1000)))
  ))

  val bwv0416 = Chordseq ( List (Chord (0 , List (Note (50, 100, 4000), Note (65, 100, 4000), Note (69, 100, 4000), Note (62, 100, 4000)))
    , Chord (4000 , List (Note (49, 100, 1000), Note (64, 100, 1000), Note (69, 100, 1000), Note (57, 100, 1000)))
    , Chord (5000 , List (Note (50, 100, 1000), Note (62, 100, 1000), Note (65, 100, 1000), Note (57, 100, 1000)))
    , Chord (6000 , List (Note (52, 100, 1000), Note (61, 100, 1000), Note (67, 100, 1000), Note (55, 100, 1000)))
    , Chord (7000 , List (Note (53, 100, 500), Note (62, 100, 500), Note (69, 100, 1000), Note (62, 100, 1000)))
    , Chord (7500 , List (Note (55, 100, 500), Note (64, 100, 500)))
    , Chord (8000 , List (Note (57, 100, 1000), Note (65, 100, 1000), Note (65, 100, 1000), Note (62, 100, 1000)))
    , Chord (9000 , List (Note (45, 100, 1000), Note (61, 100, 1000), Note (64, 100, 1000), Note (57, 100, 1000)))
    , Chord (10000 , List (Note (50, 100, 1000), Note (57, 100, 1000), Note (62, 100, 1000), Note (53, 100, 1000)))
    , Chord (11000 , List (Note (50, 100, 1000), Note (65, 100, 1000), Note (69, 100, 1000), Note (62, 100, 1000)))
    , Chord (12000 , List (Note (48, 100, 1000), Note (64, 100, 1000), Note (69, 100, 1000), Note (57, 100, 1000)))
    , Chord (13000 , List (Note (46, 100, 1000), Note (62, 100, 1000), Note (67, 100, 1000), Note (58, 100, 1000)))
    , Chord (14000 , List (Note (45, 100, 500), Note (64, 100, 1000), Note (72, 100, 1000), Note (60, 100, 1000)))
    , Chord (14500 , List (Note (48, 100, 500)))
    , Chord (15000 , List (Note (53, 100, 1000), Note (65, 100, 1000), Note (69, 100, 1000), Note (60, 100, 1000)))
    , Chord (16000 , List (Note (58, 100, 1000), Note (65, 100, 1000), Note (65, 100, 1000), Note (62, 100, 500)))
    , Chord (16500 , List (Note (61, 100, 500)))
    , Chord (17000 , List (Note (58, 100, 1000), Note (64, 100, 500), Note (67, 100, 1000), Note (62, 100, 1000)))
    , Chord (17500 , List (Note (62, 100, 500)))
    , Chord (18000 , List (Note (57, 100, 1000), Note (64, 100, 1000), Note (69, 100, 1000), Note (61, 100, 1000)))
    , Chord (19000 , List (Note (57, 100, 500), Note (64, 100, 1000), Note (69, 100, 500), Note (60, 100, 1000)))
    , Chord (19500 , List (Note (55, 100, 500), Note (71, 100, 500)))
    , Chord (20000 , List (Note (53, 100, 1500), Note (69, 100, 1000), Note (72, 100, 1000), Note (60, 100, 500)))
    , Chord (20500 , List (Note (57, 100, 500)))
    , Chord (21000 , List (Note (67, 100, 1000), Note (74, 100, 500), Note (59, 100, 500)))
    , Chord (21500 , List (Note (52, 100, 500), Note (76, 100, 500), Note (61, 100, 500)))
    , Chord (22000 , List (Note (50, 100, 1000), Note (65, 100, 1000), Note (77, 100, 1000), Note (62, 100, 500)))
    , Chord (22500 , List (Note (60, 100, 500)))
    , Chord (23000 , List (Note (55, 100, 1000), Note (67, 100, 1000), Note (76, 100, 1000), Note (58, 100, 1000)))
    , Chord (24000 , List (Note (56, 100, 1000), Note (65, 100, 500), Note (74, 100, 1000), Note (59, 100, 1000)))
    , Chord (24500 , List (Note (64, 100, 500)))
    , Chord (25000 , List (Note (57, 100, 1000), Note (64, 100, 1000), Note (73, 100, 1000), Note (57, 100, 1000)))
    , Chord (26000 , List (Note (50, 100, 1000), Note (65, 100, 1000), Note (74, 100, 1000), Note (57, 100, 1000)))
    , Chord (27000 , List (Note (55, 100, 1000), Note (65, 100, 1000), Note (74, 100, 1000), Note (59, 100, 1000)))
    , Chord (28000 , List (Note (48, 100, 1000), Note (64, 100, 1000), Note (76, 100, 1000), Note (59, 100, 500)))
    , Chord (28500 , List (Note (57, 100, 1000)))
    , Chord (29000 , List (Note (47, 100, 1000), Note (71, 100, 1000), Note (74, 100, 1000)))
    , Chord (29500 , List (Note (56, 100, 500)))
    , Chord (30000 , List (Note (45, 100, 1000), Note (64, 100, 1000), Note (72, 100, 1000), Note (57, 100, 1000)))
    , Chord (31000 , List (Note (52, 100, 1000), Note (64, 100, 500), Note (71, 100, 1000), Note (56, 100, 1000)))
    , Chord (31500 , List (Note (62, 100, 500)))
    , Chord (32000 , List (Note (53, 100, 1000), Note (60, 100, 500), Note (69, 100, 1000), Note (57, 100, 1000)))
    , Chord (32500 , List (Note (62, 100, 500)))
    , Chord (33000 , List (Note (52, 100, 1000), Note (64, 100, 1000), Note (68, 100, 1000), Note (59, 100, 1000)))
    , Chord (34000 , List (Note (45, 100, 1000), Note (64, 100, 1000), Note (69, 100, 1000), Note (60, 100, 1000)))
    , Chord (35000 , List (Note (55, 100, 500), Note (67, 100, 1000), Note (74, 100, 1000), Note (59, 100, 1000)))
    , Chord (35500 , List (Note (53, 100, 500)))
    , Chord (36000 , List (Note (52, 100, 1000), Note (67, 100, 1000), Note (72, 100, 1000), Note (60, 100, 1000)))
    , Chord (37000 , List (Note (50, 100, 1000), Note (65, 100, 1000), Note (71, 100, 1000), Note (62, 100, 1000)))
    , Chord (38000 , List (Note (48, 100, 1000), Note (67, 100, 1000), Note (72, 100, 1000), Note (64, 100, 1000)))
    , Chord (39000 , List (Note (50, 100, 500), Note (65, 100, 1000), Note (69, 100, 1000), Note (62, 100, 1000)))
    , Chord (39500 , List (Note (48, 100, 500)))
    , Chord (40000 , List (Note (46, 100, 1000), Note (65, 100, 1000), Note (69, 100, 1000), Note (62, 100, 500)))
    , Chord (40500 , List (Note (60, 100, 500)))
    , Chord (41000 , List (Note (48, 100, 1000), Note (64, 100, 1000), Note (67, 100, 1000), Note (58, 100, 1000)))
    , Chord (42000 , List (Note (53, 100, 1000), Note (60, 100, 1000), Note (65, 100, 1000), Note (57, 100, 1000)))
    , Chord (43000 , List (Note (54, 100, 1000), Note (62, 100, 1000), Note (69, 100, 1000), Note (57, 100, 1000)))
    , Chord (44000 , List (Note (55, 100, 1000), Note (62, 100, 1000), Note (70, 100, 1000), Note (55, 100, 500)))
    , Chord (44500 , List (Note (53, 100, 500)))
    , Chord (45000 , List (Note (49, 100, 1000), Note (64, 100, 1000), Note (69, 100, 500), Note (52, 100, 1000)))
    , Chord (45500 , List (Note (67, 100, 500)))
    , Chord (46000 , List (Note (50, 100, 500), Note (62, 100, 1000), Note (65, 100, 1000), Note (57, 100, 1000)))
    , Chord (46500 , List (Note (48, 100, 500)))
    , Chord (47000 , List (Note (46, 100, 500), Note (62, 100, 1000), Note (67, 100, 1000), Note (55, 100, 500)))
    , Chord (47500 , List (Note (45, 100, 500), Note (57, 100, 500)))
    , Chord (48000 , List (Note (44, 100, 1333), Note (62, 100, 1333), Note (65, 100, 1333), Note (59, 100, 1333)))
    , Chord (49333 , List (Note (45, 100, 1333), Note (61, 100, 1333), Note (64, 100, 1333), Note (52, 100, 667)))
    , Chord (50000 , List (Note (53, 100, 333)))
    , Chord (50333 , List (Note (55, 100, 333)))
    , Chord (50667 , List (Note (50, 100, 1333), Note (57, 100, 1333), Note (62, 100, 1333), Note (54, 100, 1333)))
  ))

  val bwv0417 = Chordseq ( List (Chord (0 , List (Note (54, 100, 2000), Note (61, 100, 4000), Note (66, 100, 4000), Note (58, 100, 4000)))
    , Chord (2000 , List (Note (52, 100, 2000)))
    , Chord (4000 , List (Note (50, 100, 1000), Note (66, 100, 1000), Note (71, 100, 1000), Note (59, 100, 1500)))
    , Chord (5000 , List (Note (49, 100, 1000), Note (64, 100, 1000), Note (73, 100, 1000)))
    , Chord (5500 , List (Note (58, 100, 500)))
    , Chord (6000 , List (Note (47, 100, 500), Note (62, 100, 500), Note (74, 100, 1000), Note (59, 100, 1500)))
    , Chord (6500 , List (Note (45, 100, 500), Note (66, 100, 500)))
    , Chord (7000 , List (Note (44, 100, 500), Note (71, 100, 500), Note (76, 100, 1000)))
    , Chord (7500 , List (Note (52, 100, 500), Note (68, 100, 500), Note (59, 100, 500)))
    , Chord (8000 , List (Note (45, 100, 500), Note (68, 100, 500), Note (73, 100, 1500), Note (59, 100, 500)))
    , Chord (8500 , List (Note (47, 100, 500), Note (66, 100, 500), Note (57, 100, 500)))
    , Chord (9000 , List (Note (49, 100, 1000), Note (65, 100, 250), Note (56, 100, 500)))
    , Chord (9250 , List (Note (63, 100, 250)))
    , Chord (9500 , List (Note (65, 100, 500), Note (71, 100, 500), Note (61, 100, 500)))
    , Chord (10000 , List (Note (42, 100, 1000), Note (66, 100, 1000), Note (69, 100, 1000), Note (61, 100, 1000)))
    , Chord (11000 , List (Note (54, 100, 500), Note (66, 100, 1000), Note (69, 100, 1000), Note (61, 100, 1000)))
    , Chord (11500 , List (Note (52, 100, 500)))
    , Chord (12000 , List (Note (50, 100, 500), Note (66, 100, 1000), Note (71, 100, 1000), Note (59, 100, 500)))
    , Chord (12500 , List (Note (54, 100, 500), Note (57, 100, 500)))
    , Chord (13000 , List (Note (55, 100, 1000), Note (64, 100, 500), Note (71, 100, 1000), Note (55, 100, 2000)))
    , Chord (13500 , List (Note (62, 100, 500)))
    , Chord (14000 , List (Note (46, 100, 1000), Note (64, 100, 1000), Note (73, 100, 500)))
    , Chord (14500 , List (Note (71, 100, 500)))
    , Chord (15000 , List (Note (46, 100, 1000), Note (64, 100, 500), Note (73, 100, 1000), Note (54, 100, 1000)))
    , Chord (15500 , List (Note (66, 100, 250)))
    , Chord (15750 , List (Note (64, 100, 250)))
    , Chord (16000 , List (Note (47, 100, 4000), Note (62, 100, 4000), Note (66, 100, 4000), Note (54, 100, 4000)))
    , Chord (20000 , List (Note (47, 100, 2000), Note (71, 100, 4000), Note (78, 100, 4000), Note (62, 100, 4000)))
    , Chord (22000 , List (Note (45, 100, 2000)))
    , Chord (24000 , List (Note (43, 100, 500), Note (71, 100, 1000), Note (76, 100, 1000), Note (64, 100, 500)))
    , Chord (24500 , List (Note (55, 100, 500), Note (66, 100, 500)))
    , Chord (25000 , List (Note (54, 100, 500), Note (71, 100, 1000), Note (74, 100, 1000), Note (67, 100, 1000)))
    , Chord (25500 , List (Note (52, 100, 500)))
    , Chord (26000 , List (Note (57, 100, 1000), Note (69, 100, 1000), Note (74, 100, 1000), Note (66, 100, 500)))
    , Chord (26500 , List (Note (64, 100, 250)))
    , Chord (26750 , List (Note (62, 100, 250)))
    , Chord (27000 , List (Note (45, 100, 1000), Note (69, 100, 1000), Note (73, 100, 1000), Note (64, 100, 500)))
    , Chord (27500 , List (Note (66, 100, 250)))
    , Chord (27750 , List (Note (67, 100, 250)))
    , Chord (28000 , List (Note (50, 100, 3000), Note (69, 100, 3000), Note (74, 100, 3000), Note (66, 100, 3000)))
    , Chord (31000 , List (Note (57, 100, 500), Note (69, 100, 1000), Note (73, 100, 1000), Note (64, 100, 1000)))
    , Chord (31500 , List (Note (55, 100, 500)))
    , Chord (32000 , List (Note (54, 100, 500), Note (69, 100, 1000), Note (74, 100, 1000), Note (66, 100, 1000)))
    , Chord (32500 , List (Note (52, 100, 500)))
    , Chord (33000 , List (Note (50, 100, 500), Note (69, 100, 1000), Note (76, 100, 1000), Note (64, 100, 1000)))
    , Chord (33500 , List (Note (49, 100, 500)))
    , Chord (34000 , List (Note (50, 100, 500), Note (69, 100, 1500), Note (78, 100, 1000), Note (62, 100, 500)))
    , Chord (34500 , List (Note (52, 100, 250), Note (57, 100, 500)))
    , Chord (34750 , List (Note (54, 100, 250)))
    , Chord (35000 , List (Note (47, 100, 1000), Note (78, 100, 1000), Note (62, 100, 1000)))
    , Chord (35500 , List (Note (68, 100, 500)))
    , Chord (36000 , List (Note (49, 100, 500), Note (69, 100, 500), Note (76, 100, 1500), Note (61, 100, 1000)))
    , Chord (36500 , List (Note (50, 100, 500), Note (66, 100, 500)))
    , Chord (37000 , List (Note (52, 100, 1000), Note (68, 100, 1000), Note (59, 100, 250)))
    , Chord (37250 , List (Note (57, 100, 250)))
    , Chord (37500 , List (Note (74, 100, 500), Note (59, 100, 500)))
    , Chord (38000 , List (Note (45, 100, 1000), Note (64, 100, 1000), Note (73, 100, 1000), Note (57, 100, 1000)))
    , Chord (39000 , List (Note (46, 100, 1000), Note (66, 100, 1000), Note (73, 100, 500), Note (61, 100, 1000)))
    , Chord (39500 , List (Note (74, 100, 250)))
    , Chord (39750 , List (Note (76, 100, 250)))
    , Chord (40000 , List (Note (47, 100, 500), Note (66, 100, 1000), Note (74, 100, 1000), Note (61, 100, 500)))
    , Chord (40500 , List (Note (59, 100, 1000), Note (59, 100, 500)))
    , Chord (41000 , List (Note (66, 100, 1000), Note (73, 100, 750), Note (61, 100, 500)))
    , Chord (41500 , List (Note (57, 100, 500), Note (63, 100, 500)))
    , Chord (41750 , List (Note (71, 100, 250)))
    , Chord (42000 , List (Note (55, 100, 500), Note (67, 100, 500), Note (71, 100, 1000), Note (64, 100, 500)))
    , Chord (42500 , List (Note (57, 100, 250), Note (66, 100, 500), Note (63, 100, 500)))
    , Chord (42750 , List (Note (59, 100, 250)))
    , Chord (43000 , List (Note (52, 100, 1000), Note (67, 100, 1000), Note (71, 100, 500), Note (64, 100, 500)))
    , Chord (43500 , List (Note (73, 100, 250), Note (59, 100, 500)))
    , Chord (43750 , List (Note (74, 100, 250)))
    , Chord (44000 , List (Note (53, 100, 500), Note (68, 100, 500), Note (73, 100, 1500), Note (56, 100, 500)))
    , Chord (44500 , List (Note (54, 100, 250), Note (66, 100, 500), Note (57, 100, 250)))
    , Chord (44750 , List (Note (56, 100, 250), Note (59, 100, 250)))
    , Chord (45000 , List (Note (49, 100, 1000), Note (65, 100, 1000), Note (61, 100, 1000)))
    , Chord (45500 , List (Note (71, 100, 500)))
    , Chord (46000 , List (Note (54, 100, 1000), Note (66, 100, 1000), Note (69, 100, 1000), Note (61, 100, 1000)))
    , Chord (47000 , List (Note (47, 100, 500), Note (66, 100, 500), Note (67, 100, 1000), Note (62, 100, 500)))
    , Chord (47500 , List (Note (49, 100, 500), Note (64, 100, 500), Note (57, 100, 500)))
    , Chord (48000 , List (Note (50, 100, 500), Note (62, 100, 500), Note (66, 100, 1000), Note (57, 100, 1000)))
    , Chord (48500 , List (Note (49, 100, 500), Note (64, 100, 500)))
    , Chord (49000 , List (Note (50, 100, 500), Note (66, 100, 500), Note (71, 100, 1000), Note (62, 100, 500)))
    , Chord (49500 , List (Note (52, 100, 500), Note (67, 100, 500), Note (61, 100, 500)))
    , Chord (50000 , List (Note (54, 100, 500), Note (66, 100, 500), Note (71, 100, 500), Note (62, 100, 500)))
    , Chord (50500 , List (Note (52, 100, 500), Note (67, 100, 250), Note (73, 100, 500), Note (64, 100, 250)))
    , Chord (50750 , List (Note (66, 100, 250), Note (62, 100, 250)))
    , Chord (51000 , List (Note (54, 100, 1000), Note (64, 100, 500), Note (70, 100, 1000), Note (61, 100, 500)))
    , Chord (51500 , List (Note (66, 100, 250), Note (54, 100, 500)))
    , Chord (51750 , List (Note (64, 100, 250)))
    , Chord (52000 , List (Note (47, 100, 4000), Note (63, 100, 4000), Note (71, 100, 4000), Note (54, 100, 4000)))
  ))

  val bwv0418 = Chordseq ( List (Chord (0 , List (Note (45, 100, 4000), Note (60, 100, 2000), Note (64, 100, 4000), Note (57, 100, 2000)))
    , Chord (2000 , List (Note (62, 100, 2000), Note (59, 100, 2000)))
    , Chord (4000 , List (Note (57, 100, 1000), Note (64, 100, 1000), Note (69, 100, 1000), Note (60, 100, 1000)))
    , Chord (5000 , List (Note (56, 100, 1000), Note (64, 100, 1000), Note (71, 100, 1000), Note (59, 100, 1000)))
    , Chord (6000 , List (Note (57, 100, 500), Note (64, 100, 1000), Note (72, 100, 1000), Note (57, 100, 1000)))
    , Chord (6500 , List (Note (55, 100, 500)))
    , Chord (7000 , List (Note (54, 100, 1000), Note (62, 100, 1000), Note (74, 100, 1000), Note (57, 100, 1000)))
    , Chord (8000 , List (Note (55, 100, 1000), Note (62, 100, 500), Note (71, 100, 1500), Note (59, 100, 2000)))
    , Chord (8500 , List (Note (64, 100, 500)))
    , Chord (9000 , List (Note (51, 100, 1000), Note (66, 100, 1000)))
    , Chord (9500 , List (Note (69, 100, 500)))
    , Chord (10000 , List (Note (52, 100, 1000), Note (64, 100, 1000), Note (67, 100, 1000), Note (59, 100, 1000)))
    , Chord (11000 , List (Note (48, 100, 1000), Note (64, 100, 1000), Note (67, 100, 1000), Note (60, 100, 1000)))
    , Chord (12000 , List (Note (53, 100, 1000), Note (64, 100, 1000), Note (69, 100, 1000), Note (60, 100, 500)))
    , Chord (12500 , List (Note (59, 100, 500)))
    , Chord (13000 , List (Note (54, 100, 1000), Note (62, 100, 1000), Note (69, 100, 1000), Note (57, 100, 1000)))
    , Chord (14000 , List (Note (55, 100, 1000), Note (62, 100, 500), Note (71, 100, 1000), Note (55, 100, 500)))
    , Chord (14500 , List (Note (65, 100, 500), Note (57, 100, 500)))
    , Chord (15000 , List (Note (56, 100, 1000), Note (64, 100, 500), Note (71, 100, 1000), Note (59, 100, 1000)))
    , Chord (15500 , List (Note (62, 100, 500)))
    , Chord (16000 , List (Note (57, 100, 3000), Note (60, 100, 3000), Note (64, 100, 3000), Note (57, 100, 3000)))
    , Chord (19000 , List (Note (52, 100, 500), Note (59, 100, 1000), Note (64, 100, 1000), Note (56, 100, 1000)))
    , Chord (19500 , List (Note (50, 100, 500)))
    , Chord (20000 , List (Note (48, 100, 1000), Note (64, 100, 1000), Note (69, 100, 1000), Note (57, 100, 1000)))
    , Chord (21000 , List (Note (47, 100, 1000), Note (66, 100, 500), Note (71, 100, 1000), Note (62, 100, 1000)))
    , Chord (21500 , List (Note (68, 100, 500)))
    , Chord (22000 , List (Note (45, 100, 500), Note (69, 100, 1000), Note (72, 100, 1000), Note (64, 100, 1000)))
    , Chord (22500 , List (Note (55, 100, 500)))
    , Chord (23000 , List (Note (53, 100, 500), Note (69, 100, 500), Note (74, 100, 500), Note (57, 100, 1000)))
    , Chord (23500 , List (Note (52, 100, 500), Note (67, 100, 500), Note (72, 100, 500)))
    , Chord (24000 , List (Note (50, 100, 500), Note (65, 100, 500), Note (71, 100, 1500), Note (57, 100, 500)))
    , Chord (24500 , List (Note (52, 100, 500), Note (64, 100, 500), Note (56, 100, 500)))
    , Chord (25000 , List (Note (53, 100, 1000), Note (62, 100, 1000), Note (57, 100, 1000)))
    , Chord (25500 , List (Note (69, 100, 500)))
    , Chord (26000 , List (Note (52, 100, 1000), Note (64, 100, 1000), Note (67, 100, 1000), Note (59, 100, 1000)))
    , Chord (27000 , List (Note (47, 100, 1000), Note (62, 100, 1000), Note (67, 100, 1000), Note (59, 100, 1000)))
    , Chord (28000 , List (Note (48, 100, 1000), Note (64, 100, 1000), Note (69, 100, 1000), Note (57, 100, 500)))
    , Chord (28500 , List (Note (59, 100, 500)))
    , Chord (29000 , List (Note (49, 100, 1000), Note (64, 100, 1000), Note (69, 100, 1000), Note (57, 100, 500)))
    , Chord (29500 , List (Note (55, 100, 500)))
    , Chord (30000 , List (Note (50, 100, 1000), Note (62, 100, 500), Note (71, 100, 1000), Note (54, 100, 500)))
    , Chord (30500 , List (Note (64, 100, 500), Note (55, 100, 500)))
    , Chord (31000 , List (Note (51, 100, 1000), Note (66, 100, 1000), Note (71, 100, 1000), Note (57, 100, 1000)))
    , Chord (32000 , List (Note (52, 100, 3000), Note (59, 100, 3000), Note (64, 100, 3000), Note (56, 100, 3000)))
    , Chord (35000 , List (Note (57, 100, 500), Note (60, 100, 1000), Note (76, 100, 1000), Note (57, 100, 1000)))
    , Chord (35500 , List (Note (55, 100, 500)))
    , Chord (36000 , List (Note (53, 100, 1000), Note (69, 100, 1000), Note (74, 100, 1000), Note (57, 100, 1000)))
    , Chord (37000 , List (Note (54, 100, 1000), Note (69, 100, 1000), Note (72, 100, 1000), Note (62, 100, 1000)))
    , Chord (38000 , List (Note (55, 100, 1000), Note (67, 100, 1000), Note (72, 100, 1000), Note (62, 100, 500)))
    , Chord (38500 , List (Note (64, 100, 500)))
    , Chord (39000 , List (Note (43, 100, 1000), Note (67, 100, 1000), Note (71, 100, 1000), Note (65, 100, 500)))
    , Chord (39500 , List (Note (62, 100, 500)))
    , Chord (40000 , List (Note (48, 100, 3000), Note (67, 100, 3000), Note (72, 100, 3000), Note (64, 100, 3000)))
    , Chord (43000 , List (Note (55, 100, 1000), Note (67, 100, 500), Note (71, 100, 1000), Note (62, 100, 1000)))
    , Chord (43500 , List (Note (65, 100, 500)))
    , Chord (44000 , List (Note (57, 100, 1000), Note (64, 100, 1000), Note (72, 100, 1000), Note (60, 100, 1000)))
    , Chord (45000 , List (Note (59, 100, 1000), Note (62, 100, 1000), Note (74, 100, 1000), Note (67, 100, 1000)))
    , Chord (46000 , List (Note (60, 100, 500), Note (60, 100, 1000), Note (76, 100, 1000), Note (67, 100, 1000)))
    , Chord (46500 , List (Note (59, 100, 500)))
    , Chord (47000 , List (Note (57, 100, 500), Note (64, 100, 1000), Note (76, 100, 1000), Note (60, 100, 500)))
    , Chord (47500 , List (Note (55, 100, 500), Note (59, 100, 500)))
    , Chord (48000 , List (Note (54, 100, 500), Note (69, 100, 500), Note (74, 100, 1500), Note (57, 100, 1000)))
    , Chord (48500 , List (Note (52, 100, 500), Note (67, 100, 500)))
    , Chord (49000 , List (Note (54, 100, 500), Note (69, 100, 500), Note (62, 100, 1000)))
    , Chord (49500 , List (Note (50, 100, 500), Note (66, 100, 500), Note (72, 100, 500)))
    , Chord (50000 , List (Note (55, 100, 1000), Note (67, 100, 1000), Note (71, 100, 1000), Note (62, 100, 1000)))
    , Chord (51000 , List (Note (55, 100, 500), Note (67, 100, 1000), Note (71, 100, 1000), Note (62, 100, 1000)))
    , Chord (51500 , List (Note (53, 100, 500)))
    , Chord (52000 , List (Note (52, 100, 500), Note (67, 100, 500), Note (72, 100, 1000), Note (55, 100, 1000)))
    , Chord (52500 , List (Note (50, 100, 500), Note (65, 100, 500)))
    , Chord (53000 , List (Note (52, 100, 500), Note (67, 100, 500), Note (71, 100, 1000), Note (60, 100, 1000)))
    , Chord (53500 , List (Note (48, 100, 500), Note (64, 100, 500)))
    , Chord (54000 , List (Note (53, 100, 500), Note (60, 100, 1000), Note (69, 100, 1000), Note (60, 100, 500)))
    , Chord (54500 , List (Note (55, 100, 500), Note (59, 100, 500)))
    , Chord (55000 , List (Note (53, 100, 500), Note (60, 100, 1000), Note (69, 100, 1000), Note (57, 100, 500)))
    , Chord (55500 , List (Note (52, 100, 500), Note (55, 100, 500)))
    , Chord (56000 , List (Note (51, 100, 500), Note (59, 100, 500), Note (71, 100, 1500), Note (54, 100, 500)))
    , Chord (56500 , List (Note (52, 100, 500), Note (61, 100, 500), Note (55, 100, 500)))
    , Chord (57000 , List (Note (54, 100, 1000), Note (63, 100, 1000), Note (57, 100, 1000)))
    , Chord (57500 , List (Note (69, 100, 500)))
    , Chord (58000 , List (Note (52, 100, 1000), Note (64, 100, 1000), Note (67, 100, 1000), Note (59, 100, 1000)))
    , Chord (59000 , List (Note (47, 100, 1000), Note (62, 100, 1000), Note (67, 100, 500), Note (55, 100, 1000)))
    , Chord (59500 , List (Note (65, 100, 500)))
    , Chord (60000 , List (Note (48, 100, 1000), Note (62, 100, 500), Note (64, 100, 1000), Note (55, 100, 1000)))
    , Chord (60500 , List (Note (60, 100, 500)))
    , Chord (61000 , List (Note (53, 100, 1000), Note (60, 100, 500), Note (69, 100, 1000), Note (57, 100, 500)))
    , Chord (61500 , List (Note (62, 100, 500), Note (59, 100, 500)))
    , Chord (62000 , List (Note (52, 100, 500), Note (64, 100, 500), Note (69, 100, 500), Note (60, 100, 500)))
    , Chord (62500 , List (Note (50, 100, 500), Note (65, 100, 500), Note (71, 100, 500), Note (62, 100, 500)))
    , Chord (63000 , List (Note (52, 100, 500), Note (64, 100, 1000), Note (68, 100, 1000), Note (59, 100, 1000)))
    , Chord (63500 , List (Note (40, 100, 500)))
    , Chord (64000 , List (Note (45, 100, 4000), Note (64, 100, 4000), Note (69, 100, 4000), Note (61, 100, 4000)))
  ))


  // Test for Question 1
  //val remote = system.actorSelection("akka.tcp://Player@127.0.0.1:6005/user/PlayerActor")
  //To test your cantate send this msg
  //remote ! cantate
  //remote ! bwv0416
  //remote ! bwv0417
  //remote ! bwv0418

  //End Question 1


  ////////////////////QUESTION 2

  playMusic (3, 5, 4)

  def playMusic(nbWorks: Int, nbElement: Int, nbVoice: Int): Unit = {
    val system = ActorSystem("VoicerSystem")

    val listener = system.actorOf(Props[Listener], name = "listener")

    val master = system.actorOf(
      Props(new Master(3, 5, 5, listener)),
      name = "master"
    )

    // Test for Question 2
    //master ! Play(bwv0416)
    //master ! Play(bwv0417)
    master ! Play(bwv0418)

    ////////////////////END QUESTION 2
  }
}