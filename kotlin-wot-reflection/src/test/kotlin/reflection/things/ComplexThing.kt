package ai.ancf.lmos.wot.reflection.things

import ai.ancf.lmos.wot.reflection.annotations.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

@Thing(
    id = "complexThing",
    title = "Complex Thing",
    description = "A thing with complex properties, actions, and events."
)
@Links(
    values = [Link(
        href = "my/link",
        rel = "my-rel",
        type = "my/type",
        anchor = "my-anchor",
        sizes = "my-sizes",
        hreflang = ["my-lang-1", "my-lang-2"]
    )]
)
@VersionInfo(instance = "1.0.0")
class ComplexThing(@Property(readOnly = true) val constructorProperty: String = "Hello World") {

    private val statusChangedFlow = MutableSharedFlow<String>(replay = 1) // Replay last emitted value

    @Property(readOnly = true)
    val observableProperty : MutableStateFlow<String> = MutableStateFlow("Hello World")

    // A nested configuration represented as a read-only property
    @Property(description = "A nested configuration object", readOnly = true)
    val nestedConfig: NestedConfig = NestedConfig(
        name = "defaultValue",
        values = listOf(1, 2, 3)
    )

    @Property(readOnly = true)
    val exampleStringProperty: String = "Hello World"

    @Property(readOnly = true)
    val exampleIntProperty: Int = 42

    @Property(readOnly = true)
    val exampleBooleanProperty: Boolean = true

    @Property(readOnly = true)
    val exampleNumberProperty: Number = 3.14

    // New properties of specific types (Double, Float, and Long)
    @Property(readOnly = true)
    val exampleDoubleProperty: Double = 3.1415

    @Property(readOnly = true)
    val exampleFloatProperty: Float = 2.71f

    @Property(readOnly = true)
    val exampleLongProperty: Long = 10000000000L

    @Property(readOnly = true)
    val exampleIntArrayProperty: Array<Int> = arrayOf(1, 2, 3)

    @Property(readOnly = true)
    val exampleStringArrayProperty: Array<String> = arrayOf("apple", "banana", "cherry")

    @Property(readOnly = true)
    val exampleBooleanArrayProperty: Array<Boolean> = arrayOf(true, false, true)

    @Property(readOnly = true)
    val exampleNumberArrayProperty: Array<Number> = arrayOf(3.14, 42, 99.9)

    @Property(readOnly = true)
    val exampleListProperty: List<String> = listOf("item1", "item2", "item3")

    @Property(readOnly = true)
    val exampleSetProperty: Set<String> = setOf("itemA", "itemB", "itemC")

    @Property(readOnly = true)
    val exampleMapProperty: Map<String, Int> = mapOf("key1" to 1, "key2" to 2, "key3" to 3)

    @Action()
    fun processData(input1: Int, input2: String): ComplexResult {
        return ComplexResult("Processed: $input2", input1 * 2)
    }

    @Action()
    fun compute(params: ComputationParams): ComputationResult {
        return ComputationResult(params.a + params.b, params.a * params.b)
    }

    @Action()
    fun returnString(): String {
        return "Hello from action!"
    }

    @Action()
    fun returnInt(): Int {
        return 100
    }

    @Action()
    fun returnBoolean(): Boolean {
        return true
    }

    @Action()
    fun doNothing() {
        // This action returns nothing and does nothing
    }

    @Action()
    fun processInput(input: String) {
        // This action takes an input and does not return anything
        println("Processed input: $input")
    }

    @Action()
    fun sum(a: Int, b: Int): Int {
        return a + b
    }

    // Event example for dynamic updates (optional for the test, but a useful addition)
    @Event(description = "Fires when the status changes")
    fun statusChanged(): Flow<String> {
        return statusChangedFlow
    }

    // Event stream that emits computation results periodically
    @Event(description = "Stream of periodic computation results")
    fun computationUpdates(): Flow<ComputationResult> {
        return flow {
            while (true) {
                // Simulate computation with random values
                val randomA = Random.nextInt(1, 100)
                val randomB = Random.nextInt(1, 100)
                val result = ComputationResult(
                    sum = randomA + randomB,
                    product = randomA * randomB
                )
                emit(result) // Emit the result
                delay(1000L) // Wait 1 second before emitting the next event
            }
        }
    }
}

// Data classes for input and output structures
data class NestedConfig(val name: String, val values: List<Int>)
data class ComplexResult(val message: String, val result: Int)
data class ComputationParams(val a: Int, val b: Int)
data class ComputationResult(val sum: Int, val product: Int)