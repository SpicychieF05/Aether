package com.example.data.model

enum class TerrainCategory(
    val displayName: String,
    val shortLabel: String,
    val description: String,
    val sampleLocation: String
) {
    CITY_SKYLINE(
        displayName = "Dense Modern Skyline",
        shortLabel = "Skyline",
        description = "Towering isometric skyscrapers, glass facades, and glowing city lights",
        sampleLocation = "New York, Tokyo, Dubai Downtown"
    ),
    COASTAL(
        displayName = "Coastal Shore & Bay",
        shortLabel = "Coastal",
        description = "Gentle ocean waves, sandy shoreline, distant sailboats, and seaside breeze",
        sampleLocation = "Kolkata, Mumbai, Miami, Barcelona"
    ),
    DESERT(
        displayName = "Desert Dunes",
        shortLabel = "Desert",
        description = "Sweeping golden sand ridges, warm heat shimmer, and starry desert skies",
        sampleLocation = "Sahara, Jaisalmer, Phoenix, Riyadh"
    ),
    ALPINE(
        displayName = "Snowy Alpine Peaks",
        shortLabel = "Alpine",
        description = "Dramatic snow-crested mountain ridges, pine trees, and cozy chalets",
        sampleLocation = "Zermatt, Innsbruck, Denver, Shimla"
    ),
    HILLS_PLAINS(
        displayName = "Green Hills & Plains",
        shortLabel = "Hills",
        description = "Rolling emerald green terraces, stone fences, and tranquil countryside",
        sampleLocation = "Kyoto, Cotswolds, Tuscany, Midwest"
    ),
    TROPICAL(
        displayName = "Tropical Rainforest",
        shortLabel = "Tropical",
        description = "Lush jungle canopy, exotic palm fronds, waterfall mist, and deep greens",
        sampleLocation = "Bali, Manaus, Kerala, Costa Rica"
    ),
    VILLAGE(
        displayName = "Rustic Village",
        shortLabel = "Village",
        description = "Cobblestone streets, terracotta rooftops, and warm glowing lanterns",
        sampleLocation = "Hallstatt, Shirakawa-go, Cotswold Hamlets"
    ),
    SUBURBAN(
        displayName = "Suburban Neighborhood",
        shortLabel = "Suburban",
        description = "Quiet tree-lined residential avenues, cozy homes, and garden lawns",
        sampleLocation = "Palo Alto, Kyoto Suburbs, Surrey"
    );

    companion object {
        fun resolveTerrain(
            name: String,
            country: String? = null,
            admin1: String? = null,
            latitude: Double = 0.0,
            longitude: Double = 0.0,
            elevation: Double? = null,
            population: Long? = null
        ): TerrainCategory {
            val normalizedName = name.lowercase().trim()
            val normalizedCountry = country?.lowercase()?.trim() ?: ""
            val normalizedAdmin = admin1?.lowercase()?.trim() ?: ""
            val fullString = "$normalizedName $normalizedCountry $normalizedAdmin"

            // 1. Explicit known curated matches
            when {
                // Kolkata: Gangetic delta / Hooghly riverside / Bay of Bengal marine influence
                normalizedName.contains("kolkata") || normalizedName.contains("calcutta") -> return COASTAL
                normalizedName.contains("kyoto") -> return HILLS_PLAINS
                normalizedName.contains("zermatt") || normalizedName.contains("innsbruck") ||
                        normalizedName.contains("aspen") || normalizedName.contains("whistler") ||
                        normalizedName.contains("chamonix") || normalizedName.contains("leh") ||
                        normalizedName.contains("shimla") || normalizedName.contains("manali") -> return ALPINE
                normalizedName.contains("santorini") || normalizedName.contains("miami") ||
                        normalizedName.contains("mumbai") || normalizedName.contains("barcelona") ||
                        normalizedName.contains("sydney") || normalizedName.contains("honolulu") ||
                        normalizedName.contains("goa") || normalizedName.contains("nice") ||
                        normalizedName.contains("rio de janeiro") -> return COASTAL
                normalizedName.contains("cairo") || normalizedName.contains("dubai") ||
                        normalizedName.contains("riyadh") || normalizedName.contains("jaisalmer") ||
                        normalizedName.contains("phoenix") || normalizedName.contains("doha") ||
                        normalizedName.contains("las vegas") || normalizedName.contains("abu dhabi") -> return DESERT
                normalizedName.contains("bali") || normalizedName.contains("manaus") ||
                        normalizedName.contains("phuket") || normalizedName.contains("kochi") ||
                        normalizedName.contains("singapore") || normalizedName.contains("amazon") -> return TROPICAL
                normalizedName.contains("hallstatt") || normalizedName.contains("shirakawa") ||
                        normalizedName.contains("giethoorn") || normalizedName.contains("bibury") -> return VILLAGE
                normalizedName.contains("new york") || normalizedName.contains("tokyo") ||
                        normalizedName.contains("hong kong") || normalizedName.contains("london") ||
                        normalizedName.contains("paris") || normalizedName.contains("shanghai") ||
                        normalizedName.contains("chicago") || normalizedName.contains("toronto") -> return CITY_SKYLINE
            }

            // 2. Keyword heuristic checks
            if (fullString.contains("beach") || fullString.contains("coast") ||
                fullString.contains("port ") || fullString.contains("island") ||
                fullString.contains("bay") || fullString.contains("harbor") || fullString.contains("harbour")
            ) {
                return COASTAL
            }

            if (fullString.contains("desert") || fullString.contains("dune") ||
                fullString.contains("sahara") || fullString.contains("oasis")
            ) {
                return DESERT
            }

            if (fullString.contains("mountain") || fullString.contains("peak") ||
                fullString.contains("alpine") || fullString.contains("alps") ||
                fullString.contains("himalaya")
            ) {
                return ALPINE
            }

            if (fullString.contains("rainforest") || fullString.contains("jungle") ||
                fullString.contains("tropics")
            ) {
                return TROPICAL
            }

            // 3. Elevation heuristic (> 1200m usually mountainous / alpine)
            if (elevation != null && elevation > 1200.0) {
                return ALPINE
            }

            // 4. Arid latitude/longitude regions (Thar, Sahara, Arabian Peninsula)
            if ((latitude in 15.0..32.0 && longitude in -15.0..55.0) &&
                (elevation == null || elevation < 800) &&
                (fullString.contains("saudi") || fullString.contains("egypt") || fullString.contains("oman") ||
                        fullString.contains("emirates") || fullString.contains("rajasthan"))
            ) {
                return DESERT
            }

            // 5. Population-based heuristic
            if (population != null) {
                return when {
                    population > 2_000_000 -> CITY_SKYLINE
                    population > 300_000 -> SUBURBAN
                    population in 15_000..300_000 -> SUBURBAN
                    population < 15_000 -> VILLAGE
                    else -> HILLS_PLAINS
                }
            }

            // 6. Default to green hills & plains if countryside, or skyline for known big country hubs
            return HILLS_PLAINS
        }
    }
}
