package cn.toside.music.mobile.lyric;

import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class LyricModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext reactContext;
    Lyric lyric;
    
    boolean isShowTranslation = false;
    boolean isShowRoma = false;
    float playbackRate = 1;
    private MediaSession mediaSession;
    private int listenerCount = 0;

    public LyricModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        initializeMediaSession();
    }

    @Override
    public String getName() {
        return "LyricModule";
    }

    private void initializeMediaSession() {
        try {
            mediaSession = new MediaSession(reactContext, "LXMusicCarSession");
            mediaSession.setActive(true);
            mediaSession.setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | 
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
            );
            Log.i("LyricModule", "MediaSession初始化成功");
        } catch (Exception e) {
            Log.e("LyricModule", "MediaSession初始化失败", e);
        }
    }

    @ReactMethod
    public void addListener(String eventName) {
        if (listenerCount == 0) {
            // Set up any upstream listeners or background tasks as necessary
        }
        listenerCount += 1;
    }

    @ReactMethod
    public void removeListeners(Integer count) {
        listenerCount -= count;
        if (listenerCount == 0) {
            // Remove upstream listeners, stop unnecessary background tasks
        }
    }

    // 原有的歌词方法保持不变
    @ReactMethod
    public void showDesktopLyric(ReadableMap data, Promise promise) {
        if (lyric == null) lyric = new Lyric(reactContext, isShowTranslation, isShowRoma, playbackRate);
        lyric.showDesktopLyric(Arguments.toBundle(data), promise);
    }

    @ReactMethod
    public void hideDesktopLyric(Promise promise) {
        if (lyric != null) lyric.hideDesktopLyric();
        promise.resolve(null);
    }

    @ReactMethod
    public void setSendLyricTextEvent(boolean isSend, Promise promise) {
        if (lyric == null) lyric = new Lyric(reactContext, isShowTranslation, isShowRoma, playbackRate);
        lyric.setSendLyricTextEvent(isSend);
        promise.resolve(null);
    }

    @ReactMethod
    public void setLyric(String lyric, String translation, String romaLyric, Promise promise) {
        Log.d("Lyric", "设置歌词: " + (lyric != null ? lyric.substring(0, Math.min(50, lyric.length())) : "null"));
        
        // 新增：触发ContentCatcher内容捕获
        triggerContentCatcherForLyrics();
        
        if (this.lyric != null) this.lyric.setLyric(lyric, translation, romaLyric);
        promise.resolve(null);
    }

    @ReactMethod
    public void setPlaybackRate(float playbackRate, Promise promise) {
        this.playbackRate = playbackRate;
        if (lyric != null) lyric.setPlaybackRate(playbackRate);
        promise.resolve(null);
    }

    @ReactMethod
    public void toggleTranslation(boolean isShowTranslation, Promise promise) {
        this.isShowTranslation = isShowTranslation;
        if (lyric != null) lyric.toggleTranslation(isShowTranslation);
        promise.resolve(null);
    }

    @ReactMethod
    public void toggleRoma(boolean isShowRoma, Promise promise) {
        this.isShowRoma = isShowRoma;
        if (lyric != null) lyric.toggleRoma(isShowRoma);
        promise.resolve(null);
    }

    @ReactMethod
    public void play(int time, Promise promise) {
        Log.d("Lyric", "播放歌词: " + time);
        if (lyric != null) lyric.play(time);
        
        // 新增：播放时触发ContentCatcher捕获和发送车载事件
        triggerContentCatcherForLyrics();
        sendCarLyricEvent("onLyricLinePlay", "播放时间: " + time);
        
        promise.resolve(null);
    }

    @ReactMethod
    public void pause(Promise promise) {
        Log.d("Lyric", "播放暂停");
        if (lyric != null) lyric.pauseLyric();
        
        // 新增：暂停时发送事件
        sendCarLyricEvent("onLyricLinePlay", "播放暂停");
        
        promise.resolve(null);
    }

    @ReactMethod
    public void toggleLock(boolean isLock, Promise promise) {
        if (lyric != null) {
            if (isLock) {
                lyric.lockLyric();
            } else {
                lyric.unlockLyric();
            }
        }
        promise.resolve(null);
    }

    @ReactMethod
    public void setColor(String unplayColor, String playedColor, String shadowColor, Promise promise) {
        if (lyric != null) lyric.setPlayedColor(unplayColor, playedColor, shadowColor);
        promise.resolve(null);
    }

    @ReactMethod
    public void setAlpha(float alpha, Promise promise) {
        if (lyric != null) lyric.setAlpha(alpha);
        promise.resolve(null);
    }

    @ReactMethod
    public void setTextSize(float size, Promise promise) {
        if (lyric != null) lyric.setTextSize(size);
        promise.resolve(null);
    }

    @ReactMethod
    public void setMaxLineNum(int maxLineNum, Promise promise) {
        if (lyric != null) lyric.setMaxLineNum(maxLineNum);
        promise.resolve(null);
    }

    @ReactMethod
    public void setSingleLine(boolean singleLine, Promise promise) {
        if (lyric != null) lyric.setSingleLine(singleLine);
        promise.resolve(null);
    }

    @ReactMethod
    public void setShowToggleAnima(boolean showToggleAnima, Promise promise) {
        if (lyric != null) lyric.setShowToggleAnima(showToggleAnima);
        promise.resolve(null);
    }

    @ReactMethod
    public void setWidth(int width, Promise promise) {
        if (lyric != null) lyric.setWidth(width);
        promise.resolve(null);
    }

    @ReactMethod
    public void setLyricTextPosition(String positionX, String positionY, Promise promise) {
        if (lyric != null) lyric.setLyricTextPosition(positionX, positionY);
        promise.resolve(null);
    }

    @ReactMethod
    public void checkOverlayPermission(Promise promise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(reactContext)) {
            promise.reject(new Exception("Permission denied"));
        }
        promise.resolve(null);
    }

    @ReactMethod
    public void openOverlayPermissionActivity(Promise promise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(reactContext)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + reactContext.getApplicationContext().getPackageName()));
            reactContext.startActivityForResult(intent, 1, null);
        }
        promise.resolve(null);
    }

    // 新增：车载歌词相关方法
    @ReactMethod
    public void onLyricLinePlay(String lyric) {
        Log.i("LyricModule", "onLyricLinePlay: " + lyric);
        sendCarLyricEvent("onLyricLinePlay", lyric);
    }

    @ReactMethod
    public void setPlayingUcarInfo(int currentTime, String title, String artist, String album, String currentLyric) {
        Log.i("LyricModule", "setPlayingUcarInfo: " + title + " - " + artist + ", 时间: " + currentTime + ", 歌词: " + currentLyric);
        
        try {
            // 更新MediaSession元数据
            updateMediaMetadata(currentTime, title, artist, album, currentLyric);
            
            // 发送到React Native层用于调试
            sendUcarInfoToReactNative(currentTime, title, artist, album, currentLyric);
            
        } catch (Exception e) {
            Log.e("LyricModule", "设置车载信息失败", e);
        }
    }

    /**
     * 触发ContentCatcher捕获歌词内容
     */
    private void triggerContentCatcherForLyrics() {
        try {
            Log.d("LyricModule", "触发ContentCatcher歌词内容捕获");
        } catch (Exception e) {
            Log.e("LyricModule", "触发ContentCatcher捕获失败", e);
        }
    }

    /**
     * 发送歌词事件到车载系统
     */
    private void sendCarLyricEvent(String eventName, String lyric) {
        try {
            WritableMap params = Arguments.createMap();
            params.putString("lyric", lyric);
            params.putLong("timestamp", System.currentTimeMillis());
            
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
        } catch (Exception e) {
            Log.e("LyricModule", "发送车载歌词事件失败", e);
        }
    }

    /**
     * 更新MediaSession元数据
     */
    private void updateMediaMetadata(int currentTime, String title, String artist, String album, String currentLyric) {
        if (mediaSession == null) return;
        
        try {
            MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
            
            metadataBuilder
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 180000);
            
            Bundle extras = new Bundle();
            extras.putString("android.media.metadata.LYRICS", currentLyric);
            extras.putLong("android.media.metadata.LYRIC_TIME", currentTime);
            metadataBuilder.setExtras(extras);
            
            MediaMetadata metadata = metadataBuilder.build();
            mediaSession.setMetadata(metadata);
            
            Log.d("LyricModule", "MediaSession元数据更新成功");
            
        } catch (Exception e) {
            Log.e("LyricModule", "更新MediaSession元数据失败", e);
        }
    }

    /**
     * 发送ucar信息到React Native层（用于调试）
     */
    private void sendUcarInfoToReactNative(int currentTime, String title, String artist, String album, String currentLyric) {
        try {
            WritableMap params = Arguments.createMap();
            params.putInt("currentTime", currentTime);
            params.putString("title", title);
            params.putString("artist", artist);
            params.putString("album", album);
            params.putString("currentLyric", currentLyric);
            params.putLong("timestamp", System.currentTimeMillis());
            
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("setPlayingUcarInfo", params);
        } catch (Exception e) {
            Log.e("LyricModule", "发送ucar信息到React Native失败", e);
        }
    }
}
