package fr.xebia.mlkitinactions.emoji

enum class FruitType(val label: String, val emoji: String) {
    APPLE("apple", "\uD83C\uDF4E"),
    BANANA("banana", "\uD83C\uDF4C"),
    GRAPE("grape", "\uD83C\uDF47"),
    KIWI("kiwi", "\uD83E\uDD5D"),
    MANGO("mango", ""),
    ORANGE("orange", "\uD83C\uDF4A"),
    PINEAPPLE("pineapple", "\uD83C\uDF4D"),
    RASPBERRY("raspberry", ""),
    STRAWBERRY("strawberry", "\uD83C\uDF53"),
    UNKNOWN("unknown", "");

    companion object {
        fun getEmojiByName(name: String): FruitType {
            return try {
                FruitType.valueOf(name.toUpperCase())
            } catch (e: IllegalArgumentException) {
                FruitType.UNKNOWN
            }
        }
    }

}