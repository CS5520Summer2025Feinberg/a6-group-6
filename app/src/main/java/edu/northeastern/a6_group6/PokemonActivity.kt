package edu.northeastern.a6_group6

import android.media.AudioAttributes
import android.media.MediaPlayer
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import edu.northeastern.a6_group6.ui.theme.A6Group6Theme
import java.io.IOException
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.apollographql.apollo.exception.ApolloException
import edu.northeastern.a6_group6.graphql.PokemonQuery
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.text.firstOrNull
import kotlin.text.lowercase

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
        val coroutineScope = rememberCoroutineScope()
        PokemonSearchScreen(
            modifier = modifier,
            onSearch = { name, image, cry ->
                coroutineScope.launch {
                    val realData = performGraphQlQuery(name, image, cry)
                    pokemonData = realData
                    showResults = true
                }
            }
        )
    }
}

private suspend fun performGraphQlQuery(
    name: String,
    includeImage: Boolean,
    includeCry: Boolean
): PokemonData? {
    Log.d("GraphQL", "Searching for: $name")
    try {
        val response = Apollo.apolloClient.query(PokemonQuery(name = name.lowercase())).execute()

        val firstPokemon = response.data?.pokemon?.firstOrNull()
        if (firstPokemon == null) {
            Log.w("GraphQL", "Success but no data for '$name'. Errors: ${response.errors}")
            return null
        }

        val imageUrl = if (includeImage) {
            val spritesAny = firstPokemon.pokemonsprites.firstOrNull()?.sprites
            extractFrontSpriteUrl(spritesAny)
        } else null

        val cryUrl = if (includeCry) {
            val criesAny = firstPokemon.pokemoncries.firstOrNull()?.cries
            extractLatestCryUrl(criesAny)
        } else null

        Log.d("GraphQL", "Final Image URL: $imageUrl")
        Log.d("GraphQL", "Final Cry URL: $cryUrl")

        return PokemonData(
            name = firstPokemon.name.replaceFirstChar { it.uppercase() },
            imageUrl = imageUrl,
            cryUrl = cryUrl
        )
    } catch (e: ApolloException) {
        Log.e("GraphQL", "Failure", e)
        return null
    }
}

private fun extractFrontSpriteUrl(spritesAny: Any?): String? {
    when (spritesAny) {
        is String -> {
            return try {
                val obj = JSONObject(spritesAny)
                obj.optString("front_default").takeIf { it.isNotBlank() }
                    ?: obj.optJSONObject("other")?.optJSONObject("official-artwork")
                        ?.optString("front_default")?.takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }
        is Map<*, *> -> {
            (spritesAny["front_default"] as? String)?.let { if (it.isNotBlank()) return it }

            val other = spritesAny["other"] as? Map<*, *>
            val official = other?.get("official-artwork") as? Map<*, *>
            val art = official?.get("front_default") as? String
            if (!art.isNullOrBlank()) return art
        }
    }
    return null
}

private fun extractLatestCryUrl(criesAny: Any?): String? {
    when (criesAny) {
        is String -> {
            return try {
                JSONObject(criesAny).optString("latest").takeIf { it.isNotBlank() }
            } catch (_: Exception) { null }
        }
        is Map<*, *> -> {
            return (criesAny["latest"] as? String)?.takeIf { it.isNotBlank() }
        }
    }
    return null
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
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Name: ${pokemonData.name}", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (pokemonData.imageUrl != null) {
            AsyncImage(
                model = pokemonData.imageUrl,
                contentDescription = "${pokemonData.name} Image",
                modifier = Modifier.size(128.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (pokemonData.cryUrl != null) {
            Button(
                onClick = {
                    if (!isPlaying) {
                        isPlaying = true
                        playPokemonCry(
                            url = pokemonData.cryUrl,
                            onCompletion = { isPlaying = false },
                            onError = { isPlaying = false }
                        )
                    }
            },
                enabled = !isPlaying) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Pokémon Cry"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Pokémon Cry")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(onClick = onSearchAgain) {
            Text("Search Again")
        }
    }
}

private fun playPokemonCry(url: String, onCompletion: () -> Unit, onError: () -> Unit) {
    val mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        try {
            setDataSource(url)
            prepareAsync() // Prepare the media player asynchronously
            setOnPreparedListener {
                Log.d("MediaPlayer", "Media prepared, starting playback.")
                start() // Start playback once prepared
            }
            setOnCompletionListener {
                Log.d("MediaPlayer", "Playback completed.")
                it.release() // Release resources
                onCompletion()
            }
            setOnErrorListener { _, _, _ ->
                Log.e("MediaPlayer", "Error during playback.")
                release() // Release resources on error
                onError()
                true // Indicate that the error has been handled
            }
        } catch (e: IOException) {
            Log.e("MediaPlayer", "Failed to set data source", e)
            release()
            onError()
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