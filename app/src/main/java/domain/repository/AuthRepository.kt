package domain.repository

data class AuthUser(val uid: String, val email: String = "")

interface AuthRepository {
    val currentUser: AuthUser?
}
