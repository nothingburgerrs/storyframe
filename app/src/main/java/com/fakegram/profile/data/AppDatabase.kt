package com.fakegram.profile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 2 -> 3 only adds the Google panel tables, so existing Instagram profiles are preserved. */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `panels` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `query` TEXT NOT NULL, `title` TEXT NOT NULL, `subtitle` TEXT NOT NULL, `overview` TEXT NOT NULL, `sourceLabel` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `panel_images` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `panelId` INTEGER NOT NULL, `uri` TEXT NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`panelId`) REFERENCES `panels`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `panel_facts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `panelId` INTEGER NOT NULL, `label` TEXT NOT NULL, `value` TEXT NOT NULL, `detail` TEXT NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`panelId`) REFERENCES `panels`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `panel_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `panelId` INTEGER NOT NULL, `kind` TEXT NOT NULL, `title` TEXT NOT NULL, `subtitle` TEXT NOT NULL, `imageUri` TEXT, `position` INTEGER NOT NULL, FOREIGN KEY(`panelId`) REFERENCES `panels`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
    }
}

/** 3 -> 4 adds the opened-post details and the comments table; existing data is kept. */
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `caption` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `likeCount` TEXT NOT NULL DEFAULT '0'")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `commentCount` TEXT NOT NULL DEFAULT '0'")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `repostCount` TEXT NOT NULL DEFAULT '0'")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `shareCount` TEXT NOT NULL DEFAULT '0'")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `dateText` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `posts` ADD COLUMN `musicText` TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE TABLE IF NOT EXISTS `comments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `postId` INTEGER NOT NULL, `username` TEXT NOT NULL, `avatarUri` TEXT, `text` TEXT NOT NULL, `timeText` TEXT NOT NULL, `likeCount` TEXT NOT NULL, `isVerified` INTEGER NOT NULL, `hasTranslation` INTEGER NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`postId`) REFERENCES `posts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
    }
}

/** 4 -> 5 adds the saved-accounts table. */
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT NOT NULL, `avatarUri` TEXT, `isVerified` INTEGER NOT NULL)")
    }
}

/** 5 -> 6 adds the Bubble chat tables. */
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `bubble_chats` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `artistName` TEXT NOT NULL, `badgeText` TEXT NOT NULL, `artistAvatarUri` TEXT, `theme` TEXT NOT NULL, `use24h` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `bubble_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `chatId` INTEGER NOT NULL, `fromArtist` INTEGER NOT NULL, `kind` TEXT NOT NULL, `text` TEXT NOT NULL, `translation` TEXT NOT NULL, `imageUri` TEXT, `timeText` TEXT NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`chatId`) REFERENCES `bubble_chats`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
    }
}

/** 6 -> 7 adds the custom chat background. */
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `bubble_chats` ADD COLUMN `backgroundUri` TEXT")
    }
}

/** 7 -> 8 adds the Character OS tables (characters + their schedule); existing data is untouched. */
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `characters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `name` TEXT NOT NULL, `handle` TEXT NOT NULL, `bio` TEXT NOT NULL, `lore` TEXT NOT NULL, `avatarUri` TEXT, `memberIds` TEXT NOT NULL, `activePlatforms` TEXT NOT NULL, `instagramProfileId` INTEGER, `mood` TEXT NOT NULL, `quote` TEXT NOT NULL, `quoteSource` TEXT NOT NULL, `birthday` TEXT NOT NULL, `vibe` TEXT NOT NULL, `postsPerWeek` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `schedule_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `characterId` INTEGER NOT NULL, `date` TEXT NOT NULL, `platform` TEXT NOT NULL, `note` TEXT NOT NULL, FOREIGN KEY(`characterId`) REFERENCES `characters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
    }
}

/** 8 -> 9 lets Google panels and Bubble chats belong to a character; existing ones stay unlinked. */
private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `panels` ADD COLUMN `characterId` INTEGER")
        db.execSQL("ALTER TABLE `bubble_chats` ADD COLUMN `characterId` INTEGER")
    }
}

/** 9 -> 10 adds YouTube videos + comments and the optional calendar stamp photo. */
private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `youtube_videos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `characterId` INTEGER, `title` TEXT NOT NULL, `channelName` TEXT NOT NULL, `channelAvatarUri` TEXT, `thumbnailUri` TEXT, `viewsText` TEXT NOT NULL, `timeText` TEXT NOT NULL, `likeCount` TEXT NOT NULL, `subtitleText` TEXT NOT NULL, `durationText` TEXT NOT NULL, `autoDubbed` INTEGER NOT NULL, `showAd` INTEGER NOT NULL, `adImageUri` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `youtube_comments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `videoId` INTEGER NOT NULL, `username` TEXT NOT NULL, `avatarUri` TEXT, `text` TEXT NOT NULL, `timeText` TEXT NOT NULL, `likeCount` TEXT NOT NULL, `replyCount` INTEGER NOT NULL, `hasTranslation` INTEGER NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`videoId`) REFERENCES `youtube_videos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("ALTER TABLE `schedule_events` ADD COLUMN `imageUri` TEXT")
    }
}

/** 10 -> 11 turns YouTube on for characters made before it existed. */
private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE `characters` SET `activePlatforms` = `activePlatforms` || ',youtube' WHERE `activePlatforms` NOT LIKE '%youtube%'")
    }
}

/** 11 -> 12 adds the Subscribe toggle to YouTube videos. */
private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `youtube_videos` ADD COLUMN `subscribed` INTEGER NOT NULL DEFAULT 0")
    }
}

/** 12 -> 13 adds the YouTube layout toggles and the editable comment count. */
private val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `youtube_videos` ADD COLUMN `showPostInfo` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `youtube_videos` ADD COLUMN `showCaption` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `youtube_videos` ADD COLUMN `commentCount` TEXT NOT NULL DEFAULT ''")
    }
}

/** 13 -> 14 adds the switch that hides the player buttons. */
private val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `youtube_videos` ADD COLUMN `showControls` INTEGER NOT NULL DEFAULT 1")
    }
}

/** 14 -> 15 adds the switch for the recommended videos under the comments. */
private val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `youtube_videos` ADD COLUMN `showRelated` INTEGER NOT NULL DEFAULT 1")
    }
}

/** 15 -> 16 adds Pinterest and turns it on for existing characters. */
private val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `pinterest_pages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `characterId` INTEGER, `avatarUri` TEXT, `tabs` TEXT NOT NULL, `searchQuery` TEXT NOT NULL, `chips` TEXT NOT NULL, `showDot` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `pinterest_pins` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `pageId` INTEGER NOT NULL, `screen` INTEGER NOT NULL, `tabId` INTEGER NOT NULL, `imageUri` TEXT, `aspect` REAL NOT NULL, `title` TEXT NOT NULL, `sponsored` INTEGER NOT NULL, `sponsorName` TEXT NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`pageId`) REFERENCES `pinterest_pages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("UPDATE `characters` SET `activePlatforms` = `activePlatforms` || ',pinterest' WHERE `activePlatforms` NOT LIKE '%pinterest%'")
    }
}

/** 16 -> 17 marks events the weekly-goal scheduler placed. */
private val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `schedule_events` ADD COLUMN `auto` INTEGER NOT NULL DEFAULT 0")
    }
}

/** 17 -> 18 adds the draggable progress bar position. */
private val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `youtube_videos` ADD COLUMN `progress` REAL NOT NULL DEFAULT 0.04")
    }
}

class Converters {
    @TypeConverter
    fun fromStoryRingType(value: StoryRingType): String = value.name

    @TypeConverter
    fun toStoryRingType(value: String): StoryRingType =
        runCatching { StoryRingType.valueOf(value) }.getOrDefault(StoryRingType.NONE)

    @TypeConverter
    fun fromNoteType(value: NoteType): String = value.name

    @TypeConverter
    fun toNoteType(value: String): NoteType =
        runCatching { NoteType.valueOf(value) }.getOrDefault(NoteType.NONE)

    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType =
        runCatching { MediaType.valueOf(value) }.getOrDefault(MediaType.IMAGE)
}

@Database(
    entities = [
        ProfileEntity::class,
        PillEntity::class,
        FollowedByEntity::class,
        HighlightEntity::class,
        PostEntity::class,
        CarouselItemEntity::class,
        PanelEntity::class,
        PanelImageEntity::class,
        PanelFactEntity::class,
        PanelItemEntity::class,
        CommentEntity::class,
        AccountEntity::class,
        BubbleChatEntity::class,
        BubbleMessageEntity::class,
        CharacterEntity::class,
        ScheduleEventEntity::class,
        YoutubeVideoEntity::class,
        YoutubeCommentEntity::class,
        PinterestPageEntity::class,
        PinEntity::class
    ],
    version = 18,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun pillDao(): PillDao
    abstract fun followedByDao(): FollowedByDao
    abstract fun highlightDao(): HighlightDao
    abstract fun postDao(): PostDao
    abstract fun carouselItemDao(): CarouselItemDao
    abstract fun panelDao(): PanelDao
    abstract fun panelImageDao(): PanelImageDao
    abstract fun panelFactDao(): PanelFactDao
    abstract fun panelItemDao(): PanelItemDao
    abstract fun commentDao(): CommentDao
    abstract fun accountDao(): AccountDao
    abstract fun bubbleDao(): BubbleDao
    abstract fun characterDao(): CharacterDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun youtubeDao(): YoutubeDao
    abstract fun pinterestDao(): PinterestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "storyframe.db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
