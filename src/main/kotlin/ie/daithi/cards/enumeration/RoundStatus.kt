package ie.daithi.cards.enumeration

enum class RoundStatus(
        val order: Int
) {
    CALLING(0), CALLED(1), BUYING(2), PLAYING(3), FINISHED(4);
}