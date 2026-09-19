package ge.yet.game.pattern

fun <T> Pattern<T>.humanize(
    maxOffset: CycleTime,
    seed: Long,
): Pattern<T> {
    require(maxOffset >= CycleTime.ZERO && maxOffset < CycleTime.ONE) {
        "A humanize offset must be non-negative and less than one cycle"
    }
    return streamingPattern { arc, budget, output ->
        budget.consumeOperation()
        val first = output.size
        queryInto(
            TimeArc(arc.start - maxOffset, arc.endExclusive + maxOffset),
            budget,
            output,
        )
        var write = first
        for (read in first until output.size) {
            val shift = deterministicOffset(seed, output.stableTimeKeyAt(read), maxOffset)
            val wholeStart = output.wholeStartAt(read) + shift
            val wholeEnd = output.wholeEndExclusiveAt(read) + shift
            val activeStart = output.activeStartAt(read) + shift
            val activeEnd = output.activeEndExclusiveAt(read) + shift
            val clipped = TimeArc(activeStart, activeEnd).intersection(arc) ?: continue
            output.copyEvent(read, write)
            output.replaceTimes(write, wholeStart, wholeEnd, clipped.start, clipped.endExclusive)
            write += 1
        }
        output.truncate(write)
        output.stableSortByActiveStart(first)
    }
}

fun <T> Pattern<T>.swing(
    subdivisions: Int,
    amount: CycleTime,
): Pattern<T> {
    require(subdivisions in 2..64) { "Swing subdivisions must be in 2..64" }
    require(amount >= CycleTime.ZERO && amount < CycleTime.ONE) {
        "A swing amount must be non-negative and less than one cycle"
    }
    return streamingPattern { arc, budget, output ->
        budget.consumeOperation()
        val first = output.size
        queryInto(TimeArc(arc.start - amount, arc.endExclusive), budget, output)
        var write = first
        for (read in first until output.size) {
            val originalStart = output.wholeStartAt(read)
            val gridIndex = (originalStart * CycleTime.of(subdivisions.toLong())).floorToLong()
            val shift = if (floorMod(gridIndex, subdivisions.toLong()) % 2L == 1L) amount else CycleTime.ZERO
            val wholeStart = originalStart + shift
            val wholeEnd = output.wholeEndExclusiveAt(read) + shift
            val activeStart = output.activeStartAt(read) + shift
            val activeEnd = output.activeEndExclusiveAt(read) + shift
            val clipped = TimeArc(activeStart, activeEnd).intersection(arc) ?: continue
            output.copyEvent(read, write)
            output.replaceTimes(write, wholeStart, wholeEnd, clipped.start, clipped.endExclusive)
            write += 1
        }
        output.truncate(write)
        output.stableSortByActiveStart(first)
    }
}

private fun deterministicOffset(seed: Long, key: ULong, bound: CycleTime): CycleTime {
    if (bound == CycleTime.ZERO) return CycleTime.ZERO
    val unit = deterministicUnit(seed, key)
    val signedStep = (unit * (OFFSET_RESOLUTION * 2L + 1L)).toLong() - OFFSET_RESOLUTION
    return bound * CycleTime.of(signedStep, OFFSET_RESOLUTION)
}

private const val OFFSET_RESOLUTION = 65_535L
