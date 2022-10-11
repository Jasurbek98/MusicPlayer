package uz.jsoft.mediaplayer.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.jsoft.mediaplayer.data.local.LocalStorage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class StorageModule {

    @Provides
    @Singleton
    fun getLocalStorage(): LocalStorage = LocalStorage.instance
}