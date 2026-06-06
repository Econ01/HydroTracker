package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme

/**
 * Shared building blocks for the settings document sheets (Changelog, Sources, Privacy, License).
 *
 * A small in-house Markdown renderer — no external libraries. Handles `#` through `#####` headers,
 * `- ` bullets (with double / triple indent), numbered lists, fully-bold `**…**` lines,
 * inline `**bold**`, `*italic*`, `_italic_`, and `---` dividers.
 */

/** A bottom sheet that loads [assetFileName] from `assets/` and renders it as Markdown. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarkdownBottomSheet(
    title: String,
    assetFileName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var content by remember { mutableStateOf("Loading…") }

    LaunchedEffect(assetFileName) {
        content = try {
            withContext(Dispatchers.IO) {
                context.assets.open(assetFileName).bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            "Error loading $assetFileName: ${e.message}"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MarkdownText(text = content, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Renders a (small subset of) markdown [text] as a vertical stack of Text rows. */
@Composable
internal fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val lines = text.split("\n")
    var versionCount = 0

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (line in lines) {
            val trimmed = line.trimStart()
            val indentLevel = (line.length - trimmed.length) / 2

            when {
                trimmed.matches(Regex("^\\[.+]$")) -> {
                    if (versionCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    versionCount++
                    Text(
                        text = "Version ${trimmed.removeSurrounding("[", "]")}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                trimmed == "Added" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Added",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                trimmed == "Fixed" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Fixed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                line.startsWith("# ") -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = line.substring(2),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                line.startsWith("## ") -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = line.substring(3),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                line.startsWith("### ") -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = line.substring(4),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                line.startsWith("#### ") -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = line.substring(5),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                line.startsWith("##### ") -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = line.substring(6),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("• ") -> {
                    val bulletText = if (trimmed.startsWith("- ")) trimmed.substring(2) else trimmed.substring(2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (indentLevel * 16).dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(bulletText),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val number = trimmed.substringBefore(". ")
                    val numberText = trimmed.substringAfter(". ")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (indentLevel * 16).dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "$number.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(numberText),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                line.trim().startsWith("**") && line.trim().endsWith("**") -> {
                    Text(
                        text = line.trim().removeSurrounding("**"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(2.dp))
                }
                line.trim() == "---" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    Text(
                        text = parseInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                    )
                }
            }
        }
    }
}

private fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("*", i) -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("_", i) -> {
                    val end = text.indexOf('_', i + 1)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MarkdownTextPreview() {
    HydroTrackerTheme {
        MarkdownText(
            modifier = Modifier.padding(16.dp),
            text = """
                # Heading 1
                ## Heading 2
                ### Heading 3
                #### Heading 4
                ##### Heading 5
                
                Normal paragraph with **inline bold** and *italic* text.
                You can also use _underscores_ for emphasis.
                
                **Fully bold line**
                
                - Bullet item one
                - Bullet item with **bold inside**
                  - Double indented bullet
                  - Another double indent with *italic*
                    - Triple indented bullet
                    - Triple with **bold** and _italic_
                
                1. First numbered item
                2. Second item with *italic*
                3. Third item with **bold**
                
                ---
                
                [1.0.6.1]
                • Small preparations for F-Droid release
                
                [1.0.6]
                Added
                • Widgets and notifications now respect the system time configuration
                • Add manual restore button to the HealthConnect page
                Fixed
                • Fix Health Connect read pagination — now reads all pages instead of only the first page
                • Fix restored entry date assignment to use user-day (wake-up time) instead of calendar day
                
                [1.0.5]
                Added
                • Now you can remove and re-order beverages
            """.trimIndent()
        )
    }
}
