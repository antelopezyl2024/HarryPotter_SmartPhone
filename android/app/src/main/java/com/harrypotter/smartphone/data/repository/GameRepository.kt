package com.harrypotter.smartphone.data.repository

import com.harrypotter.smartphone.BuildConfig
import com.harrypotter.smartphone.data.model.*
import com.harrypotter.smartphone.data.network.NetworkModule
import com.harrypotter.smartphone.data.network.MockData

class GameRepository {
    private val api = NetworkModule.api

    suspend fun listDLCs(): ListDLCsResponse {
        return if (BuildConfig.USE_MOCK) {
            ListDLCsResponse(dlcs = MockData.dlcs)
        } else {
            api.listDLCs()
        }
    }

    suspend fun startPlaythrough(playerUuid: String, dlcId: String) =
        api.startPlaythrough(StartPlaythroughRequest(playerUuid, dlcId))

    suspend fun getCurrentScene(playthroughId: String) =
        api.getCurrentScene(playthroughId)

    suspend fun submitChoice(playthroughId: String, sceneId: String, choiceId: String) =
        api.submitChoiceDecision(playthroughId, ChoiceDecisionRequest(sceneId, choiceId = choiceId))

    suspend fun submitFreeText(playthroughId: String, sceneId: String, text: String) =
        api.submitFreeTextDecision(playthroughId, FreeTextDecisionRequest(sceneId, text = text))

    suspend fun getEnding(playthroughId: String) =
        api.getEnding(playthroughId)

    suspend fun hermioneQuery(playerUuid: String, query: String, playthroughId: String?) =
        api.hermioneQuery(HermioneQueryRequest(playerUuid, query, playthroughId))
}
