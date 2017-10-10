package com.michalfudala.semanticlight

import com.intellij.ide.ui.search.SearchableOptionContributor
import com.intellij.ide.ui.search.SearchableOptionProcessor
import com.intellij.ide.ui.search.SearchableOptionsRegistrar
import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.progress.ProgressIndicator


class HighlightOptionContributor : SearchableOptionContributor() {

  class HighlightConfigurable: SearchableConfigurable {
    override fun apply() {

    }

    companion object {
      val ID = "highlight.configurable"
    }

    override fun getId(): String {
      return ID
    }
  }

  override fun processOptions(processor: SearchableOptionProcessor) {
    val text = "Semantic highlight current file"
    processor.addOptions(text, null, text, HighlightConfigurable.ID, null, true)
  }

}