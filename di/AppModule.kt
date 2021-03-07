package com.luo.face2.di

import com.luo.face2.adapters.FaceListAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

/**
 * @author: Luo-DH
 * @date: 3/7/21
 */
@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideFaceListAdapter() = FaceListAdapter()

}