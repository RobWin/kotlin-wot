package ai.ancf.lmos.wot.reflection

import ai.ancf.lmos.wot.Servient
import ai.ancf.lmos.wot.Wot
import ai.ancf.lmos.wot.reflection.things.ComplexThing
import ai.ancf.lmos.wot.thing.schema.*
import kotlin.test.*

class ComplexThingTest {

    lateinit var servient: Servient
    lateinit var wot: Wot
    lateinit var complexThing: ComplexThing
    lateinit var exposedThing: WoTExposedThing
    lateinit var thingDescription: WoTThingDescription

    @BeforeTest
    fun setUp() {
        // Set up the servient and WoT instance
        servient = Servient()
        wot = Wot.create(servient)

        // Create an instance of ComplexThing
        complexThing = ComplexThing()

        // Generate ThingDescription from the class
        exposedThing = ExposedThingBuilder.createExposedThing(wot, complexThing, ComplexThing::class) as WoTExposedThing
        thingDescription = exposedThing.getThingDescription()
    }

    @Test
    fun `test ThingDescription creation for ComplexThing`() {
        // Validate Thing metadata
        assertEquals("complexThing", thingDescription.id, "ThingDescription ID should match the class ID")
        assertEquals("Complex Thing", thingDescription.title, "ThingDescription title should match")
        assertEquals("A thing with complex properties, actions, and events.", thingDescription.description, "ThingDescription description should match")
        assertEquals("1.0.0", thingDescription.version?.instance)
        assertEquals(listOf(Link("my/link", "my/type", "my-rel", "my-anchor", "my-sizes", listOf("my-lang-1", "my-lang-2"))), thingDescription.links)
    }

    @Test
    fun `test exampleStringProperty in ThingDescription`() {
        // Assertions for String Property
        assertTrue(thingDescription.properties.containsKey("exampleStringProperty"), "ThingDescription should contain 'exampleStringProperty' property")
        val exampleStringProperty = thingDescription.properties["exampleStringProperty"]
        assertNotNull(exampleStringProperty, "'exampleStringProperty' property should not be null")
        assertIs<StringProperty>(exampleStringProperty, "'exampleStringProperty' should be a StringProperty")
        assertEquals("Hello World", exampleStringProperty.const)
    }

    @Test
    fun `test constructorProperty in ThingDescription`() {
        // Assertions for String Property
        assertTrue(thingDescription.properties.containsKey("constructorProperty"), "ThingDescription should contain 'constructorProperty' property")
        val exampleStringProperty = thingDescription.properties["constructorProperty"]
        assertNotNull(exampleStringProperty, "'constructorProperty' property should not be null")
        assertIs<StringProperty>(exampleStringProperty, "'constructorProperty' should be a StringProperty")
        assertEquals("Hello World", exampleStringProperty.const)
    }

    @Test
    fun `test exampleIntProperty in ThingDescription`() {
        // Assertions for Int Property
        assertTrue(thingDescription.properties.containsKey("exampleIntProperty"), "ThingDescription should contain 'exampleIntProperty' property")
        val exampleIntProperty = thingDescription.properties["exampleIntProperty"]
        assertNotNull(exampleIntProperty, "'exampleIntProperty' property should not be null")
        assertIs<IntProperty>(exampleIntProperty, "'exampleIntProperty' should be an IntProperty")
        assertEquals(42, exampleIntProperty.const)
    }

    @Test
    fun `test exampleBooleanProperty in ThingDescription`() {
        // Assertions for Boolean Property
        assertTrue(thingDescription.properties.containsKey("exampleBooleanProperty"), "ThingDescription should contain 'exampleBooleanProperty' property")
        val exampleBooleanProperty = thingDescription.properties["exampleBooleanProperty"]
        assertNotNull(exampleBooleanProperty, "'exampleBooleanProperty' property should not be null")
        assertIs<BooleanProperty>(exampleBooleanProperty, "'exampleBooleanProperty' should be a BooleanProperty")
        assertEquals(true, exampleBooleanProperty.const)
    }

    @Test
    fun `test exampleNumberProperty in ThingDescription`() {
        // Assertions for Number Property
        assertTrue(thingDescription.properties.containsKey("exampleNumberProperty"), "ThingDescription should contain 'exampleNumberProperty' property")
        val exampleNumberProperty = thingDescription.properties["exampleNumberProperty"]
        assertNotNull(exampleNumberProperty, "'exampleNumberProperty' property should not be null")
        assertIs<NumberProperty>(exampleNumberProperty, "'exampleNumberProperty' should be a NumberProperty")
        assertEquals(3.14, exampleNumberProperty.const)
    }

    @Test
    fun `test exampleDoubleProperty in ThingDescription`() {
        // Assertions for Double Property
        assertTrue(thingDescription.properties.containsKey("exampleDoubleProperty"), "ThingDescription should contain 'exampleDoubleProperty' property")
        val exampleDoubleProperty = thingDescription.properties["exampleDoubleProperty"]
        assertNotNull(exampleDoubleProperty, "'exampleDoubleProperty' property should not be null")
        assertIs<NumberProperty>(exampleDoubleProperty, "'exampleDoubleProperty' should be a NumberProperty")
        assertEquals(3.1415, exampleDoubleProperty.const)
    }

    @Test
    fun `test exampleFloatProperty in ThingDescription`() {
        // Assertions for Float Property
        assertTrue(thingDescription.properties.containsKey("exampleFloatProperty"), "ThingDescription should contain 'exampleFloatProperty' property")
        val exampleFloatProperty = thingDescription.properties["exampleFloatProperty"]
        assertNotNull(exampleFloatProperty, "'exampleFloatProperty' property should not be null")
        assertIs<NumberProperty>(exampleFloatProperty, "'exampleFloatProperty' should be a NumberProperty")
        assertEquals(2.71f, exampleFloatProperty.const)

    }

    @Test
    fun `test exampleLongProperty in ThingDescription`() {
        // Assertions for Long Property
        assertTrue(thingDescription.properties.containsKey("exampleLongProperty"), "ThingDescription should contain 'exampleLongProperty' property")
        val exampleLongProperty = thingDescription.properties["exampleLongProperty"]
        assertNotNull(exampleLongProperty, "'exampleLongProperty' property should not be null")
        assertIs<NumberProperty>(exampleLongProperty, "'exampleLongProperty' should be a NumberProperty")
        assertEquals(10000000000L, exampleLongProperty.const)
    }

    @Test
    fun `test exampleArrayProperty in ThingDescription`() {
        // Assertions for Array Property
        assertTrue(thingDescription.properties.containsKey("exampleIntArrayProperty"), "ThingDescription should contain 'exampleIntArrayProperty' property")
        val exampleArrayProperty = thingDescription.properties["exampleIntArrayProperty"]
        assertNotNull(exampleArrayProperty, "'exampleIntArrayProperty' property should not be null")
        assertIs<ArrayProperty<Int>>(exampleArrayProperty, "'exampleIntArrayProperty' should be an ArrayProperty")
    }
    @Test
    fun `test exampleStringArrayProperty in ThingDescription`() {
        assertTrue(thingDescription.properties.containsKey("exampleStringArrayProperty"), "ThingDescription should contain 'exampleStringArrayProperty' property")
        val exampleStringArrayProperty = thingDescription.properties["exampleStringArrayProperty"]
        assertNotNull(exampleStringArrayProperty, "'exampleStringArrayProperty' property should not be null")
        assertIs<ArrayProperty<String>>(exampleStringArrayProperty, "'exampleStringArrayProperty' should be an ArrayProperty<String>")
    }

    @Test
    fun `test exampleBooleanArrayProperty in ThingDescription`() {
        // Assertions for Array<Boolean> Property
        assertTrue(thingDescription.properties.containsKey("exampleBooleanArrayProperty"), "ThingDescription should contain 'exampleBooleanArrayProperty' property")
        val exampleBooleanArrayProperty = thingDescription.properties["exampleBooleanArrayProperty"]
        assertNotNull(exampleBooleanArrayProperty, "'exampleBooleanArrayProperty' property should not be null")
        assertIs<ArrayProperty<Boolean>>(exampleBooleanArrayProperty, "'exampleBooleanArrayProperty' should be an ArrayProperty<Boolean>")
    }

    @Test
    fun `test exampleNumberArrayProperty in ThingDescription`() {
        // Assertions for Array<Number> Property
        assertTrue(thingDescription.properties.containsKey("exampleNumberArrayProperty"), "ThingDescription should contain 'exampleNumberArrayProperty' property")
        val exampleNumberArrayProperty = thingDescription.properties["exampleNumberArrayProperty"]
        assertNotNull(exampleNumberArrayProperty, "'exampleNumberArrayProperty' property should not be null")
        assertIs<ArrayProperty<Number>>(exampleNumberArrayProperty, "'exampleNumberArrayProperty' should be an ArrayProperty<Number>")
    }

    @Test
    fun `test observableProperty in ThingDescription`() {
        // Assertions for Array<Number> Property
        assertTrue(thingDescription.properties.containsKey("observableProperty"), "ThingDescription should contain 'observableProperty' property")
        val observableProperty = thingDescription.properties["observableProperty"]
        assertNotNull(observableProperty, "'observableProperty' property should not be null")
        assertIs<StringSchema>(observableProperty, "'observableProperty' should be an StringSchema")
        assertEquals(true, observableProperty.observable)
    }

    @Test
    fun `test complex properties in ThingDescription for ComplexThing`() {
        // Assertions for properties
        assertTrue(thingDescription.properties.containsKey("nestedConfig"), "ThingDescription should contain 'nestedConfig' property")
        val nestedConfigProperty = thingDescription.properties["nestedConfig"]
        assertNotNull(nestedConfigProperty, "'nestedConfig' property should not be null")
        assertIs<ObjectProperty>(nestedConfigProperty, "'nestedConfig' should be a ObjectProperty")
        assertEquals(2, nestedConfigProperty.properties.size, "'nestedConfig' should contain 2 properties")
        assertIs<StringSchema>(nestedConfigProperty.properties["name"])
        assertIs<ArraySchema<*>>(nestedConfigProperty.properties["values"],"'values' should be an ArraySchema<Int>")
        val array = nestedConfigProperty.properties["values"] as ArraySchema<*>
        assertIs<IntegerSchema>(array.items, "'value items' should be an ArraySchema<Int>")
    }

    @Test
    fun `test exampleListProperty in ThingDescription`() {
        // Assertions for List Property
        assertTrue(thingDescription.properties.containsKey("exampleListProperty"), "ThingDescription should contain 'exampleListProperty' property")
        val exampleListProperty = thingDescription.properties["exampleListProperty"]
        assertNotNull(exampleListProperty, "'exampleListProperty' property should not be null")
        assertIs<ArrayProperty<*>>(exampleListProperty, "'exampleListProperty' should be a ArrayProperty")
    }

    @Test
    fun `test exampleSetProperty in ThingDescription`() {
        // Assertions for Set Property
        assertTrue(thingDescription.properties.containsKey("exampleSetProperty"), "ThingDescription should contain 'exampleSetProperty' property")
        val exampleSetProperty = thingDescription.properties["exampleSetProperty"]
        assertNotNull(exampleSetProperty, "'exampleSetProperty' property should not be null")
        assertIs<ArrayProperty<*>>(exampleSetProperty, "'exampleSetProperty' should be a ArrayProperty")
    }

    @Test
    fun `test exampleMapProperty in ThingDescription`() {
        // Assertions for Map Property
        assertTrue(thingDescription.properties.containsKey("exampleMapProperty"), "ThingDescription should contain 'exampleMapProperty' property")
        val exampleMapProperty = thingDescription.properties["exampleMapProperty"]
        assertNotNull(exampleMapProperty, "'exampleMapProperty' property should not be null")
        assertIs<ObjectProperty>(exampleMapProperty, "'exampleMapProperty' should be a ObjectProperty")
        assertEquals(2, exampleMapProperty.properties.size, "'exampleMapProperty' should contain 2 properties")
    }

    @Test
    fun `test processData action in ThingDescription`() {
        // Assertions for processData action
        assertTrue(thingDescription.actions.containsKey("processData"), "ThingDescription should contain 'processData' action")
        val processDataAction = thingDescription.actions["processData"]
        assertNotNull(processDataAction, "'processData' action should not be null")
        assertIs<ActionAffordance<*, *>>(processDataAction, "'processData' should be an ActionAffordance")
        assertIs<ObjectSchema>(processDataAction.input, "'processData' action input should be an ObjectSchema")
        assertIs<ObjectSchema>(processDataAction.output, "'processData' action output should be an ObjectSchema")
    }

    @Test
    fun `test compute action in ThingDescription`() {
        // Assertions for compute action
        assertTrue(thingDescription.actions.containsKey("compute"), "ThingDescription should contain 'compute' action")
        val computeAction = thingDescription.actions["compute"]
        assertNotNull(computeAction, "'compute' action should not be null")
        assertIs<ActionAffordance<*, *>>(computeAction, "'compute' should be an ActionAffordance")
        assertIs<ObjectSchema>(computeAction.input, "'compute' action input should be an ObjectSchema")
        assertIs<ObjectSchema>(computeAction.output, "'compute' action output should be an ObjectSchema")
    }

    @Test
    fun `test returnString action in ThingDescription`() {
        // Assertions for returnString action (No input, String output)
        assertTrue(thingDescription.actions.containsKey("returnString"), "ThingDescription should contain 'returnString' action")
        val returnStringAction = thingDescription.actions["returnString"]
        assertNotNull(returnStringAction, "'returnString' action should not be null")
        assertIs<ActionAffordance<*, *>>(returnStringAction, "'returnString' should be an ActionAffordance")
        assertIs<NullSchema>(returnStringAction.input, "'returnString' action input should be a NullSchema")
        assertIs<StringSchema>(returnStringAction.output, "'returnString' action output should be a StringSchema")
    }

    @Test
    fun `test returnInt action in ThingDescription`() {
        // Assertions for returnInt action (No input, Int output)
        assertTrue(thingDescription.actions.containsKey("returnInt"), "ThingDescription should contain 'returnInt' action")
        val returnIntAction = thingDescription.actions["returnInt"]
        assertNotNull(returnIntAction, "'returnInt' action should not be null")
        assertIs<ActionAffordance<*, *>>(returnIntAction, "'returnInt' should be an ActionAffordance")
        assertIs<NullSchema>(returnIntAction.input, "'returnInt' action input should be a NullSchema")
        assertIs<IntegerSchema>(returnIntAction.output, "'returnInt' action output should be a NumberSchema")
    }

    @Test
    fun `test returnBoolean action in ThingDescription`() {
        // Assertions for returnBoolean action (No input, Boolean output)
        assertTrue(thingDescription.actions.containsKey("returnBoolean"), "ThingDescription should contain 'returnBoolean' action")
        val returnBooleanAction = thingDescription.actions["returnBoolean"]
        assertNotNull(returnBooleanAction, "'returnBoolean' action should not be null")
        assertIs<ActionAffordance<*, *>>(returnBooleanAction, "'returnBoolean' should be an ActionAffordance")
        assertIs<NullSchema>(returnBooleanAction.input, "'returnBoolean' action input should be a NullSchema")
        assertIs<BooleanSchema>(returnBooleanAction.output, "'returnBoolean' action output should be a BooleanSchema")
    }

    @Test
    fun `test doNothing action in ThingDescription`() {
        // Assertions for doNothing action (No input, No output)
        assertTrue(thingDescription.actions.containsKey("doNothing"), "ThingDescription should contain 'doNothing' action")
        val doNothingAction = thingDescription.actions["doNothing"]
        assertNotNull(doNothingAction, "'doNothing' action should not be null")
        assertIs<ActionAffordance<*, *>>(doNothingAction, "'doNothing' should be an ActionAffordance")
        assertIs<NullSchema>(doNothingAction.input, "'doNothing' action input should be a NullSchema")
        assertIs<NullSchema>(doNothingAction.output, "'doNothing' action output should be a NullSchema")
    }

    @Test
    fun `test processInput action in ThingDescription`() {
        // Assertions for processInput action (Object input, No output)
        assertTrue(thingDescription.actions.containsKey("processInput"), "ThingDescription should contain 'processInput' action")
        val processInputAction = thingDescription.actions["processInput"]
        assertNotNull(processInputAction, "'processInput' action should not be null")
        assertIs<ActionAffordance<*, *>>(processInputAction, "'processInput' should be an ActionAffordance")
        assertIs<StringSchema>(processInputAction.input, "'processInput' action input should be an StringSchema")
        assertIs<NullSchema>(processInputAction.output, "'processInput' action output should be a NullSchema")
    }

    @Test
    fun `test sum action in ThingDescription`() {
        // Assertions for sum action (Int input, Int output)
        assertTrue(thingDescription.actions.containsKey("sum"), "ThingDescription should contain 'sum' action")
        val sumAction = thingDescription.actions["sum"]
        assertNotNull(sumAction, "'sum' action should not be null")
        assertIs<ActionAffordance<*, *>>(sumAction, "'sum' should be an ActionAffordance")
        assertIs<ObjectSchema>(sumAction.input, "'sum' action input should be a ObjectSchema")
        assertIs<IntegerSchema>(sumAction.output, "'sum' action output should be a NumberSchema")
    }

    @Test
    fun `test complex event in ThingDescription for ComplexThing`() {

        // Assertions for events
        assertTrue(thingDescription.events.containsKey("computationUpdates"), "ThingDescription should contain 'computationUpdates' event")
        val computationUpdatesEvent = thingDescription.events["computationUpdates"]
        assertNotNull(computationUpdatesEvent, "'computationUpdates' event should not be null")
        assertIs<ObjectSchema>(computationUpdatesEvent.data)
    }

    @Test
    fun `test simple event in ThingDescription for ComplexThing`() {

        // Assertions for events
        assertTrue(thingDescription.events.containsKey("statusChanged"), "ThingDescription should contain 'statusChanged' event")
        val computationUpdatesEvent = thingDescription.events["statusChanged"]
        assertNotNull(computationUpdatesEvent, "'statusChanged' event should not be null")
        assertIs<StringSchema>(computationUpdatesEvent.data)
    }
}