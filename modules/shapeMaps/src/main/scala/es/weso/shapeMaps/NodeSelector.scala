package es.weso.shapeMaps

import es.weso.rdf.nodes.{IRI, RDFNode}
import es.weso.rdf.path.SHACLPath
import io.circe._
import io.circe.syntax._
import es.weso.json.DecoderUtils._

abstract class NodeSelector
case class RDFNodeSelector(node: RDFNode) extends NodeSelector
case class TriplePattern(
  subjectPattern: Pattern,
  path: SHACLPath,
  objectPattern: Pattern) extends NodeSelector
case class SparqlSelector(query: String) extends NodeSelector
case class GenericSelector(iri: IRI, param: String) extends NodeSelector

object NodeSelector {

  implicit val encodeNodeSelector: Encoder[NodeSelector] = new Encoder[NodeSelector] {
    final def apply(nodeSelector: NodeSelector): Json = {
      nodeSelector match {
        case RDFNodeSelector(node) => Json.fromString(node.toString)
        case TriplePattern(subj, path, obj) => {
          Json.fromJsonObject(JsonObject.empty.
            add("subject", subj.asJson).
            add("path", path.asJson).
            add("object", obj.asJson))
        }
        case SparqlSelector(query) =>
          Json.fromJsonObject(JsonObject.empty.add("sparql", Json.fromString(query)))
      }
    }
  }

  implicit lazy val decodeRDFNodeSelector: Decoder[RDFNodeSelector] = Decoder.instance { c =>
    c.as[String].flatMap(s => RDFNode.fromString(s).fold(
      s => Left(DecodingFailure(s, Nil)),
      node => Right(RDFNodeSelector(node))))
  }

  implicit lazy val decodeTriplePattern: Decoder[TriplePattern] = Decoder.instance { c =>
    for {
      subj <- fieldDecode[Pattern](c, "subject")
      path <- fieldDecode[SHACLPath](c, "path")
      obj <- fieldDecode[Pattern](c, "object")
    } yield TriplePattern(subj, path, obj)

  }

  implicit lazy val decodeSparql: Decoder[SparqlSelector] = Decoder.instance { c =>
    for {
      query <- fieldDecode[String](c, "sparql")
    } yield SparqlSelector(query)
  }

  implicit lazy val decodeNodeSelector: Decoder[NodeSelector] =
    Decoder[RDFNodeSelector].map(identity).or(
     Decoder[TriplePattern].map(identity).or(
      Decoder[SparqlSelector].map(identity)
    )
   )

}