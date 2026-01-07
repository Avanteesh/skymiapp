package org.example.project.datamodels

interface PostAPIClient {
    suspend fun sendData(json: String) : Result<ServerResponse>
}