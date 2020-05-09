package ie.daithi.cards.service

import ie.daithi.cards.model.Round
import ie.daithi.cards.repositories.RoundRepo
import ie.daithi.cards.web.exceptions.NotFoundException
import org.springframework.stereotype.Service

@Service
class RoundService(
        private val roundRepo: RoundRepo) {

    fun get(gameId: String): Round {
        val round = roundRepo.findById(gameId)
        if (round.isEmpty) throw NotFoundException("Round not found for game: $gameId")
        return round.get()
    }

    fun getOrNull(gameId: String): Round? {
        val round = roundRepo.findById(gameId)
        if (round.isEmpty) return null
        return round.get()
    }

    fun save(round: Round) {
        roundRepo.save(round)
    }
}