package com.example.model

enum class ServiceCategory(val displayName: String, val iconName: String) {
    INSTAGRAM_VIEWS("Instagram Views", "Visibility"),
    INSTAGRAM_FOLLOWERS("Instagram Followers", "PersonAdd"),
    INSTAGRAM_LIKES("Instagram Likes", "Favorite"),
    REELS_VIEWS("Reels Views", "PlayCircle"),
    YOUTUBE_VIEWS("YouTube Views", "VideoLibrary"),
    TELEGRAM_MEMBERS("Telegram Members", "Send")
}

data class SmmService(
    val id: String,
    val category: ServiceCategory,
    val title: String,
    val ratePer500: Double,
    val minQuantity: Int = 100,
    val maxQuantity: Int = 500000,
    val description: String,
    val linkHint: String,
    val badge: String
) {
    /**
     * Calculates the exact price for any given quantity.
     * E.g. for 500 views at ₹1 per 500 => (500 / 500) * 1 = ₹1.00
     * E.g. for 500 followers at ₹40 per 500 => (500 / 500) * 40 = ₹40.00
     */
    fun calculatePrice(quantity: Int): Double {
        return (quantity.toDouble() / 500.0) * ratePer500
    }
}

object SmmCatalog {
    val services = listOf(
        SmmService(
            id = "ig_views_fast",
            category = ServiceCategory.INSTAGRAM_VIEWS,
            title = "Instagram Views [Instant] ⚡ 500 Views = ₹1",
            ratePer500 = 1.0,
            minQuantity = 100,
            maxQuantity = 1000000,
            description = "Super fast start within 0-5 mins. High retention video views. Non-drop quality.",
            linkHint = "https://www.instagram.com/p/... or /reel/...",
            badge = "₹1 / 500 Views 🔥"
        ),
        SmmService(
            id = "ig_followers_hq",
            category = ServiceCategory.INSTAGRAM_FOLLOWERS,
            title = "Instagram Followers [High Quality] ⭐ 500 Followers = ₹40",
            ratePer500 = 40.0,
            minQuantity = 100,
            maxQuantity = 250000,
            description = "Real-looking profiles with posts & avatars. 30 days refill guarantee. Safe for accounts.",
            linkHint = "https://www.instagram.com/username (Profile link)",
            badge = "₹40 / 500 Followers ⭐"
        ),
        SmmService(
            id = "reels_views_viral",
            category = ServiceCategory.REELS_VIEWS,
            title = "Instagram Reels Viral Views 🚀 500 Views = ₹1",
            ratePer500 = 1.0,
            minQuantity = 100,
            maxQuantity = 500000,
            description = "Optimized for Instagram Explore algorithm. Instant delivery speed.",
            linkHint = "https://www.instagram.com/reel/...",
            badge = "₹1 / 500 Views ⚡"
        ),
        SmmService(
            id = "ig_likes_real",
            category = ServiceCategory.INSTAGRAM_LIKES,
            title = "Instagram Likes [Real & Active] ❤️ 500 Likes = ₹10",
            ratePer500 = 10.0,
            minQuantity = 50,
            maxQuantity = 100000,
            description = "Instant start likes from real-looking accounts with photos.",
            linkHint = "https://www.instagram.com/p/...",
            badge = "₹10 / 500 Likes"
        ),
        SmmService(
            id = "yt_views_hq",
            category = ServiceCategory.YOUTUBE_VIEWS,
            title = "YouTube Video Views [High Retention] 📺 500 Views = ₹15",
            ratePer500 = 15.0,
            minQuantity = 500,
            maxQuantity = 100000,
            description = "Safe for monetization. Worldwide traffic, natural retention.",
            linkHint = "https://www.youtube.com/watch?v=...",
            badge = "₹15 / 500 Views"
        ),
        SmmService(
            id = "tg_members",
            category = ServiceCategory.TELEGRAM_MEMBERS,
            title = "Telegram Channel Members 📢 500 Members = ₹35",
            ratePer500 = 35.0,
            minQuantity = 100,
            maxQuantity = 50000,
            description = "Non-drop channel subscribers with fast joining rate.",
            linkHint = "https://t.me/yourchannel",
            badge = "₹35 / 500 Members"
        )
    )
}
