package com.harrypotter.smartphone.data.model

import com.google.gson.annotations.SerializedName

// ── DLC ──────────────────────────────────────────────────────────────────────

data class DLCSummary(
    @SerializedName("dlc_id")    val dlcId: String,
    val title: String,
    val description: String,
    @SerializedName("max_score") val maxScore: Int
)

data class ListDLCsResponse(val dlcs: List<DLCSummary>)

// ── Scene ─────────────────────────────────────────────────────────────────────

data class ChoiceOption(val id: String, val text: String)

data class ChoiceConfig(val options: List<ChoiceOption>)

data class FreeTextConfig(
    val prompt: String,
    @SerializedName("max_chars") val maxChars: Int
)

data class SceneDecision(
    val mode: String,            // CHOICE | FREE_TEXT | EITHER
    val choice: ChoiceConfig?,
    @SerializedName("free_text") val freeText: FreeTextConfig?
)

data class Scene(
    @SerializedName("scene_id")      val sceneId: String,
    val order: Int,
    val title: String,
    val narrative: String,
    val decision: SceneDecision,
    @SerializedName("next_scene_id") val nextSceneId: String?
)

// ── Playthrough ───────────────────────────────────────────────────────────────

data class Playthrough(
    @SerializedName("playthrough_id")   val playthroughId: String,
    @SerializedName("player_uuid")      val playerUuid: String,
    @SerializedName("dlc_id")           val dlcId: String,
    @SerializedName("current_scene_id") val currentSceneId: String?,
    @SerializedName("total_score")      val totalScore: Int,
    val status: String,
    @SerializedName("started_at")       val startedAt: String = "",
    @SerializedName("ended_at")         val endedAt: String? = null,
    @SerializedName("ending_id")        val endingId: String? = null
)

data class StartPlaythroughRequest(
    @SerializedName("player_uuid") val playerUuid: String,
    @SerializedName("dlc_id")      val dlcId: String
)

data class StartPlaythroughResponse(val playthrough: Playthrough, val scene: Scene)

data class GetSceneResponse(val playthrough: Playthrough, val scene: Scene)

// ── Decision ──────────────────────────────────────────────────────────────────

data class ChoiceDecisionRequest(
    @SerializedName("scene_id")  val sceneId: String,
    val mode: String = "CHOICE",
    @SerializedName("choice_id") val choiceId: String
)

data class FreeTextDecisionRequest(
    @SerializedName("scene_id") val sceneId: String,
    val mode: String = "FREE_TEXT",
    val text: String
)

data class AIClassification(
    @SerializedName("category_id") val categoryId: String,
    val confidence: Double
)

data class SubmitDecisionResponse(
    @SerializedName("is_correct")          val isCorrect: Boolean,
    @SerializedName("delta_score")         val deltaScore: Int,
    @SerializedName("total_score")         val totalScore: Int,
    @SerializedName("narrative_feedback")  val narrativeFeedback: String,
    @SerializedName("ai_classification")   val aiClassification: AIClassification?,
    @SerializedName("next_scene")          val nextScene: Scene?,
    @SerializedName("playthrough_status")  val playthroughStatus: String
)

// ── Ending ────────────────────────────────────────────────────────────────────

data class DecisionLogEntry(
    @SerializedName("scene_id")  val sceneId: String,
    @SerializedName("mode_used") val modeUsed: String,
    @SerializedName("is_correct") val isCorrect: Boolean,
    val summary: String
)

data class GetEndingResponse(
    @SerializedName("ending_id")         val endingId: String,
    val title: String,
    val narrative: String,
    val summary: String,
    @SerializedName("total_score")       val totalScore: Int,
    @SerializedName("decisions_overview") val decisionsOverview: List<DecisionLogEntry>
)

// ── Hermione ──────────────────────────────────────────────────────────────────

data class HermioneQueryRequest(
    @SerializedName("player_uuid")    val playerUuid: String,
    val query: String,
    @SerializedName("playthrough_id") val playthroughId: String?
)

data class HermioneSource(
    @SerializedName("lore_id") val loreId: String,
    val title: String
)

data class HermioneQueryResponse(val answer: String, val sources: List<HermioneSource>)
