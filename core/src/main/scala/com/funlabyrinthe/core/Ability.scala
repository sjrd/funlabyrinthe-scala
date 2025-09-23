package com.funlabyrinthe.core

import scala.reflect.{ClassTag, classTag}

import com.funlabyrinthe.core.inspecting.*
import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.pickling.Pickleable.RemoveRefResult

/** Base class for player abilities. */
abstract class Ability

object Ability:
  def register[T <: Ability]()(
      using ClassTag[T], Universe, Pickleable[T], Inspectable[T]): Unit =
    summon[Universe].registerAbility(classTag[T].runtimeClass.asInstanceOf[Class[T]])
  end register

  given AbilityIsPickleable: Pickleable[Ability] with
    def pickle(value: Ability)(using PicklingContext): Pickle =
      getAbilityDescriptorFor(summon[PicklingContext].universe, value) match
        case Some(AbilityAndItsDescriptor(value, descriptor)) =>
          val underlyingPickle = descriptor.pickleable.pickle(value)
          ListPickle(List(StringPickle(descriptor.className), underlyingPickle))
        case None =>
          PicklingContext.error(
            s"Cannot save because there is a reference to an unregistered Ability ${value.getClass().getName()}"
          )
          ListPickle(Nil)
    end pickle

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Ability] =
      pickle match
        case ListPickle(List(StringPickle(className), underlyingPickle)) =>
          summon[PicklingContext].universe.getAbilityDescriptor(className) match
            case Some(descriptor) =>
              descriptor.pickleable.unpickle(underlyingPickle)
            case None =>
              PicklingContext.error(s"Unknown Ability type $className")
        case _ =>
          PicklingContext.typeError(s"list of a string and a second element", pickle)
    end unpickle

    def removeReferences(value: Ability, reference: Component)(using PicklingContext): RemoveRefResult[Ability] =
      getAbilityDescriptorFor(summon[PicklingContext].universe, value) match
        case Some(AbilityAndItsDescriptor(value, descriptor)) =>
          descriptor.pickleable.removeReferences(value, reference)
        case None =>
          // It's not clear whether failing or just ignoring is better; ignore for now
          RemoveRefResult.Unchanged
    end removeReferences
  end AbilityIsPickleable

  given AbilityIsInspectable: Inspectable[Ability] with
    type AltValueType // >: Union[i.EditorValueType for i in allAbilityInspectablesInTheWorld]
    type EditorValueType = (String, AltValueType)

    def editor(using Universe): Editor.Sum[AltValueType] =
      val descriptors = summon[Universe].getAllAbilityDescriptors()
      Editor.Sum(descriptors.map(_.className), descriptors.map { descriptor =>
        descriptor.inspectable.editor.asInstanceOf[Editor { type ValueType <: AltValueType }]
      })

    def toEditorValue(value: Ability)(using Universe): EditorValueType =
      getAbilityDescriptorFor(summon[Universe], value) match
        case Some(AbilityAndItsDescriptor(value, descriptor)) =>
          val altValue: descriptor.inspectable.EditorValueType = descriptor.inspectable.toEditorValue(value)
          (descriptor.className, altValue.asInstanceOf[AltValueType])
        case None =>
          throw IllegalArgumentException(
            s"Cannot inspect an unregistered Ability ${value.getClass().getName()}"
          )
    end toEditorValue

    def fromEditorValue(editorValue: (String, AltValueType))(using Universe): Ability =
      val (className, altValue) = editorValue
      summon[Universe].getAbilityDescriptor(className) match
        case Some(descriptor) =>
          descriptor.inspectable.fromEditorValue(altValue.asInstanceOf[descriptor.inspectable.EditorValueType])
        case None =>
          throw IllegalArgumentException(s"Unknown Ability type $className")
    end fromEditorValue
  end AbilityIsInspectable

  given OptionOfAbilityIsInspectable: Inspectable[Option[Ability]] with
    type AltValueType = AbilityIsInspectable.AltValueType | EmptyTuple
    type EditorValueType = (String, AltValueType)

    private val NoneStr = "(none)"

    def editor(using Universe): Editor.Sum[AltValueType] =
      val Editor.Sum(altNames, altEditors) = AbilityIsInspectable.editor
      Editor.Sum(NoneStr :: altNames, Editor.Struct[EmptyTuple](Nil, Nil) :: altEditors)

    def toEditorValue(value: Option[Ability])(using Universe): EditorValueType =
      value.fold((NoneStr, EmptyTuple))(AbilityIsInspectable.toEditorValue(_))

    def fromEditorValue(editorValue: EditorValueType)(using Universe): Option[Ability] =
      if editorValue._1 == NoneStr then None
      else Some(AbilityIsInspectable.fromEditorValue(editorValue.asInstanceOf[AbilityIsInspectable.EditorValueType]))
  end OptionOfAbilityIsInspectable

  private final case class AbilityAndItsDescriptor[T <: Ability](value: T, descriptor: AbilityDescriptor[T])

  private def getAbilityDescriptorFor(universe: Universe, value: Ability): Option[AbilityAndItsDescriptor[?]] =
    val cls = value.getClass()
    universe.getAbilityDescriptor(cls) match
      case Some(descriptor: AbilityDescriptor[t]) =>
        Some(AbilityAndItsDescriptor[t](downCast[Ability, t](value), descriptor))
      case None =>
        None
  end getAbilityDescriptorFor

  private def downCast[T, S <: T](t: T): S = t.asInstanceOf[S]
end Ability
