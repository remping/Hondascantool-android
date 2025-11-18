package id.remping.hondascantool

data class EngineData(
    val rpm: Int = 0,
    val tps1: Float = 0f,
    val tps2: Float = 0f,
    val ect1: Float = 0f,
    val ect2: Int = 0,
    val iat1: Float = 0f,
    val iat2: Int = 0,
    val map1: Float = 0f,
    val map2: Int = 0,
    val battery: Float = 0f,
    val speed: Int = 0,
    val inj: Float = 0f,
    val igt: Float = 0f
)
