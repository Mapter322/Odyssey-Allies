architectury {
    val enabledPlatforms: String by rootProject
    common(enabledPlatforms.split(","))
}

dependencies {
    val placeholderApiVersion: String by project
    val xaeroMinimapVersion: String by project
    val xaeroWorldMapVersion: String by project

    modCompileOnly(group = "tech.thatgravyboat", name = "commonats", version = "2.0")
    modImplementation(group = "eu.pb4", name = "placeholder-api", version = placeholderApiVersion)

    compileOnly(group = "xaero.minimap", name = "xaerominimap-common-1.21.1", version = xaeroMinimapVersion)
    compileOnly(group = "xaero.map", name = "xaeroworldmap-common-1.21.1", version = xaeroWorldMapVersion)
}
