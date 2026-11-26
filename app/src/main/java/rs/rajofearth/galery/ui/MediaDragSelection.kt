package rs.rajofearth.galery.ui

internal class MediaDragSelection private constructor(
    private val orderedKeys: List<String>,
    private val indicesByKey: Map<String, Int>,
    private val anchorIndex: Int,
    private val initialSelection: Set<String>,
) {
    private var previousTargetIndex: Int? = null
    private var previousSelection: Set<String>? = null

    fun moveTo(targetKey: String): Set<String>? {
        val targetIndex = indicesByKey[targetKey] ?: return null
        if (targetIndex == previousTargetIndex) return null
        previousTargetIndex = targetIndex

        val selection = buildSet {
            addAll(initialSelection)
            for (index in minOf(anchorIndex, targetIndex)..maxOf(anchorIndex, targetIndex)) {
                add(orderedKeys[index])
            }
        }
        if (selection == previousSelection) return null
        previousSelection = selection
        return selection
    }

    companion object {
        fun start(
            orderedKeys: List<String>,
            anchorKey: String,
            initialSelection: Set<String>,
        ): MediaDragSelection? {
            val keys = orderedKeys.distinct()
            val indicesByKey = keys.withIndex().associate { (index, key) -> key to index }
            val anchorIndex = indicesByKey[anchorKey] ?: return null
            return MediaDragSelection(keys, indicesByKey, anchorIndex, initialSelection.toSet())
        }
    }
}
