package ru.krirll.optimalmaps.domain.entities

import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.domain.repository.LocalRepository

class SavePointItemUseCase(
    private val repository: LocalRepository
) {

    suspend operator fun invoke(item: PointItem) {
        repository.savePointItem(item)
    }
}