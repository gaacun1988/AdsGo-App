package com.adsweb.proxismart

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {
    val client = createSupabaseClient(
        supabaseUrl = "https://supabase.com/dashboard/project/nvvpgkgpdapftbssyiuk",
        supabaseKey = "sb_publishable_LkSTWe-dfRgIRmlcWsMn2w_ypb3UrNr"
    ) {
        install(Postgrest)
    }
}