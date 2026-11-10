package io.github.guilhermedkdk.wakeguard.challenge

enum class Difficulty { EASY, MEDIUM, HARD }

enum class ChallengeType(
    val amounts: IntProgression,
    val defaultAmount: Int,
) {
    MATH(1..10, 3),
    MEMORY(1..5, 2),
    SHAKE(10..200 step 10, 30),
    TYPING(1..5, 1),
}

/** One challenge of an alarm: what to do, how hard and how many rounds. */
data class ChallengeSpec(
    val type: ChallengeType,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val amount: Int = type.defaultAmount,
) {
    init {
        require(amount in type.amounts) { "amount $amount is not allowed for $type" }
    }
}
