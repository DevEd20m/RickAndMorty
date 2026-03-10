package com.example.rickandmorty.feature.characters.data.mapper

import com.example.rickandmorty.core.network.dto.CharacterDto
import com.example.rickandmorty.feature.characters.domain.model.Character
import com.example.rickandmorty.feature.characters.domain.model.CharacterStatus

fun CharacterDto.toDomain(): Character = Character(
    id = id,
    name = name,
    status = CharacterStatus.fromString(status),
    imageUrl = image
)
