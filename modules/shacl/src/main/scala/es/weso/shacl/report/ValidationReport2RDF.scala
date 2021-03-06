package es.weso.shacl.report

import cats.data.State
import com.typesafe.scalalogging.LazyLogging
import es.weso.rdf.RDFBuilder
import es.weso.rdf.saver.RDFSaver
import es.weso.shacl.SHACLPrefixes._
import es.weso.rdf.PREFIXES.{sh => _, _}
import es.weso.rdf.nodes.{BooleanLiteral, RDFNode}
import es.weso.rdf.path._
import es.weso.shacl.LiteralValue

class ValidationReport2RDF extends RDFSaver with LazyLogging {

  def toRDF(vr: ValidationReport, initial: RDFBuilder): RDFBuilder = {
    val result = validationReport(vr).run(initial)
    result.value._1
  }

  private def validationReport(vr: ValidationReport): RDFSaver[Unit] = for {
    _ <- addPrefix("sh", sh.str)
    node <- createBNode
    _ <- addTriple(node, rdf_type, sh_ValidationReport)
    _ <- addTriple(node, sh_conforms, BooleanLiteral(vr.conforms))
    _ <- results(node, vr.results)
  } yield ()

  private def results(id: RDFNode, ts: Seq[ValidationResult]): RDFSaver[Unit] =
    saveList(ts.toList, result(id))

  private def result(id: RDFNode)(vr: ValidationResult): RDFSaver[Unit] = for {
    node <- createBNode()
    _ <- addTriple(id, sh_result, node)
    _ <- addTriple(node, rdf_type, sh_ValidationResult)
    _ <- addTriple(node, sh_resultSeverity, vr.resultSeverity.toIRI)
    _ <- addTriple(node, sh_focusNode, vr.focusNode)
    _ <- addTriple(node, sh_sourceConstraintComponent, vr.sourceConstraintComponent)
    _ <- saveList(vr.message.toList, message(node))
    _ <- vr.focusPath match {
      case None => ok(())
      case Some(path) => for {
        path <- makePath(path)
        _ <- addTriple(node, sh_resultPath, path)
      } yield ()
    }
  } yield ()

  private def makePath(path: SHACLPath): RDFSaver[RDFNode] = path match {
    case PredicatePath(iri) => State.pure(iri)
    case InversePath(p) => for {
      node <- createBNode
      pathNode <- makePath(p)
      _ <- addTriple(node, sh_inversePath, pathNode)
    } yield node
    case ZeroOrOnePath(p) => for {
      node <- createBNode
      pathNode <- makePath(p)
      _ <- addTriple(node, sh_zeroOrOnePath, pathNode)
    } yield node
    case ZeroOrMorePath(p) => for {
      node <- createBNode
      pathNode <- makePath(p)
      _ <- addTriple(node, sh_zeroOrMorePath, pathNode)
    } yield node
    case OneOrMorePath(p) => for {
      node <- createBNode
      pathNode <- makePath(p)
      _ <- addTriple(node, sh_oneOrMorePath, pathNode)
    } yield node
    /*    case SequencePath(ps) => for {
      list <- saveRDFList(ps, )
      pathNodes <- makePath(p)
      _ <- addTriple(node,sh_oneOrMorePath,pathNode)
    } yield node
    case AlternativePath(ps) => for {
      node <- createBNode
      pathNodes <- makePath(p)
      _ <- addTriple(node,sh_oneOrMorePath,pathNode)
    } yield node */
    case _ => throw new Exception(s"Unimplemented conversion of path: $path")
  }

  private def message(node: RDFNode)(msg: LiteralValue): RDFSaver[Unit] = for {
    _ <- addTriple(node,sh_message,msg.literal)
  } yield ()

}

object ValidationReport2RDF {

  def apply(vr: ValidationReport, builder: RDFBuilder): RDFBuilder =
    new ValidationReport2RDF().toRDF(vr,builder)

}