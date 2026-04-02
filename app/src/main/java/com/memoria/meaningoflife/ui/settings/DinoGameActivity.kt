package com.memoria.meaningoflife.ui.settings

import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.annotation.Keep
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.ui.BaseActivity
import kotlin.math.abs
import kotlin.random.Random

@Keep
class DinoGameActivity : BaseActivity() {

    private lateinit var gameView: GameView
    private var isRunning = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameView = GameView(this)
        setContentView(gameView)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "跳跃小游戏"
    }

    override fun onResume() {
        super.onResume()
        isRunning = true
        gameView.startGame()
    }

    override fun onPause() {
        super.onPause()
        isRunning = false
        gameView.stopGame()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class GameView(context: android.content.Context) : View(context) {

        // ==================== 可调参数 ====================
        private var CHARACTER_SIZE_RATIO = 0.30f
        private var OBSTACLE_SPACING = 1000f
        private var OBSTACLE_SIZE_RATIO = 0.5f
        private var INITIAL_GAME_SPEED = 8f
        private var JUMP_POWER = -30f

        // ==================== 游戏数据 ====================
        private var playerY = 0f
        private var playerVelocity = 0f
        private var isJumping = false

        private val obstacles = mutableListOf<Obstacle>()
        private val flyingObstacles = mutableListOf<FlyingObstacle>()  // 被打飞的障碍物
        private val particles = mutableListOf<Particle>()  // 粒子动画

        private var score = 0
        private var gameOver = false
        private var gameWin = false
        private var gameStarted = false
        private var isSelectingCharacter = true

        // 屏幕尺寸
        private var screenWidth = 0
        private var screenHeight = 0
        private var groundY = 0f

        // 人物大小
        private var playerSize = 0f
        private var playerX = 0f

        // 游戏速度
        private var gameSpeed = INITIAL_GAME_SPEED

        // 障碍物贴图
        private var obstacleBitmap: Bitmap? = null

        // Banner
        private var bannerBitmap: Bitmap? = null

        // 人物选择
        private val characters = mutableListOf<Character>()
        private var selectedCharacter = 0
        private var lastTouchX = 0f

        // 技能相关（被动技能，只能用一次）
        private var octopusInfiniteJump = false      // 章鱼：无限段跳（被动）
        private var knightImmune = false              // 橘它骑士：免疫一次障碍物
        private var knightUsed = false                // 橘它骑士技能是否已使用
        private var kangarooHitCount = 0              // 袋鼠克星已打飞数量（最多2个）
        private var kangarooMaxHits = 2               // 袋鼠克星最多打飞2个
        private var explosionUsed = false             // 爆炸静默是否已使用
        private var explosionActive = false           // 爆炸是否激活（无敌状态）
        private var explosionTimer = 0                // 爆炸计时器
        private val EXPLOSION_DURATION = 300          // 5秒 (60fps * 5)
        private var vipAutoJump = false               // VIP：自动跳跃

        // 屏幕震动
        private var shakeOffsetX = 0f
        private var shakeOffsetY = 0f
        private var shakeTimer = 0

        // VIP自动通关
        private var vipClearedCount = 0
        private val VIP_WIN_COUNT = 50

        // 绘画工具
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 人物数据类
        data class Character(
            val id: Int,
            val name: String,
            val ability: String,
            val desc: String,
            val bitmap: Bitmap?
        )

        // 障碍物类
        inner class Obstacle(
            var x: Float,
            var width: Float
        )

        // 被打飞的障碍物（用于动效）
        inner class FlyingObstacle(
            var x: Float,
            var y: Float,
            var vx: Float,
            var vy: Float,
            var rotate: Float,
            var rotateSpeed: Float,
            var width: Float,
            var height: Float,
            var life: Int = 30
        )

        // 粒子类
        inner class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Int)

        // 游戏循环
        private var gameThread: GameThread? = null

        init {
            setBackgroundColor(Color.parseColor("#FDF8F0"))
            paint.isAntiAlias = true
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            screenWidth = w
            screenHeight = h
            groundY = h - h * 0.12f

            playerSize = w * CHARACTER_SIZE_RATIO
            playerX = w * 0.1f
            playerY = groundY - playerSize

            loadImages()
            initCharacters()
            resetGame()
        }

        private fun loadImages() {
            try {
                obstacleBitmap = BitmapFactory.decodeResource(resources, R.drawable.game_obstacle_1)
            } catch (e: Exception) { }
            if (obstacleBitmap == null) {
                val defaultBmp = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
                defaultBmp.eraseColor(Color.parseColor("#FF8C42"))
                obstacleBitmap = defaultBmp
            }

            try {
                bannerBitmap = BitmapFactory.decodeResource(resources, R.drawable.game_banner_1)
            } catch (e: Exception) { }
        }

        private fun initCharacters() {
            val names = listOf("章鱼？！", "橘它骑士", "袋鼠克星", "爆炸静默", "VIP蛋馓")
            val abilities = listOf("无限段跳", "免疫一次障碍物", "打飞2个障碍物", "爆炸状态5秒无敌", "自动跳跃")
            val descs = listOf(
                "在空中可以无限跳跃",
                "第一次碰到障碍物不会死",
                "可以打飞前两个障碍物",
                "第一次碰到障碍物时炸飞障碍物，进入5秒无敌滞空状态",
                "自动跳跃，跳过50个障碍物获胜"
            )
            val resIds = listOf(
                R.drawable.game_character_1,
                R.drawable.game_character_2,
                R.drawable.game_character_3,
                R.drawable.game_character_4,
                R.drawable.game_character_5
            )

            characters.clear()
            for (i in names.indices) {
                var bmp: Bitmap? = null
                try {
                    bmp = BitmapFactory.decodeResource(resources, resIds[i])
                    if (bmp != null) {
                        val scaledBmp = Bitmap.createScaledBitmap(bmp, playerSize.toInt(), playerSize.toInt(), true)
                        bmp = scaledBmp
                    }
                } catch (e: Exception) { }
                characters.add(Character(i, names[i], abilities[i], descs[i], bmp))
            }
        }

        private fun resetGame() {
            obstacles.clear()
            flyingObstacles.clear()
            particles.clear()
            score = 0
            gameOver = false
            gameWin = false
            gameStarted = false
            isSelectingCharacter = true
            gameSpeed = INITIAL_GAME_SPEED
            playerY = groundY - playerSize
            isJumping = false
            playerVelocity = 0f
            vipClearedCount = 0
            shakeTimer = 0

            // 重置爆炸静默相关状态
            explosionActive = false
            explosionTimer = 0

            // 重置技能状态
            val char = characters.getOrNull(selectedCharacter)
            octopusInfiniteJump = char?.id == 0
            knightImmune = char?.id == 1
            knightUsed = false
            kangarooHitCount = 0
            explosionUsed = false
            vipAutoJump = char?.id == 4
        }

        fun startGame() {
            gameThread = GameThread(this)
            gameThread?.start()
        }

        fun stopGame() {
            gameThread?.stopGame()
            gameThread = null
        }

        private fun update() {
            if (!gameStarted || gameOver || gameWin) return

            // 处理爆炸激活计时
            if (explosionActive) {
                explosionTimer--
                if (explosionTimer <= 0) {
                    explosionActive = false
                    // 爆炸结束，让玩家落地
                    if (isJumping) {
                        playerVelocity = 0f
                        isJumping = false
                        playerY = groundY - playerSize
                    }
                }
            }

            // 自动跳跃（VIP）
            if (vipAutoJump) {
                val nearestObstacle = obstacles.minByOrNull { it.x }
                if (nearestObstacle != null) {
                    val distance = nearestObstacle.x - playerX
                    if (distance in 30f..100f && !isJumping) {
                        performJump()
                    }
                }
                // 定期跳跃防止卡住
                if (!isJumping && obstacles.isNotEmpty()) {
                    performJump()
                }
            }

            // 物理更新（支持爆炸滞空效果）
            if (explosionActive) {
                // 爆炸激活期间，玩家在空中飘浮，缓慢下落
                if (isJumping) {
                    playerVelocity += 0.3f  // 降低重力，缓慢下落
                    playerY += playerVelocity
                    if (playerY >= groundY - playerSize) {
                        // 爆炸结束前不应该落地，保持在空中
                        playerY = groundY - playerSize - 20f
                    }
                }
            } else {
                if (isJumping) {
                    playerVelocity += 0.8f
                    playerY += playerVelocity

                    if (playerY >= groundY - playerSize) {
                        playerY = groundY - playerSize
                        isJumping = false
                        playerVelocity = 0f
                    }
                }
            }

            // 更新被打飞的障碍物动效
            val flyIterator = flyingObstacles.iterator()
            while (flyIterator.hasNext()) {
                val fly = flyIterator.next()
                fly.x += fly.vx
                fly.y += fly.vy
                fly.vy += 0.5f
                fly.rotate += fly.rotateSpeed
                fly.life--

                if (fly.life <= 0 || fly.y > screenHeight) {
                    flyIterator.remove()
                }
            }

            // 障碍物更新
            val iterator = obstacles.iterator()
            while (iterator.hasNext()) {
                val ob = iterator.next()
                ob.x -= gameSpeed

                if (ob.x + ob.width < 0) {
                    iterator.remove()
                    score++
                    if (vipAutoJump) {
                        vipClearedCount++
                        if (vipClearedCount >= VIP_WIN_COUNT) {
                            gameWin = true
                            gameStarted = false
                        }
                    }
                    continue
                }

                // 碰撞检测
                if (ob.x < playerX + playerSize && ob.x + ob.width > playerX &&
                    playerY + playerSize > groundY - ob.width) {

                    val char = characters.getOrNull(selectedCharacter)

                    when (char?.id) {
                        0 -> { // 章鱼：无限段跳，不免疫，直接死亡
                            gameOver = true
                            createExplosion(playerX + playerSize / 2, playerY + playerSize / 2)
                        }
                        1 -> { // 橘它骑士：免疫一次
                            if (!knightUsed) {
                                knightUsed = true
                                // 打飞当前障碍物
                                addFlyingObstacle(ob)
                                iterator.remove()
                                createHitEffect(playerX + playerSize / 2, playerY + playerSize / 2)
                            } else {
                                gameOver = true
                                createExplosion(playerX + playerSize / 2, playerY + playerSize / 2)
                            }
                        }
                        2 -> { // 袋鼠克星：打飞前2个障碍物
                            if (kangarooHitCount < kangarooMaxHits) {
                                kangarooHitCount++
                                addFlyingObstacle(ob)
                                iterator.remove()
                                createHitEffect(playerX + playerSize / 2, playerY + playerSize / 2)
                            } else {
                                gameOver = true
                                createExplosion(playerX + playerSize / 2, playerY + playerSize / 2)
                            }
                        }
                        3 -> { // 爆炸静默
                            if (!explosionUsed) {
                                // 第一次碰到障碍物：激活爆炸
                                explosionUsed = true
                                explosionActive = true
                                explosionTimer = EXPLOSION_DURATION

                                // 大爆炸特效
                                createBigExplosion(ob.x + ob.width / 2, groundY - ob.width)
                                shakeTimer = 20

                                // 炸飞当前障碍物
                                addFlyingObstacle(ob)
                                iterator.remove()

                                // 炸飞周围两个障碍物
                                val toRemove = mutableListOf<Obstacle>()
                                var count = 0
                                for (obs in obstacles) {
                                    if (count < 2 && obs != ob) {
                                        toRemove.add(obs)
                                        addFlyingObstacle(obs)
                                        count++
                                    }
                                }
                                obstacles.removeAll(toRemove)

                                // 让玩家弹起
                                isJumping = true
                                playerVelocity = -25f

                            } else if (explosionActive) {
                                // 爆炸激活期间，无敌，障碍物直接消失
                                addFlyingObstacle(ob)
                                iterator.remove()
                            } else {
                                // 爆炸已使用且不在激活期间，死亡
                                gameOver = true
                                createExplosion(playerX + playerSize / 2, playerY + playerSize / 2)
                            }
                        }
                        4 -> { // VIP：无敌，不死亡，障碍物直接消失
                            iterator.remove()
                            score++
                            vipClearedCount++
                            if (vipClearedCount >= VIP_WIN_COUNT) {
                                gameWin = true
                                gameStarted = false
                            }
                        }
                        else -> {
                            gameOver = true
                            createExplosion(playerX + playerSize / 2, playerY + playerSize / 2)
                        }
                    }
                }
            }

            // 生成新障碍物
            if (obstacles.isEmpty() || obstacles.last().x < screenWidth - OBSTACLE_SPACING) {
                val obstacleWidth = playerSize * OBSTACLE_SIZE_RATIO
                obstacles.add(Obstacle(screenWidth.toFloat(), obstacleWidth))
            }

            // 增加难度
            if (score > 0 && score % 10 == 0 && gameSpeed < 15f) {
                gameSpeed += 0.2f
            }
        }

        private fun addFlyingObstacle(ob: Obstacle) {
            val obstacleHeight = ob.width
            val fly = FlyingObstacle(
                x = ob.x,
                y = groundY - obstacleHeight,
                vx = Random.nextFloat() * 15 - 10,
                vy = -15f,
                rotate = 0f,
                rotateSpeed = Random.nextFloat() * 20 - 10,
                width = ob.width,
                height = obstacleHeight,
                life = 40
            )
            flyingObstacles.add(fly)
        }

        private fun createHitEffect(x: Float, y: Float) {
            for (i in 0..10) {
                particles.add(Particle(x, y, Random.nextFloat() * 8 - 4, Random.nextFloat() * -8, 30))
            }
        }

        private fun createExplosion(x: Float, y: Float) {
            for (i in 0..20) {
                particles.add(Particle(x, y, Random.nextFloat() * 10 - 5, Random.nextFloat() * -10, 50))
            }
        }

        private fun createBigExplosion(x: Float, y: Float) {
            for (i in 0..80) {
                particles.add(Particle(x, y, Random.nextFloat() * 20 - 10, Random.nextFloat() * -25, 80))
            }
            shakeTimer = 25
        }

        private fun performJump() {
            val char = characters.getOrNull(selectedCharacter)

            if (char?.id == 0) {
                // 章鱼：无限段跳
                isJumping = true
                playerVelocity = JUMP_POWER
            } else {
                // 普通跳跃
                if (!isJumping) {
                    isJumping = true
                    playerVelocity = JUMP_POWER
                }
            }
        }

        private fun switchCharacter(delta: Int) {
            val newIndex = (selectedCharacter + delta + characters.size) % characters.size
            if (newIndex != selectedCharacter) {
                selectedCharacter = newIndex
                // 更新技能状态
                val char = characters.getOrNull(selectedCharacter)
                octopusInfiniteJump = char?.id == 0
                knightImmune = char?.id == 1
                knightUsed = false
                kangarooHitCount = 0
                explosionUsed = false
                explosionActive = false
                explosionTimer = 0
                vipAutoJump = char?.id == 4
                invalidate()
            }
        }

        override fun onDraw(canvas: Canvas) {
            // 屏幕震动
            if (shakeTimer > 0) {
                shakeOffsetX = Random.nextFloat() * 10 - 5
                shakeOffsetY = Random.nextFloat() * 10 - 5
                shakeTimer--
            } else {
                shakeOffsetX = 0f
                shakeOffsetY = 0f
            }
            canvas.translate(shakeOffsetX, shakeOffsetY)

            // 背景
            paint.color = Color.parseColor("#FDF8F0")
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)

            // Banner
            val bannerHeight = screenHeight * 0.3f
            if (bannerBitmap != null && !gameStarted && isSelectingCharacter) {
                canvas.drawBitmap(bannerBitmap!!, null, Rect(0, 0, screenWidth, bannerHeight.toInt()), null)
            }

            // 地面
            paint.color = Color.parseColor("#8A9B68")
            canvas.drawRect(0f, groundY, screenWidth.toFloat(), screenHeight.toFloat(), paint)
            paint.color = Color.parseColor("#6B8C4A")
            canvas.drawRect(0f, groundY - 5f, screenWidth.toFloat(), groundY, paint)

            // 绘制被打飞的障碍物（动效）
            for (fly in flyingObstacles) {
                if (obstacleBitmap != null) {
                    canvas.save()
                    canvas.rotate(fly.rotate, fly.x + fly.width / 2, fly.y + fly.height / 2)
                    val src = Rect(0, 0, obstacleBitmap!!.width, obstacleBitmap!!.height)
                    val dst = Rect(fly.x.toInt(), fly.y.toInt(), (fly.x + fly.width).toInt(), (fly.y + fly.height).toInt())
                    canvas.drawBitmap(obstacleBitmap!!, src, dst, null)
                    canvas.restore()
                }
            }

            // 障碍物
            for (ob in obstacles) {
                if (obstacleBitmap != null) {
                    val src = Rect(0, 0, obstacleBitmap!!.width, obstacleBitmap!!.height)
                    val dst = Rect(ob.x.toInt(), (groundY - ob.width).toInt(), (ob.x + ob.width).toInt(), groundY.toInt())
                    canvas.drawBitmap(obstacleBitmap!!, src, dst, null)
                } else {
                    paint.color = Color.parseColor("#FF8C42")
                    canvas.drawRect(ob.x, groundY - ob.width, ob.x + ob.width, groundY, paint)
                }
            }

            // 绘制粒子效果
            val pIter = particles.iterator()
            while (pIter.hasNext()) {
                val p = pIter.next()
                p.x += p.vx
                p.y += p.vy
                p.vy += 0.3f
                p.life--
                paint.color = Color.argb(255 * p.life / 50, 255, 165, 0)
                canvas.drawCircle(p.x, p.y, 8f, paint)
                if (p.life <= 0) pIter.remove()
            }

            // 角色选择界面
            if (!gameStarted && isSelectingCharacter) {
                drawCharacterSelection(canvas, bannerHeight)
            }
            // 游戏中
            else if (gameStarted && !gameOver && !gameWin) {
                drawGame(canvas)
            }
            // 胜利
            else if (gameWin) {
                drawGame(canvas)
                drawWinScreen(canvas)
            }
            // 失败
            else if (gameOver) {
                drawGame(canvas)
                drawGameOver(canvas)
            }
        }

        private fun drawCharacterSelection(canvas: Canvas, bannerHeight: Float) {
            val centerX = screenWidth / 2f
            val centerY = bannerHeight + (screenHeight - bannerHeight) / 2f
            val charWidth = playerSize * 1.5f
            val charHeight = charWidth

            val currentChar = characters.getOrNull(selectedCharacter)

            // 绘制左右箭头指示
            paint.color = Color.parseColor("#FF8C42")
            paint.textSize = 48f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("◀", centerX - charWidth - 30, centerY + charHeight / 2, paint)
            canvas.drawText("▶", centerX + charWidth + 30, centerY + charHeight / 2, paint)

            // 绘制选中角色的边框
            paint.color = Color.parseColor("#FF8C42")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 8f
            canvas.drawRect(centerX - charWidth / 2 - 10, centerY - charHeight / 2 - 10,
                centerX + charWidth / 2 + 10, centerY + charHeight / 2 + 10, paint)
            paint.style = Paint.Style.FILL

            // 绘制角色头像
            if (currentChar?.bitmap != null) {
                val src = Rect(0, 0, currentChar.bitmap!!.width, currentChar.bitmap!!.height)
                val dst = Rect((centerX - charWidth / 2).toInt(), (centerY - charHeight / 2).toInt(),
                    (centerX + charWidth / 2).toInt(), (centerY + charHeight / 2).toInt())
                canvas.drawBitmap(currentChar.bitmap!!, src, dst, null)
            } else {
                paint.color = Color.parseColor("#5D7A5C")
                canvas.drawRect(centerX - charWidth / 2, centerY - charHeight / 2,
                    centerX + charWidth / 2, centerY + charHeight / 2, paint)
            }

            // 绘制名字和技能
            paint.color = Color.BLACK
            paint.textSize = 24f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(currentChar?.name ?: "", centerX, centerY + charHeight / 2 + 40, paint)

            paint.color = Color.parseColor("#FF8C42")
            paint.textSize = 18f
            canvas.drawText(currentChar?.ability ?: "", centerX, centerY + charHeight / 2 + 70, paint)

            // 绘制技能说明
            paint.color = Color.GRAY
            paint.textSize = 16f
            canvas.drawText(currentChar?.desc ?: "", centerX, (screenHeight - 60).toFloat(), paint)

            // 开始游戏按钮
            paint.color = Color.parseColor("#FF8C42")
            paint.style = Paint.Style.FILL
            val btnWidth = 200f
            val btnHeight = 50f
            val btnX = centerX - btnWidth / 2
            val btnY = screenHeight - 110f
            canvas.drawRoundRect(btnX, btnY, btnX + btnWidth, btnY + btnHeight, 25f, 25f, paint)

            paint.color = Color.WHITE
            paint.textSize = 24f
            canvas.drawText("开始游戏", centerX, btnY + btnHeight / 2 + 8, paint)
        }

        private fun drawGame(canvas: Canvas) {
            // 玩家
            val char = characters.getOrNull(selectedCharacter)
            if (char?.bitmap != null) {
                val src = Rect(0, 0, char.bitmap!!.width, char.bitmap!!.height)
                val dst = Rect(playerX.toInt(), playerY.toInt(), (playerX + playerSize).toInt(), (playerY + playerSize).toInt())
                canvas.drawBitmap(char.bitmap!!, src, dst, null)
            } else {
                paint.color = Color.parseColor("#5D7A5C")
                canvas.drawRect(playerX, playerY, playerX + playerSize, playerY + playerSize, paint)
                paint.color = Color.WHITE
                canvas.drawCircle(playerX + playerSize * 0.7f, playerY + playerSize * 0.3f, playerSize * 0.1f, paint)
                paint.color = Color.BLACK
                canvas.drawCircle(playerX + playerSize * 0.72f, playerY + playerSize * 0.28f, playerSize * 0.05f, paint)
            }

            // 分数
            paint.color = Color.BLACK
            paint.textSize = 28f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Score: $score", 20f, 70f, paint)

            // 技能状态
            char?.let {
                paint.textSize = 16f
                paint.color = Color.parseColor("#FF8C42")
                canvas.drawText("${it.name}·${it.ability}", 20f, 110f, paint)

                when (it.id) {
                    1 -> canvas.drawText("免疫: ${if (knightUsed) "已使用" else "可用"}", 20f, 140f, paint)
                    2 -> canvas.drawText("打飞: ${kangarooHitCount}/$kangarooMaxHits", 20f, 140f, paint)
                    3 -> {
                        if (explosionActive) {
                            val remainingSeconds = (explosionTimer / 60) + 1
                            canvas.drawText("💥 爆炸状态 - 滞空中 - 剩余 ${remainingSeconds}s", 20f, 140f, paint)
                        } else if (explosionUsed) {
                            canvas.drawText("💣 炸弹: 已使用", 20f, 140f, paint)
                        } else {
                            canvas.drawText("💣 炸弹: 可用", 20f, 140f, paint)
                        }
                    }
                    4 -> canvas.drawText("通关进度: $vipClearedCount/$VIP_WIN_COUNT", 20f, 140f, paint)
                }
            }
        }

        private fun drawWinScreen(canvas: Canvas) {
            paint.color = Color.argb(180, 0, 0, 0)
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)

            paint.color = Color.parseColor("#FFD700")
            paint.textSize = 60f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("🎉 VICTORY! 🎉", screenWidth / 2f, screenHeight / 2f - 50, paint)

            paint.color = Color.WHITE
            paint.textSize = 32f
            canvas.drawText("Score: $score", screenWidth / 2f, screenHeight / 2f + 30, paint)

            paint.textSize = 24f
            canvas.drawText("点击屏幕返回角色选择", screenWidth / 2f, screenHeight / 2f + 100, paint)
        }

        private fun drawGameOver(canvas: Canvas) {
            paint.color = Color.argb(180, 0, 0, 0)
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)

            paint.color = Color.RED
            paint.textSize = 60f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("GAME OVER", screenWidth / 2f, screenHeight / 2f - 50, paint)

            paint.color = Color.WHITE
            paint.textSize = 32f
            canvas.drawText("Score: $score", screenWidth / 2f, screenHeight / 2f + 30, paint)

            paint.textSize = 24f
            canvas.drawText("点击屏幕重新开始", screenWidth / 2f, screenHeight / 2f + 100, paint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x

                    // 胜利或失败后点击重新开始
                    if (gameWin || gameOver) {
                        resetGame()
                        invalidate()
                        return true
                    }

                    // 角色选择界面
                    if (!gameStarted && isSelectingCharacter) {
                        // 检测是否点击了开始游戏按钮区域
                        val centerX = screenWidth / 2f
                        val btnWidth = 200f
                        val btnHeight = 50f
                        val btnX = centerX - btnWidth / 2
                        val btnY = screenHeight - 110f

                        if (event.x in btnX..btnX + btnWidth && event.y in btnY..btnY + btnHeight) {
                            gameStarted = true
                            isSelectingCharacter = false
                            invalidate()
                            return true
                        }

                        // 检测左右箭头点击
                        val centerY = (screenHeight * 0.3f + (screenHeight - screenHeight * 0.3f) / 2f)
                        val charWidth = playerSize * 1.5f

                        if (event.x < centerX - charWidth / 2 - 20) {
                            switchCharacter(-1)
                            return true
                        }
                        if (event.x > centerX + charWidth / 2 + 20) {
                            switchCharacter(1)
                            return true
                        }
                    }

                    // 游戏中：跳跃
                    if (gameStarted && !gameOver && !gameWin) {
                        performJump()
                        invalidate()
                    }
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    // 角色选择界面支持滑动切换
                    if (!gameStarted && isSelectingCharacter) {
                        val deltaX = event.x - lastTouchX
                        if (abs(deltaX) > 50) {
                            if (deltaX > 0) {
                                switchCharacter(-1)
                            } else {
                                switchCharacter(1)
                            }
                            lastTouchX = event.x
                        }
                    }
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        inner class GameThread(private val view: GameView) : Thread() {
            private var running = true

            fun stopGame() {
                running = false
            }

            override fun run() {
                while (running) {
                    try {
                        sleep(16)
                        view.update()
                        handler.post { view.invalidate() }
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }

            private val handler = Handler(Looper.getMainLooper())
        }
    }
}