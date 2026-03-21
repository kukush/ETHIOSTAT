package com.ethiostat.app.domain.model

data class AccountSource(
    val id: Long = 0,
    val name: String,
    val type: AccountSourceType,
    val phoneNumber: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Canonical registry of all supported Ethiopian financial platforms.
 *
 * Each entry carries the SMS short code ([shortCode]) and optional USSD dial code ([ussdCode]).
 * This is the single source of truth — no external mapping tables needed.
 *
 * Sorted alphabetically by [displayName] via [ALL_SOURCES].
 * First 4 defaults are pre-seeded on fresh install via [DEFAULT_SOURCES].
 *
 * To add a new platform: add an entry here and add SMS parser support.
 * To disable a platform: either delete the DB row or set isEnabled = false.
 */
enum class AccountSourceType(
    val displayName: String,
    val shortCode: String,     // SMS originating address / sender number
    val ussdCode: String = ""  // USSD dial code, e.g. *127#
) {
    TELEBIRR     ("Telebirr",                    "127",    "*127#"),
    MPESA        ("M-PESA",                      "701",    "*701#"),
    @Deprecated("Removed", ReplaceWith("UNKNOWN"))
    AMOLE        ("Amole (Dashen Bank)",          "842",    "*842#"),
    AWASH_BIRR   ("Awash Birr",                  "901",    "*901#"),
    HELLOCASH    ("Hellocash",                   "803",    "*803#"),
    KIFIYA       ("Kifiya",                      "732",    "*732#"),
    BERHAN_BANK  ("Berhan Bank",                 "816",    "*816#"),
    BUNNA_BANK   ("Bunna Bank",                  "802",    "*802#"),
    LIB          ("Lion International Bank",     "801",    "*801#"),
    AMHARA_BANK  ("Amhara Bank",                 "827",    "*827#"),
    GOH_BANK     ("Goh Betoch Bank",             "855",    "*855#"),
    OROMIA_BANK  ("Oromia Bank",                 "840",    "*840#"),
    SIINQEE_BANK ("Siinqee Bank",                "871",    "*871#"),
    CBO          ("Coop Bank of Oromia",         "841",    "*841#"),
    CBE         ("Commercial Bank of Ethiopia", "889",    "*889#"),
    BOA        ("Bank of Abyssinia",           "815",    "*815#"),
    GADAA_BANK   ("Gadaa Bank",                  "851",    "*851#"),
    TELECOM      ("Ethio Telecom",               "251994", "*804#"),

    /** Legacy fallback — existing DB rows using old enum names map here */
    @Deprecated("Use specific type", ReplaceWith("AWASH_BIRR"))
    BANK_AWASH   ("Awash Bank (legacy)",         "901",    "*901#"),
    @Deprecated("Use specific type", ReplaceWith("UNKNOWN"))
    BANK_OTHER   ("Other Bank (legacy)",         "",       ""),

    UNKNOWN      ("Unknown",                     "",       "");

    companion object {
        /**
         * The 6 sources pre-seeded on a fresh install.
         * Sorted to appear first in the source tabs.
         */
        val DEFAULT_SOURCES: List<AccountSourceType> =
            listOf(TELEBIRR, CBE, BOA, AWASH_BIRR, AMOLE, HELLOCASH)

        /**
         * All non-deprecated, non-unknown sources the user can pick from.
         * Sorted alphabetically for the Add Source picker.
         */
        val ALL_SOURCES: List<AccountSourceType> = values()
            .filter { it != UNKNOWN && it != BANK_AWASH && it != BANK_OTHER }
            .sortedBy { it.displayName }

        /** Lookup by SMS originating address (short code). */
        fun fromShortCode(sender: String): AccountSourceType =
            ALL_SOURCES.firstOrNull {
                it.shortCode.isNotEmpty() &&
                sender.contains(it.shortCode, ignoreCase = true)
            } ?: UNKNOWN
    }
}

data class TransactionSource(
    val sourceId: Long,
    val sourceName: String,
    val sourceType: AccountSourceType,
    val phoneNumber: String
)
