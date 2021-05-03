package ie.daithi.cards.service

import ie.daithi.cards.model.*
import ie.daithi.cards.repositories.SpectatorRepo
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class SpectatorService(
        private val spectatorRepo: SpectatorRepo
) {

    // register as a spectator
    fun register(spectatorId: String, gameId: String) {
        spectatorRepo.save(
                Spectator(id = spectatorId,
                    gameId = gameId,
                    timestamp = LocalDateTime.now())
        )
    }

    // Get spectators for a game
    fun getSpectators(id: String): List<Spectator> {
        return spectatorRepo.findAllByGameId(id)
    }
}