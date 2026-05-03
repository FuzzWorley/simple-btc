package com.bitcoinportfolio.ui.portfolio

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitcoinportfolio.data.secure.SecureStorage
import com.bitcoinportfolio.domain.model.ChartMode
import com.bitcoinportfolio.domain.model.Currency
import com.bitcoinportfolio.domain.model.DataResult
import com.bitcoinportfolio.domain.model.Portfolio
import com.bitcoinportfolio.domain.model.PricePoint
import com.bitcoinportfolio.domain.model.TimeRange
import com.bitcoinportfolio.domain.usecase.AuthUseCase
import com.bitcoinportfolio.domain.usecase.GetPortfolioValueUseCase
import com.bitcoinportfolio.domain.usecase.GetPriceHistoryUseCase
import com.bitcoinportfolio.domain.usecase.UpdatePortfolioAmountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PortfolioUiState(
    val portfolio: Portfolio? = null,
    val priceHistory: List<PricePoint> = emptyList(),
    val selectedCurrency: Currency = Currency.USD,
    val selectedChartMode: ChartMode = ChartMode.PORTFOLIO_VALUE,
    val selectedTimeRange: TimeRange = TimeRange.ONE_MONTH,
    val isStale: Boolean = false,
    val staleAsOf: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdatedMs: Long? = null,
    val isEditingAmount: Boolean = false,
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPortfolioValueUseCase: GetPortfolioValueUseCase,
    private val getPriceHistoryUseCase: GetPriceHistoryUseCase,
    private val updatePortfolioAmountUseCase: UpdatePortfolioAmountUseCase,
    private val authUseCase: AuthUseCase,
    private val secureStorage: SecureStorage,
) : ViewModel() {

    val isAuthenticated: Boolean = savedStateHandle["isAuthenticated"] ?: false

    // MutableStateFlow so amount edits restart the portfolio collection via flatMapLatest.
    // Spoofed sessions generate a fresh spoof; authenticated sessions use real stored amount.
    private val currentBtcAmount = MutableStateFlow(
        if (isAuthenticated) secureStorage.getBtcAmount() ?: 0.0
        else authUseCase.generateAndStoreSpoofAmount()
    )

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    init {
        collectPortfolio()
        collectHistory()
    }

    private fun collectPortfolio() {
        viewModelScope.launch {
            currentBtcAmount
                .flatMapLatest { amount -> getPortfolioValueUseCase(amount) }
                .collect { result ->
                    when (result) {
                        DataResult.Loading -> _uiState.update {
                            it.copy(isLoading = it.portfolio == null)
                        }
                        is DataResult.Error -> _uiState.update {
                            it.copy(error = result.message, isLoading = false)
                        }
                        is DataResult.Success -> _uiState.update {
                            it.copy(
                                portfolio = result.data,
                                isStale = result.isStale,
                                staleAsOf = result.staleAsOf,
                                isLoading = false,
                                error = null,
                                lastUpdatedMs = if (!result.isStale) System.currentTimeMillis() else it.lastUpdatedMs,
                            )
                        }
                    }
                }
        }
    }

    // Re-subscribes whenever selectedTimeRange or selectedCurrency changes.
    private fun collectHistory() {
        viewModelScope.launch {
            _uiState
                .map { it.selectedTimeRange to it.selectedCurrency }
                .distinctUntilChanged()
                .flatMapLatest { (timeRange, currency) ->
                    getPriceHistoryUseCase(timeRange, currency)
                }
                .collect { result ->
                    if (result is DataResult.Success) {
                        _uiState.update { it.copy(priceHistory = result.data) }
                    }
                }
        }
    }

    fun onCurrencyToggle(currency: Currency) =
        _uiState.update { it.copy(selectedCurrency = currency) }

    fun onChartModeToggle(mode: ChartMode) =
        _uiState.update { it.copy(selectedChartMode = mode) }

    fun onTimeRangeSelected(range: TimeRange) =
        _uiState.update { it.copy(selectedTimeRange = range) }

    fun onEditAmountRequested() = _uiState.update { it.copy(isEditingAmount = true) }

    fun onEditAmountDismissed() = _uiState.update { it.copy(isEditingAmount = false) }

    fun onEditAmountConfirmed(newAmount: Double) {
        if (isAuthenticated) {
            updatePortfolioAmountUseCase.execute(newAmount)
        }
        // Updating currentBtcAmount restarts the portfolio flow with the new amount.
        // Spoofed sessions never persist — the new value lives only in ViewModel memory.
        currentBtcAmount.value = newAmount
        _uiState.update { it.copy(isEditingAmount = false) }
    }
}
