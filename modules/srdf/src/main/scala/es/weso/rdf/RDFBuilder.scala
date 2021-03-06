package es.weso.rdf

import es.weso.rdf.triples._
import es.weso.rdf.nodes._
import PREFIXES._

trait RDFBuilder extends RDFReader {

  type Rdf <: RDFBuilder

  def addPrefixMap(pm: PrefixMap): Rdf

  def addPrefix(alias: String, iri: String): Rdf

  def createBNode: (RDFNode, Rdf)

  def mkBNode: Either[String, (RDFNode, Rdf)] = Right(createBNode)

  def addTriples(triples: Set[RDFTriple]): Either[String,Rdf]

  def addTriple(triple: RDFTriple): Either[String,Rdf] = {
    addTriples(Set(triple))
  }

  def addType(node: RDFNode, typeNode: RDFNode): Either[String,Rdf] = {
    addTriple(RDFTriple(node, rdf_type, typeNode))
  }

  def rmTriple(triple: RDFTriple): Either[String,Rdf]

  def empty: Rdf

}

