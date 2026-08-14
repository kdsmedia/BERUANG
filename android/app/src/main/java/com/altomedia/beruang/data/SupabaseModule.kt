package com.altomedia.beruang.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    // Supabase project credentials (publishable/anon key only — safe for the client).
    private const val SUPABASE_URL = "https://blbszehspckqjpfeaqqp.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_aaifz_Dx5zhsrPu_pilspw_a4mzc-lc"

    @Provides @Singleton
    fun client(): SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY,
    ) {
        install(Postgrest)
        install(Auth)
        install(Realtime)
    }

    // The ViewModels inject Auth/Realtime directly (each uses only one, so the
    // Postgrest+Auth-together KSP resolution quirk doesn't apply).
    @Provides @Singleton fun auth(client: SupabaseClient): Auth = client.auth
    @Provides @Singleton fun realtime(client: SupabaseClient): Realtime = client.realtime
}

