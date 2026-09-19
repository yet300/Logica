package ge.yet.game.miniapp.audio

import ge.yet.game.pattern.Pattern
import ge.yet.game.pattern.sequence

fun humanizedNotes(
    notes: List<MidiNote>,
    velocity: ClosedFloatingPointRange<Float>,
    seed: Long,
): Pattern<AudioNote> {
    require(notes.isNotEmpty()) { "A humanized phrase requires at least one note" }
    require(
        velocity.start.isFinite() &&
            velocity.endInclusive.isFinite() &&
            velocity.start in 0f..1f &&
            velocity.endInclusive in 0f..1f &&
            velocity.start <= velocity.endInclusive,
    ) { "A velocity range must be finite, ordered, and inside 0..1" }
    val width = velocity.endInclusive - velocity.start
    val phrase = List<AudioNote>(notes.size) { index ->
        val unit = deterministicUnit(seed, index)
        AudioNote.Pitched(notes[index], velocity.start + width * unit)
    }
    return sequence(phrase)
}

private fun deterministicUnit(seed: Long, index: Int): Float {
    val bits = mix64(seed.toULong() xor index.toULong()) shr 40
    return bits.toFloat() / (1uL shl 24).toFloat()
}

private fun mix64(input: ULong): ULong {
    var value = input + 0x9E3779B97F4A7C15uL
    value = (value xor (value shr 30)) * 0xBF58476D1CE4E5B9uL
    value = (value xor (value shr 27)) * 0x94D049BB133111EBuL
    return value xor (value shr 31)
}
