import { 
  init as initLyricPlayer, 
  toggleTranslation, 
  toggleRoma, 
  play, 
  pause, 
  stop, 
  setLyric, 
  setPlaybackRate,
  lyricLines,  // 歌词行列表（需在@/core/lyric中定义）
  currentLyricIndex  // 当前歌词索引（需在@/core/lyric中定义）
} from '@/core/lyric' 
import { updateSetting } from '@/core/common' 
import { onDesktopLyricPositionChange, showDesktopLyric, onLyricLinePlay, showRemoteLyric } from '@/core/desktopLyric' 
import playerState from '@/store/player/state' 
import { updateNowPlayingTitles } from '@/plugins/player/utils' 
import { setLastLyric } from '@/core/player/playInfo' 
import { state } from '@/plugins/player/playList' 


// 蓝牙歌词三行显示逻辑（上一句+当前句+下一句，当前句居中）
const updateRemoteLyric = async(lrc?: string) => {
  setLastLyric(lrc)
  
  // 获取上/当前/下句歌词（处理边界情况，避免索引越界）
  const prevLine = currentLyricIndex > 0 ? lyricLines[currentLyricIndex - 1]?.text || '' : ''
  const currentLine = lyricLines[currentLyricIndex]?.text || ''
  const nextLine = currentLyricIndex < lyricLines.length - 1 ? lyricLines[currentLyricIndex + 1]?.text || '' : ''
  
  // 拼接三行文本（用换行符分隔，车载系统会自动解析换行）
  const threeLineLyric = `${prevLine}\n${currentLine}\n${nextLine}`

  if (lrc == null) {
    void updateNowPlayingTitles(
      (state.prevDuration || 0) * 1000,
      playerState.musicInfo.name,
      `${playerState.musicInfo.name}${playerState.musicInfo.singer ? ` - ${playerState.musicInfo.singer}` : ''}`,
      playerState.musicInfo.album ?? ''
    )
  } else {
    void updateNowPlayingTitles(
      (state.prevDuration || 0) * 1000,
      threeLineLyric,  // 传入三行歌词
      `${playerState.musicInfo.name}${playerState.musicInfo.singer ? ` - ${playerState.musicInfo.singer}` : ''}`,
      playerState.musicInfo.album ?? ''
    )
  }
}


export default async(setting: LX.AppSetting) => {
  await initLyricPlayer()
  await Promise.all([
    setPlaybackRate(setting['player.playbackRate']),
    toggleTranslation(setting['player.isShowLyricTranslation']),
    toggleRoma(setting['player.isShowLyricRoma']),
  ])

  if (setting['desktopLyric.enable']) {
    showDesktopLyric().catch(() => {
      updateSetting({ 'desktopLyric.enable': false })
    })
  }
  if (setting['player.isShowBluetoothLyric']) {
    // 车载场景下强制启用远程歌词（适配CarWith/JoviCar）
    showRemoteLyric(true).catch(() => {
      updateSetting({ 'player.isShowBluetoothLyric': false })
    })
  }

  onDesktopLyricPositionChange(position => {
    updateSetting({
      'desktopLyric.position.x': position.x,
      'desktopLyric.position.y': position.y,
    })
  })

  onLyricLinePlay(({ text, extendedLyrics }) => {
    if (!text && !state.isPlaying) {
      void updateRemoteLyric()
    } else {
      void updateRemoteLyric(text)
    }
  })


  global.app_event.on('play', play)
  global.app_event.on('pause', pause)
  global.app_event.on('stop', stop)
  global.app_event.on('error', pause)
  global.app_event.on('musicToggled', stop)
  global.app_event.on('lyricUpdated', setLyric)
}
