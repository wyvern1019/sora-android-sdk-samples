package jp.shiguredo.sora.sample.ui

import android.annotation.TargetApi
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.facade.SoraAudioChannel
import jp.shiguredo.sora.sample.option.SoraStreamType
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import kotlinx.android.synthetic.main.activity_voice_chat_room.*
import org.webrtc.PeerConnection

class VoiceChatRoomActivity : AppCompatActivity() {

    companion object {
        private val TAG = VoiceChatRoomActivity::class.simpleName
    }

    private var channelName: String = ""

    private var audioCodec:  SoraAudioOption.Codec = SoraAudioOption.Codec.OPUS
    private var audioBitRate: Int? = null
    private var streamType   = SoraStreamType.BIDIRECTIONAL

    private var oldAudioMode: Int = AudioManager.MODE_INVALID

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setupWindow()

        setContentView(R.layout.activity_voice_chat_room)

        channelName = intent.getStringExtra("CHANNEL_NAME")

        audioCodec = SoraAudioOption.Codec.valueOf(
                intent.getStringExtra("AUDIO_CODEC"))

        audioBitRate = when (intent.getStringExtra("AUDIO_BIT_RATE")) {
            "UNDEFINED" -> null
            else -> intent.getStringExtra("AUDIO_BIT_RATE")?.toInt()
        }

        streamType = when (intent.getStringExtra("STREAM_TYPE")) {
            "BIDIRECTIONAL" -> SoraStreamType.BIDIRECTIONAL
            "SINGLE-UP"     -> SoraStreamType.SINGLE_UP
            "SINGLE-DOWN"   -> SoraStreamType.SINGLE_DOWN
            "MULTI-DOWN"    -> SoraStreamType.MULTI_DOWN
            else            -> SoraStreamType.BIDIRECTIONAL
        }

        channelNameText.text = channelName
        closeButton.setOnClickListener { close() }

        connectChannel()
    }

    private fun setupWindow() {
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
            or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setWindowVisibility()
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun setWindowVisibility() {
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    override fun onResume() {
        super.onResume()
        this.volumeControlStream = AudioManager.STREAM_VOICE_CALL

        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager
        oldAudioMode = audioManager.mode
        Log.d(TAG, "AudioManager mode change: ${oldAudioMode} => MODE_IN_COMMUNICATION(3)")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager
        Log.d(TAG, "AudioManager mode change: MODE_IN_COMMUNICATION(3) => ${oldAudioMode}")
        audioManager.mode = oldAudioMode
        close()
    }

    internal fun close() {
        Log.d(TAG, "close")
        disposeChannel()
        finish()
    }

    private var channel: SoraAudioChannel? = null
    private var channelListener: SoraAudioChannel.Listener = object : SoraAudioChannel.Listener {

        override fun onConnect(channel: SoraAudioChannel) {
            changeStateText("CONNECTED")
        }

        override fun onClose(channel: SoraAudioChannel) {
            changeStateText("CLOSED")
            close()
        }

        override fun onError(channel: SoraAudioChannel, reason: SoraErrorReason) {
            changeStateText("ERROR")
            Toast.makeText(this@VoiceChatRoomActivity, "Error: ${reason.name}", Toast.LENGTH_LONG).show()
            close()
        }

        override fun onAttendeesCountUpdated(channel: SoraAudioChannel, attendees: ChannelAttendeesCount) {
            Log.d(TAG, "onAttendeesCountUpdated")
        }

    }

    private fun connectChannel() {
        Log.d(TAG, "connectChannel")

        channel = SoraAudioChannel(
                context           = this,
                handler           = Handler(),
                signalingEndpoint = BuildConfig.SIGNALING_ENDPOINT,
                channelId         = channelName,
                signalingMetadata = "",
                audioCodec             = audioCodec,
                audioBitRate           = audioBitRate,
                streamType        = streamType,
                listener          = channelListener
        )
        channel!!.connect()
    }

    internal fun changeStateText(msg: String) {
        stateText.text = msg
    }

    private fun disposeChannel() {
        Log.d(TAG, "disposeChannel")
        channel?.dispose()
    }
}
