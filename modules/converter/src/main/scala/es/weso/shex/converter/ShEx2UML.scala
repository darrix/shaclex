package es.weso.shex.converter

import cats._
import cats.data._
import cats.implicits._
import cats.data.{EitherT, State, StateT}
import es.weso.shex.{Schema, ShapeExpr}
import es.weso.uml.UMLDiagram.{UML, UMLClass}

object ShEx2UML {

  type S[A] = State[UML,A]
  type Converter[A] = EitherT[S,String,A]

  def ok[A](x:A): Converter[A] =
    EitherT.pure[S, String](x)

  def err[A](s: String): Converter[A] =
    EitherT.left[A](State.pure(s))

  def modify(fn: UML => UML): Converter[Unit] =
    EitherT.liftF(State.modify(fn))

  def schema2Uml(schema: Schema): Either[String,UML] = {
    val (uml, maybe) = cnvSchema(schema).value.run(UML.empty).value
    maybe.map(_ => uml)
  }

  def cnvSchema(schema: Schema): Converter[Unit] = {
    schema.shapes match {
      case None => err(s"No shapes in schema")
      case Some(shapes) => {
        def cmb(x: Unit, s: ShapeExpr): Converter[Unit] = for {
          cls <- cnvShapeExpr(s)
          _ <- modify(_.addClass(cls))
        } yield (())
        shapes.foldM(())(cmb)
      }
    }
  }

 def cnvShapeExpr(se: ShapeExpr): Converter[UMLClass] = se.id match {
   case None => ???
   case Some(label) => ???
 }


}