import org.apache.pekko.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.concurrent.Await
import scala.concurrent.duration.*
import scala.language.postfixOps

@main def main(): Unit = {
  println("Starting Pekko Actor System Tutorial")
  println("=====================================")
  println("Two actors (Alicia and Bob) will exchange greetings every 5 seconds.")
  println("The system will automatically shutdown after 1 minute.")
  println()

  val system = HelloSystem()
  system ! HelloSystemMessage.Start

  try {
    // Block the main thread until the system terminates
    Await.result(system.whenTerminated, Duration.Inf)
  } finally {
    println()
    println("=====================================")
    println("Pekko Actor System Tutorial Complete")
  }
}

enum HelloSystemMessage {
  case Start, Stop, Shutdown
}

class HelloSystem(context: ActorContext[HelloSystemMessage], timer: TimerScheduler[HelloSystemMessage])
  extends AbstractBehavior[HelloSystemMessage](context) {
  import HelloSystemMessage.{Start, Stop, Shutdown}
  import HelloMessage.{GoodBye, Greet}

  private var alice = Option.empty[ActorRef[HelloMessage]]
  private var bob = Option.empty[ActorRef[HelloMessage]]

  override def onMessage(msg: HelloSystemMessage): Behavior[HelloSystemMessage] = msg match {
      case Start =>
        val aliceRef = HelloActor(context, "Alice")
        alice = Some(aliceRef)
        context.log.info(s"Alice: $aliceRef")
        val bobRef = HelloActor(context, "Bob")
        bob = Some(bobRef)
        context.log.info(s"Bob: $bobRef")
        aliceRef ! Greet(bobRef)
        bobRef ! Greet(aliceRef)

        timer.startSingleTimer(Stop, 12.seconds)

        Behaviors.same
      case Stop =>
        alice.foreach(_ ! GoodBye)
        bob.foreach(_ ! GoodBye)
        timer.startSingleTimer(Shutdown, 2.seconds)
        Behaviors.same
      case Shutdown =>
        context.system.terminate()
        Behaviors.stopped
    }
}

object HelloSystem {
  def apply(): ActorSystem[HelloSystemMessage] = {
    val behavior = Behaviors.withTimers[HelloSystemMessage] { timer =>
      Behaviors.setup(context => new HelloSystem(context, timer))
    }
    ActorSystem(behavior, "hello-system") // no whitespace allowed in the system's name
  }
}

enum HelloMessage {
  case Greet(actorRef: ActorRef[HelloMessage])
  case ScheduleMessage(reply: String)
  case Message(hello: String)
  case GoodBye
}

class HelloActor(context: ActorContext[HelloMessage], timer: TimerScheduler[HelloMessage], name: String)
  extends AbstractBehavior[HelloMessage](context) {
  import HelloMessage.{GoodBye, Greet, Message, ScheduleMessage}

  private var theOtherActorRef: Option[ActorRef[HelloMessage]] = None

  override def onMessage(msg: HelloMessage): Behavior[HelloMessage] = msg match {
    case Greet(ref) =>
      theOtherActorRef = Some(ref)
      timer.startSingleTimer(ScheduleMessage(s"Hello from $name"), 3.seconds)
      Behaviors.same
    case ScheduleMessage(str: String) =>
      theOtherActorRef.foreach(_ ! Message(str))
      Behaviors.same
    case Message(hello) =>
      context.log.info(s"[$name] I received a hello: $hello")
      timer.startSingleTimer(ScheduleMessage(s"Hello from $name"), 5.seconds)
      Behaviors.same
    case GoodBye =>
      context.log.info(s"[$name] Goodbye")
      Behaviors.stopped
  }
}

object HelloActor {
  def apply(context: ActorContext[?], name: String): ActorRef[HelloMessage] = {
    val behavior = Behaviors.withTimers[HelloMessage] { timer =>
      Behaviors.setup(new HelloActor(_, timer, name))
    }
    context.spawn(behavior, name)
  }
}

/*
https://pekko.apache.org/docs/pekko/snapshot/typed/interaction-patterns.html
 */