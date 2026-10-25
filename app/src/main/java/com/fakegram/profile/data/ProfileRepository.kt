package com.fakegram.profile.data

import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val db: AppDatabase) {

    val profiles: Flow<List<ProfileEntity>> = db.profileDao().observeAll()

    fun observeProfile(id: Long): Flow<ProfileEntity?> = db.profileDao().observeById(id)
    fun observePills(profileId: Long): Flow<List<PillEntity>> = db.pillDao().observeForProfile(profileId)
    fun observeFollowedBy(profileId: Long): Flow<List<FollowedByEntity>> = db.followedByDao().observeForProfile(profileId)
    fun observeHighlights(profileId: Long): Flow<List<HighlightEntity>> = db.highlightDao().observeForProfile(profileId)
    fun observePosts(profileId: Long): Flow<List<PostEntity>> = db.postDao().observeForProfile(profileId)
    fun observeCarousel(postId: Long): Flow<List<CarouselItemEntity>> = db.carouselItemDao().observeForPost(postId)

    suspend fun getProfile(id: Long): ProfileEntity? = db.profileDao().getById(id)

    suspend fun createProfile(): Long = db.profileDao().insert(ProfileEntity())

    suspend fun updateProfile(profile: ProfileEntity) {
        db.profileDao().update(profile.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProfile(id: Long) = db.profileDao().deleteById(id)

    suspend fun duplicateProfile(id: Long): Long? {
        val source = db.profileDao().getById(id) ?: return null
        val newId = db.profileDao().insert(
            source.copy(
                id = 0,
                username = source.username + "_copy",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        for (p in db.pillDao().getForProfile(id)) {
            db.pillDao().insert(p.copy(id = 0, profileId = newId))
        }
        for (f in db.followedByDao().getForProfile(id)) {
            db.followedByDao().insert(f.copy(id = 0, profileId = newId))
        }
        for (h in db.highlightDao().getForProfile(id)) {
            db.highlightDao().insert(h.copy(id = 0, profileId = newId))
        }
        for (post in db.postDao().getForProfile(id)) {
            val newPostId = db.postDao().insert(post.copy(id = 0, profileId = newId))
            for (c in db.carouselItemDao().getForPost(post.id)) {
                db.carouselItemDao().insert(c.copy(id = 0, postId = newPostId))
            }
        }
        return newId
    }

    // Pills
    suspend fun addPill(pill: PillEntity): Long = db.pillDao().insert(pill)
    suspend fun updatePill(pill: PillEntity) = db.pillDao().update(pill)
    suspend fun deletePill(pill: PillEntity) = db.pillDao().delete(pill)

    // Followed by
    suspend fun addFollowedBy(entry: FollowedByEntity): Long = db.followedByDao().insert(entry)
    suspend fun updateFollowedBy(entry: FollowedByEntity) = db.followedByDao().update(entry)
    suspend fun deleteFollowedBy(id: Long) = db.followedByDao().deleteById(id)

    // Highlights
    suspend fun addHighlight(highlight: HighlightEntity): Long = db.highlightDao().insert(highlight)
    suspend fun updateHighlight(highlight: HighlightEntity) = db.highlightDao().update(highlight)
    suspend fun deleteHighlight(id: Long) = db.highlightDao().deleteById(id)

    // Posts
    suspend fun addPost(post: PostEntity): Long = db.postDao().insert(post)
    suspend fun updatePost(post: PostEntity) = db.postDao().update(post)
    suspend fun deletePost(id: Long) = db.postDao().deleteById(id)
    suspend fun getPost(id: Long): PostEntity? = db.postDao().getById(id)
    suspend fun getPostsForProfile(profileId: Long): List<PostEntity> = db.postDao().getForProfile(profileId)

    suspend fun addCarouselItem(item: CarouselItemEntity): Long = db.carouselItemDao().insert(item)
    suspend fun getCarouselForPost(postId: Long): List<CarouselItemEntity> = db.carouselItemDao().getForPost(postId)
    suspend fun deleteCarouselForPost(postId: Long) = db.carouselItemDao().deleteForPost(postId)
}
