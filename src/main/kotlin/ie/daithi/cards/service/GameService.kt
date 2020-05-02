package ie.daithi.cards.service

import ie.daithi.cards.enumeration.GameStatus
import ie.daithi.cards.model.Game
import ie.daithi.cards.model.Player
import ie.daithi.cards.repositories.mongodb.AppUserRepo
import ie.daithi.cards.repositories.mongodb.GameRepo
import ie.daithi.cards.validation.EmailValidator
import ie.daithi.cards.web.exceptions.InvalidEmailException
import ie.daithi.cards.web.exceptions.InvalidSatusException
import ie.daithi.cards.web.exceptions.NotFoundException
import ie.daithi.cards.web.security.model.AppUser
import ie.daithi.cards.web.security.model.Authority
import org.apache.logging.log4j.LogManager
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom

@Service
class GameService(
        private val gameRepo: GameRepo,
        private val emailValidator: EmailValidator,
        private val emailService: EmailService,
        private val appUserRepo: AppUserRepo,
        private val passwordEncoder: BCryptPasswordEncoder,
        private val mongoOperations: MongoOperations,
        private val publishService: PublishService
) {
    fun create(name: String, playerEmails: List<String>): Game {
        logger.info("Attempting to start a 110")

        // 1. Put all emails to lower case
        val lowerCaseEmails = playerEmails.map(String::toLowerCase)

        // 2. Validate Emails
        lowerCaseEmails.forEach {
            if (!emailValidator.isValid(it))
                throw InvalidEmailException("Invalid email $it")
        }

        // 3. Create Players and Issue emails
        val users = arrayListOf<AppUser>()
        lowerCaseEmails.forEach{
            val passwordByte = ByteArray(16)
            secureRandom.nextBytes(passwordByte)
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(passwordByte)
            val password = digest.fold("", { str, byt -> str + "%02x".format(byt) })

            val user = AppUser(username = it,
                    password = passwordEncoder.encode(password),
                    authorities = listOf(Authority.PLAYER))

            appUserRepo.deleteByUsernameIgnoreCase(it)
            appUserRepo.save(user)
            users.add(user)
            emailService.sendQuizInvite(it, password)
        }

        // 5. Create Game
        val game = Game(
                status = GameStatus.ACTIVE,
                name = name,
                players = users.map { Player(id = it.id!!, displayName = it.username!!) })

        gameRepo.save(game)

        logger.info("Game started successfully ${game.id}")
        return game
    }

    fun get(id: String): Game {
        val game = gameRepo.findById(id)
        if (!game.isPresent)
            throw NotFoundException("Game $id not found")
        return game.get()
    }

    fun delete(id: String) {
        gameRepo.deleteById(id)
    }

    fun getAll(): List<Game> {
        return gameRepo.findAll()
    }

    fun getActive(): List<Game> {
        return gameRepo.findAllByStatus(GameStatus.ACTIVE)
    }

    fun getByPlayerId(id: String): Game {
        return gameRepo.getByPlayerId(id)
    }

    fun finish(id: String) {
        val game = get(id)
        if( game.status == GameStatus.ACTIVE) throw InvalidSatusException("Can only finish a game that is in STARTED state not ${game.status}")
        game.status = GameStatus.COMPLETED
        gameRepo.save(game)
    }

    fun cancel(id: String) {
        val game = get(id)
        if( game.status == GameStatus.CANCELLED) throw InvalidSatusException("Game is already in CANCELLED state")
        game.status = GameStatus.CANCELLED
        gameRepo.save(game)
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java)
        private val secureRandom = SecureRandom()
    }

}