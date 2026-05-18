package com.cardbuddy.app.util

object BrandUtils {
    val categories = listOf(
        "Supermarket",
        "Home & DIY",
        "Health & Beauty",
        "Fashion",
        "Electronics",
        "Fuel",
        "Liquor",
        "Other"
    )

    fun getLogoUrl(storeName: String): String? {
        val query = storeName.lowercase().trim()
        val domain = when {
            query.contains("albert heijn") || query == "ah" -> "ah.nl"
            query.contains("lidl") -> "lidl.nl"
            query.contains("jumbo") -> "jumbo.com"
            query.contains("hema") -> "hema.nl"
            query.contains("kruidvat") -> "kruidvat.nl"
            query.contains("etos") -> "etos.nl"
            query.contains("trekpleister") -> "trekpleister.nl"
            query.contains("gamma") -> "gamma.nl"
            query.contains("karwei") -> "karwei.nl"
            query.contains("praxis") -> "praxis.nl"
            query.contains("hornbach") -> "hornbach.nl"
            query.contains("ikea") -> "ikea.com"
            query.contains("makro") -> "makro.nl"
            query.contains("mediamarkt") -> "mediamarkt.nl"
            query.contains("coolblue") -> "coolblue.nl"
            query.contains("action") -> "action.com"
            query.contains("blokker") -> "blokker.nl"
            query.contains("zeeman") -> "zeeman.com"
            query.contains("wibra") -> "wibra.nl"
            query.contains("xenos") -> "xenos.nl"
            query.contains("bijenkorf") -> "debijenkorf.nl"
            query.contains("gall") -> "gall.nl"
            query.contains("spar") -> "spar.nl"
            query.contains("dirk") -> "dirk.nl"
            query.contains("aldi") -> "aldi.nl"
            query.contains("plus") -> "plus.nl"
            query.contains("coop") -> "coop.nl"
            query.contains("decathlon") -> "decathlon.nl"
            query.contains("shell") -> "shell.nl"
            query.contains("bp") -> "bp.com"
            query.contains("esso") -> "esso.com"
            query.contains("texaco") -> "texaco.nl"
            query.contains("carrefour") -> "carrefour.fr"
            query.contains("leclerc") -> "e.leclerc"
            query.contains("mercadona") -> "mercadona.es"
            query.contains("rewe") -> "rewe.de"
            query.contains("edeka") -> "edeka.de"
            query.contains("mcdonalds") -> "mcdonalds.com"
            query.contains("starbucks") -> "starbucks.com"
            query.contains("douglas") -> "douglas.nl"
            query.contains("ici paris") -> "iciparisxl.nl"
            query.contains("primera") -> "primera.nl"
            query.contains("bruna") -> "bruna.nl"
            query.contains("vomar") -> "vomar.nl"
            query.contains("hoogvliet") -> "hoogvliet.com"
            query.contains("nettorama") -> "nettorama.nl"
            query.contains("poiesz") -> "poiesz-supermarkten.nl"
            query.contains("jan linders") -> "janlinders.nl"
            query.contains("h&m") || query.contains("h m") -> "hm.com"
            query.contains("c&a") || query.contains("c a") -> "c-and-a.com"
            query.contains("zara") -> "zara.com"
            query.contains("primark") -> "primark.com"
            query.contains("we fashion") -> "wefashion.com"
            query.contains("intratuin") -> "intratuin.nl"
            query.contains("welkoop") -> "welkoop.nl"
            query.contains("pets place") -> "petsplace.nl"
            query.contains("kwantum") -> "kwantum.nl"
            query.contains("leen bakker") -> "leenbakker.nl"
            query.contains("jysk") -> "jysk.nl"
            query.contains("bristol") -> "bristol.nl"
            query.contains("scapino") -> "scapino.nl"
            query.contains("decathlon") -> "decathlon.nl"
            else -> null
        }
        return if (domain != null) "https://logo.clearbit.com/$domain" else null
    }

    fun getFallbackLogoUrl(storeName: String): String? {
        val query = storeName.lowercase().trim()
        val domain = when {
            query.contains("albert heijn") || query == "ah" -> "ah.nl"
            query.contains("lidl") -> "lidl.nl"
            query.contains("jumbo") -> "jumbo.com"
            query.contains("hema") -> "hema.nl"
            query.contains("kruidvat") -> "kruidvat.nl"
            query.contains("etos") -> "etos.nl"
            query.contains("trekpleister") -> "trekpleister.nl"
            query.contains("gamma") -> "gamma.nl"
            query.contains("karwei") -> "karwei.nl"
            query.contains("praxis") -> "praxis.nl"
            query.contains("hornbach") -> "hornbach.nl"
            query.contains("ikea") -> "ikea.com"
            query.contains("makro") -> "makro.nl"
            query.contains("mediamarkt") -> "mediamarkt.nl"
            query.contains("coolblue") -> "coolblue.nl"
            query.contains("action") -> "action.com"
            query.contains("blokker") -> "blokker.nl"
            query.contains("zeeman") -> "zeeman.com"
            query.contains("wibra") -> "wibra.nl"
            query.contains("xenos") -> "xenos.nl"
            query.contains("bijenkorf") -> "debijenkorf.nl"
            query.contains("gall") -> "gall.nl"
            query.contains("spar") -> "spar.nl"
            query.contains("dirk") -> "dirk.nl"
            query.contains("aldi") -> "aldi.nl"
            query.contains("plus") -> "plus.nl"
            query.contains("coop") -> "coop.nl"
            query.contains("decathlon") -> "decathlon.nl"
            query.contains("shell") -> "shell.nl"
            query.contains("bp") -> "bp.com"
            query.contains("esso") -> "esso.com"
            query.contains("texaco") -> "texaco.nl"
            query.contains("carrefour") -> "carrefour.fr"
            query.contains("leclerc") -> "e.leclerc"
            query.contains("mercadona") -> "mercadona.es"
            query.contains("rewe") -> "rewe.de"
            query.contains("edeka") -> "edeka.de"
            query.contains("mcdonalds") -> "mcdonalds.com"
            query.contains("starbucks") -> "starbucks.com"
            query.contains("douglas") -> "douglas.nl"
            query.contains("ici paris") -> "iciparisxl.nl"
            query.contains("primera") -> "primera.nl"
            query.contains("bruna") -> "bruna.nl"
            query.contains("vomar") -> "vomar.nl"
            query.contains("hoogvliet") -> "hoogvliet.com"
            query.contains("nettorama") -> "nettorama.nl"
            query.contains("poiesz") -> "poiesz-supermarkten.nl"
            query.contains("jan linders") -> "janlinders.nl"
            query.contains("h&m") || query.contains("h m") -> "hm.com"
            query.contains("c&a") || query.contains("c a") -> "c-and-a.com"
            query.contains("zara") -> "zara.com"
            query.contains("primark") -> "primark.com"
            query.contains("we fashion") -> "wefashion.com"
            query.contains("intratuin") -> "intratuin.nl"
            query.contains("welkoop") -> "welkoop.nl"
            query.contains("pets place") -> "petsplace.nl"
            query.contains("kwantum") -> "kwantum.nl"
            query.contains("leen bakker") -> "leenbakker.nl"
            query.contains("jysk") -> "jysk.nl"
            query.contains("bristol") -> "bristol.nl"
            query.contains("scapino") -> "scapino.nl"
            query.contains("decathlon") -> "decathlon.nl"
            else -> null
        }
        return if (domain != null) "https://www.google.com/s2/favicons?domain=$domain&sz=128" else null
    }

    fun getBrandColor(storeName: String): String {
        val query = storeName.lowercase().trim()
        return when {
            query.contains("albert heijn") || query == "ah" -> "#00A0E2"
            query.contains("lidl") -> "#0050AA"
            query.contains("jumbo") -> "#EEB717"
            query.contains("ikea") -> "#0051BA"
            query.contains("gamma") -> "#003057"
            query.contains("karwei") -> "#000000"
            query.contains("praxis") -> "#E30613"
            query.contains("hornbach") -> "#FF6600"
            query.contains("makro") -> "#003057"
            query.contains("mediamarkt") -> "#DF0000"
            query.contains("coolblue") -> "#2196F3"
            query.contains("hema") -> "#E2001A"
            query.contains("kruidvat") || query.contains("trekpleister") -> "#E2001A"
            query.contains("etos") -> "#00B2B2"
            query.contains("action") -> "#00A9E0"
            query.contains("blokker") -> "#FF6600"
            query.contains("zeeman") -> "#003DA5"
            query.contains("spar") -> "#E21017"
            query.contains("gall") -> "#2E2E2E"
            query.contains("shell") -> "#FFD500"
            query.contains("bp") -> "#009639"
            query.contains("esso") -> "#EF3340"
            query.contains("carrefour") -> "#003580"
            query.contains("leclerc") -> "#0066ad"
            query.contains("mercadona") -> "#008a4f"
            query.contains("rewe") -> "#cc071e"
            query.contains("edeka") -> "#ffed00"
            query.contains("mcdonalds") -> "#FFC72C"
            query.contains("starbucks") -> "#00704A"
            query.contains("vomar") -> "#E30613"
            query.contains("hoogvliet") -> "#006738"
            query.contains("primera") -> "#003087"
            query.contains("bruna") -> "#E2001A"
            query.contains("h&m") || query.contains("h m") -> "#E50011"
            query.contains("zara") -> "#000000"
            query.contains("decathlon") -> "#0082C3"
            query.contains("jysk") -> "#003399"
            else -> "#1A1C1E"
        }
    }

    fun getBrandCategory(storeName: String): String {
        val query = storeName.lowercase().trim()
        return when {
            query.contains("albert heijn") || query == "ah" || query.contains("jumbo") || 
            query.contains("lidl") || query.contains("aldi") || query.contains("spar") || 
            query.contains("plus") || query.contains("coop") || query.contains("dirk") ||
            query.contains("carrefour") || query.contains("mercadona") || query.contains("rewe") ||
            query.contains("edeka") || query.contains("leclerc") || query.contains("vomar") ||
            query.contains("hoogvliet") || query.contains("nettorama") || query.contains("poiesz") ||
            query.contains("jan linders") -> "Supermarket"
            
            query.contains("gamma") || query.contains("karwei") || query.contains("praxis") || 
            query.contains("hornbach") || query.contains("ikea") || query.contains("hubo") ||
            query.contains("intratuin") || query.contains("welkoop") || query.contains("kwantum") ||
            query.contains("leen bakker") || query.contains("jysk") -> "Home & DIY"
            
            query.contains("kruidvat") || query.contains("etos") || query.contains("trekpleister") || 
            query.contains("da") || query.contains("boots") || query.contains("douglas") || 
            query.contains("ici paris") -> "Health & Beauty"
            
            query.contains("gall") || query.contains("drank") -> "Liquor"
            
            query.contains("mediamarkt") || query.contains("coolblue") || query.contains("expert") -> "Electronics"
            
            query.contains("h&m") || query.contains("c&a") || query.contains("zara") || 
            query.contains("primark") || query.contains("we fashion") || query.contains("zeeman") || 
            query.contains("wibra") || query.contains("bristol") || query.contains("scapino") -> "Fashion"
            
            query.contains("shell") || query.contains("bp") || query.contains("total") || 
            query.contains("esso") || query.contains("texaco") -> "Fuel"
            
            else -> "Other"
        }
    }
}
