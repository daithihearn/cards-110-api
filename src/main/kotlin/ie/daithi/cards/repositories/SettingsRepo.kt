package ie.daithi.cards.repositories

import ie.daithi.cards.model.PlayerSettings
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SettingsRepo: CrudRepository<PlayerSettings, String>