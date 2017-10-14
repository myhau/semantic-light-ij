package com.michalfudala.semanticlight

import com.intellij.ide.hierarchy.HierarchyBrowserBaseEx
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor
import com.intellij.ide.hierarchy.call.CalleeMethodsTreeStructure
import com.intellij.ide.ui.search.SearchableOptionsRegistrar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import java.awt.Color

class HighlightDeep : AnAction() {

  companion object {
    var lastHighlighters: List<RangeHighlighter> = emptyList()
  }

  val foregroundMostImportant = 0.6f
  val foregroundLeastImportant = 0.1f

  val hue = 0.1f
  val brightness = 1.0f

  val layer = 5

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: error("no project")
    val editor = e.getData(CommonDataKeys.EDITOR) ?: error("no editor")

    val selection = editor.selectionModel

    val fileManager = FileEditorManager.getInstance(project)

    val fle = fileManager.selectedFiles.first()!!

    val psi = PsiManager.getInstance(project).findFile(fle) as? PsiJavaFile
    val firstClass = psi?.classes?.get(0) ?: return

    val usagesCount = usagesOfMethodsFromSameClass(firstClass, project)

    lastHighlighters.forEach { editor.markupModel.removeHighlighter(it) }

    val maxUsages = usagesCount.map { it.count() }.max()?.toFloat() ?: 0f

    println(
        usagesCount.map { it.count() }
    )

    val highlighers =
        usagesCount.map { usage ->

          val a = foregroundMostImportant
          val b = foregroundLeastImportant

          val howMuch = (usage.count().toFloat() / maxUsages) * (a - b) + b
          val backgroundColor = Color.getHSBColor(hue, howMuch, brightness)

          val textAttributes = TextAttributes(null, backgroundColor, null, null, 10)

          val (start, end) = usage.range()

          editor.markupModel.addRangeHighlighter(start, end, layer, textAttributes, HighlighterTargetArea.LINES_IN_RANGE)
        }

    lastHighlighters = highlighers


  }

  data class MethodC(
      val method: PsiMethod,
      val calls: List<MethodC>
  ) {
    companion object {
      fun fromOneLevelOf(tree: CalleeMethodsTreeStructure): MethodC {
        return fromOneLevelOf(tree, tree.rootElement as CallHierarchyNodeDescriptor)
      }

      fun fromOneLevelOf(tree: CalleeMethodsTreeStructure, desc: CallHierarchyNodeDescriptor): MethodC {

        val calls = tree.getChildElements(desc)
            .map {
              val newDesc = it as CallHierarchyNodeDescriptor
              val meth = it.targetElement as? PsiMethod
              meth?.let { fromOneLevelOf(tree, newDesc) }
            }
            .filterNotNull()

        val method = desc.targetElement as PsiMethod

        return MethodC(method, calls)
      }
    }

    fun range(): Pair<Int, Int> {
      val range = method.textRange
      return Pair(range.startOffset, range.endOffset)
    }

    fun count(): Int {
      return calls.fold(0, { acc, s -> acc + 1 + s.count()})
    }
  }

  private fun usagesOfMethodsFromSameClass(psiClass: PsiClass, project: Project): List<MethodC> {
    val methods = psiClass.methods

    val callHierarchies =
        methods
            .map { Pair(it, CalleeMethodsTreeStructure(project, it, HierarchyBrowserBaseEx.SCOPE_CLASS)) }

    val methodz = callHierarchies.map { MethodC.fromOneLevelOf(it.second) }


    return methodz
  }

  private inline fun printAndReturn(str: String) {
    println(str)
    return
  }
}