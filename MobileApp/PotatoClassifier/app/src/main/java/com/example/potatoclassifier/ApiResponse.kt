/* ApiResponse.kt */
package com.example.potatoclassifier

data class ApiResponse(
    val prediction: String,
    val confidence: Double
)