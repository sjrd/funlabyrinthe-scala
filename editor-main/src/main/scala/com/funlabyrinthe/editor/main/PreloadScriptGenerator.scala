package com.funlabyrinthe.editor.main

import scala.quoted.*

import scala.scalajs.js

import com.funlabyrinthe.editor.main.electron.ipcMain

object PreloadScriptGenerator:
  def compose(generatedServices: String*): String =
    val header = "const { contextBridge, ipcRenderer } = require('electron');\n"
    header + generatedServices.mkString

  inline def generateFor[Service <: js.Object](inline name: String): String =
    ${ generateForImpl[Service]('name) }

  def generateForImpl[Service <: js.Object](nameExpr: Expr[String])(using Quotes, Type[Service]): Expr[String] =
    import quotes.reflect.*

    val serviceName = nameExpr.valueOrAbort

    val tp = TypeRepr.of[Service]
    val tpCls = tp.classSymbol.getOrElse {
      report.errorAndAbort(s"$tp because it is not a class")
    }

    val exposedLines: List[String] =
      for m <- tpCls.declaredMethods yield
        val name = m.name
        val termParamSyms = m.paramSymss.flatten.filter(_.isTerm)
        val paramNames = termParamSyms.map(_.name)

        val commandNameString = s"'$serviceName:$name'"
        val commandAndParamNames = commandNameString :: paramNames

        s"$name: (${paramNames.mkString(", ")}) => ipcRenderer.invoke(${commandAndParamNames.mkString(", ")})"
    end exposedLines

    val exposedObject = exposedLines.mkString("{\n  ", ",\n  ", "\n}")

    val result = s"contextBridge.exposeInMainWorld('$serviceName', $exposedObject);\n"
    Expr(result)
  end generateForImpl

  inline def registerHandler[Service <: js.Object](inline name: String, service: Service): Unit =
    ${ registerHandlerImpl[Service]('name, 'service) }

  def registerHandlerImpl[Service <: js.Object](nameExpr: Expr[String], serviceExpr: Expr[Service])(using Quotes, Type[Service]): Expr[Unit] =
    import quotes.reflect.*

    val serviceName = nameExpr.valueOrAbort

    val tp = TypeRepr.of[Service]
    val tpCls = tp.classSymbol.getOrElse {
      report.errorAndAbort(s"$tp because it is not a class")
    }

    val seqApplySym = TypeRepr.of[Seq[Any]].classSymbol.get.methodMember("apply").head

    val registrations: List[Expr[Unit]] =
      for m <- tpCls.declaredMethods yield
        val name = m.name
        val termParamSyms = m.paramSymss.flatten.filter(_.isTerm)
        val MethodType(paramNames, paramTypes, resultType) = tp.memberType(m): @unchecked

        val commandName = s"$serviceName:$name"

        def genCall(argsExpr: Expr[Seq[Any]])(using Quotes): Expr[js.Promise[Any]] =
          Apply(Select(serviceExpr.asTerm, m), ({
            for (paramType, i) <- paramTypes.zipWithIndex yield
              paramType.widenByName.asType match
                case '[t] =>
                  val argAnyTerm = Apply(Select(argsExpr.asTerm, seqApplySym), List(Expr(i).asTerm))
                  '{ ${ argAnyTerm.asExpr }.asInstanceOf[t] }.asTerm
          })).asExprOf[js.Promise[Any]]
        end genCall

        '{
          ipcMain.handle(${ Expr(commandName) }, ({ (event, args) =>
            ${ genCall('args) }
          }: HandlerFunction))
        }
    end registrations

    Block(registrations.map(_.asTerm), '{ () }.asTerm).asExprOf[Unit]
  end registerHandlerImpl

  private trait HandlerFunction extends js.Function:
    def apply(event: js.Object, args: Any*): js.Promise[Any]
  end HandlerFunction
end PreloadScriptGenerator
