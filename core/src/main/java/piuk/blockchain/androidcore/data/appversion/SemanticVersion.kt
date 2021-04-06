package piuk.blockchain.androidcore.data.appversion

data class SemanticVersion(val patch: Int, val major: Int, val minor: Int) {

    operator fun compareTo(other: SemanticVersion): Int = when {
        patch != other.patch -> patch - other.patch
        major != other.major -> major - other.major
        else -> minor - other.minor
    }

    companion object {
        operator fun invoke(latestVersion: String): SemanticVersion {
            return latestVersion.takeIf { it.split(".").size == 3 }?.let {
                return@let SemanticVersion(latestVersion.split(".")[0].toIntOrNull() ?: 0,
                    latestVersion.split(".")[1].toIntOrNull() ?: 0,
                    latestVersion.split(".")[2].toIntOrNull() ?: 0)
            } ?: return SemanticVersion(0, 0, 0)
        }
    }
}