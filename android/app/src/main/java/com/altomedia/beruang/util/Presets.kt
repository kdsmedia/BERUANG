package com.altomedia.beruang.util

data class Feeling(val emoji: String, val label: String)

val FEELINGS = listOf(
    Feeling("😊","happy"), Feeling("😂","amused"), Feeling("🥳","celebrating"), Feeling("😎","cool"),
    Feeling("😢","sad"), Feeling("😭","crying"), Feeling("😡","angry"), Feeling("🤔","thoughtful"),
    Feeling("😴","sleepy"), Feeling("🤩","excited"), Feeling("😇","blessed"), Feeling("🥰","loved"),
    Feeling("😱","scared"), Feeling("🤗","hugging"), Feeling("🤯","mind-blown"), Feeling("🥺","pleading"),
    Feeling("😏","smug"), Feeling("🤓","nerdy"), Feeling("🤠","cowboy"), Feeling("🥶","freezing"),
    Feeling("🤒","sick"), Feeling("🤤","drooling"), Feeling("😋","hungry"), Feeling("🧐","skeptical"),
    Feeling("🤫","quiet"), Feeling("🤐","speechless"), Feeling("🤥","lying"), Feeling("😜","playful"),
    Feeling("🤪","crazy"), Feeling("🤨","suspicious"), Feeling("😔","pensive"), Feeling("😬","awkward"),
    Feeling("😌","relieved"), Feeling("😤","determined"), Feeling("🙏","grateful"), Feeling("🥱","bored")
)

val LOCATIONS = listOf(
    "Jakarta","Bandung","Surabaya","Medan","Semarang","Makassar","Yogyakarta","Denpasar",
    "Palembang","Balikpapan","Singapore","Kuala Lumpur","Bangkok","Manila","Tokyo","Seoul",
    "London","New York","Paris","Berlin","Sydney","Dubai","Hong Kong","Taipei","Mumbai",
    "Hanoi","Ho Chi Minh City","Beijing","Shanghai","Istanbul"
)

val EMOJI_CATEGORIES: Map<String, List<String>> = mapOf(
    "Smileys" to listOf("😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃","😉","😊","😇","🥰","😍","🤩","😘","😗","😚","😙","🥲","😋","😛","😜","🤪","😝","🤑","🤗","🤭","🤫","🤔","🤐","🤨","😐","😑","😶","😏","😒","🙄","😬","😌","😔","😪","🤤","😴","😷","🤒","🤕","🤢","🤮"),
    "Gestures" to listOf("👍","👎","👌","🤌","🤏","✌️","🤞","🤟","🤘","🤙","👈","👉","👆","👇","☝️","👋","🤚","🖐️","✋","🖖","👏","🙌","👐","🤲","🙏","✍️","💪","🦾","🦵","🦶","👂","🦻","👃","🧠","🦷","🦴","👀","👁️","👅","👄","💋"),
    "Hearts" to listOf("❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❣️","💕","💞","💓","💗","💖","💘","💝","💟","♥️","💌"),
    "Animals" to listOf("🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🐔","🐧","🐦","🐤","🦆","🦅","🦉","🦇","🐺","🐗","🐴","🦄","🐝","🐛","🦋","🐌","🐞","🐢","🐍","🦎","🐙","🦑","🦐","🦞","🦀","🐠","🐟","🐬","🐳","🐋","🦈","🐊"),
    "Food" to listOf("🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🫐","🍈","🍒","🍑","🥭","🍍","🥥","🥝","🍅","🍆","🥑","🥦","🥬","🥒","🌶️","🫑","🌽","🥕","🧄","🧅","🥔","🍠","🥐","🥯","🍞","🥖","🥨","🧀","🥚","🍳","🧈","🥞","🧇","🥓","🥩","🍗","🍖","🌭","🍔","🍟","🍕"),
    "Travel" to listOf("🚗","🚕","🚙","🚌","🚎","🏎️","🚓","🚑","🚒","🚐","🚚","🚛","🚜","🛵","🏍️","🚲","🛴","🚡","🚠","🚟","🚃","🚋","🚞","🚝","🚄","🚅","🚈","🚂","🚆","✈️","🛫","🛬","🚀","🛸","🚁","🛶","⛵","🚤","🛥️","🛳️","⛴️","🚢","⚓","⛽","🚧","🚦","🚥","🗺️","🗿","🗽","🗼"),
    "Activities" to listOf("⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉","🥏","🎱","🪀","🏓","🏸","🏒","🏑","🥍","🏏","🪃","🥅","⛳","🪁","🏹","🎣","🤿","🥊","🥋","🎽","🛹","🛼","🛷","⛸️","🥌","🎿","⛷️","🏂","🪂","🏋️","🤼","🤸","⛹️","🤺","🤾","🏌️","🏇","🧘","🏄","🏊","🤽","🚣","🧗")
)
