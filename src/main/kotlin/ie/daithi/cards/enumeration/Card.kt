package ie.daithi.cards.enumeration

enum class Card(val value: Int, val coldValue: Int, val suit: Suit, val renegable: Boolean) {
    EMPTY(0, 0, Suit.EMPTY, false),
    TWO_HEARTS(101, 2, Suit.HEARTS, false),
    THREE_HEARTS(102, 3, Suit.HEARTS, false),
    FOUR_HEARTS(103, 4, Suit.HEARTS, false),
    SIX_HEARTS(104, 6, Suit.HEARTS, false),
    SEVEN_HEARTS(105, 7, Suit.HEARTS, false),
    EIGHT_HEARTS(106, 8, Suit.HEARTS, false),
    NINE_HEARTS(107, 9, Suit.HEARTS, false),
    TEN_HEARTS(108, 10, Suit.HEARTS, false),
    QUEEN_HEARTS(109, 12, Suit.HEARTS, false),
    KING_HEARTS(110, 13, Suit.HEARTS, false),
    ACE_HEARTS(112, 0, Suit.WILD, true),
    JACK_HEARTS(114, 11, Suit.HEARTS, true),
    FIVE_HEARTS(115, 5, Suit.HEARTS, true),
    TWO_DIAMONDS(101, 2, Suit.DIAMONDS, false),
    THREE_DIAMONDS(102, 3, Suit.DIAMONDS, false),
    FOUR_DIAMONDS(103, 4, Suit.DIAMONDS, false),
    SIX_DIAMONDS(104, 6, Suit.DIAMONDS, false),
    SEVEN_DIAMONDS(105, 7, Suit.DIAMONDS, false),
    EIGHT_DIAMONDS(106, 8, Suit.DIAMONDS, false),
    NINE_DIAMONDS(107, 9, Suit.DIAMONDS, false),
    TEN_DIAMONDS(108, 10, Suit.DIAMONDS, false),
    QUEEN_DIAMONDS(109, 12, Suit.DIAMONDS, false),
    KING_DIAMONDS(110, 13, Suit.DIAMONDS, false),
    ACE_DIAMONDS(111, 1, Suit.DIAMONDS, false),
    JACK_DIAMONDS(114, 11, Suit.DIAMONDS, true),
    FIVE_DIAMONDS(115, 5, Suit.DIAMONDS, true),
    TEN_CLUBS(101, 1, Suit.CLUBS, false),
    NINE_CLUBS(102, 2, Suit.CLUBS, false),
    EIGHT_CLUBS(103, 3, Suit.CLUBS, false),
    SEVEN_CLUBS(104, 4, Suit.CLUBS, false),
    SIX_CLUBS(105, 5, Suit.CLUBS, false),
    FOUR_CLUBS(106, 7, Suit.CLUBS, false),
    THREE_CLUBS(107, 8, Suit.CLUBS, false),
    TWO_CLUBS(108, 9, Suit.CLUBS, false),
    QUEEN_CLUBS(109, 12, Suit.CLUBS, false),
    KING_CLUBS(110, 13, Suit.CLUBS, false),
    ACE_CLUBS(111, 10, Suit.CLUBS, false),
    JACK_CLUBS(114, 11, Suit.CLUBS, true),
    FIVE_CLUBS(115, 6, Suit.CLUBS, true),
    TEN_SPADES(101, 1, Suit.SPADES, false),
    NINE_SPADES(102, 2, Suit.SPADES, false),
    EIGHT_SPADES(103, 3, Suit.SPADES, false),
    SEVEN_SPADES(104, 4, Suit.SPADES, false),
    SIX_SPADES(105, 5, Suit.SPADES, false),
    FOUR_SPADES(106, 7, Suit.SPADES, false),
    THREE_SPADES(107, 8, Suit.SPADES, false),
    TWO_SPADES(108, 9, Suit.SPADES, false),
    QUEEN_SPADES(109, 12, Suit.SPADES, false),
    KING_SPADES(110, 13, Suit.SPADES, false),
    ACE_SPADES(111, 10, Suit.SPADES, false),
    JACK_SPADES(114, 11, Suit.SPADES, true),
    FIVE_SPADES(115, 6, Suit.SPADES, true),
    JOKER(113, 0, Suit.WILD, true)
}
