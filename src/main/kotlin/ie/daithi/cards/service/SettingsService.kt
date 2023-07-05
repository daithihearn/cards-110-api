package ie.daithi.cards.service

import ie.daithi.cards.model.PlayerSettings
import ie.daithi.cards.repositories.SettingsRepo
import org.springframework.stereotype.Service

@Service
class SettingsService(private val settingsRepo: SettingsRepo) {
    fun getSettings(playerId: String): PlayerSettings {
        val settings = settingsRepo.findById(playerId)
        if (settings.isPresent) {
            return settings.get()
        }
        val newSettings = PlayerSettings(playerId)
        settingsRepo.save(newSettings)
        return newSettings
    }

    fun updateSettings(playerSettings: PlayerSettings) = settingsRepo.save(playerSettings)
}
