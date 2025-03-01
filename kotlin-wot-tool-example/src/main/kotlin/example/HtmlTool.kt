package ai.ancf.lmos.wot.example


import ai.ancf.lmos.wot.protocol.LMOSContext
import ai.ancf.lmos.wot.protocol.LMOSThingType
import ai.ancf.lmos.wot.reflection.annotations.Action
import ai.ancf.lmos.wot.reflection.annotations.Context
import ai.ancf.lmos.wot.reflection.annotations.Thing
import ai.ancf.lmos.wot.reflection.annotations.VersionInfo
import org.jsoup.Jsoup
import org.springframework.stereotype.Component


@Thing(id= "scraper", title="Tool",
    description= "An HTML scraper.", type = LMOSThingType.TOOL)
@VersionInfo(instance = "1.0.0")
@Context(prefix = LMOSContext.prefix, url = LMOSContext.url)
@Component
class HtmlTool() {

    @Action(title = "Fetch Content", description = "Fetches the content from the specified URL.")
    suspend fun fetchContent(url: String): String {
        return try {
            val document = Jsoup.connect(url).get()
            document.outerHtml()
        } catch (e: Exception) {
            "Error fetching content"
        }
    }
}

