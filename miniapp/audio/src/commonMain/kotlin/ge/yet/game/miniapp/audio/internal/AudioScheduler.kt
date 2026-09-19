package ge.yet.game.miniapp.audio.internal

import ge.yet.game.miniapp.audio.AudioNote
import ge.yet.game.miniapp.audio.AudioMobileBudget
import ge.yet.game.miniapp.audio.CompiledAudioProgram
import ge.yet.game.miniapp.audio.MidiNote
import ge.yet.game.pattern.CycleTime
import ge.yet.game.pattern.PatternEventBuffer
import ge.yet.game.pattern.Pattern
import ge.yet.game.pattern.PatternQueryBudget
import ge.yet.game.pattern.TimeArc
import kotlin.math.roundToLong

internal data class ScheduledAudioEvent(
    val trackIndex: Int,
    val note: MidiNote,
    val absoluteStartFrame: Long,
    val frameOffset: Int,
    val durationFrames: Long,
    val velocity: Float,
    internal val orderInTrack: Int,
)

/** Fixed-capacity primitive storage reused by the realtime scheduler. */
internal class ScheduledAudioEventBuffer(
    private val capacity: Int = AudioMobileBudget.MAX_TRACKS * PatternQueryBudget.DEFAULT_MAX_EVENTS,
) : AbstractList<ScheduledAudioEvent>() {
    private val trackIndices = IntArray(capacity)
    private val notes = IntArray(capacity)
    private val absoluteStartFrames = LongArray(capacity)
    private val frameOffsets = IntArray(capacity)
    private val durationFrames = LongArray(capacity)
    private val velocities = FloatArray(capacity)
    private val ordersInTrack = IntArray(capacity)

    override var size: Int = 0
        private set

    override fun get(index: Int): ScheduledAudioEvent {
        require(index in 0 until size)
        return ScheduledAudioEvent(
            trackIndex = trackIndices[index],
            note = MidiNote.of(notes[index]),
            absoluteStartFrame = absoluteStartFrames[index],
            frameOffset = frameOffsets[index],
            durationFrames = durationFrames[index],
            velocity = velocities[index],
            orderInTrack = ordersInTrack[index],
        )
    }

    fun clear() {
        size = 0
    }

    fun add(
        trackIndex: Int,
        note: MidiNote,
        absoluteStartFrame: Long,
        frameOffset: Int,
        durationFrames: Long,
        velocity: Float,
        orderInTrack: Int,
    ): Boolean {
        if (size == capacity) return false
        trackIndices[size] = trackIndex
        notes[size] = note.value
        absoluteStartFrames[size] = absoluteStartFrame
        frameOffsets[size] = frameOffset
        this.durationFrames[size] = durationFrames
        velocities[size] = velocity
        ordersInTrack[size] = orderInTrack
        size += 1
        return true
    }

    fun sort() {
        for (index in 1 until size) {
            val trackIndex = trackIndices[index]
            val note = notes[index]
            val startFrame = absoluteStartFrames[index]
            val frameOffset = frameOffsets[index]
            val duration = durationFrames[index]
            val velocity = velocities[index]
            val order = ordersInTrack[index]
            var insertion = index
            while (insertion > 0 && comesBefore(startFrame, trackIndex, order, insertion - 1)) {
                copy(from = insertion - 1, to = insertion)
                insertion -= 1
            }
            trackIndices[insertion] = trackIndex
            notes[insertion] = note
            absoluteStartFrames[insertion] = startFrame
            frameOffsets[insertion] = frameOffset
            durationFrames[insertion] = duration
            velocities[insertion] = velocity
            ordersInTrack[insertion] = order
        }
    }

    fun trackIndexAt(index: Int): Int = trackIndices[index]
    fun noteAt(index: Int): MidiNote = MidiNote.of(notes[index])
    fun frameOffsetAt(index: Int): Int = frameOffsets[index]
    fun durationFramesAt(index: Int): Long = durationFrames[index]
    fun velocityAt(index: Int): Float = velocities[index]

    private fun comesBefore(startFrame: Long, trackIndex: Int, order: Int, other: Int): Boolean =
        startFrame < absoluteStartFrames[other] ||
            (startFrame == absoluteStartFrames[other] && trackIndex < trackIndices[other]) ||
            (startFrame == absoluteStartFrames[other] && trackIndex == trackIndices[other] && order < ordersInTrack[other])

    private fun copy(from: Int, to: Int) {
        trackIndices[to] = trackIndices[from]
        notes[to] = notes[from]
        absoluteStartFrames[to] = absoluteStartFrames[from]
        frameOffsets[to] = frameOffsets[from]
        durationFrames[to] = durationFrames[from]
        velocities[to] = velocities[from]
        ordersInTrack[to] = ordersInTrack[from]
    }
}

internal class AudioScheduler(
    private val sampleRate: Int,
) {
    private lateinit var program: CompiledAudioProgram
    private var tempoNumerator = 1L
    private var tempoDenominator = 1L
    private val scheduled = ScheduledAudioEventBuffer()
    private val patternEvents = PatternEventBuffer<AudioNote>()
    private val patternBudget = PatternQueryBudget()

    internal val patternEventBufferAllocationCount: Int get() = 1
    internal val patternQueryBudgetAllocationCount: Int get() = 1
    internal val patternListFallbackCount: Int get() = patternEvents.fallbackQueriesUsed

    constructor(program: CompiledAudioProgram, sampleRate: Int) : this(sampleRate) {
        reset(program)
    }

    init {
        require(sampleRate in 8_000..192_000)
    }

    fun reset(program: CompiledAudioProgram) {
        this.program = program
        val scaledTempo = (program.tempo.bpm * TEMPO_PRECISION).roundToLong()
        val divisor = greatestCommonDivisor(scaledTempo, TEMPO_PRECISION)
        tempoNumerator = scaledTempo / divisor
        tempoDenominator = TEMPO_PRECISION / divisor
        scheduled.clear()
        patternEvents.clear()
        patternBudget.reset()
    }

    fun scheduleBlock(startFrame: Long, frameCount: Int): List<ScheduledAudioEvent> =
        scheduleBlockInto(startFrame, frameCount).toList()

    fun scheduleBlockInto(startFrame: Long, frameCount: Int): ScheduledAudioEventBuffer {
        require(startFrame >= 0 && frameCount > 0)
        val endFrame = checkedAddPositive(startFrame, frameCount.toLong())
        val scanStartFrame = if (startFrame == 0L) 0L else startFrame - 1L
        val scanArc = TimeArc(frameToCycle(scanStartFrame), frameToCycle(endFrame))
        scheduled.clear()

        for (trackIndex in program.source.musicTracks.indices) {
            val track = program.source.musicTracks[trackIndex]
            var orderInTrack = 0
            var cycle = floorCycle(scanArc.start)
            val lastCycleExclusive = ceilCycle(scanArc.endExclusive)
            while (cycle < lastCycleExclusive) {
                val cycleArc = TimeArc(CycleTime.of(cycle), CycleTime.of(cycle + 1))
                val chunkArc = cycleArc.intersection(scanArc)
                if (chunkArc != null) {
                    var sectionBaseCycle = 0L
                    var transposeSemitones = 0
                    var selectedPattern: Pattern<AudioNote>? = track.pattern
                    if (track.sections.isNotEmpty()) {
                        var arrangementCycles = 0L
                        for (section in track.sections) arrangementCycles += section.cycles
                        val arrangementCycle = floorMod(cycle, arrangementCycles)
                        var sectionStart = 0L
                        selectedPattern = null
                        for (section in track.sections) {
                            val sectionEnd = sectionStart + section.cycles
                            if (arrangementCycle < sectionEnd) {
                                selectedPattern = section.pattern
                                transposeSemitones = section.transposeSemitones
                                sectionBaseCycle = cycle - (arrangementCycle - sectionStart)
                                break
                            }
                            sectionStart = sectionEnd
                        }
                    }
                    if (selectedPattern == null) {
                        cycle += 1
                        continue
                    }
                    val sectionOffset = CycleTime.of(sectionBaseCycle)
                    val localChunkArc = TimeArc(
                        chunkArc.start - sectionOffset,
                        chunkArc.endExclusive - sectionOffset,
                    )
                    patternEvents.clear()
                    patternBudget.reset()
                    selectedPattern.queryInto(localChunkArc, patternBudget, patternEvents)
                    for (eventIndex in 0 until patternEvents.size) {
                        val wholeStart = patternEvents.wholeStartAt(eventIndex) + sectionOffset
                        if (wholeStart !in cycleArc) continue
                        val pitched = patternEvents.valueAt(eventIndex) as? AudioNote.Pitched ?: continue
                        val transposedMidi = pitched.midi.value + transposeSemitones
                        if (transposedMidi !in 0..127) continue
                        val absoluteStart = cycleToFrame(wholeStart)
                        if (absoluteStart !in startFrame until endFrame) continue
                        val absoluteEnd = cycleToFrame(patternEvents.wholeEndExclusiveAt(eventIndex) + sectionOffset)
                        val duration = absoluteEnd - absoluteStart
                        if (duration <= 0) continue
                        scheduled.add(
                            trackIndex = trackIndex,
                            note = MidiNote.of(transposedMidi),
                            absoluteStartFrame = absoluteStart,
                            frameOffset = (absoluteStart - startFrame).toInt(),
                            durationFrames = duration,
                            velocity = pitched.velocity,
                            orderInTrack = orderInTrack++,
                        )
                    }
                }
                cycle += 1
            }
        }

        scheduled.sort()
        return scheduled
    }

    private fun frameToCycle(frame: Long): CycleTime = CycleTime.of(
        numerator = checkedMultiplyPositive(frame, tempoNumerator),
        denominator = checkedMultiplyPositive(sampleRate.toLong() * BEATS_PER_CYCLE, tempoDenominator),
    )

    private fun cycleToFrame(time: CycleTime): Long = (
        time.numerator.toDouble() * sampleRate * BEATS_PER_CYCLE * tempoDenominator /
            (time.denominator.toDouble() * tempoNumerator)
        ).roundToLong()
}

private fun greatestCommonDivisor(first: Long, second: Long): Long {
    var left = first
    var right = second
    while (right != 0L) {
        val remainder = left % right
        left = right
        right = remainder
    }
    return left
}

private fun floorCycle(time: CycleTime): Long = time.numerator / time.denominator

private fun ceilCycle(time: CycleTime): Long =
    time.numerator / time.denominator + if (time.numerator % time.denominator == 0L) 0 else 1

private fun floorMod(value: Long, positiveDivisor: Long): Long {
    val remainder = value % positiveDivisor
    return if (remainder < 0L) remainder + positiveDivisor else remainder
}

private fun checkedAddPositive(left: Long, right: Long): Long {
    require(left >= 0 && right >= 0 && left <= Long.MAX_VALUE - right) { "Audio frame range overflow" }
    return left + right
}

private fun checkedMultiplyPositive(left: Long, right: Long): Long {
    require(left >= 0 && right > 0 && (left == 0L || left <= Long.MAX_VALUE / right)) {
        "Audio time conversion overflow"
    }
    return left * right
}

private const val TEMPO_PRECISION = 1_000_000L
private const val BEATS_PER_CYCLE = 240L
