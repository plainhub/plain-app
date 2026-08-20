package com.ismartcoding.ksp.kgraphql

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import java.io.OutputStreamWriter

/**
 * KSP2 processor that scans `@GraphQLType` / `@GraphQLInput` / `@GraphQLUnion`
 * / `@GraphQLInterface` annotations and generates reflection-free descriptors
 * in commonMain.
 *
 * Generated code uses only KMP-safe constructs:
 *  - `ClassName::propertyName` references (KProperty1.get() works on iOS)
 *  - `KClass::class` references
 *  - `kotlinx.serialization` serializers (resolved via reified inline at call site)
 *
 * No `kotlin.reflect.full` — no `memberProperties`, `isSubclassOf`,
 * `sealedSubclasses`, `primaryConstructor`, `createType`, `findAnnotation`.
 */
class GraphQLSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    // Annotation FQNs (referenced by string — no compile dependency on :shared)
    private val TYPE_ANN = "com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType"
    private val INPUT_ANN = "com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLInput"
    private val INTERFACE_ANN = "com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLInterface"
    private val UNION_ANN = "com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLUnion"
    private val IGNORE_ANN = "com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLIgnore"
    private val FIELD_ANN = "com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLField"
    private val QUERY_ANN = "com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery"
    private val MUTATION_ANN = "com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation"
    private val SUBSCRIPTION_ANN = "com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLSubscription"

    // Collected during process() — used to generate the registration file
    private data class TypeEntry(
        val packageName: String,
        val className: String,
        val descriptorObject: String,
        val isInterface: Boolean,
        val kClassFqn: String,
    )

    private data class InputEntry(
        val packageName: String,
        val className: String,
        val descriptorObject: String,
        val kClassFqn: String,
    )

    private data class UnionEntry(
        val packageName: String,
        val descriptorObject: String,
        val name: String,
        val memberFqns: List<String>,
        val kClassFqn: String,
    )

    private data class InterfaceEntry(
        val packageName: String,
        val className: String,
        val descriptorObject: String,
        val kClassFqn: String,
        val implementorFqns: List<String>,
    )

    private val typeEntries = mutableListOf<TypeEntry>()
    private val inputEntries = mutableListOf<InputEntry>()
    private val unionEntries = mutableListOf<UnionEntry>()
    private val interfaceEntries = mutableListOf<InterfaceEntry>()

    /**
     * Mirror of `GraphQLSchemaTarget` from the shared annotations. Kept local
     * because this processor module has no compile dependency on :shared — it
     * resolves the annotation argument to one of these three constants.
     */
    private enum class GraphQLSchemaTarget { MAIN, PEER, GUEST }

    private data class ResolverEntry(
        val operationType: String,
        val name: String,
        val description: String?,
        val functionFqn: String,
        val functionName: String,
        val packageName: String,
        val parameters: List<Pair<String, String>>,
        val returnTypeString: String,
        val target: GraphQLSchemaTarget,
    )

    private val resolverEntries = mutableListOf<ResolverEntry>()

    // All @GraphQLType classes by FQN — used to find interface implementors
    private val allTypeClasses = mutableListOf<KSClassDeclaration>()

    // Track whether the registration file has been generated in this KSP run
    // to avoid FileAlreadyExistsException across processing rounds
    private var registrationGenerated = false

    // FQNs of classes we've already generated a TypeDescriptor for — prevents
    // duplicate generation when a type is both annotated and auto-discovered.
    private val generatedTypeFqns = mutableSetOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 1. Scan @GraphQLType — annotation-driven only.
        // KMP-safe: only explicitly annotated classes get descriptors.
        // Platform-specific classes (NSObject subclasses, Swift interop) are
        // never accidentally picked up because they live in iosMain/androidMain
        // and are not annotated.
        resolver.getSymbolsWithAnnotation(TYPE_ANN)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { cls ->
                allTypeClasses.add(cls)
                generateTypeDescriptor(cls, isInterface = false)
            }

        // 2. Scan @GraphQLInput
        resolver.getSymbolsWithAnnotation(INPUT_ANN)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { cls -> generateInputDescriptor(cls) }

        // 3. Scan @GraphQLUnion (sealed classes)
        resolver.getSymbolsWithAnnotation(UNION_ANN)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { cls -> generateUnionDescriptor(cls) }

        // 4. Scan @GraphQLInterface — after @GraphQLType so we can find implementors
        resolver.getSymbolsWithAnnotation(INTERFACE_ANN)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { cls -> generateInterfaceDescriptor(cls) }

        // 5. Scan @GraphQLQuery / @GraphQLMutation / @GraphQLSubscription
        resolver.getSymbolsWithAnnotation(QUERY_ANN)
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach { fn -> collectResolver(fn, "query") }
        resolver.getSymbolsWithAnnotation(MUTATION_ANN)
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach { fn -> collectResolver(fn, "mutation") }
        resolver.getSymbolsWithAnnotation(SUBSCRIPTION_ANN)
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach { fn -> collectResolver(fn, "subscription") }

        // 6. Generate the registration entry point (only once per KSP run).
        // Always generated — even when empty — so commonMain code that calls
        // GeneratedSchemaRegistration.registerAll() compiles unconditionally.
        if (!registrationGenerated) {
            generateRegistrationFile()
            generateResolverRegistrationFile()
            registrationGenerated = true
        }

        return emptyList()
    }

    // =========================================================================
    // @GraphQLType
    // =========================================================================

    private fun generateTypeDescriptor(cls: KSClassDeclaration, isInterface: Boolean) {
        val kClassFqn = cls.qualifiedName!!.asString()
        if (kClassFqn in generatedTypeFqns) return
        generatedTypeFqns.add(kClassFqn)

        val packageName = cls.packageName.asString()
        val className = cls.simpleName.asString()
        val classRef = cls.classRef()
        val descriptorObject = "${className}_GraphQLDescriptor"

        // Get custom name from annotation, default to class simple name
        val typeName = cls.getAnnotation(TYPE_ANN)?.getArgument("name")?.takeIf { it.isNotEmpty() }
            ?: className

        val fields = collectFields(cls)

        val fieldsCode = fields.joinToString(",\n            ") { f ->
            buildFieldDescriptor(f, classRef)
        }

        val content = """
            |package $packageName
            |
            |import com.ismartcoding.plain.lib.kgraphql.generated.FieldDescriptor
            |import com.ismartcoding.plain.lib.kgraphql.generated.TypeDescriptor
            |import kotlin.reflect.typeOf
            |
            |/**
            | * KSP-generated descriptor for [$classRef]. Do not edit — regenerated on every build.
            | * Replaces runtime reflection (`memberProperties`, `isPublicVisibility`) with
            | * compile-time-fixed `::foo` property references that work on iOS (Kotlin/Native).
            | */
            |object $descriptorObject {
            |    val descriptor: TypeDescriptor<$classRef> = TypeDescriptor(
            |        kClass = $classRef::class,
            |        name = "$typeName",
            |        isInterface = $isInterface,
            |        fields = listOf(
            |            $fieldsCode
            |        )
            |    )
            |}
        """.trimMargin()

        writeGeneratedFile(packageName, descriptorObject, content, cls.containingFile)

        typeEntries.add(
            TypeEntry(
                packageName = packageName,
                className = className,
                descriptorObject = descriptorObject,
                isInterface = isInterface,
                kClassFqn = kClassFqn,
            )
        )

        // Auto-discover: scan field types for un-annotated custom data classes
        // and generate descriptors for them recursively. This replaces the old
        // reflection-based memberProperties auto-discovery — users don't need
        // to annotate every nested type (e.g. MessageAttachment inside Message).
        cls.declarations
            .filterIsInstance<KSPropertyDeclaration>()
            .filter { prop -> isPublicProperty(prop) }
            .filterNot { prop -> prop.isAnnotationPresent(IGNORE_ANN) }
            .forEach { prop ->
                extractCustomType(prop.type.resolve())?.let { refCls ->
                    val refFqn = refCls.qualifiedName?.asString() ?: return@let
                    if (refFqn !in generatedTypeFqns && isDiscoverableType(refCls)) {
                        generateTypeDescriptor(refCls, isInterface = false)
                    }
                }
            }
    }

    /**
     * Extracts a custom [KSClassDeclaration] from a [KSType], unwrapping
     * collections and nullability. Returns null for standard library types
     * (kotlin.*, kotlinx.*, java.*).
     */
    private fun extractCustomType(type: KSType): KSClassDeclaration? {
        val decl = type.declaration as? KSClassDeclaration ?: return null
        val fqn = decl.qualifiedName?.asString() ?: return null
        // Skip standard library / platform types
        if (fqn.startsWith("kotlin.") || fqn.startsWith("kotlinx.") ||
            fqn.startsWith("java.") || fqn.startsWith("android.") ||
            fqn.startsWith("androidx.")
        ) {
            // For collections, recurse into the element type
            val elementKey = if (fqn == "kotlin.collections.Map") 1 else 0
            if (fqn.startsWith("kotlin.collections.") && type.arguments.isNotEmpty()) {
                val elementType = type.arguments.getOrNull(elementKey)?.type?.resolve() ?: return null
                return extractCustomType(elementType)
            }
            return null
        }
        return decl
    }

    /**
     * Returns true if [cls] is a candidate for auto-discovery: a public CLASS
     * (not interface/object/enum) with no type parameters, no companion object,
     * and at least one public property. Accepts both data classes and regular
     * classes with var/val properties (e.g. DeviceInfo). Excludes wrapper types
     * like ID (which has a companion object), private/internal types, and
     * generic classes.
     */
    private fun isDiscoverableType(cls: KSClassDeclaration): Boolean {
        if (cls.classKind != ClassKind.CLASS) return false
        // Skip non-public types (private/internal) — can't be referenced from generated code
        if (Modifier.PRIVATE in cls.modifiers || Modifier.INTERNAL in cls.modifiers) return false
        // Skip abstract / open classes — only concrete types are valid GraphQL Objects
        if (Modifier.ABSTRACT in cls.modifiers || Modifier.OPEN in cls.modifiers) return false
        // Skip generic classes — they need explicit @GraphQLType with type parameters resolved
        if (cls.typeParameters.isNotEmpty()) return false
        // Skip types with companion objects (e.g. ID with KSerializer companion)
        if (cls.declarations.any {
                it is KSClassDeclaration &&
                    it.classKind == ClassKind.OBJECT &&
                    it.simpleName.asString() == "Companion"
            }) return false
        // Skip types that already have @GraphQLType — they're processed explicitly
        if (cls.getAnnotation(TYPE_ANN) != null) return false
        // Skip @GraphQLInput — those are handled separately
        if (cls.getAnnotation(INPUT_ANN) != null) return false
        // Skip @GraphQLIgnore — user explicitly opted out
        if (cls.getAnnotation(IGNORE_ANN) != null) return false
        // Must have at least one public property to be a valid GraphQL Object
        return cls.declarations
            .filterIsInstance<KSPropertyDeclaration>()
            .any { prop -> isPublicProperty(prop) && !prop.isAnnotationPresent(IGNORE_ANN) }
    }

    // =========================================================================
    // @GraphQLInput
    // =========================================================================

    private fun generateInputDescriptor(cls: KSClassDeclaration) {
        val packageName = cls.packageName.asString()
        val className = cls.simpleName.asString()
        val classRef = cls.classRef()
        val kClassFqn = cls.qualifiedName!!.asString()
        val descriptorObject = "${className}_GraphQLInputDescriptor"

        val typeName = cls.getAnnotation(INPUT_ANN)?.getArgument("name")?.takeIf { it.isNotEmpty() }
            ?: className

        val fields = collectFields(cls)
        val optionalParams = collectOptionalParams(cls)

        val fieldsCode = fields.joinToString(",\n            ") { f ->
            buildFieldDescriptor(f, classRef)
        }
        val optionalCode = optionalParams.joinToString(", ") { "\"$it\"" }

        // Generate fromMap factory: directly calls the constructor with explicit
        // field accesses from the map. No primaryConstructor.callBy() reflection.
        // For optional params (with default values), use `as?` + Elvis operator
        // with a type-appropriate default so the result matches the non-nullable
        // constructor parameter type.
        // For Set<T> types, convert from List (GraphQL arrays are always parsed
        // as List) to Set via .toSet().
        val constructorParams = fields.joinToString(",\n        ") { f ->
            val typeStr = f.kotlinTypeString
            val isSet = typeStr.startsWith("kotlin.collections.Set<") || typeStr.startsWith("kotlin.collections.MutableSet<")
            val paramValue = if (f.name in optionalParams) {
                val default = defaultLiteralForType(typeStr)
                if (isSet) {
                    "(values[\"${f.name}\"] as? kotlin.collections.Collection<*>)?.toSet() as? $typeStr ?: $default"
                } else if (typeStr.endsWith("?")) {
                    "values[\"${f.name}\"] as? ${typeStr.removeSuffix("?")}"
                } else {
                    "values[\"${f.name}\"] as? $typeStr ?: $default"
                }
            } else {
                if (isSet) {
                    "(values[\"${f.name}\"] as kotlin.collections.Collection<*>).toSet() as $typeStr"
                } else {
                    "values[\"${f.name}\"] as $typeStr"
                }
            }
            "${f.name} = $paramValue"
        }

        val content = """
            |package $packageName
            |import com.ismartcoding.plain.lib.kgraphql.generated.FieldDescriptor
            |import com.ismartcoding.plain.lib.kgraphql.generated.InputDescriptor
            |import kotlin.reflect.typeOf
            |
            |/**
            | * KSP-generated descriptor for input type [$classRef]. Do not edit.
            | * Input objects are instantiated via [fromMap] (direct constructor call)
            | * instead of `primaryConstructor.callBy()` reflection — iOS compatible.
            | */
            |object $descriptorObject {
            |    val descriptor: InputDescriptor<$classRef> = InputDescriptor(
            |        kClass = $classRef::class,
            |        name = "$typeName",
            |        fields = listOf(
            |            $fieldsCode
            |        ),
            |        optionalParams = setOf($optionalCode),
            |        fromMap = { values ->
            |            $classRef(
            |                $constructorParams
            |            )
            |        }
            |    )
            |}
        """.trimMargin()

        writeGeneratedFile(packageName, descriptorObject, content, cls.containingFile)

        inputEntries.add(
            InputEntry(
                packageName = packageName,
                className = className,
                descriptorObject = descriptorObject,
                kClassFqn = kClassFqn,
            )
        )
    }

    // =========================================================================
    // @GraphQLUnion (sealed class)
    // =========================================================================

    private fun generateUnionDescriptor(cls: KSClassDeclaration) {
        val packageName = cls.packageName.asString()
        val className = cls.simpleName.asString()
        val kClassFqn = cls.qualifiedName!!.asString()
        val descriptorObject = "${className}_GraphQLUnionDescriptor"

        val unionName = cls.getAnnotation(UNION_ANN)?.getArgument("name")?.takeIf { it.isNotEmpty() }
            ?: className

        // getSealedSubclasses() is a KSP Symbol API — compile-time, not runtime reflection
        val members = cls.getSealedSubclasses().toList()
        val memberFqns = members.map { it.qualifiedName!!.asString() }
        val memberClassRefs = members.joinToString(", ") { member ->
            val fqn = member.qualifiedName!!.asString()
            // Use fully-qualified reference to avoid import issues
            "$fqn::class"
        }

        val content = """
            |package $packageName
            |import com.ismartcoding.plain.lib.kgraphql.generated.UnionDescriptor
            |
            |/**
            | * KSP-generated descriptor for union type [$className] (sealed class).
            | * Members are read at compile time via KSP's [getSealedSubclasses] —
            | * no `sealedSubclasses` runtime reflection.
            | */
            |object $descriptorObject {
            |    val descriptor = UnionDescriptor(
            |        kClass = $kClassFqn::class,
            |        name = "$unionName",
            |        members = listOf($memberClassRefs)
            |    )
            |}
        """.trimMargin()

        writeGeneratedFile(packageName, descriptorObject, content, cls.containingFile)

        unionEntries.add(
            UnionEntry(
                packageName = packageName,
                descriptorObject = descriptorObject,
                name = unionName,
                memberFqns = memberFqns,
                kClassFqn = kClassFqn,
            )
        )
    }

    // =========================================================================
    // @GraphQLInterface
    // =========================================================================

    private fun generateInterfaceDescriptor(cls: KSClassDeclaration) {
        val packageName = cls.packageName.asString()
        val className = cls.simpleName.asString()
        val classRef = cls.classRef()
        val kClassFqn = cls.qualifiedName!!.asString()
        val descriptorObject = "${className}_GraphQLInterfaceDescriptor"

        val interfaceName = cls.getAnnotation(INTERFACE_ANN)?.getArgument("name")?.takeIf { it.isNotEmpty() }
            ?: className

        // Find all @GraphQLType classes that implement this interface.
        // This replaces runtime `isKotlinSubclassOf` reflection.
        val interfaceFqn = kClassFqn
        val implementors = allTypeClasses.filter { typeCls ->
            typeCls != cls && typeCls.implementsInterface(interfaceFqn)
        }
        val implementorFqns = implementors.map { it.qualifiedName!!.asString() }
        val implementorRefs = implementorFqns.joinToString(", ") { "$it::class" }

        // Interface fields: properties declared in the interface itself
        val fields = collectFields(cls)
        val fieldsCode = fields.joinToString(",\n            ") { f ->
            buildFieldDescriptor(f, classRef)
        }

        val content = """
            |package $packageName
            |import com.ismartcoding.plain.lib.kgraphql.generated.FieldDescriptor
            |import com.ismartcoding.plain.lib.kgraphql.generated.TypeDescriptor
            |import kotlin.reflect.typeOf
            |
            |/**
            | * KSP-generated descriptor for interface type [$classRef].
            | * Possible types are resolved at compile time by scanning all
            | * @GraphQLType classes that implement this interface — no
            | * `isKotlinSubclassOf` runtime reflection.
            | */
            |object $descriptorObject {
            |    val descriptor: TypeDescriptor<$classRef> = TypeDescriptor(
            |        kClass = $classRef::class,
            |        name = "$interfaceName",
            |        isInterface = true,
            |        fields = listOf(
            |            $fieldsCode
            |        ),
            |        possibleTypes = listOf($implementorRefs)
            |    )
            |}
        """.trimMargin()

        writeGeneratedFile(packageName, descriptorObject, content, cls.containingFile)

        interfaceEntries.add(
            InterfaceEntry(
                packageName = packageName,
                className = className,
                descriptorObject = descriptorObject,
                kClassFqn = kClassFqn,
                implementorFqns = implementorFqns,
            )
        )
    }

    // =========================================================================
    // Registration entry point
    // =========================================================================

    private fun generateRegistrationFile() {
        val packageName = "com.ismartcoding.plain.lib.kgraphql.generated"
        val fileName = "GeneratedSchemaRegistration"

        val typeImports = typeEntries.filter { !it.isInterface }.joinToString("\n") {
            "import ${it.packageName}.${it.descriptorObject}"
        }
        val interfaceImports = interfaceEntries.joinToString("\n") {
            "import ${it.packageName}.${it.descriptorObject}"
        }
        val inputImports = inputEntries.joinToString("\n") {
            "import ${it.packageName}.${it.descriptorObject}"
        }
        val unionImports = unionEntries.joinToString("\n") {
            "import ${it.packageName}.${it.descriptorObject}"
        }

        val typeRegistrations = typeEntries.filter { !it.isInterface }.joinToString("\n") {
            "    GeneratedSchemaRegistry.registerType(${it.descriptorObject}.descriptor)"
        }
        val interfaceRegistrations = interfaceEntries.joinToString("\n") {
            "    GeneratedSchemaRegistry.registerInterface(${it.descriptorObject}.descriptor)"
        }
        val inputRegistrations = inputEntries.joinToString("\n") {
            "    GeneratedSchemaRegistry.registerInput(${it.descriptorObject}.descriptor)"
        }
        val unionRegistrations = unionEntries.joinToString("\n") {
            "    GeneratedSchemaRegistry.registerUnion(${it.descriptorObject}.descriptor)"
        }

        val allImports = listOf(typeImports, interfaceImports, inputImports, unionImports)
            .filter { it.isNotEmpty() }
            .joinToString("\n")

        val content = """
            |package $packageName
            |
            |$allImports
            |/**
            | * KSP-generated actual for [registerAllGeneratedSchema]. Called by
            | * [com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder.registerGeneratedSchema]
            | * to populate [GeneratedSchemaRegistry] with all compile-time-discovered types.
            | *
            | * Do not edit — regenerated on every build by :kgraphql-ksp.
            | */
            |internal actual fun registerAllGeneratedSchema() {
            $typeRegistrations
            $interfaceRegistrations
            $inputRegistrations
            $unionRegistrations
            |}
        """.trimMargin()

        writeGeneratedFile(packageName, fileName, content, null)
    }

    // =========================================================================
    // @GraphQLQuery / @GraphQLMutation / @GraphQLSubscription
    // =========================================================================

    /** Resolve the `target` annotation argument to a [GraphQLSchemaTarget]. */
    private fun resolveTarget(targetArg: KSValueArgument?): GraphQLSchemaTarget {
        val v = targetArg?.value ?: return GraphQLSchemaTarget.MAIN
        val name = when (v) {
            is KSType -> v.declaration.simpleName.asString()
            is String -> v
            else -> v.toString().substringAfterLast('.')
        }
        return when (name) {
            "PEER" -> GraphQLSchemaTarget.PEER
            "GUEST" -> GraphQLSchemaTarget.GUEST
            else -> GraphQLSchemaTarget.MAIN
        }
    }

    private fun collectResolver(fn: KSFunctionDeclaration, operationType: String) {
        val annFqn = when (operationType) {
            "query" -> QUERY_ANN
            "mutation" -> MUTATION_ANN
            else -> SUBSCRIPTION_ANN
        }
        val ann = fn.annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == annFqn
        }
        val customName = ann?.getArgument("name")?.takeIf { it.isNotEmpty() }
        val description = ann?.getArgument("description")?.takeIf { it.isNotEmpty() }
        val targetArg = ann?.arguments?.firstOrNull { it.name?.asString() == "target" }
        val target = resolveTarget(targetArg)
        val name = customName ?: fn.simpleName.asString()

        val params = fn.parameters.map { p ->
            val pname = p.name?.asString() ?: throw IllegalStateException(
                "@GraphQL$operationType function ${fn.qualifiedName?.asString()} has unnamed parameter — KSP cannot derive GraphQL arg name"
            )
            val ptype = renderKotlinType(p.type.resolve())
            pname to ptype
        }

        val retType = fn.returnType?.resolve()
            ?: throw IllegalStateException(
                "@GraphQL$operationType function ${fn.qualifiedName?.asString()} must have an explicit return type"
            )
        val retTypeStr = renderKotlinType(retType)

        val fqn = fn.qualifiedName?.asString()
            ?: throw IllegalStateException(
                "@GraphQL$operationType function must be top-level or in an object"
            )
        val pkg = fn.packageName.asString()
        val simpleName = fqn.removePrefix("$pkg.")

        resolverEntries.add(
            ResolverEntry(
                operationType = operationType,
                name = name,
                description = description,
                functionFqn = fqn,
                functionName = simpleName,
                packageName = pkg,
                parameters = params,
                returnTypeString = retTypeStr,
                target = target,
            )
        )
    }

    private fun generateResolverRegistrationFile() {
        val packageName = "com.ismartcoding.plain.lib.kgraphql.generated"
        val fileName = "GeneratedResolverRegistration"

        val mainEntries = resolverEntries.filter { it.target == GraphQLSchemaTarget.MAIN }
        val peerEntries = resolverEntries.filter { it.target == GraphQLSchemaTarget.PEER }
        val guestEntries = resolverEntries.filter { it.target == GraphQLSchemaTarget.GUEST }

        val imports = mutableSetOf(
            "com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder",
            "com.ismartcoding.plain.lib.kgraphql.schema.model.FunctionWrapper",
        )
        resolverEntries.forEach { e -> imports.add(e.packageName + "." + e.functionName) }

        val importBlock = imports.sorted().joinToString("\n") { "import $it" }

        val mainBody = if (mainEntries.isEmpty()) "" else
            "\n" + mainEntries.joinToString("\n\n") { e -> buildResolverBlock(e) }
        val peerBody = if (peerEntries.isEmpty()) "" else
            "\n" + peerEntries.joinToString("\n\n") { e -> buildResolverBlock(e) }
        val guestBody = if (guestEntries.isEmpty()) "" else
            "\n" + guestEntries.joinToString("\n\n") { e -> buildResolverBlock(e) }

        val content = """
            |package $packageName
            |
            |$importBlock
            |
            |internal actual fun SchemaBuilder.registerGeneratedResolvers() {$mainBody
            |}
            |
            |internal actual fun SchemaBuilder.registerGeneratedPeerResolvers() {$peerBody
            |}
            |
            |internal actual fun SchemaBuilder.registerGeneratedGuestResolvers() {$guestBody
            |}
        """.trimMargin()

        writeGeneratedFile(packageName, fileName, content, null)
    }

    private fun buildResolverBlock(e: ResolverEntry): String {
        val descLine = e.description?.let { "        description = \"$it\"\n" } ?: ""
        val params = e.parameters
        val n = params.size

        val typeParams = if (n == 0) {
            e.returnTypeString
        } else {
            (listOf(e.returnTypeString) + params.map { it.second }).joinToString(", ")
        }

        val argNames = if (n > 0) {
            params.joinToString(", ") { "\"${it.first}\"" } + ", "
        } else ""

        val lambdaParams = if (n > 0) {
            params.joinToString(", ") { "${it.first}: ${it.second}" } + " ->"
        } else ""

        val callArgs = params.joinToString(", ") { it.first }

        return """
            |    ${e.operationType}("${e.name}") {
            |${descLine}        resolver(FunctionWrapper.on<$typeParams>($argNames{ $lambdaParams
            |            ${e.functionName}($callArgs)
            |        })
            |        )
            |    }
        """.trimMargin()
    }

    // =========================================================================
    // Field collection & code generation helpers
    // =========================================================================

    private data class FieldInfo(
        val name: String,
        val graphQLName: String,
        val description: String?,
        val isIgnored: Boolean,
        val kotlinTypeString: String,
    )

    /**
     * Collects public properties from a class declaration.
     * Skips private/protected/internal properties (matching the legacy
     * `isPublicVisibility()` filter in SchemaCompilation).
     * Skips `@GraphQLIgnore` properties entirely.
     * Honors `@GraphQLField` for name override and description.
     */
    private fun collectFields(cls: KSClassDeclaration): List<FieldInfo> {
        // Use declarations + filterIsInstance instead of getDeclaredProperties()
        // for maximum KSP version compatibility
        return cls.declarations
            .filterIsInstance<KSPropertyDeclaration>()
            .filter { prop -> isPublicProperty(prop) }
            .filterNot { prop -> prop.isAnnotationPresent(IGNORE_ANN) }
            .map { prop ->
                val fieldAnn = prop.findAnnotation(FIELD_ANN)
                FieldInfo(
                    name = prop.simpleName.asString(),
                    graphQLName = fieldAnn?.getArgument("name")?.takeIf { it.isNotEmpty() }
                        ?: prop.simpleName.asString(),
                    description = fieldAnn?.getArgument("description")?.takeIf { it.isNotEmpty() },
                    isIgnored = false,
                    kotlinTypeString = renderKotlinType(prop.type.resolve()),
                )
            }
            .toList()
    }

    /**
     * Renders a KSType to a Kotlin type literal for use in generated code.
     * Uses fully-qualified names for all types so that `typeOf<FQN>()` and
     * `as FQN` casts compile without additional imports.
     * Examples: kotlin.String, kotlin.Int, kotlin.collections.List<kotlin.String>,
     *           kotlin.time.Instant?, com.ismartcoding.plain.enums.DeviceType
     */
    private fun renderKotlinType(type: KSType): String {
        val decl = type.declaration
        val baseName = decl.qualifiedName?.asString() ?: decl.simpleName.asString()
        val args = type.arguments
        val argsStr = if (args.isNotEmpty()) {
            args.joinToString(", ") { arg ->
                arg.type?.resolve()?.let { renderKotlinType(it) } ?: "*"
            }.let { "<$it>" }
        } else ""
        val nullable = if (type.isMarkedNullable) "?" else ""
        return "$baseName$argsStr$nullable"
    }

    private fun isPublicProperty(prop: KSPropertyDeclaration): Boolean {
        val modifiers = prop.modifiers
        // Skip private, protected — only public properties are exposed
        return Modifier.PRIVATE !in modifiers &&
            Modifier.PROTECTED !in modifiers
    }

    /**
     * Collects constructor parameter names that have default values.
     * These are optional in GraphQL input objects — clients may omit them.
     */
    private fun collectOptionalParams(cls: KSClassDeclaration): List<String> {
        val constructor = cls.primaryConstructor ?: return emptyList()
        return constructor.parameters
            .filter { it.hasDefault }
            .map { it.name?.asString() ?: "" }
            .filter { it.isNotEmpty() }
            .toList()
    }

    private fun buildFieldDescriptor(f: FieldInfo, classRef: String): String {
        val descArg = f.description?.let { "\"$it\"" } ?: "null"
        return """FieldDescriptor(name = "${f.graphQLName}", kProperty = $classRef::${f.name}, returnType = typeOf<${f.kotlinTypeString}>(), description = $descArg, isIgnored = ${f.isIgnored})"""
    }

    /**
     * Returns a Kotlin literal/expression that can serve as a default value for
     * the given type string (used in the Elvis operator for optional input
     * params). For collection types, returns the appropriate `emptyXxx()` call.
     * For unknown/custom types, falls back to a `TODO()` that will never execute
     * (the constructor's own default would be used if the value is missing).
     */
    private fun defaultLiteralForType(typeStr: String): String {
        return when {
            typeStr.endsWith("?") -> "null"
            typeStr == "kotlin.String" -> "\"\""
            typeStr == "kotlin.Int" -> "0"
            typeStr == "kotlin.Long" -> "0L"
            typeStr == "kotlin.Short" -> "0.toShort()"
            typeStr == "kotlin.Byte" -> "0.toByte()"
            typeStr == "kotlin.Boolean" -> "false"
            typeStr == "kotlin.Float" -> "0f"
            typeStr == "kotlin.Double" -> "0.0"
            typeStr == "kotlin.Char" -> "' '"
            typeStr.startsWith("kotlin.collections.List<") -> "emptyList()"
            typeStr.startsWith("kotlin.collections.Set<") -> "emptySet()"
            typeStr.startsWith("kotlin.collections.Map<") -> "emptyMap()"
            typeStr.startsWith("kotlin.Array<") -> "emptyArray()"
            else -> "TODO(\"missing optional field: \$typeStr\")"
        }
    }

    // =========================================================================
    // KSP utility extensions
    // =========================================================================

    /**
     * Returns the class reference relative to its package.
     * For top-level classes: "Peer"
     * For nested classes: "ChatItemContent.MessageImages"
     * This is used in generated code so that nested classes are properly qualified.
     */
    private fun KSClassDeclaration.classRef(): String {
        val fqn = qualifiedName!!.asString()
        val pkg = packageName.asString()
        return if (fqn.startsWith("$pkg.")) {
            fqn.removePrefix("$pkg.")
        } else {
            fqn
        }
    }

    private fun KSClassDeclaration.getAnnotation(fqn: String): KSAnnotation? =
        annotations.firstOrNull { it.shortName.asString() == fqn.substringAfterLast('.') &&
            it.annotationType.resolve().declaration.qualifiedName?.asString() == fqn }

    private fun KSAnnotation.getArgument(name: String): String? {
        val arg = arguments.firstOrNull { it.name?.asString() == name } ?: return null
        return when (val value = arg.value) {
            is String -> value
            is KSType -> value.declaration.simpleName.asString()
            else -> value?.toString()
        }
    }

    private fun KSPropertyDeclaration.isAnnotationPresent(fqn: String): Boolean =
        annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() == fqn }

    private fun KSPropertyDeclaration.findAnnotation(fqn: String): KSAnnotation? =
        annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == fqn }

    /**
     * Checks whether this class implements (directly or transitively) the
     * interface identified by [interfaceFqn]. Replaces runtime
     * `isKotlinSubclassOf` reflection for interface/possible-type discovery.
     */
    private fun KSClassDeclaration.implementsInterface(interfaceFqn: String): Boolean {
        return superTypes.any { supertypeRef ->
            val resolved = supertypeRef.resolve()
            val decl = resolved.declaration as? KSClassDeclaration ?: return@any false
            decl.qualifiedName?.asString() == interfaceFqn ||
                decl.implementsInterface(interfaceFqn) // recursive for transitive
        }
    }

    // =========================================================================
    // File writing
    // =========================================================================

    private fun writeGeneratedFile(
        packageName: String,
        fileName: String,
        content: String,
        sourceFile: KSFile?,
    ) {
        val deps = if (sourceFile != null) {
            Dependencies(aggregating = false, sourceFile)
        } else {
            Dependencies.ALL_FILES
        }
        val outputStream = codeGenerator.createNewFile(deps, packageName, fileName)
        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
            writer.write(content)
        }
    }
}
