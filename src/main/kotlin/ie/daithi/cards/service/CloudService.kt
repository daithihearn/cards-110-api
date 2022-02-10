package ie.daithi.cards.service

interface CloudService {
    fun uploadImage(imageUri: String): String
}