package info.glennengstrand.news

import info.glennengstrand.io._
import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest

trait MockWriter extends PersistentDataStoreWriter {
  def write(o: PersistentDataStoreBindings, state: Map[String, Any], criteria: Map[String, Any]): Map[String, Any] = {
    Map()
  }
}

class MockPerformanceLogger extends PerformanceLogger {
  def log(topic: String, entity: String, operation: String, duration: Long): Unit = {

  }
}

class MockFactoryClass extends FactoryClass {

  val performanceLogger = new MockPerformanceLogger
  def getParticipant(id: Long): Participant = {
    new Participant(id, "test") with MockWriter with MockCacheAware
  }
  def getParticipant(state: String): Participant = {
    val s = IO.fromFormPost(state)
    new Participant(s("id").asInstanceOf[String].toLong, s("name").asInstanceOf[String]) with MockWriter with MockCacheAware
  }
  def getFriends(id: Long): Friends = {
    val state: Iterable[Map[String, Any]] = List(
      Map[String, Any](
      "FriendsID" -> "1",
      "ParticipantID" -> "2"
      )
    )
    new Friends(1l, state)
  }
  def getFriend(state: String): Friend = {
    val s = IO.fromFormPost(state)
    new Friend(s("FriendsID").asInstanceOf[String].toLong, s("FromParticipantID").asInstanceOf[String].toLong, s("ToParticipantID").asInstanceOf[String].toLong) with MockWriter with MockCacheAware
  }
  def getInbound(id: Int): InboundFeed = {
    val state: Iterable[Map[String, Any]] = List(
      Map[String, Any](
        "participantID" -> "1",
        "occurred" -> "2014-01-12 22:56:36-0800",
        "fromParticipantID" -> "2",
        "subject" -> "test",
        "story" -> "this is a unit test"
      )
    )
    new InboundFeed(1, state)
  }
  def getInbound(state: String): Inbound = {
    val s = IO.fromFormPost(state)
    new Inbound(s("participantID").asInstanceOf[String].toLong, IO.df.parse(s("occurred").asInstanceOf[String]), s("fromParticipantID").asInstanceOf[String].toLong, s("subject").asInstanceOf[String], s("story").asInstanceOf[String]) with MockWriter with MockCacheAware
  }
  def getObject(name: String, id: Long): Option[Object] = {
    name match {
      case "participant" => Some(getParticipant(id))
      case "friends" => Some(getFriends(id))
      case _ => None
    }
  }
  def getObject(name: String, id: Int): Option[Object] = {
    name match {
      case "inbound" => Some(getInbound(id))
      case _ => None
    }
  }
  def getObject(name: String, state: String): Option[Object] = {
    name match {
      case "participant" => Some(getParticipant(state))
      case "friend" => Some(getFriend(state))
      case "inbound" => Some(getInbound(state))
      case _ => None
    }
  }
  def getObject(name: String): Option[Object] = {
    name match {
      case "logger" => Some(performanceLogger)
      case _ => None
    }
  }
}

class FeedSpec extends Specification with Specs2RouteTest with Feed {
  def actorRefFactory = system
  Feed.factory = new MockFactoryClass

  "Feed" should {

    "return the correct data when fetching a participant" in {
      Get("/participant/2") ~> myRoute ~> check {
        responseAs[String] must contain("test")
      }
    }

    "return the correct data when fetching friends" in {
      Get("/friends/1") ~> myRoute ~> check {
        responseAs[String] must contain("2")
      }
    }

    "return the correct data when fetching inbound" in {
      Get("/inbound/1") ~> myRoute ~> check {
        responseAs[String] must contain("test")
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/kermit") ~> myRoute ~> check {
        handled must beFalse
      }
    }

    "process post requests to create a new participant properly" in {
      Post("/participant.new", "id=2&name=smith") ~> myRoute ~> check {
        responseAs[String] must contain("smith")
      }
    }
  }
}