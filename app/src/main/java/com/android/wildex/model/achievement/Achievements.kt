package com.android.wildex.model.achievement

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.utils.Id

object Achievements {
  private val likeRepository: LikeRepository = RepositoryProvider.likeRepository
  private val postRepository: PostsRepository = RepositoryProvider.postRepository
  private val commentRepository: CommentRepository = RepositoryProvider.commentRepository

  private val animalRepository: AnimalRepository = RepositoryProvider.animalRepository

  // ---------- Achievements ----------

  // ─────────────────────────────────────────────────────────────────────────────
  // INPUT CONVENTIONS
  // - POST_IDS: list of the current user's post IDs.
  // - LIKE_IDS: list of post IDs that the current user has liked.
  // - COMMENT_IDS: list of comment IDs authored by the current user.
  // ALWAYS PASS A MAP CONTAINING ALL THESE THREE INPUT KEYS TO THE ACHIEVEMENT UPDATE METHOD
  // OTHERWISE SOME ACHIEVEMENTS MAY GET DROPPED UNINTENTIONALLY.
  // ─────────────────────────────────────────────────────────────────────────────

  /** First Post — awarded for creating the first post. */
  val firstPost =
      Achievement(
          achievementId = "achievement_1",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FPhoto_bronze.png?alt=media&token=2b995a78-cb37-46c4-a980-1e4fbd823da7",
          description = "Create your first post",
          name = "First Post",
          progress = { userId ->
            listOf(Triple("Posts", postRepository.getAllPostsByGivenAuthor(userId).size, 1))
          },
      )

  /** Post Master — awarded for publishing at least 10 posts. */
  val postMaster =
      Achievement(
          achievementId = "achievement_2",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FPhoto_silver.png?alt=media&token=975d39ea-893c-43d5-8bb2-6cce79be3a2c",
          description = "Reach 10 posts",
          name = "Post Master",
          progress = { userId ->
            listOf(Triple("Posts", postRepository.getAllPostsByGivenAuthor(userId).size, 10))
          },
      )

  /** Post Legend — awarded for publishing at least 25 posts. */
  val postLegend =
      Achievement(
          achievementId = "achievement_3",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FPhoto_gold.png?alt=media&token=1f2f1941-89ea-48ec-af1f-c708e85723a7",
          description = "Reach 25 posts",
          name = "Post Legend",
          progress = { userId ->
            listOf(Triple("Posts", postRepository.getAllPostsByGivenAuthor(userId).size, 25))
          },
      )

  /** Dog lover — awarded for publishing at least 5 dogs. */
  val dogLover =
      Achievement(
          achievementId = "achievement_4",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FDog.png?alt=media&token=f1fc5a85-621f-4a22-bd72-1de57fe208ca",
          description = "Reach 5 dog posts",
          name = "Dog Lover",
          progress = { userId ->
            listOf(
                Triple(
                    "Dogs",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "domestic dog"
                    },
                    5))
          },
      )

  /** Cat lover — awarded for publishing at least 5 cats. */
  val catLover =
      Achievement(
          achievementId = "achievement_5",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FCat.png?alt=media&token=3dff771f-8792-48e4-a372-46de2b6c7261",
          description = "Reach 5 cat posts",
          name = "Cat Lover",
          progress = { userId ->
            listOf(
                Triple(
                    "Cats",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "domestic cat"
                    },
                    5))
          },
      )

  /** Insect lover — awarded for publishing at least 1 centipede. */
  val insectLover =
      Achievement(
          achievementId = "achievement_6",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FInsect.png?alt=media&token=ee202c9d-18f4-4e17-804d-c9763bd770c0",
          description = "Post a centipede",
          name = "Insect Lover",
          progress = { userId ->
            listOf(
                Triple(
                    "Centipedes",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "centipede"
                    },
                    1))
          },
      )

  /**
   * First Like — awarded for liking 1 post. Verifies likes by checking the LikeRepository for each
   * provided post ID.
   */
  val firstLike =
      Achievement(
          achievementId = "achievement_7",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FHeart_bronze.png?alt=media&token=5621360b-8237-45ad-a583-90e1db8da642",
          description = "Like your first post",
          name = "First Like",
          progress = { userId ->
            listOf(Triple("Likes", likeRepository.getAllLikesByUser(userId).size, 1))
          },
      )

  /**
   * Social Butterfly — awarded for liking 50 different posts. Verifies likes by checking the
   * LikeRepository for each provided post ID.
   */
  val socialButterfly =
      Achievement(
          achievementId = "achievement_8",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FHeart_silver.png?alt=media&token=0dad2e28-7a78-4a10-8581-4c35f2c34a44",
          description = "Like 50 posts",
          name = "Social Butterfly",
          progress = { userId ->
            listOf(Triple("Likes", likeRepository.getAllLikesByUser(userId).size, 50))
          },
      )

  /**
   * Golden Heart — awarded for liking 100 different posts. Verifies likes by checking the
   * LikeRepository for each provided post ID.
   */
  val goldenHeart =
      Achievement(
          achievementId = "achievement_9",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FHeart_gold.png?alt=media&token=5dbb57fb-dbcc-4a1f-b7ec-d944de91d62c",
          description = "Like 100 posts",
          name = "Golden Heart",
          progress = { userId ->
            listOf(Triple("Likes", likeRepository.getAllLikesByUser(userId).size, 100))
          },
      )

  /** First Comment — awarded for writing 1 comment. */
  val firstComment =
      Achievement(
          achievementId = "achievement_10",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FText_infinity_bronze.png?alt=media&token=9182444c-05d0-4447-8989-311cbfc2a9a4",
          description = "Write your first comment",
          name = "First Comment",
          progress = { userId ->
            listOf(Triple("Comments", commentRepository.getCommentsByUser(userId).size, 1))
          },
      )

  /** Community Builder — awarded for writing at least 20 comments. */
  val communityBuilder =
      Achievement(
          achievementId = "achievement_11",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FText_infinity_silver.png?alt=media&token=f9aeb95a-59f9-4727-8f8d-82f9c19e0f3e",
          description = "Write 20 comments",
          name = "Community Builder",
          progress = { userId ->
            listOf(Triple("Comments", commentRepository.getCommentsByUser(userId).size, 20))
          },
      )

  /** Conversationalist — awarded for writing at least 50 comments. */
  val conversationalist =
      Achievement(
          achievementId = "achievement_12",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FText_infinity_gold.png?alt=media&token=feee2d29-3ade-43dd-9d83-c4826428ab41",
          description = "Write 50 comments",
          name = "Conversationalist",
          progress = { userId ->
            listOf(Triple("Comments", commentRepository.getCommentsByUser(userId).size, 50))
          },
      )

  /** Easter Egg Newbie — awarded for publishing at least 5 birds and 5 bunnies. */
  val easterEggNewbie =
      Achievement(
          achievementId = "achievement_13",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FEggs_bronze.png?alt=media&token=be4d3396-cddc-4584-814c-3a72fd8f1d50",
          description = "Reach 5 bird posts and 5 bunny posts",
          name = "Easter Egg Newbie",
          progress = { userId ->
            listOf(
                Triple(
                    "Birds",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "bird"
                    },
                    5),
                Triple(
                    "Bunnies",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "bunny"
                    },
                    5))
          },
      )

  /** Easter Egg Master — awarded for publishing at least 10 birds and 10 bunnies. */
  val easterEggMaster =
      Achievement(
          achievementId = "achievement_14",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FEggs_silver.png?alt=media&token=f8eb13c3-d649-435f-a9aa-27eb6791612f",
          description = "Reach 10 bird posts and 10 bunny posts",
          name = "Easter Egg Master",
          progress = { userId ->
            listOf(
                Triple(
                    "Birds",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "bird"
                    },
                    10),
                Triple(
                    "Bunnies",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "bunny"
                    },
                    10))
          },
      )

  /** Easter Egg Legend — awarded for publishing at least 15 birds and 15 bunnies. */
  val easterEggLegend =
      Achievement(
          achievementId = "achievement_15",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FEggs_gold.png?alt=media&token=32c938bd-928c-4693-ad29-2347873b7b4d",
          description = "Reach 15 bird posts and 15 bunny posts",
          name = "Easter Egg Legend",
          progress = { userId ->
            listOf(
                Triple(
                    "Birds",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "bird"
                    },
                    15),
                Triple(
                    "Bunnies",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "bunny"
                    },
                    15))
          },
      )

  /**
   * Nature Enthusiast — awarded for being active posting multiple species:
   * - At least 1 cat posted
   * - At least 1 dog posted
   * - At least 1 bird posted
   * - At least 1 bunny posted
   */
  val natureEnthusiast =
      Achievement(
          achievementId = "achievement_16",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FTrophy_bronze.png?alt=media&token=4ceb018c-8591-40ad-917d-ed45e8e21e3a",
          description = "Post at least 1 cat, 1 dog, 1 bird, and 1 bunny",
          name = "Nature Enthusiast",
          progress = { userId ->
            listOf(
                Triple(
                    "Dog",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "domestic dog"
                    },
                    1),
                Triple(
                    "Cat",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "domestic cat"
                    },
                    1),
                Triple(
                    "Bird",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "bird"
                    },
                    1),
                Triple(
                    "Bunny",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "bunny"
                    },
                    1))
          },
      )

  /**
   * Nature Lover — awarded for being active posting multiple species:
   * - At least 5 cats posted
   * - At least 5 dogs posted
   * - At least 5 birds posted
   * - At least 5 bunnies posted
   */
  val natureLover =
      Achievement(
          achievementId = "achievement_17",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FTrophy_silver.png?alt=media&token=daf8a808-2262-44ac-95f4-ee1f9b778f8a",
          description = "Post at least 5 cats, 5 dogs, 5 birds, and 5 bunnies",
          name = "Nature Lover",
          progress = { userId ->
            listOf(
                Triple(
                    "Dogs",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "domestic dog"
                    },
                    5),
                Triple(
                    "Cats",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "domestic cat"
                    },
                    5),
                Triple(
                    "Birds",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "bird"
                    },
                    5),
                Triple(
                    "Bunnies",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "bunny"
                    },
                    5))
          },
      )

  /**
   * Nature Legend — awarded for being active posting multiple species:
   * - At least 10 cats posted
   * - At least 10 dogs posted
   * - At least 10 birds posted
   * - At least 10 bunnies posted
   */
  val natureLegend =
      Achievement(
          achievementId = "achievement_18",
          pictureURL =
              "https://firebasestorage.googleapis.com/v0/b/wildex-170b0.firebasestorage.app/o/achievements%2FTrophy_gold.png?alt=media&token=157c4143-3fde-4c58-b3e7-b2416c6b0a32",
          description = "Post at least 10 cats, 10 dogs, 10 birds, and 10 bunnies",
          name = "Nature Legend",
          progress = { userId ->
            listOf(
                Triple(
                    "Dogs",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "domestic dog"
                    },
                    10),
                Triple(
                    "Cats",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "domestic cat"
                    },
                    10),
                Triple(
                    "Birds",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "bird"
                    },
                    10),
                Triple(
                    "Bunnies",
                    postRepository.getAllPostsByGivenAuthor(userId).count {
                      animalRepository.getAnimal(it.animalId).name == "bunny"
                    },
                    10))
          },
      )

  // ---------- Collections ----------

  val ALL =
      listOf(
          firstPost,
          postMaster,
          postLegend,
          dogLover,
          catLover,
          insectLover,
          firstLike,
          socialButterfly,
          goldenHeart,
          firstComment,
          communityBuilder,
          conversationalist,
          easterEggNewbie,
          easterEggMaster,
          easterEggLegend,
          natureEnthusiast,
          natureLover,
          natureLegend,
      )

  val achievementById: Map<Id, Achievement> = ALL.associateBy { it.achievementId }
}
