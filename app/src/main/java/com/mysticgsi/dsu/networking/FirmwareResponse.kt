package com.mysticgsi.dsu.networking

data class FirmwareResponse(
    val include: List<String>,
    val images: List<Image>
)

data class Image(
    val name: String,
    val os_version: String,
    val cpu_abi: String,
    val details: String,
    val spl: String,
    val tos: String,
    val uri: String,
    val desc: String
)