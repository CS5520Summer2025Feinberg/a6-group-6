package edu.northeastern.a6_group6
import com.apollographql.apollo.ApolloClient

object Apollo {
    val apolloClient: ApolloClient by lazy {
        ApolloClient.Builder()
            .serverUrl("https://graphql.pokeapi.co/v1beta2")
            .build()
    }
}