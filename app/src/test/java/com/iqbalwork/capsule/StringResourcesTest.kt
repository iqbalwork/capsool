package com.iqbalwork.capsule

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps the English and Indonesian string files in step: a string added to one language but not
 * the other, or translated with different format arguments, would only show up at runtime as a
 * missing text or a crash in `getString`.
 */
class StringResourcesTest {

    private val resDir = listOf("src/main/res", "app/src/main/res").map(::File).first { it.isDirectory }

    /** key -> text for every <string> in the strings files of one language folder. */
    private fun load(folder: String): Map<String, String> {
        val files = File(resDir, folder).listFiles { f -> f.name.startsWith("strings") && f.name.endsWith(".xml") }
            .orEmpty().sortedBy { it.name }
        val result = linkedMapOf<String, String>()
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        for (file in files) {
            val nodes = builder.parse(file).getElementsByTagName("string")
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                if (node.attributes.getNamedItem("translatable")?.nodeValue == "false") continue
                val name = node.attributes.getNamedItem("name").nodeValue
                assertTrue("duplicate string '$name' (again in ${file.name})", name !in result)
                result[name] = node.textContent
            }
        }
        return result
    }

    private val placeholder = Regex("%(\\d+\\$)?[sdf]")

    @Test
    fun everyEnglishStringHasAnIndonesianTranslationAndViceVersa() {
        val en = load("values")
        val id = load("values-in")
        assertTrue("no English strings found in $resDir", en.isNotEmpty())
        assertEquals("missing in values-in", emptySet<String>(), en.keys - id.keys)
        assertEquals("only in values-in", emptySet<String>(), id.keys - en.keys)
    }

    @Test
    fun translationsUseTheSameFormatArguments() {
        val en = load("values")
        val id = load("values-in")
        val mismatched = en.keys.intersect(id.keys).filter { key ->
            placeholder.findAll(en.getValue(key)).map { it.value }.sorted().toList() !=
                placeholder.findAll(id.getValue(key)).map { it.value }.sorted().toList()
        }
        assertEquals("format arguments differ between languages", emptyList<String>(), mismatched)
    }

    @Test
    fun noTranslationIsBlank() {
        val blank = load("values-in").filterValues { it.isBlank() }.keys
        assertEquals(emptySet<String>(), blank)
    }
}
