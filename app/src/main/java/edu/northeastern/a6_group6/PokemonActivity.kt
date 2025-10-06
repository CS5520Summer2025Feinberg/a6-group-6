package edu.northeastern.a6_group6

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.northeastern.a6_group6.ui.theme.A6Group6Theme

data class PokemonData(
    val name: String,
    val imageUrl: String? = null,
    val cryUrl: String? = null
)

class PokemonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            A6Group6Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PokemonScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PokemonScreen(modifier: Modifier = Modifier) {
    var showResults by remember { mutableStateOf(false) }
    var pokemonData by remember { mutableStateOf<PokemonData?>(null) }

    if (showResults && pokemonData != null) {
        PokemonResultsScreen(
            modifier = modifier,
            pokemonData = pokemonData!!,
            onSearchAgain = {
                showResults = false
                pokemonData = null
            }
        )
    } else {
        PokemonSearchScreen(
            modifier = modifier,
            onSearch = { name, image, cry ->
                val fakeData = performGraphQlQuery(name, image, cry)
                pokemonData = fakeData
                showResults = true
            }
        )
    }
}

private fun performGraphQlQuery(name: String, includeImage: Boolean, includeCry: Boolean): PokemonData {
    Log.d("GraphQL", "Searching for: $name, Image: $includeImage, Cry: $includeCry")
    return PokemonData(
        name = name.replaceFirstChar { it.uppercase() },
        imageUrl = if (includeImage) "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png" else null, // Fake Pikachu image URL
        cryUrl = if (includeCry) "https://raw.githubusercontent.com/PokeAPI/cries/main/cries/pokemon/latest/25.ogg" else null // Fake Pikachu cry URL
    )
}

@Composable
fun PokemonSearchScreen(modifier: Modifier = Modifier, onSearch: (name: String, includeImage: Boolean, includeCry: Boolean) -> Unit) {
    var pokemonName by remember { mutableStateOf("") }
    var includeImage by remember { mutableStateOf(false) }
    var includeCry by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Search for a Pokémon")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pokemonName,
            onValueChange = { pokemonName = it},
            label = {Text("Pokemon Name")}
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = includeImage,
                    onCheckedChange = { includeImage = it },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Image")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = includeCry,
                    onCheckedChange = { includeCry = it },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pokémon Cry")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSearch(pokemonName, includeImage, includeCry) },
            enabled = pokemonName.isNotBlank()
        ) {
            Text("Search")
        }
    }
}

@Composable
fun PokemonResultsScreen(
    modifier: Modifier = Modifier,
    pokemonData: PokemonData,
    onSearchAgain: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Name: ${pokemonData.name}")
        Spacer(modifier = Modifier.height(8.dp))
        if (pokemonData.imageUrl != null) {
            Text("Image will be shown here. URL: ${pokemonData.imageUrl}")
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (pokemonData.cryUrl != null) {
            Button(onClick = {}) {
                Text("Play Pokémon Cry")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Button(onClick = onSearchAgain) {
            Text("Search Again")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PokemonSearchScreenPreview() {
    A6Group6Theme {
        PokemonSearchScreen(onSearch = { _, _, _ -> })
    }
}

@Preview(showBackground = true)
@Composable
fun PokemonResultsScreenPreview() {
    A6Group6Theme {
        val sampleData = PokemonData(
            name = "Pikachu",
            imageUrl = "some-url.png",
            cryUrl = "some-url.ogg"
        )
        PokemonResultsScreen(pokemonData = sampleData, onSearchAgain = {})
    }
}