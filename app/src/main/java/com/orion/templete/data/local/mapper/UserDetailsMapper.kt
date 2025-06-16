
import com.orion.templete.data.local.entity.UserEntity
import com.orion.templete.data.model.user_model.UserDTO

// Extension function to convert UserDTO to UserEntity
fun UserDTO.toEntity(): UserEntity {
    return UserEntity(
        _id = this.id,
        name = this.name,
        profilePicture = this.profilePicture,
        dob = this.dob,
        gender = this.gender,
        language = this.language,
        countryIso2 = this.countryIso2,
        follow = this.follow
    )
}

// Extension function to convert UserEntity to UserDTO
fun UserEntity.toDto(): UserDTO {
    return UserDTO(
        id = this._id,
        name = this.name,
        profilePicture = this.profilePicture,
        dob = this.dob,
        gender = this.gender,
        language = this.language,
        countryIso2 = this.countryIso2,
        follow = this.follow,
        artist = true // fix is needed currently not working perfectly.
    )
}

// Extension function to convert List of UserDTO to List of UserEntity
fun List<UserDTO>.toEntityList(): List<UserEntity> {
    return this.map { it.toEntity() }
}

// Extension function to convert List of UserEntity to List of UserDTO
fun List<UserEntity>.toDtoList(): List<UserDTO> {
    return this.map { it.toDto() }
}

// Companion object for creating UserEntity from individual fields
object UserEntityFactory {
    fun create(
        id: String,
        name: String,
        profilePicture: String,
        dob: String,
        gender: String,
        language: String,
        countryIso2: String,
        follow:Boolean,
    ): UserEntity {
        return UserEntity(
            _id = id,
            name = name,
            profilePicture = profilePicture,
            dob = dob,
            gender = gender,
            language = language,
            countryIso2 = countryIso2,
            follow = follow
        )
    }
}