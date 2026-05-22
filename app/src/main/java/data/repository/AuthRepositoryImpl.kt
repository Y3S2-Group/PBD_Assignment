package data.repository

import domain.repository.AuthRepository
import domain.repository.AuthUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {
    override val currentUser: AuthUser? = AuthUser(uid = "demo-user")
}
