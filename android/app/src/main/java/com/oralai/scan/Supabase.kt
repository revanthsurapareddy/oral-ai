package com.oralai.scan

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlin.time.Duration.Companion.seconds

val supabase = createSupabaseClient(
    supabaseUrl = "https://gduqgsxwcnrzdjqkextl.supabase.co",
    supabaseKey = "sb_publishable_1V-1Pqu_6ZKe4I3MDadz1w_0fUURFdo"
) {
    requestTimeout = 60.seconds
    install(Auth)
    install(Postgrest)
}
