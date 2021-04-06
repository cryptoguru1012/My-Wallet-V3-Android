package info.blockchain.wallet.payment

import info.blockchain.api.data.UnspentOutput

/**
 * Sort coins for different selection optimizations.
 */
interface CoinSortingMethod {
    fun sort(coins: List<UnspentOutput>): List<UnspentOutput>
}

/**
 * Prioritizes smaller coins, better coin consolidation but a higher fee.
 */
object AscentDraw : CoinSortingMethod {
    override fun sort(coins: List<UnspentOutput>) = coins.sortedBy { it.value }
}

/**
 * Prioritizes larger coins, worse coin consolidation but a lower fee.
 */
object DescentDraw : CoinSortingMethod {
    override fun sort(coins: List<UnspentOutput>) = coins.sortedByDescending { it.value }
}

/**
 * The smallest non-replayable coin, followed by all replayable coins (largest to smallest),
 * followed by all remaining non-replayable coins (also largest to smallest). Adds replay protection.
 */
class ReplayProtection(private val nonReplayableInput: UnspentOutput) : CoinSortingMethod {
    override fun sort(coins: List<UnspentOutput>): List<UnspentOutput> {
        if (coins.isEmpty()) {
            return coins
        }
        val (replayable, nonReplayable) = AscentDraw.sort(coins).partition {
            it.isReplayable
        }
        return listOf(nonReplayable.firstOrNull() ?: nonReplayableInput) +
                DescentDraw.sort(replayable) +
                DescentDraw.sort(nonReplayable.drop(1))
    }
}
