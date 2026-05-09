package com.harrypotter.smartphone.data.api

import com.harrypotter.smartphone.data.model.*
import retrofit2.http.*

interface HarryPotterApi {

    @GET("dlcs")
    suspend fun listDLCs(): ListDLCsResponse

    @POST("playthroughs")
    suspend fun startPlaythrough(@Body request: StartPlaythroughRequest): StartPlaythroughResponse

    @GET("playthroughs/{id}/scene")
    suspend fun getCurrentScene(@Path("id") playthroughId: String): GetSceneResponse

    @POST("playthroughs/{id}/decisions")
    suspend fun submitChoiceDecision(
        @Path("id") playthroughId: String,
        @Body request: ChoiceDecisionRequest
    ): SubmitDecisionResponse

    @POST("playthroughs/{id}/decisions")
    suspend fun submitFreeTextDecision(
        @Path("id") playthroughId: String,
        @Body request: FreeTextDecisionRequest
    ): SubmitDecisionResponse

    @GET("playthroughs/{id}/ending")
    suspend fun getEnding(@Path("id") playthroughId: String): GetEndingResponse

    @POST("hermione/query")
    suspend fun hermioneQuery(@Body request: HermioneQueryRequest): HermioneQueryResponse
}
