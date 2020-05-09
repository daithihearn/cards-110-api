package ie.daithi.cards.enumeration

enum class Card(val value: Int, val suit: Suit, val renegable: Boolean) {
    TWO_HEARTS(1, Suit.HEARTS, false),
    THREE_HEARTS(2, Suit.HEARTS, false),
    FOUR_HEARTS(3, Suit.HEARTS, false),
    SIX_HEARTS(4, Suit.HEARTS, false),
    SEVEN_HEARTS(5, Suit.HEARTS, false),
    EIGHT_HEARTS(6, Suit.HEARTS, false),
    NINE_HEARTS(7, Suit.HEARTS, false),
    TEN_HEARTS(8, Suit.HEARTS, false),
    QUEEN_HEARTS(9, Suit.HEARTS, false),
    KING_HEARTS(10, Suit.HEARTS, false),
    ACE_HEARTS(12, Suit.WILD, true),
    JACK_HEARTS(14, Suit.HEARTS, true),
    FIVE_HEARTS(15, Suit.HEARTS, true),
    TWO_DIAMONDS(1, Suit.DIAMONDS, false),
    THREE_DIAMONDS(2, Suit.DIAMONDS, false),
    FOUR_DIAMONDS(3, Suit.DIAMONDS, false),
    SIX_DIAMONDS(4, Suit.DIAMONDS, false),
    SEVEN_DIAMONDS(5, Suit.DIAMONDS, false),
    EIGHT_DIAMONDS(6, Suit.DIAMONDS, false),
    NINE_DIAMONDS(7, Suit.DIAMONDS, false),
    TEN_DIAMONDS(8, Suit.DIAMONDS, false),
    QUEEN_DIAMONDS(9, Suit.DIAMONDS, false),
    KING_DIAMONDS(10, Suit.DIAMONDS, false),
    ACE_DIAMONDS(11, Suit.DIAMONDS, false),
    JACK_DIAMONDS(14, Suit.DIAMONDS, true),
    FIVE_DIAMONDS(15, Suit.DIAMONDS, true),
    TEN_CLUBS(1, Suit.CLUBS, false),
    NINE_CLUBS(2, Suit.CLUBS, false),
    EIGHT_CLUBS(3, Suit.CLUBS, false),
    SEVEN_CLUBS(4, Suit.CLUBS, false),
    SIX_CLUBS(5, Suit.CLUBS, false),
    FOUR_CLUBS(6, Suit.CLUBS, false),
    THREE_CLUBS(7, Suit.CLUBS, false),
    TWO_CLUBS(8, Suit.CLUBS, false),
    QUEEN_CLUBS(9, Suit.CLUBS, false),
    KING_CLUBS(10, Suit.CLUBS, false),
    ACE_CLUBS(11, Suit.CLUBS, false),
    JACK_CLUBS(14, Suit.CLUBS, true),
    FIVE_CLUBS(15, Suit.CLUBS, true),
    TEN_SPADES(1, Suit.SPADES, false),
    NINE_SPADES(2, Suit.SPADES, false),
    EIGHT_SPADES(3, Suit.SPADES, false),
    SEVEN_SPADES(4, Suit.SPADES, false),
    SIX_SPADES(5, Suit.SPADES, false),
    FOUR_SPADES(6, Suit.SPADES, false),
    THREE_SPADES(7, Suit.SPADES, false),
    TWO_SPADES(8, Suit.SPADES, false),
    QUEEN_SPADES(9, Suit.SPADES, false),
    KING_SPADES(10, Suit.SPADES, false),
    ACE_SPADES(11, Suit.SPADES, false),
    JACK_SPADES(14, Suit.SPADES, true),
    FIVE_SPADES(15, Suit.SPADES, true),
    JOKER(13, Suit.WILD, true);
}