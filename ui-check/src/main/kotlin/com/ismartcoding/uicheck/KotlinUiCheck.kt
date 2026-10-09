package com.ismartcoding.uicheck

import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

internal data class Finding(val key: String, val line: Int, val replacement: String)
internal data class Component(val name: String, val line: Int)

@OptIn(CompilerConfiguration.Internals::class, org.jetbrains.kotlin.CoreEnvironmentDeprecation::class, org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)
internal class KotlinUiCheck : AutoCloseable {
    private val disposable = Disposer.newDisposable()
    private val environment = KotlinCoreEnvironment.createForProduction(
        disposable, CompilerConfiguration().apply { extensionsStorage = CompilerPluginRegistrar.ExtensionStorage() }, EnvironmentConfigFiles.JVM_CONFIG_FILES,
    )
    private val factory = KtPsiFactory(environment.project)

    private val forbidden = mapOf(
        "androidx.compose.material3.Scaffold" to "PScaffold",
        "androidx.compose.material3.TopAppBar" to "PTopAppBar",
        "androidx.compose.material3.CenterAlignedTopAppBar" to "PTopAppBar",
        "androidx.compose.material3.MediumTopAppBar" to "PTopAppBar",
        "androidx.compose.material3.LargeTopAppBar" to "PTopAppBar",
        "androidx.compose.material3.ModalBottomSheet" to "PModalBottomSheet",
        "androidx.compose.material3.Button" to "PFilledButton",
        "androidx.compose.material3.OutlinedButton" to "POutlinedButton",
        "androidx.compose.material3.TextButton" to "PTextButton",
        "androidx.compose.material3.TextField" to "PTextField",
        "androidx.compose.material3.OutlinedTextField" to "PTextField",
        "androidx.compose.foundation.shape.RoundedCornerShape" to "MaterialTheme.shapes / CircleShape",
    )

    private val sheetRows = setOf("PSheetPrimaryActionsCard", "PSheetPrimaryActionsRow")
    private val sheetActions = setOf("PSheetPrimaryAction", "PSheetPrimaryTrashAction", "PSheetPrimaryDeleteAction")
    private val contractSymbols = (sheetRows + sheetActions + setOf("PListItem", "PInputChip"))
        .map { "com.ismartcoding.plain.ui.base.$it" }.toSet()

    fun inspect(path: String, source: String, library: Boolean = false): Pair<List<Finding>, List<Component>> {
        val file = factory.createFile(source)
        val imports = file.importDirectives.filter { !it.isAllUnder }.associate {
            (it.aliasName ?: it.importedFqName!!.shortName().asString()) to it.importedFqName!!.asString()
        }
        val wildcardPackages = file.importDirectives.filter { it.isAllUnder }.mapNotNull { it.importedFqName?.asString() }
        fun line(element: KtElement) = source.take(element.textOffset).count { it == '\n' } + 1
        fun owner(element: KtElement): String {
            val names = generateSequence(element.parent) { it.parent }.mapNotNull {
                when (it) {
                    is KtNamedFunction -> it.name
                    is KtClassOrObject -> it.name
                    is KtProperty -> it.name
                    else -> null
                }
            }.toList().reversed()
            return names.joinToString(".").ifEmpty { "<top-level>" }
        }
        fun resolve(call: KtCallExpression): String? {
            val callee = call.calleeExpression?.text ?: return null
            val qualified = call.parent as? KtDotQualifiedExpression
            if (qualified?.selectorExpression == call) {
                val receiver = qualified.receiverExpression.text
                val prefix = imports[receiver] ?: receiver
                return "$prefix.$callee".takeIf { it in forbidden || it in contractSymbols }
            }
            // Local declarations shadow unqualified imports; this is a syntax guard,
            // not a replacement for the compiler's type resolution.
            val localDeclaration = file.declarations.filterIsInstance<KtNamedFunction>().any { it.name == callee }
            if (localDeclaration) return null
            imports[callee]?.let { return it }
            return wildcardPackages.map { "$it.$callee" }.singleOrNull { it in forbidden || it in contractSymbols }
        }
        val findings = mutableListOf<Finding>()
        val components = mutableListOf<Component>()
        file.accept(object : KtTreeVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)
                if (!library) {
                    val symbol = resolve(expression) ?: return
                    forbidden[symbol]?.let { replacement ->
                        findings += Finding("$path|${owner(expression)}|$symbol", line(expression), replacement)
                    }
                    val shortName = symbol.substringAfterLast('.')
                    if (symbol in contractSymbols) {
                        if (shortName in sheetRows) {
                            val body = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()?.bodyExpression
                            val directActions = body?.statements?.filterIsInstance<KtCallExpression>()?.count {
                                resolve(it) in sheetActions.map { action -> "com.ismartcoding.plain.ui.base.$action" }
                            } ?: 0
                            if (directActions > 4) findings += Finding("$path|${owner(expression)}|sheet-overflow", line(expression), "Move actions after the fourth to PSheetActionCard")
                        }
                        val arguments = expression.valueArguments.associate { it.getArgumentName()?.asName?.asString() to it.getArgumentExpression() }
                        if (shortName == "PListItem" && arguments["titleTrailing"] is KtLambdaExpression && arguments["titleSuffix"] is KtLambdaExpression) {
                            findings += Finding("$path|${owner(expression)}|conflicting-title-slots", line(expression), "Choose either titleTrailing or titleSuffix")
                        }
                        if (shortName == "PInputChip" && arguments["onClick"]?.text == "null" && (arguments["onClose"] == null || arguments["onClose"]?.text == "null")) {
                            findings += Finding("$path|${owner(expression)}|chip-without-action", line(expression), "Provide onClose for a remove-only chip")
                        }
                    }
                }
            }

            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                if (!library || function.hasModifier(KtTokens.PRIVATE_KEYWORD) || function.hasModifier(KtTokens.INTERNAL_KEYWORD)) return
                if (function.annotationEntries.none { (imports[it.shortName?.asString()] ?: it.typeReference?.text) in setOf("androidx.compose.runtime.Composable", "Composable") }) return
                val name = function.name ?: return
                if (!name.first().isUpperCase()) return
                components += Component(name, line(function))
                if (!path.contains("/theme/") && function.valueParameters.none { it.name == "modifier" }) {
                    findings += Finding("$path|$name|missing-modifier", line(function), "Add a root modifier parameter")
                }
                function.valueParameters.forEach { parameter ->
                    val default = parameter.defaultValue as? KtLambdaExpression ?: return@forEach
                    if (default.bodyExpression?.statements?.isEmpty() == true && (parameter.name?.startsWith("on") == true || parameter.name == "click")) {
                        findings += Finding("$path|$name.${parameter.name}|empty-action", line(parameter), "Require the action or use a nullable callback")
                    }
                }
            }
        })
        if (library && components.map { it.name }.distinct().size > 1) {
            findings += Finding("$path|<file>|multiple-components", components[1].line, "Place each public component in its own file")
        }
        return findings to components
    }

    override fun close() = Disposer.dispose(disposable)
}
