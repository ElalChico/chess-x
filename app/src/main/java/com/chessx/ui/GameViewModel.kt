package com.chessx.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessx.core.*
import com.chessx.data.*
import com.chessx.ui.components.BoardSelection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject

data class UiGameState(
    val snapshot: BoardSnapshot,
    val selection: BoardSelection = BoardSelection(null),
    val localColor: Color = Color.WHITE,
    val localName: String = "Tú",
    val remoteName: String = "Rival",
    val remoteColor: Color = Color.BLACK,
    val isLocalTurn: Boolean = true,
    val inCheck: Boolean = false,
    val checkKing: Int? = null,
    val gameEnd: GameEnd? = null,
    val paused: Boolean = false,
    val pauseBy: String = "",
    val lastError: String? = null,
    val lastMoveSan: String = "",
    val history: List<String> = emptyList(),
    val clock: ClockState = ClockState(300000, 300000, Color.WHITE, false, false),
    val connected: Boolean = false,
    val lobby: LobbyState = LobbyState(),
    val soundEnabled: Boolean = true,
    val promotionPending: Pair<Int, Int>? = null, // from->to awaiting promotion choice
    val countdown: Int? = null, // null = jugando/terminado · 3..1 = cuenta regresiva en curso
    val tournament: TournamentState = TournamentState(),
    val sessionGames: Int = 0 // partidas jugadas en esta sesión (nueva partida)
)

/**
 * Very robust ViewModel: all engine mutations guarded, Bluetooth callbacks
 * marshalled to Main, no crash on disconnect/timeout/parse.
 */
class GameViewModel(private val appContext: Context) : ViewModel() {

    val engine = ChessEngine()
    lateinit var bluetooth: BluetoothClassicManager
    lateinit var clock: ClockManager
    lateinit var sound: SoundManager

    private val _ui = MutableStateFlow(UiGameState(engine.snapshot))
    val ui: StateFlow<UiGameState> = _ui.asStateFlow()

    /** Perfil del rival, recibido por Bluetooth (nombre, foto, historial resumido). */
    private val _remoteProfile = MutableStateFlow<RemoteProfile?>(null)
    val remoteProfile: StateFlow<RemoteProfile?> = _remoteProfile.asStateFlow()

    private var localName: String = "Jugador"
    private var remoteName: String = ""
    private var isHost: Boolean = false
    private var colorChosen: Boolean = false
    private var moveHistory = mutableListOf<String>()
    private var uciHistory = mutableListOf<String>()
    private var moveClockHistory = mutableListOf<Long>()
    private var pendingDrawOffer = false
    private var checkSignalEnabled: Boolean = true
    private var lastLocalColor: Color = Color.WHITE

    // Estados de diálogo personalizados
    private val _showGameEndDialog = MutableStateFlow(false)
    val showGameEndDialog: StateFlow<Boolean> = _showGameEndDialog.asStateFlow()

    private val _showPromotionDialog = MutableStateFlow(false)
    val showPromotionDialog: StateFlow<Boolean> = _showPromotionDialog.asStateFlow()

    private val _promotionPending = MutableStateFlow<Pair<Int, Int>?>(null)
    val promotionPending: StateFlow<Pair<Int, Int>?> = _promotionPending.asStateFlow()

    // Callbacks para diálogos
    var onGameEndOptionSameGame: (() -> Unit)? = null
    var onGameEndOptionRandomGame: (() -> Unit)? = null
    var onGameEndOptionTournament: (() -> Unit)? = null
    var onGameEndOptionContinue: (() -> Unit)? = null
    var onPromotionChoice: ((PieceType) -> Unit)? = null
    var onTournamentEndDialog: (() -> Unit)? = null

    fun init() {
        if (this::bluetooth.isInitialized) return
        bluetooth = BluetoothClassicManager(appContext, viewModelScope)
        sound = SoundManager(appContext)
        checkSignalEnabled = appContext.getSharedPreferences("chessbt_prefs", Context.MODE_PRIVATE).getBoolean("check_sig", true)
        clock = ClockManager(
            viewModelScope, isHost = false,
            onSyncRequest = { msg -> bluetooth.send(msg) },
            onLowTime = { side ->
                // Suena el aviso de tiempo bajo solo en el móvil cuyo turno está por agotarse
                if (side == _ui.value.localColor) sound.play(SoundType.LOWTIME)
            },
            onTimeout = { loser ->
                if (_ui.value.gameEnd != null) return@ClockManager
                val winner = loser.opposite()
                val end = GameEnd(winner, GameEndReason.TIMEOUT,
                    if (winner == _ui.value.localColor) "¡Tiempo agotado! Has ganado" else "Tiempo agotado. Ha ganado el rival")
                _ui.value = _ui.value.copy(gameEnd = end, clock = _ui.value.clock.copy(finished = true))
                sound.play(if (winner == _ui.value.localColor) SoundType.WIN else SoundType.LOSE)
                bluetooth.send(GameEndMessage(MessageParser.stringFromColor(winner), "TIMEOUT", end.message))
                clock.finish()
                saveGameRecord(end)
                recordTournamentResult(end)
                _showGameEndDialog.value = true
            }
        )
        observeBluetooth()
    }

    private fun observeBluetooth() {
        viewModelScope.launch {
            bluetooth.incoming.collect { msg ->
                if (msg == null) return@collect
                try { handleIncoming(msg) } catch (e: Exception) { /* never crash */ }
            }
        }
        viewModelScope.launch {
            bluetooth.state.collect { st ->
                val connected = st == BtState.CONNECTED
                _ui.value = _ui.value.copy(connected = connected,
                    lobby = _ui.value.lobby.copy(connected = connected))
                if (!connected) _remoteProfile.value = null
            }
        }
        viewModelScope.launch {
            clock.state.collect { cs ->
                _ui.value = _ui.value.copy(clock = cs)
            }
        }
    }

    // ---------- Lobby / setup ----------
    fun setLocalName(name: String) {
        localName = name.trim().ifBlank { (100000 + java.util.Random().nextInt(900000)).toString() }.take(20)
        _ui.value = _ui.value.copy(localName = localName, lobby = _ui.value.lobby.copy(localName = localName))
    }

    fun setIsHost(host: Boolean) {
        isHost = host
        if (isHost) colorChosen = false
        // Host is the clock authority; just switch the flag (no recreation, no leaked collectors).
        if (this::clock.isInitialized) clock.isHost = host
        _ui.value = _ui.value.copy(lobby = _ui.value.lobby.copy(isHost = host, colorRandom = true),
            localColor = if (host) Color.WHITE else Color.BLACK,
            remoteColor = if (host) Color.BLACK else Color.WHITE)
    }

    fun setTimeControl(timeMs: Long, incrementMs: Long) {
        if (!isHost) return
        _ui.value = _ui.value.copy(lobby = _ui.value.lobby.copy(timeMs = timeMs, incrementMs = incrementMs))
    }

    /** Solo el host elige el lado para ambos jugadores; el invitado refleja la elección. */
    fun setLocalColor(color: Color) {
        if (!isHost) return
        colorChosen = true
        _ui.value = _ui.value.copy(
            localColor = color,
            remoteColor = color.opposite(),
            lobby = _ui.value.lobby.copy(localColor = color, remoteColor = color.opposite(), colorRandom = false)
        )
        sendLobbyConfig()
    }

    /** Modo "color aleatorio": se decide al pulsar Comenzar (sólo host). */
    fun setRandomColor() {
        if (!isHost) return
        colorChosen = false
        _ui.value = _ui.value.copy(lobby = _ui.value.lobby.copy(colorRandom = true))
        sendLobbyConfig()
    }

    fun sendHello() {
        val color = if (isHost) "white" else "black"
        bluetooth.send(HelloMessage(localName, isHost, color))
    }

    /** Envía el perfil propio por SPP respetando la privacidad (foto / historial / PGN). */
    fun sendProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = AppPreferences(appContext)
                val records = GameHistoryStore.load(appContext)
                var wins = 0; var draws = 0; var losses = 0
                records.forEach { r -> when (r.localWon) { true -> wins++; false -> losses++; null -> draws++ } }
                val gamesJson = if (prefs.privacy.shareHistory) buildHistoryJson(records, prefs.privacy.sharePgn) else "[]"
                val photo = if (prefs.privacy.sharePhoto) downscaledPhotoBase64(prefs.profilePhotoBase64) else ""
                val color = if (_ui.value.lobby.colorRandom) "random" else MessageParser.stringFromColor(_ui.value.lobby.localColor)
                bluetooth.send(ProfileMessage(localName, color, photo, records.size, wins, draws, losses, gamesJson))
            } catch (_: Exception) { }
        }
    }

    /** JSON con el historial (máx 30 partidas). PGN incluida sólo si sharePgn. */
    private fun buildHistoryJson(records: List<GameRecord>, sharePgn: Boolean): String {
        return try {
            val arr = JSONArray()
            records.take(30).forEach { r ->
                val o = JSONObject()
                o.put("d", r.dateMs)
                o.put("res", r.result)
                r.localWon?.let { o.put("lw", it) }
                o.put("n", r.movesSan.size)
                o.put("t", r.timeLabel)
                o.put("me", r.playerName)
                o.put("opp", r.opponent)
                arr.put(o)
            }
            arr.toString()
        } catch (_: Exception) { "[]" }
    }

    /** Reduce la foto de perfil a <=128px JPEG para que quepa por SPP. */
    private fun downscaledPhotoBase64(b64: String): String {
        if (b64.isBlank()) return ""
        return try {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return ""
            val maxDim = 128
            val scale = maxDim.toFloat() / maxOf(bmp.width, bmp.height).coerceAtLeast(1)
            val small = if (scale < 1f) {
                Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt().coerceAtLeast(1), (bmp.height * scale).toInt().coerceAtLeast(1), true)
            } else bmp
            val out = java.io.ByteArrayOutputStream()
            small.compress(Bitmap.CompressFormat.JPEG, 55, out)
            "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) { "" }
    }

    private fun parseRemoteGames(json: String): List<RemoteGame> {
        return try {
            val arr = JSONArray(json)
            val out = mutableListOf<RemoteGame>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.add(RemoteGame(
                    dateMs = o.optLong("d", 0L),
                    result = o.optString("res", "1/2-1/2"),
                    won = if (o.has("lw")) o.optBoolean("lw") else null,
                    moves = o.optInt("n", 0),
                    timeLabel = o.optString("t", ""),
                    playerName = o.optString("me", ""),
                    opponent = o.optString("opp", "")
                ))
            }
            out
        } catch (_: Exception) { emptyList() }
    }

    private fun handleProfileMessage(msg: ProfileMessage) {
        _remoteProfile.value = RemoteProfile(
            name = msg.name.ifBlank { "Rival" },
            color = msg.color,
            photoBase64 = msg.photoBase64.ifBlank { null },
            total = msg.total,
            wins = msg.wins,
            draws = msg.draws,
            losses = msg.losses,
            games = parseRemoteGames(msg.gamesJson)
        )
    }

    fun sendLobbyConfig() {
        if (!isHost) return
        val lobby = _ui.value.lobby
        bluetooth.send(LobbyConfigMessage(
            hostName = localName,
            guestName = lobby.remoteName.ifBlank { "---" },
            timeMs = lobby.timeMs,
            incrementMs = lobby.incrementMs,
            hostColor = MessageParser.stringFromColor(lobby.localColor)
        ))
    }

    fun startGameAsHost() {
        if (!isHost) return
        if (_ui.value.remoteName.isBlank() || !_ui.value.connected) {
            showError("Conecta con el rival antes de comenzar")
            return
        }
        if (!colorChosen) {
            val randomColor = randomSide()
            _ui.value = _ui.value.copy(
                localColor = randomColor,
                remoteColor = randomColor.opposite(),
                lobby = _ui.value.lobby.copy(localColor = randomColor, remoteColor = randomColor.opposite())
            )
        }
        val lobby = _ui.value.lobby
        startGameInternal(lobby.localColor, lobby.timeMs, lobby.incrementMs)
    }

    /** Nueva partida tras terminar: "same" (mismo color y tiempo), "random" o "tournament". */
    fun requestNewGame(mode: String) {
        val lobby = _ui.value.lobby
        when (mode) {
            "same" -> if (isHost) startGameInternal(lastLocalColor, lobby.timeMs, lobby.incrementMs)
                      else bluetooth.send(RematchMessage("same", localName))
            "random" -> if (isHost) startGameInternal(randomSide(), lobby.timeMs, lobby.incrementMs)
                        else bluetooth.send(RematchMessage("random", localName))
            "tournament" -> if (isHost) advanceTournament() else bluetooth.send(RematchMessage("tournament", localName))
        }
    }

    /** Vuelve a la sala conservando la conexión Bluetooth. */
    fun backToRoom() {
        clock.stop()
        _ui.value = _ui.value.copy(
            gameEnd = null, paused = false, promotionPending = null, countdown = null,
            inCheck = false, checkKing = null, selection = BoardSelection(null),
            lobby = _ui.value.lobby.copy(gameStarted = false)
        )
    }

    private fun randomSide(): Color = if (kotlin.random.Random.nextBoolean()) Color.WHITE else Color.BLACK

    /** El anfitrión crea un torneo: N partidas seguidas con colores alternos. */
    fun startTournament(games: Int, timeMs: Long, incMs: Long) {
        if (!isHost) return
        if (_ui.value.remoteName.isBlank() || !_ui.value.connected) {
            showError("Conecta con el rival antes de crear el torneo")
            return
        }
        val hostWhite = if (colorChosen) _ui.value.localColor == Color.WHITE else kotlin.random.Random.nextBoolean()
        _ui.value = _ui.value.copy(
            tournament = TournamentState(
                config = TournamentConfig(games, timeMs, incMs),
                localColorWhite = hostWhite,
                active = true
            )
        )
        bluetooth.send(TournamentStartMessage(games, timeMs, incMs, hostWhite))
        startGameInternal(if (hostWhite) Color.WHITE else Color.BLACK, timeMs, incMs)
    }

    /** Siguiente partida del torneo: los colores se alternan. */
    private fun advanceTournament() {
        val t = _ui.value.tournament
        if (!t.active || t.finished) return
        val nextWhite = !t.localColorWhite
        _ui.value = _ui.value.copy(tournament = t.copy(localColorWhite = nextWhite))
        startGameInternal(if (nextWhite) Color.WHITE else Color.BLACK, t.config.timeMs, t.config.incrementMs)
    }

    private fun onTournamentStarted(msg: TournamentStartMessage) {
        _ui.value = _ui.value.copy(
            tournament = TournamentState(
                config = TournamentConfig(msg.games, msg.timeMs, msg.incrementMs),
                localColorWhite = !msg.hostColorWhite,
                active = true
            )
        )
    }

    /** Acumula el resultado en el torneo en curso y guarda el historial al terminar. */
    private fun recordTournamentResult(end: GameEnd) {
        val t = _ui.value.tournament
        if (!t.active) return
        val localWon = end.winner?.let { it == _ui.value.localColor }
        val nt = when (localWon) {
            true -> t.copy(localWins = t.localWins + 1, played = t.played + 1)
            false -> t.copy(remoteWins = t.remoteWins + 1, played = t.played + 1)
            null -> t.copy(draws = t.draws + 1, played = t.played + 1)
        }
        _ui.value = _ui.value.copy(tournament = nt, sessionGames = _ui.value.sessionGames + 1)
        if (nt.finished) {
            try {
                TournamentStore.append(appContext, TournamentRecord(
                    id = java.util.UUID.randomUUID().toString(),
                    dateMs = System.currentTimeMillis(),
                    opponent = remoteName.ifBlank { "Rival" },
                    games = nt.totalGames,
                    localWins = nt.localWins,
                    remoteWins = nt.remoteWins,
                    draws = nt.draws,
                    timeLabel = if (nt.config.timeMs == 0L) "Ilimitado"
                                else "${nt.config.timeMs / 60000} + ${nt.config.incrementMs / 1000}",
                    gameResults = emptyList()
                ))
            } catch (_: Exception) { }
            _ui.value = _ui.value.copy(tournament = nt.copy(active = false))
        }
    }

    private fun startGameInternal(myColor: Color, timeMs: Long, incMs: Long) {
        lastLocalColor = myColor
        engine.reset()
        moveHistory.clear()
        uciHistory.clear()
        moveClockHistory.clear()
        val fen = engine.toFEN()
        clock.configure(timeMs, incMs)
        _ui.value = _ui.value.copy(
            snapshot = engine.snapshot,
            localColor = myColor, remoteColor = myColor.opposite(),
            isLocalTurn = myColor == Color.WHITE, gameEnd = null, paused = false,
            promotionPending = null, countdown = null,
            inCheck = false, checkKing = null, selection = BoardSelection(null),
            history = emptyList(),
            lobby = _ui.value.lobby.copy(
                gameStarted = true, connected = true,
                localColor = myColor, remoteColor = myColor.opposite(),
                timeMs = timeMs, incrementMs = incMs, colorRandom = false,
                isHost = _ui.value.lobby.isHost // preserve host role
            )
        )
        if (isHost) {
            bluetooth.send(StartGameMessage(fen, timeMs, incMs,
                MessageParser.stringFromColor(myColor),
                hostName = localName, guestName = _ui.value.remoteName.ifBlank { "Rival" },
                hostTimestamp = System.currentTimeMillis()))
        }
        launchCountdown()
    }

    private fun onGameStartedAsGuest(msg: StartGameMessage) {
        engine.parseFEN(msg.startFen)
        moveHistory.clear()
        uciHistory.clear()
        moveClockHistory.clear()
        val hostColor = MessageParser.colorFromString(msg.hostColor)
        val local = hostColor.opposite()
        lastLocalColor = local
        clock.configure(msg.timeMs, msg.incrementMs)
        _ui.value = _ui.value.copy(
            snapshot = engine.snapshot,
            localColor = local, remoteColor = hostColor,
            localName = localName, remoteName = msg.hostName.ifBlank { "Anfitrión" },
            isLocalTurn = local == Color.WHITE,
            lobby = _ui.value.lobby.copy(gameStarted = true, connected = true, timeMs = msg.timeMs, incrementMs = msg.incrementMs, isHost = false),
            gameEnd = null, paused = false, countdown = null, promotionPending = null,
            inCheck = false, checkKing = null, selection = BoardSelection(null),
            history = emptyList()
        )
        launchCountdown()
    }

    /** Cuenta 3-2-1 visible en ambos móviles; el reloj arranca al terminar. */
    private fun launchCountdown() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(countdown = 3)
            for (n in 3 downTo 1) {
                _ui.value = _ui.value.copy(countdown = n)
                sound.play(when (n) { 3 -> SoundType.COUNTDOWN3; 2 -> SoundType.COUNTDOWN2; else -> SoundType.COUNTDOWN1 })
                delay(1000)
            }
            _ui.value = _ui.value.copy(countdown = null)
            clock.start()
            // El anfitrión sincroniza el reloj al arrancar para que ambos empiecen igual
            if (isHost) {
                val c = clock.state.value
                bluetooth.send(ClockSyncMessage(c.whiteMs, c.blackMs, "white", System.currentTimeMillis(), false))
            }
        }
    }

    // ---------- Moves ----------
    fun onSquareTap(sq: Int) {
        if (_ui.value.gameEnd != null || _ui.value.paused) { showError("Partida en pausa o terminada"); return }
        if (_ui.value.countdown != null) return // cuenta regresiva activa · no tocar
        if (_ui.value.promotionPending != null) return
        if (!isMyTurn()) return
        val sel = _ui.value.selection
        // If tapping own piece -> select
        val piece = engine.pieceAt(sq)
        if (piece != null && piece.color == _ui.value.localColor) {
            val targets = engine.legalMovesFrom(sq).map { it.to }.distinct()
            _ui.value = _ui.value.copy(selection = sel.copy(from = sq, legalTargets = targets, errorSquare = null))
            return
        }
        // If tapping legal target -> try move
        if (sel.from != null && sel.legalTargets.contains(sq)) {
            attemptMove(sel.from, sq)
            return
        }
        // Else clear + error if we had selection
        if (sel.from != null) {
            _ui.value = _ui.value.copy(
                selection = sel.copy(from = null, legalTargets = emptyList(), errorSquare = sq),
                lastError = "Movimiento ilegal"
            )
            sound.play(SoundType.ILLEGAL)
            viewModelScope.launch { delay(900); _ui.value = _ui.value.copy(selection = _ui.value.selection.copy(errorSquare = null), lastError = null) }
        } else if (piece != null && piece.color != _ui.value.localColor) {
            _ui.value = _ui.value.copy(selection = sel.copy(errorSquare = sq))
            sound.play(SoundType.ILLEGAL)
            viewModelScope.launch { delay(700); _ui.value = _ui.value.copy(selection = _ui.value.selection.copy(errorSquare = null)) }
        }
    }

    private fun isMyTurn(): Boolean = engine.sideToMove() == _ui.value.localColor

    private fun attemptMove(from: Int, to: Int, promotion: PieceType? = null) {
        val validation = engine.validateMove(from, to, promotion)
        if (validation != MoveValidation.OK) {
            when (validation) {
                MoveValidation.NO_PIECE -> showError("No hay pieza ahí")
                MoveValidation.WRONG_COLOR -> showError("No es tu pieza")
                else -> {
                    _ui.value = _ui.value.copy(lastError = "Movimiento ilegal", selection = _ui.value.selection.copy(errorSquare = to))
                    sound.play(SoundType.ILLEGAL)
                    viewModelScope.launch { delay(900); _ui.value = _ui.value.copy(selection = _ui.value.selection.copy(errorSquare = null, from = null, legalTargets = emptyList()), lastError = null) }
                }
            }
            return
        }
        // Promotion needs UI choice if pawn reaching last rank
        val piece = engine.pieceAt(from)
        if (piece?.type == PieceType.PAWN && (to / 8 == 7 || to / 8 == 0) && promotion == null) {
            _ui.value = _ui.value.copy(promotionPending = Pair(from, to))
            return
        }
        val move = engine.findMove(from, to, promotion) ?: run {
            showError("Error interno al mover")
            return
        }
        val san = engine.toSAN(move)
        val isCapture = move.isCapture
        engine.doMove(move)
        val fenAfter = engine.toFEN()
        moveHistory.add(san)
        val uci = move.uci()
        uciHistory.add(uci)
        // Clock: remaining of the mover AFTER the move (+increment)
        clock.onMovePlayed(_ui.value.localColor)
        runCatching {
            val cs = clock.state.value
            moveClockHistory.add(if (_ui.value.localColor == Color.WHITE) cs.whiteMs else cs.blackMs)
        }
        // Check/mate detection after move
        val inCheck = engine.isInCheck(engine.sideToMove())
        val end = engine.detectEnd()
        _ui.value = _ui.value.copy(
            snapshot = engine.snapshot,
            selection = BoardSelection(null, emptyList(), lastMove = Pair(from, to), checkKingSquare = if (inCheck && checkSignalEnabled) engine.findKing(engine.sideToMove()) else null),
            inCheck = inCheck,
            checkKing = if (inCheck && checkSignalEnabled) engine.findKing(engine.sideToMove()) else null,
            lastMoveSan = san,
            history = moveHistory.toList(),
            lastError = null
        )
        // Sound ONLY for my own pieces (local move)
        if (inCheck) sound.play(SoundType.CHECK) else if (isCapture) sound.play(SoundType.CAPTURE) else sound.play(SoundType.MOVE)
        bluetooth.send(MoveMessage(from, to, promotion?.name?.take(1) ?: "", san, uci, fenAfter, System.currentTimeMillis()))
        if (end != null) {
            _ui.value = _ui.value.copy(gameEnd = end)
            sound.play(SoundType.CHECKMATE)
            bluetooth.send(GameEndMessage(
                if (end.winner == null) "draw" else MessageParser.stringFromColor(end.winner),
                end.reason.name, end.message
            ))
            clock.finish()
            saveGameRecord(end)
            recordTournamentResult(end)
            _showGameEndDialog.value = true
        }
    }

    fun confirmPromotion(choice: PieceType) {
        val pending = _ui.value.promotionPending ?: return
        _ui.value = _ui.value.copy(promotionPending = null)
        attemptMove(pending.first, pending.second, choice)
    }
    fun cancelPromotion() { _ui.value = _ui.value.copy(promotionPending = null) }

    private fun showError(msg: String) {
        _ui.value = _ui.value.copy(lastError = msg)
        sound.play(SoundType.ILLEGAL)
        viewModelScope.launch { delay(1500); _ui.value = _ui.value.copy(lastError = null) }
    }

    // ---------- Remote ----------
    private fun handleIncoming(msg: BtMessage) {
        when (msg) {
            is HelloMessage -> {
                remoteName = msg.playerName.ifBlank { "Rival" }
                _ui.value = _ui.value.copy(remoteName = remoteName,
                    lobby = _ui.value.lobby.copy(remoteName = remoteName))
            }
            is LobbyConfigMessage -> {
                val hostColor = MessageParser.colorFromString(msg.hostColor)
                _ui.value = _ui.value.copy(
                    remoteName = msg.hostName.ifBlank { "Anfitrión" },
                    localName = _ui.value.localName,
                    localColor = hostColor.opposite(),
                    remoteColor = hostColor,
                    lobby = _ui.value.lobby.copy(
                        isHost = false,
                        timeMs = msg.timeMs, incrementMs = msg.incrementMs,
                        remoteName = msg.hostName,
                        localColor = hostColor.opposite(),
                        remoteColor = hostColor
                    )
                )
            }
            is StartGameMessage -> onGameStartedAsGuest(msg)
            is MoveMessage -> {
                try {
                    if (msg.from !in 0..63 || msg.to !in 0..63) return
                    val promo = when (msg.promotion) {
                        "Q" -> PieceType.QUEEN; "R" -> PieceType.ROOK; "B" -> PieceType.BISHOP; "N" -> PieceType.KNIGHT
                        else -> null
                    }
                    val move = engine.findMove(msg.from, msg.to, promo)
                    if (move != null) {
                        val san = msg.san.ifBlank { engine.toSAN(move) }
                        engine.doMove(move)
                        moveHistory.add(san)
                        uciHistory.add(msg.uci.ifBlank { move.uci() })
                        clock.onMovePlayed(_ui.value.remoteColor)
                        runCatching {
                            val cs = clock.state.value
                            moveClockHistory.add(if (_ui.value.remoteColor == Color.WHITE) cs.whiteMs else cs.blackMs)
                        }
                        val inCheck = engine.isInCheck(engine.sideToMove())
                        val end = engine.detectEnd()
                        _ui.value = _ui.value.copy(
                            snapshot = engine.snapshot,
                            selection = BoardSelection(null, emptyList(), lastMove = Pair(msg.from, msg.to), checkKingSquare = if (inCheck && checkSignalEnabled) engine.findKing(engine.sideToMove()) else null),
                            inCheck = inCheck, checkKing = if (inCheck && checkSignalEnabled) engine.findKing(engine.sideToMove()) else null,
                            lastMoveSan = san, history = moveHistory.toList()
                        )
                        // Rival pieces NEVER play move/capture sounds here; only check/mate alerts.
                        if (inCheck) sound.play(SoundType.CHECK)
                        if (end != null) {
                            _ui.value = _ui.value.copy(gameEnd = end)
                            sound.play(SoundType.CHECKMATE)
                            clock.finish()
                            saveGameRecord(end)
                            recordTournamentResult(end)
                            _showGameEndDialog.value = true
                        }
                    }
                } catch (e: Exception) { Log_w("move", e) }
            }
            is PauseMessage -> {
                _ui.value = _ui.value.copy(paused = msg.paused, pauseBy = msg.byName)
                clock.setPaused(msg.paused)
                sound.play(SoundType.PAUSE)
            }
            is DrawOfferMessage -> {
                pendingDrawOffer = true
                _ui.value = _ui.value.copy(lastError = "${msg.byName} ofrece tablas. Aceptar/Rechazar")
                sound.play(SoundType.CLICK)
            }
            is DrawResponseMessage -> {
                if (msg.accepted) {
                    val end = GameEnd(null, GameEndReason.DRAW_AGREED, "Tablas acordadas")
                    _ui.value = _ui.value.copy(gameEnd = end)
                    sound.play(SoundType.DRAW)
                    clock.finish()
                    saveGameRecord(end)
                    recordTournamentResult(end)
                    _showGameEndDialog.value = true
                } else {
                    showError("${msg.byName} rechaza tablas")
                }
            }
            is ResignMessage -> {
                val winner = MessageParser.colorFromString(msg.loserColor).opposite()
                val end = GameEnd(winner, GameEndReason.RESIGNATION, "${msg.byName} se rinde")
                _ui.value = _ui.value.copy(gameEnd = end)
                sound.play(if (winner == _ui.value.localColor) SoundType.WIN else SoundType.LOSE)
                clock.finish()
                saveGameRecord(end)
                recordTournamentResult(end)
                _showGameEndDialog.value = true
            }
            is ClockSyncMessage -> clock.applyRemoteSync(msg)
            is GameEndMessage -> {
                val winner = when (msg.winnerColor) {
                    "white" -> Color.WHITE; "black" -> Color.BLACK; else -> null
                }
                val reason = try { GameEndReason.valueOf(msg.reason) } catch (_: Exception) { GameEndReason.DRAW_AGREED }
                val end = GameEnd(winner, reason, msg.message.ifBlank { "Partida terminada" })
                _ui.value = _ui.value.copy(gameEnd = end)
                sound.play(SoundType.CHECKMATE)
                clock.finish()
                saveGameRecord(end)
                recordTournamentResult(end)
                _showGameEndDialog.value = true
            }
            is PingMessage -> bluetooth.send(PongMessage(msg.ts))
            is ProfileMessage -> handleProfileMessage(msg)
            is RematchMessage -> {
                if (!isHost) return
                when (msg.mode) {
                    "same" -> startGameInternal(lastLocalColor, _ui.value.lobby.timeMs, _ui.value.lobby.incrementMs)
                    "random" -> startGameInternal(randomSide(), _ui.value.lobby.timeMs, _ui.value.lobby.incrementMs)
                    "tournament" -> advanceTournament()
                }
            }
            is TournamentStartMessage -> onTournamentStarted(msg)
            is PongMessage -> { /* latency could be measured here */ }
            is ErrorMessage -> showError(msg.text)
        }
    }

    private fun saveGameRecord(end: GameEnd) {
        try {
            val localColor = _ui.value.localColor
            val result = when (end.winner) {
                null -> "1/2-1/2"
                localColor -> "1-0"
                else -> "0-1"
            }
            val localWon = if (end.winner == null) null else end.winner == localColor
            val opp = remoteName.ifBlank { "Rival" }
            val whiteName = if (localColor == Color.WHITE) localName else opp
            val blackName = if (localColor == Color.WHITE) opp else localName
            val lobby = _ui.value.lobby
            val tcLabel = if (lobby.timeMs == 0L) "∞ + 0" else "${lobby.timeMs / 60000} + ${lobby.incrementMs / 1000}"
            val base = GameRecord(
                opponent = opp,
                playerName = localName,
                result = result,
                localWon = localWon,
                localColorWhite = localColor == Color.WHITE,
                timeLabel = tcLabel,
                movesSan = moveHistory.toList(),
                pgn = "",
                finalFen = engine.toFEN(),
                uciMoves = uciHistory.toList(),
                moveClocks = moveClockHistory.toList()
            )
            GameHistoryStore.append(appContext, base.copy(pgn = buildPgn(base, whiteName, blackName)))
        } catch (_: Exception) { }
    }

    private fun Log_w(tag: String, e: Exception) { android.util.Log.w("VM", "handle $tag", e) }

    // ---------- Actions ----------
    fun togglePause() {
        val newPaused = !_ui.value.paused
        clock.setPaused(newPaused)
        _ui.value = _ui.value.copy(paused = newPaused, pauseBy = localName)
        bluetooth.send(PauseMessage(newPaused, localName))
        sound.play(SoundType.PAUSE)
    }

    fun offerDraw() {
        bluetooth.send(DrawOfferMessage(localName))
        showError("Oferta de tablas enviada")
    }

    fun respondDraw(accepted: Boolean) {
        bluetooth.send(DrawResponseMessage(accepted, localName))
        if (accepted) {
            val end = GameEnd(null, GameEndReason.DRAW_AGREED, "Tablas acordadas")
            _ui.value = _ui.value.copy(gameEnd = end)
            clock.finish()
            saveGameRecord(end)
            recordTournamentResult(end)
        }
    }

    fun resign() {
        val end = GameEnd(_ui.value.remoteColor, GameEndReason.RESIGNATION, "Te has rendido")
        _ui.value = _ui.value.copy(gameEnd = end)
        bluetooth.send(ResignMessage(localName, MessageParser.stringFromColor(_ui.value.localColor)))
        sound.play(SoundType.LOSE)
        clock.finish()
        saveGameRecord(end)
        recordTournamentResult(end)
    }

    fun toggleSound() {
        val newEnabled = !_ui.value.soundEnabled
        sound.setEnabled(newEnabled)
        _ui.value = _ui.value.copy(soundEnabled = newEnabled)
    }

    fun resetGame() {
        engine.reset()
        moveHistory.clear()
        _ui.value = _ui.value.copy(snapshot = engine.snapshot, selection = BoardSelection(null), gameEnd = null,
            history = emptyList(), isLocalTurn = _ui.value.localColor == Color.WHITE)
    }

    fun disconnect() {
        try { bluetooth.disconnect() } catch (_: Exception) {}
        try { clock.stop() } catch (_: Exception) {}
    }

    fun onClearedCleanup() {
        try { bluetooth.release() } catch (_: Exception) {}
        try { sound.release() } catch (_: Exception) {}
    }
}
