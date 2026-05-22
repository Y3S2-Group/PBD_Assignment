package presentation.income

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import data.model.Currency
import data.model.IncomeSource
import presentation.income.components.CurrencySelector
import presentation.income.components.SourceSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeScreen(
    onNavigateBack: () -> Unit,
    viewModel: IncomeViewModel = hiltViewModel()
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(IncomeSource.SALARY) }
    var selectedCurrency by remember { mutableStateOf(Currency.LKR) }
    val addState by viewModel.addIncomeState.collectAsState()

    LaunchedEffect(addState) {
        if (addState is AddIncomeState.Success) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Income") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            CurrencySelector(
                selectedCurrency = selectedCurrency,
                onCurrencySelected = { selectedCurrency = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SourceSelector(
                selectedSource = selectedSource,
                onSourceSelected = { selectedSource = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let { value ->
                        viewModel.addIncome(
                            amount = value,
                            description = description,
                            source = selectedSource,
                            currency = selectedCurrency
                        )
                    }
                },
                enabled = amount.toDoubleOrNull() != null && description.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Income")
            }
        }
    }
}
