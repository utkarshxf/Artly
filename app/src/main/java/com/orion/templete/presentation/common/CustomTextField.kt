package com.orion.templete.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.annotation.StringRes
import com.orion.templete.R
import com.orion.templete.presentation.ui.theme.LighterGray
import com.orion.templete.presentation.ui.theme.TempleteTheme

data class Country(
    val name: String,
    val code: String,
    val flagRes: Int
)

val countries = listOf(
    Country("India", "+91", R.drawable.flag_india),
    Country("United States", "+1", R.drawable.flag_united_states),
    Country("United Kingdom", "+44", R.drawable.flag_uk),
    Country("Canada", "+1", R.drawable.flag_canada),
    Country("Australia", "+61", R.drawable.flag_australia),
    Country("Germany", "+49", R.drawable.flag_germany),
    Country("China", "+86", R.drawable.flag_china),
    Country("Japan", "+81", R.drawable.flag_japan),
    Country("France", "+33", R.drawable.flag_france),
    Country("Spain", "+34", R.drawable.flag_spain),
    Country("Italy", "+39", R.drawable.flag_italy),
    Country("Brazil", "+55", R.drawable.flag_brazil),
    Country("Russia", "+7", R.drawable.flag_russia),
    Country("South Africa", "+27", R.drawable.flag_south_agrica),
    Country("Singapore", "+65", R.drawable.flag_singapore)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPasswordTextField: Boolean = false,
    isSingleLine: Boolean = true,
    @StringRes hint: Int,
    countrySelected: (Country) -> Unit = {},
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf(countries[0]) } // Default to India

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = keyboardType
        ),
        prefix = {
            if (keyboardType == KeyboardType.Phone) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showCountryPicker = true }
                ) {
                    Image(
                        painter = painterResource(id = selectedCountry.flagRes),
                        contentDescription = "Country flag",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = " ${selectedCountry.code} - ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        singleLine = isSingleLine,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = if (isSystemInDarkTheme()) {
                MaterialTheme.colorScheme.background
            } else {
                LighterGray
            },
            unfocusedContainerColor = if (isSystemInDarkTheme()) {
                MaterialTheme.colorScheme.background
            } else {
                LighterGray
            },
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent
        ),
        trailingIcon = when {
            // Priority 1: Custom trailing icon from caller (for optional fields with close button)
            trailingIcon != null -> trailingIcon
            // Priority 2: Password visibility toggle
            isPasswordTextField -> {
                {
                    PasswordEyeIcon(isPasswordVisible = isPasswordVisible) {
                        isPasswordVisible = !isPasswordVisible
                    }
                }
            }
            // Priority 3: No trailing icon
            else -> null
        },
        visualTransformation = if (isPasswordTextField) {
            if (isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            }
        } else {
            VisualTransformation.None
        },
        placeholder = {
            Text(text = stringResource(id = hint), style = MaterialTheme.typography.bodyMedium)
        },
        shape = MaterialTheme.shapes.medium
    )

    if (showCountryPicker) {
        CountryPickerDialog(
            onDismiss = { showCountryPicker = false },
            onCountrySelected = {
                selectedCountry = it
                countrySelected(it)
                showCountryPicker = false
            }
        )
    }
}

@Composable
fun CountryPickerDialog(
    onDismiss: () -> Unit,
    onCountrySelected: (Country) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Country",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn {
                    items(countries) { country ->
                        CountryItem(country) {
                            onCountrySelected(country)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CountryItem(country: Country, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = country.flagRes),
            contentDescription = country.name,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(text = country.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = country.code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun PasswordEyeIcon(
    isPasswordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit
) {
    val image = if (isPasswordVisible) {
        painterResource(id = R.drawable.show_eye_icon_filled)
    } else {
        painterResource(id = R.drawable.hide_eye_icon_filled)
    }

    IconButton(onClick = onPasswordVisibilityToggle) {
        Icon(painter = image, contentDescription = null)
    }
}

@Preview
@Composable
fun CustomTextFieldPreview() {
    TempleteTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CustomTextField(
                value = "",
                onValueChange = {},
                hint = androidx.compose.ui.R.string.default_error_message,
                keyboardType = KeyboardType.Phone
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomTextField(
                value = "",
                onValueChange = {},
                hint = androidx.compose.ui.R.string.default_error_message,
                isPasswordTextField = true
            )
        }
    }
}
