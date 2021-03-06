package info.glennengstrand.news

import java.util.Date
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.glennengstrand.io._

/** helper functions for outbound object creation */
object Outbound extends ElasticSearchSearcher {
  val log = LoggerFactory.getLogger("info.glennengstrand.news.Outbound")
  val reader: PersistentDataStoreReader = new CassandraReader
  val cache: CacheAware = new MockCache
  class OutboundBindings extends PersistentDataStoreBindings {
    def entity: String = {
      "Outbound"
    }
    def fetchInputs: Iterable[String] = {
      List("participantID")
    }
    def fetchOutputs: Iterable[(String, String)] = {
      List(("occurred", "Date"), ("subject", "String"), ("story", "String"))
    }
    def upsertInputs: Iterable[String] = {
      List("participantID", "occurred", "subject", "story")
    }
    def upsertOutputs: Iterable[(String, String)] = {
      List()
    }
    def fetchOrder: Map[String, String] = {
      Map("occurred" -> "desc")
    }
  }
  val bindings = new OutboundBindings
  def apply(id: Int) : OutboundFeed = {
    val criteria: Map[String, Any] = Map("participantID" -> id)
    new OutboundFeed(id, IO.cacheAwareRead(bindings, criteria, reader, cache)) with CassandraWriter with MockCacheAware with ElasticSearchSearcher
  }
  def apply(state: String): Outbound = {
    val s = IO.fromFormPost(state)
    val id = Link.extractId(s("from").asInstanceOf[String])
    val story = s("story").asInstanceOf[String]
    index(id, story)
    new Outbound(Link.toLink(id), IO.convertToDate(s("occurred").asInstanceOf[String]), s("subject").asInstanceOf[String], story) with CassandraWriter with MockCacheAware
  }
  def lookup(state: String): Iterable[Long] = {
    search(state)
  }
}

case class OutboundState(participantID: String, occurred: Date, subject: String, story: String)

/** represents a news feed item in the outbound feed */
class Outbound(participantID: String, occurred: Date, subject: String, story: String) extends OutboundState(participantID, occurred, subject, story) with MicroServiceSerializable {
  this: PersistentDataStoreWriter with CacheAware =>

  def getState(l: String => Any): Map[String, Any] = {
    Map(
      "participantID" -> l(participantID),
      "occurred" -> occurred,
      "subject" -> subject,
      "story" -> story
    )
  }
  
  def getState: Map[String, Any] = {
    getState((s) => Link.extractId(s).intValue)
  }
  
  def getApiState(l: String => Any): Map[String, Any] = {
    Map(
      "from" -> l(participantID),
      "occurred" -> occurred,
      "subject" -> subject,
      "story" -> story
    )
  }
    
  /** save item to db and perform social broadcast of item to inbound feed of friends */
  def save: Unit = {
    val pid = Link.extractId(participantID).intValue
    val criteria: Map[String, Any] = Map(
      "participantID" -> pid
    )
    write(Outbound.bindings, getState, criteria)
    invalidate(Outbound.bindings, criteria)
    val broadcast = Friends(pid)
    broadcast.foreach( f => {
      val inbound =  new Inbound(f.toParticipantID, occurred, participantID, subject, story) with CassandraWriter with MockCacheAware
      inbound.save
    })
  }
  override def toJson: String = {
    IO.toJson(getApiState((s) => s))
  }
  override def toJson(factory: FactoryClass): String = toJson
}

/** represents a user's outbound feed */
class OutboundFeed(id: Int, state: Iterable[Map[String, Any]]) extends Iterator[Outbound] with MicroServiceSerializable {
  val i = state.iterator
  def hasNext = i.hasNext
  def next() = {
    val kv = i.next()
    Outbound.log.debug("kv = " + kv)
    new Outbound(Link.toLink(id.longValue), IO.convertToDate(kv("occurred")), kv("subject").toString, kv("story").toString) with CassandraWriter with MockCacheAware with ElasticSearchSearcher
  }
  override def toJson: String = {
    val r = map(f => f.toJson).toList
    r.size match {
      case 0 => "[]"
      case _ => "[" +  r.reduce(_ + "," + _) + "]"
    }
  }
  override def toJson(factory: FactoryClass): String = toJson
}


