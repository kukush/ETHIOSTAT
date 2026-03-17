package com.ethiostat.app.domain.usecase

import com.ethiostat.app.domain.model.AppLanguage
import com.ethiostat.app.domain.repository.IEthioStatRepository

class ChangeLanguageUseCase(
    private val repository: IEthioStatRepository
) {
    suspend operator fun invoke(language: AppLanguage) {
        val config = repository.getConfigOnce() ?: return
        val updatedConfig = config.copy(appLanguage = language.code)
        repository.updateConfig(updatedConfig)
    }
}
