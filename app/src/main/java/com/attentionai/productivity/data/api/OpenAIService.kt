package com.attentionai.productivity.data.api

import com.attentionai.productivity.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIService {
    
    @POST("v1/chat/completions")
    suspend fun generateResponse(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatRequest
    ): Response<ChatResponse>
    
    @POST("v1/chat/completions")
    suspend fun generateStreamingResponse(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatRequest
    ): Response<ChatResponse>
}





