package com.egoal.darkestpixeldungeon.items.unclassified

import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.Actor
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.effects.CellEmitter
import com.egoal.darkestpixeldungeon.effects.DashAfterImage
import com.egoal.darkestpixeldungeon.effects.EarthStompEffect
import com.egoal.darkestpixeldungeon.effects.FrostOrbEffect
import com.egoal.darkestpixeldungeon.effects.ShockBurstEffect
import com.egoal.darkestpixeldungeon.effects.UndeadEmergeEffect
import com.egoal.darkestpixeldungeon.effects.particles.BlastParticle
import com.egoal.darkestpixeldungeon.effects.particles.BloodParticle
import com.egoal.darkestpixeldungeon.effects.particles.ColdSnowParticles
import com.egoal.darkestpixeldungeon.effects.particles.EarthParticle
import com.egoal.darkestpixeldungeon.effects.particles.ElectronParticle
import com.egoal.darkestpixeldungeon.effects.particles.ElmoParticle
import com.egoal.darkestpixeldungeon.effects.particles.EnergyParticle
import com.egoal.darkestpixeldungeon.effects.particles.FireSmokeParticle
import com.egoal.darkestpixeldungeon.effects.particles.FireSparkParticle
import com.egoal.darkestpixeldungeon.effects.particles.FireSprayParticle
import com.egoal.darkestpixeldungeon.effects.particles.FlameParticle
import com.egoal.darkestpixeldungeon.effects.particles.FlowParticle
import com.egoal.darkestpixeldungeon.effects.particles.LeafParticle
import com.egoal.darkestpixeldungeon.effects.particles.PoisonParticle
import com.egoal.darkestpixeldungeon.effects.particles.PurpleParticle
import com.egoal.darkestpixeldungeon.effects.particles.RainbowParticle
import com.egoal.darkestpixeldungeon.effects.particles.SacrificialParticle
import com.egoal.darkestpixeldungeon.effects.particles.ShadowParticle
import com.egoal.darkestpixeldungeon.effects.particles.ShaftParticle
import com.egoal.darkestpixeldungeon.effects.particles.SmokeParticle
import com.egoal.darkestpixeldungeon.effects.particles.SnowParticle
import com.egoal.darkestpixeldungeon.effects.particles.SoulFlameParticle
import com.egoal.darkestpixeldungeon.effects.particles.SparkParticle
import com.egoal.darkestpixeldungeon.effects.particles.WebParticle
import com.egoal.darkestpixeldungeon.effects.particles.WindParticle
import com.egoal.darkestpixeldungeon.effects.particles.WoolParticle
import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.scenes.CellSelector
import com.egoal.darkestpixeldungeon.scenes.GameScene
import com.egoal.darkestpixeldungeon.sprites.ItemSprite
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet
import com.watabou.noosa.particles.Emitter
import com.watabou.utils.Bundle

class ParticleTesterItem @JvmOverloads constructor(
    private var presetId: String = PRESETS.first().id
) : Item() {

    init {
        image = ItemSpriteSheet.DUST
        defaultAction = AC_PLAY
        usesTargeting = true
        unique = true
        stackable = false
        identify()
    }

    private val preset: Preset
        get() = PRESET_MAP[presetId] ?: PRESETS.first()

    override fun name(): String = preset.title

    override fun desc(): String = M.L(this, "desc", preset.title)

    override fun glowing(): ItemSprite.Glowing = ItemSprite.Glowing(preset.color, 0.8f)

    override fun actions(hero: Hero): ArrayList<String> = super.actions(hero).apply {
        add(AC_PLAY)
    }

    override fun execute(hero: Hero, action: String) {
        super.execute(hero, action)
        if (action == AC_PLAY) {
            curUser = hero
            GameScene.selectCell(object : CellSelector.Listener {
                override fun onSelect(cell: Int?) {
                    if (cell != null) {
                        preset.playAt(cell)
                    }
                }

                override fun prompt(): String = M.L(ParticleTesterItem::class.java, "prompt")
            })
        }
    }

    override fun price(): Int = 0

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(KEY_PRESET, presetId)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        val id = bundle.getString(KEY_PRESET)
        if (id.isNotEmpty() && PRESET_MAP.containsKey(id)) {
            presetId = id
        }
    }

    data class Preset(
        val id: String,
        val title: String,
        val color: Int,
        val playAt: (Int) -> Unit
    )

    companion object {
        const val AC_PLAY = "PLAY"
        private const val KEY_PRESET = "preset_id"

        private fun burst(id: String, title: String, color: Int, factory: Emitter.Factory, count: Int = 12): Preset =
            Preset(id, title, color) { cell ->
                CellEmitter.get(cell).burst(factory, count)
            }

        val PRESETS: List<Preset> = listOf(
            burst("flame", "FlameParticle", 0xFF7722, FlameParticle.FACTORY, 14),
            burst("soul_flame", "SoulFlameParticle", 0x44CCFF, SoulFlameParticle.FACTORY, 14),
            burst("fire_spray", "FireSprayParticle", 0xFFAA33, FireSprayParticle.FACTORY, 14),
            burst("fire_spark", "FireSparkParticle", 0xFFDD44, FireSparkParticle.FACTORY, 16),
            burst("fire_smoke", "FireSmokeParticle", 0x555555, FireSmokeParticle.FACTORY, 12),
            burst("blast", "BlastParticle", 0xFF5500, BlastParticle.FACTORY, 16),
            burst("spark", "SparkParticle", 0xFFFF66, SparkParticle.FACTORY, 16),
            burst("electron", "ElectronParticle", 0x66EEFF, ElectronParticle.FACTORY, 16),
            burst("energy", "EnergyParticle", 0x55BBFF, EnergyParticle.FACTORY, 16),
            burst("elmo", "ElmoParticle", 0x44FF66, ElmoParticle.FACTORY, 14),
            burst("purple", "PurpleParticle", 0xCC44FF, PurpleParticle.BURST, 16),
            burst("rainbow", "RainbowParticle", 0xFF88CC, RainbowParticle.BURST, 16),
            burst("shadow_curse", "ShadowParticle.CURSE", 0x551188, ShadowParticle.CURSE, 14),
            burst("shadow_up", "ShadowParticle.UP", 0x330055, ShadowParticle.UP, 14),
            burst("smoke", "SmokeParticle", 0x888888, SmokeParticle.FACTORY, 12),
            burst("blood", "BloodParticle", 0xCC1111, BloodParticle.BURST, 14),
            burst("poison", "PoisonParticle", 0x44CC22, PoisonParticle.SPLASH, 14),
            burst("cold_snow", "ColdSnowParticles", 0xAADDFF, ColdSnowParticles.FACTORY, 14),
            burst("snow", "SnowParticle", 0xFFFFFF, SnowParticle.FACTORY, 14),
            burst("earth", "EarthParticle", 0x886644, EarthParticle.FACTORY, 14),
            burst("leaf", "LeafParticle", 0x44AA33, LeafParticle.GENERAL, 14),
            burst("flow", "FlowParticle", 0x3388FF, FlowParticle.FACTORY, 14),
            burst("shaft", "ShaftParticle", 0xFFEEAA, ShaftParticle.FACTORY, 12),
            burst("sacrificial", "SacrificialParticle", 0xFF2244, SacrificialParticle.FACTORY, 14),
            burst("web", "WebParticle", 0xDDDDDD, WebParticle.FACTORY, 10),
            burst("wind", "WindParticle", 0xBBDDFF, WindParticle.FACTORY, 12),
            burst("wool", "WoolParticle", 0xFFF5DD, WoolParticle.FACTORY, 12),
            Preset("undead_emerge", "UndeadEmergeEffect", 0x9933FF) { cell ->
                val ch = Actor.findChar(cell)
                if (ch != null) {
                    UndeadEmergeEffect.emerge(ch)
                } else {
                    UndeadEmergeEffect.emergeAt(cell)
                }
            },
            Preset("frost_orb_hover", "FrostOrb (Gelid)", 0x55CCFF) { cell ->
                FrostOrbEffect.spawnOrb(cell)
            },
            Preset("frost_orb_launch", "FrostOrb.launch", 0x2288FF) { cell ->
                FrostOrbEffect.launch(Dungeon.hero.pos, cell)
            },
            Preset("dash_afterimage", "DashAfterImage", 0x66EEFF) { cell ->
                DashAfterImage.trail(Dungeon.hero, Dungeon.hero.pos, cell)
            },
            Preset("earth_stomp", "EarthStompEffect", 0xCC8833) { cell ->
                EarthStompEffect.play(cell)
            },
            Preset("shock_gold", "ShockBurst (Gold)", 0xFFD24A) { cell ->
                ShockBurstEffect.play(cell, 0xFFD24A)
            },
            Preset("shock_crimson", "ShockBurst (Red)", 0xFF3322) { cell ->
                ShockBurstEffect.play(cell, 0xFF3322)
            }
        )

        private val PRESET_MAP: Map<String, Preset> = PRESETS.associateBy { it.id }
    }
}
