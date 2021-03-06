package ru.krirll.optimalmaps.domain.entities

import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.domain.repository.PointRepository

class SavePointItemUseCase(
    private val repository: PointRepository
) {

    suspend operator fun invoke(item: PointItem) {
        repository.savePointItem(item)
    }
}