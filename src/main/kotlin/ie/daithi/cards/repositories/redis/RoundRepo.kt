package ie.daithi.cards.repositories.redis

import ie.daithi.cards.model.Round
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RoundRepo: CrudRepository<Round, String>