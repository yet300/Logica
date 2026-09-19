package ge.yet.game.miniapp.audio

enum class ScaleMode {
    MAJOR,
    NATURAL_MINOR,
    DORIAN,
    MIXOLYDIAN,
    MAJOR_PENTATONIC,
    MINOR_PENTATONIC,
}

fun tonalScale(root: MidiNote, mode: ScaleMode): TonalScale = TonalScale(
    root = root,
    intervals = when (mode) {
        ScaleMode.MAJOR -> intArrayOf(0, 2, 4, 5, 7, 9, 11)
        ScaleMode.NATURAL_MINOR -> intArrayOf(0, 2, 3, 5, 7, 8, 10)
        ScaleMode.DORIAN -> intArrayOf(0, 2, 3, 5, 7, 9, 10)
        ScaleMode.MIXOLYDIAN -> intArrayOf(0, 2, 4, 5, 7, 9, 10)
        ScaleMode.MAJOR_PENTATONIC -> intArrayOf(0, 2, 4, 7, 9)
        ScaleMode.MINOR_PENTATONIC -> intArrayOf(0, 3, 5, 7, 10)
    },
)

class TonalScale internal constructor(
    private val root: MidiNote,
    intervals: IntArray,
) {
    private val intervals = intervals.copyOf()

    fun midi(degree: Int, octave: Int = 0): MidiNote {
        val size = intervals.size.toLong()
        val degreeLong = degree.toLong()
        val scaleOctave = floorDivide(degreeLong, size)
        val intervalIndex = floorMod(degreeLong, size).toInt()
        val value = root.value.toLong() +
            intervals[intervalIndex] +
            (scaleOctave + octave.toLong()) * SEMITONES_PER_OCTAVE
        require(value in MIDI_MIN..MIDI_MAX) { "Scale degree resolves outside MIDI 0..127" }
        return MidiNote.of(value.toInt())
    }

    fun note(degree: Int, octave: Int = 0, velocity: Float = 1f): AudioNote.Pitched =
        AudioNote.Pitched(midi(degree, octave), velocity)
}

private fun floorDivide(value: Long, positiveDivisor: Long): Long {
    val quotient = value / positiveDivisor
    return if (value % positiveDivisor < 0L) quotient - 1L else quotient
}

private fun floorMod(value: Long, positiveDivisor: Long): Long {
    val remainder = value % positiveDivisor
    return if (remainder < 0L) remainder + positiveDivisor else remainder
}

private const val SEMITONES_PER_OCTAVE = 12L
private const val MIDI_MIN = 0L
private const val MIDI_MAX = 127L
