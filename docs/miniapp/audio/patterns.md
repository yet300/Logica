# Temporal patterns

Patterns are pure, exact and bounded descriptions of note events. They do not play audio and do not depend on tempo. Use `sequence`, `pure`, `stack`, `euclidean`, `choose`, then transform with `fast`, `slow`, `shift`, `repeat`, `every`, deterministic `degrade`, `swing`, or seeded `humanize`.

```kotlin
private val deterministicMelody = sequence(
    listOf(60, 64, 67, 72).map { AudioNote.Pitched(MidiNote.of(it)) },
).degrade(probability = 0.25f, seed = 42L)
```

The same seed and query arc produce the same events. Choose stable, explicit seeds; do not derive them from wall-clock time. Determinism makes reviews, tests and bug reports reproducible.

Keep patterns bounded. The default query budget allows 4,096 operations and 256 events; the audio compiler additionally rejects declarations that exceed its mobile event/operation budget. Prefer a short pattern plus transforms over materializing a very long note list.

`AudioNote.Rest` creates an explicit rest. `degrade(probability, seed)` removes events with the given probability: `0f` keeps all events and `1f` removes all events.

`humanizedNotes(notes, velocityRange, seed)` creates a declaration-time velocity variation without changing pitch or rhythm. `Pattern.humanize(maxOffset, seed)` moves event starts by a bounded exact `CycleTime`; the query expands internally so events crossing a block/cycle boundary are not lost. `Pattern.swing(subdivisions, amount)` delays odd subdivisions. Keep all seeds explicit and all timing offsets much smaller than the shortest note.

For tonal material, `tonalScale(root, ScaleMode)` maps signed scale degrees to checked MIDI notes. The built-in modes are major, natural minor, Dorian, Mixolydian, and major/minor pentatonic. These helpers generate notes only; rhythm remains an explicit pattern authored by the game.
