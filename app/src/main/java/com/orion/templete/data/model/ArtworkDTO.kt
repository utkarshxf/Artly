package com.orion.templete.data.model


data class ArtworkDTO(
    val content: List<Content>
) {
    operator fun plus(items: List<Content>): ArtworkDTO {
        return ArtworkDTO(this.content + items)
    }
}