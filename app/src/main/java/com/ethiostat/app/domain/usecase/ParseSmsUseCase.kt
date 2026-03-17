package com.ethiostat.app.domain.usecase

import com.ethiostat.app.domain.model.ParsedSmsData
import com.ethiostat.app.domain.repository.IEthioStatRepository

class ParseSmsUseCase(
    private val repository: IEthioStatRepository
) {
    suspend operator fun invoke(sender: String, body: String, timestamp: Long): ParsedSmsData {
        return repository.processSms(sender, body, timestamp)
    }
}
