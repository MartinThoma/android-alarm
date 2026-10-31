package com.martin_thoma.drift.feature.ringing

import android.content.Context
import android.content.Intent
import com.martin_thoma.drift.core.ringing.RingingScreen
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

internal class RingingActivityScreen
    @Inject
    constructor() : RingingScreen {
        override fun intent(context: Context): Intent =
            Intent(context, RingingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RingingScreenModule {
    @Binds
    abstract fun ringingScreen(screen: RingingActivityScreen): RingingScreen
}
