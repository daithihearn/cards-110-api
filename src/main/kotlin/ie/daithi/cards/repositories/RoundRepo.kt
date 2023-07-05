package ie.daithi.cards.repositories

import ie.daithi.cards.model.Round
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository interface RoundRepo : CrudRepository<Round, String>
