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
            for (c in db.commentDao().getForPost(post.id)) {
                db.commentDao().insert(c.copy(id = 0, postId = newPostId))
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

    // ---- Characters (outer shell) ----
    val characters: Flow<List<CharacterEntity>> = db.characterDao().observeAll()
    val scheduleEvents: Flow<List<ScheduleEventEntity>> = db.scheduleDao().observeAll()
    suspend fun getCharacter(id: Long): CharacterEntity? = db.characterDao().getById(id)
    suspend fun addCharacter(character: CharacterEntity): Long = db.characterDao().insert(character)
    suspend fun updateCharacter(character: CharacterEntity) = db.characterDao().update(character.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteCharacter(id: Long) = db.characterDao().deleteById(id)
    suspend fun addScheduleEvent(event: ScheduleEventEntity): Long = db.scheduleDao().insert(event)
    suspend fun updateScheduleEvent(event: ScheduleEventEntity) = db.scheduleDao().update(event)
    suspend fun scheduleFor(characterId: Long): List<ScheduleEventEntity> = db.scheduleDao().forCharacter(characterId)
    suspend fun deleteAutoEventsFrom(characterId: Long, fromDate: String) = db.scheduleDao().deleteAutoFrom(characterId, fromDate)
    suspend fun deleteScheduleEvent(id: Long) = db.scheduleDao().deleteById(id)

    // ---- Bubble chats ----
    val bubbleChats: Flow<List<BubbleChatEntity>> = db.bubbleDao().observeChats()
    val allBubbleMessages: Flow<List<BubbleMessageEntity>> = db.bubbleDao().observeAllMessages()
    fun observeBubbleChat(id: Long): Flow<BubbleChatEntity?> = db.bubbleDao().observeChat(id)
    fun observeBubbleMessages(chatId: Long): Flow<List<BubbleMessageEntity>> = db.bubbleDao().observeMessages(chatId)
    suspend fun getBubbleChat(id: Long): BubbleChatEntity? = db.bubbleDao().getChat(id)
    suspend fun createBubbleChat(): Long = db.bubbleDao().insertChat(BubbleChatEntity())
    suspend fun addBubbleChat(chat: BubbleChatEntity): Long = db.bubbleDao().insertChat(chat)
    suspend fun updateBubbleChat(chat: BubbleChatEntity) = db.bubbleDao().updateChat(chat.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteBubbleChat(id: Long) = db.bubbleDao().deleteChat(id)
    suspend fun addBubbleMessage(message: BubbleMessageEntity): Long = db.bubbleDao().insertMessage(message)
    suspend fun updateBubbleMessage(message: BubbleMessageEntity) = db.bubbleDao().updateMessage(message)
    suspend fun deleteBubbleMessage(id: Long) = db.bubbleDao().deleteMessage(id)
    suspend fun clearBubbleMessages(chatId: Long) = db.bubbleDao().clearMessages(chatId)

    suspend fun duplicateBubbleChat(id: Long): Long? {
        val source = db.bubbleDao().getChat(id) ?: return null
        val now = System.currentTimeMillis()
        val newId = db.bubbleDao().insertChat(source.copy(id = 0, title = source.title + " copy", createdAt = now, updatedAt = now))
        db.bubbleDao().getMessages(id).forEach { db.bubbleDao().insertMessage(it.copy(id = 0, chatId = newId)) }
        return newId
    }

    // ---- Saved accounts ----
    val accounts: Flow<List<AccountEntity>> = db.accountDao().observeAll()
    suspend fun addAccount(account: AccountEntity): Long = db.accountDao().insert(account)
    suspend fun updateAccount(account: AccountEntity) = db.accountDao().update(account)
    suspend fun deleteAccount(id: Long) = db.accountDao().deleteById(id)

    // ---- Comments ----
    fun observeComments(postId: Long): Flow<List<CommentEntity>> = db.commentDao().observeForPost(postId)
    suspend fun addComment(comment: CommentEntity): Long = db.commentDao().insert(comment)
    suspend fun updateComment(comment: CommentEntity) = db.commentDao().update(comment)
    suspend fun deleteComment(id: Long) = db.commentDao().deleteById(id)

    // ---- Google panels ----
    val panels: Flow<List<PanelEntity>> = db.panelDao().observeAll()
    val allPanelImages: Flow<List<PanelImageEntity>> = db.panelImageDao().observeAll()
    fun observePanel(id: Long): Flow<PanelEntity?> = db.panelDao().observeById(id)
    fun observePanelImages(id: Long): Flow<List<PanelImageEntity>> = db.panelImageDao().observeForPanel(id)
    fun observePanelFacts(id: Long): Flow<List<PanelFactEntity>> = db.panelFactDao().observeForPanel(id)
    fun observePanelItems(id: Long): Flow<List<PanelItemEntity>> = db.panelItemDao().observeForPanel(id)

    suspend fun getPanel(id: Long): PanelEntity? = db.panelDao().getById(id)
    suspend fun createPanel(): Long = db.panelDao().insert(PanelEntity())
    suspend fun addPanel(panel: PanelEntity): Long = db.panelDao().insert(panel)
    suspend fun updatePanel(panel: PanelEntity) = db.panelDao().update(panel.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deletePanel(id: Long) = db.panelDao().deleteById(id)

    suspend fun addPanelImage(image: PanelImageEntity): Long = db.panelImageDao().insert(image)
    suspend fun updatePanelImage(image: PanelImageEntity) = db.panelImageDao().update(image)
    suspend fun deletePanelImage(id: Long) = db.panelImageDao().deleteById(id)

    suspend fun addPanelFact(fact: PanelFactEntity): Long = db.panelFactDao().insert(fact)
    suspend fun updatePanelFact(fact: PanelFactEntity) = db.panelFactDao().update(fact)
    suspend fun deletePanelFact(id: Long) = db.panelFactDao().deleteById(id)

    suspend fun addPanelItem(item: PanelItemEntity): Long = db.panelItemDao().insert(item)
    suspend fun updatePanelItem(item: PanelItemEntity) = db.panelItemDao().update(item)
    suspend fun deletePanelItem(id: Long) = db.panelItemDao().deleteById(id)

    suspend fun duplicatePanel(id: Long): Long? {
        val source = db.panelDao().getById(id) ?: return null
        val now = System.currentTimeMillis()
        val newId = db.panelDao().insert(source.copy(id = 0, title = source.title + " copy", createdAt = now, updatedAt = now))
        db.panelImageDao().getForPanel(id).forEach { db.panelImageDao().insert(it.copy(id = 0, panelId = newId)) }
        db.panelFactDao().getForPanel(id).forEach { db.panelFactDao().insert(it.copy(id = 0, panelId = newId)) }
        db.panelItemDao().getForPanel(id).forEach { db.panelItemDao().insert(it.copy(id = 0, panelId = newId)) }
        return newId
    }

    // ---- YouTube ----
    val youtubeVideos: Flow<List<YoutubeVideoEntity>> = db.youtubeDao().observeVideos()
    fun observeYoutubeVideo(id: Long): Flow<YoutubeVideoEntity?> = db.youtubeDao().observeVideo(id)
    fun observeYoutubeComments(videoId: Long): Flow<List<YoutubeCommentEntity>> = db.youtubeDao().observeComments(videoId)
    suspend fun getYoutubeVideo(id: Long): YoutubeVideoEntity? = db.youtubeDao().getVideo(id)
    suspend fun addYoutubeVideo(video: YoutubeVideoEntity): Long = db.youtubeDao().insertVideo(video)
    suspend fun updateYoutubeVideo(video: YoutubeVideoEntity) = db.youtubeDao().updateVideo(video.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteYoutubeVideo(id: Long) = db.youtubeDao().deleteVideo(id)
    suspend fun addYoutubeComment(comment: YoutubeCommentEntity): Long = db.youtubeDao().insertComment(comment)
    suspend fun updateYoutubeComment(comment: YoutubeCommentEntity) = db.youtubeDao().updateComment(comment)
    suspend fun deleteYoutubeComment(id: Long) = db.youtubeDao().deleteComment(id)

    // ---- Pinterest ----
    val pinterestPages: Flow<List<PinterestPageEntity>> = db.pinterestDao().observePages()
    fun observePinterestPage(id: Long): Flow<PinterestPageEntity?> = db.pinterestDao().observePage(id)
    fun observePins(pageId: Long): Flow<List<PinEntity>> = db.pinterestDao().observePins(pageId)
    suspend fun getPinterestPage(id: Long): PinterestPageEntity? = db.pinterestDao().getPage(id)
    suspend fun addPinterestPage(page: PinterestPageEntity): Long = db.pinterestDao().insertPage(page)
    suspend fun updatePinterestPage(page: PinterestPageEntity) = db.pinterestDao().updatePage(page)
    suspend fun addPin(pin: PinEntity): Long = db.pinterestDao().insertPin(pin)
    suspend fun updatePin(pin: PinEntity) = db.pinterestDao().updatePin(pin)
    suspend fun deletePin(id: Long) = db.pinterestDao().deletePin(id)
    suspend fun deletePinsOfTab(pageId: Long, tabId: Long) = db.pinterestDao().deletePinsOfTab(pageId, tabId)
}
