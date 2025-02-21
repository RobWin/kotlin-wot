package ai.ancf.lmos.wot.reflection.things

import ai.ancf.lmos.wot.reflection.annotations.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

@Thing(
    id = "simpleThing",
    title = "Simple Thing",
    description = "A thing with complex properties, actions, and events."
)
@Link(
    href = "my/link",
    rel = "my-rel",
    type = "my/type",
    anchor = "my-anchor",
    sizes = "my-sizes",
    hreflang = ["my-lang-1", "my-lang-2"]
)
@VersionInfo(instance = "1.0.0")
class SimpleThing {

    var counter = 0

    @Property(title = "Observable Property", readOnly = true)
    val observableProperty : MutableStateFlow<String> = MutableStateFlow("Hello World")

    @Property()
    var mutableProperty: String = "test"

    @Property(readOnly = true)
    val readyOnlyProperty: String = "test"

    @Property(writeOnly = true)
    var writeOnlyProperty: String = "test"

    @Action()
    fun voidAction() {
        println("Action executed")
        counter += 1
    }

    @Action()
    fun changeObservableProperty(){
        observableProperty.value = "Hello from action!"
    }

    @Action()
    fun outputAction() : String {
        return "test"
    }

    @Action()
    fun inputAction(input : String) {
        println("Action executed")
        counter += 1
    }

    @Action()
    fun inOutAction(input : String) : String {
        println("Action executed")
        return "$input output"
    }

    @Event()
    fun statusUpdated(): Flow<String> {
        return flow {
            emit("Status updated")
        }
    }
}