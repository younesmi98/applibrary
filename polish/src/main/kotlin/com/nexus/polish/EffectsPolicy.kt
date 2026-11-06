package com.nexus.polish

enum class EffectLevel { HIGH, BALANCED, LOW }
data class DeviceInfo(val ramMb: Int, val batterySaver: Boolean, val systemReducedMotion: Boolean, val userChoice: EffectLevel? = null, val userReducedMotion: Boolean = false)
data class Effects(val level: EffectLevel, val blur: Boolean, val animations: Boolean, val artworkMaxPx: Int)

/** Scenario H: weak phones drop expensive effects by themselves. The user's choice may lower effects freely, but cannot force blur on while Battery Saver is active. */
object EffectsPolicy {
    fun resolve(d: DeviceInfo): Effects {
        val auto = when {
            d.batterySaver || d.ramMb < 3_000 -> EffectLevel.LOW
            d.ramMb < 6_000 -> EffectLevel.BALANCED
            else -> EffectLevel.HIGH
        }
        val level = when {
            d.userChoice == null -> auto
            d.batterySaver -> EffectLevel.LOW
            else -> d.userChoice
        }
        return Effects(level, blur = level == EffectLevel.HIGH, animations = !(d.systemReducedMotion || d.userReducedMotion) && level != EffectLevel.LOW,
            artworkMaxPx = when (level) { EffectLevel.HIGH -> 1080; EffectLevel.BALANCED -> 720; EffectLevel.LOW -> 480 })
    }
}
