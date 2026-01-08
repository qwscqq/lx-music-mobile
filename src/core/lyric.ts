import {
  play as lrcPlay,
  setLyric as lrcSetLyric,
  pause as lrcPause,
  setPlaybackRate as lrcSetPlaybackRate,
  toggleTranslation as lrcToggleTranslation,
  toggleRoma as lrcToggleRoma,
  init as lrcInit,
} from '@/plugins/lyric'
import {
  playDesktopLyric,
  setDesktopLyric,
  pauseDesktopLyric,
  setDesktopLyricPlaybackRate,
  toggleDesktopLyricTranslation,
  toggleDesktopLyricRoma,
} from '@/core/desktopLyric'
import { getPosition } from '@/plugins/player'
import playerState from '@/store/player/state'
import { NativeModules } from 'react-native'

const { LyricModule } = NativeModules

// 车载歌词服务状态
let carLyricInterval: any = null
let currentLyrics: Array<{time: number, text: string}> = []
let currentMusicInfo: any = null

/**
 * 解析LRC歌词格式
 */
const parseLrc = (lrcText: string): Array<{time: number, text: string}> => {
  if (!lrcText) return []
  
  const lines = lrcText.split('\n')
  const lyrics: Array<{time: number, text: string}> = []
  
  const timePattern = /\[(\d+):(\d+)(?:[:\.](\d+))?\](.*)/
  
  lines.forEach(line => {
    const match = line.match(timePattern)
    if (match) {
      const minutes = parseInt(match[1])
      const seconds = parseInt(match[2])
      const milliseconds = match[3] ? parseInt(match[3]) : 0
      const time = minutes * 60000 + seconds * 1000 + milliseconds * 10
      const text = match[4].trim()
      
      if (text && !text.startsWith('[')) {
        lyrics.push({ time, text })
      }
    }
  })
  
  return lyrics.sort((a, b) => a.time - b.time)
}

/**
 * 获取当前时间对应的歌词行
 */
const getCurrentLyric = (currentTime: number): {current: string, next: string} => {
  if (currentLyrics.length === 0) {
    return { current: '', next: '' }
  }
  
  let currentIndex = -1
  for (let i = currentLyrics.length - 1; i >= 0; i--) {
    if (currentTime >= currentLyrics[i].time) {
      currentIndex = i
      break
    }
  }
  
  const currentLyric = currentIndex >= 0 ? currentLyrics[currentIndex].text : ''
  const nextLyric = currentIndex >= 0 && currentIndex + 1 < currentLyrics.length 
    ? currentLyrics[currentIndex + 1].text 
    : ''
  
  return { current: currentLyric, next: nextLyric }
}

/**
 * 发送歌词到车载系统
 */
const sendLyricToCarSystems = (currentTime: number, currentLyric: string, nextLyric: string = '') => {
  if (!LyricModule || !currentMusicInfo) return
  
  try {
    const musicInfo = currentMusicInfo
    
    // 调试日志
    console.log('🚗 发送车载歌词:', {
      time: currentTime,
      song: musicInfo.name,
      currentLyric: currentLyric.substring(0, 30) + (currentLyric.length > 30 ? '...' : '')
    })
    
    // 事件1：实时单行歌词（用于快速更新）
    LyricModule.onLyricLinePlay(currentLyric)
    
    // 事件2：完整车载信息（包含多行支持）
    LyricModule.setPlayingUcarInfo(
      Math.floor(currentTime),
      musicInfo.name || '',
      musicInfo.singer || '',
      musicInfo.album || '',
      currentLyric
    )
    
  } catch (error) {
    console.error('发送车载歌词失败:', error)
  }
}

/**
 * 启动车载歌词服务
 */
const startCarLyricService = () => {
  if (carLyricInterval) {
    clearInterval(carLyricInterval)
  }
  
  carLyricInterval = setInterval(async () => {
    if (!playerState.isPlay || !currentMusicInfo || currentLyrics.length === 0) {
      return
    }
    
    try {
      const position = await getPosition()
      const currentTime = Math.floor(position * 1000)
      const { current, next } = getCurrentLyric(currentTime)
      
      if (current) {
        sendLyricToCarSystems(currentTime, current, next)
      }
    } catch (error) {
      console.error('更新车载歌词失败:', error)
    }
  }, 500)
}

/**
 * 停止车载歌词服务
 */
const stopCarLyricService = () => {
  if (carLyricInterval) {
    clearInterval(carLyricInterval)
    carLyricInterval = null
  }
  currentLyrics = []
  currentMusicInfo = null
}

/**
 * 更新歌词数据
 */
export const updateLyricData = (lyricText: string, musicInfo: any) => {
  currentLyrics = parseLrc(lyricText)
  currentMusicInfo = musicInfo
}

/**
 * init lyric
 */
export const init = async() => {
  return lrcInit()
}

/**
 * set lyric
 * @param lyric lyric str
 * @param translation lyric translation
 */
const handleSetLyric = async(lyric: string, translation = '', romalrc = '') => {
  lrcSetLyric(lyric, translation, romalrc)
  await setDesktopLyric(lyric, translation, romalrc)
  
  // 新增：更新车载歌词数据
  if (playerState.musicInfo) {
    updateLyricData(lyric, playerState.musicInfo)
  }
}

/**
 * play lyric
 * @param time play time
 */
export const handlePlay = (time: number) => {
  lrcPlay(time)
  void playDesktopLyric(time)
  
  // 新增：启动车载歌词服务
  if (playerState.musicInfo && currentLyrics.length > 0) {
    startCarLyricService()
  }
}

/**
 * pause lyric
 */
export const pause = () => {
  lrcPause()
  void pauseDesktopLyric()
  
  // 新增：停止车载歌词服务
  stopCarLyricService()
}

/**
 * stop lyric
 */
export const stop = () => {
  void handleSetLyric('')
  stopCarLyricService()
}

/**
 * set playback rate
 * @param playbackRate playback rate
 */
export const setPlaybackRate = async(playbackRate: number) => {
  lrcSetPlayback率(playbackRate)
  await setDesktopLyricPlayback率(playbackRate)
  if (playerState.isPlay) {
    setTimeout(() => {
      void getPosition().then((position) => {
        handlePlay(position * 1000)
      })
    })
  }
}

/**
 * toggle show translation
 * @param isShowTranslation is show translation
 */
export const toggleTranslation = async(isShowTranslation: boolean) => {
  lrcToggleTranslation(isShowTranslation)
  await toggleDesktopLyricTranslation(isShowTranslation)
  if (playerState.isPlay) play()
}

/**
 * toggle show roma lyric
 * @param isShowLyricRoma is show roma lyric
 */
export const toggleRoma = async(isShowLyricRoma: boolean) => {
  lrcToggleRoma(isShowLyricRoma)
  await toggleDesktopLyricRoma(isShowLyricRoma)
  if (playerState.isPlay) play()
}

export const play = () => {
  void getPosition().then((position) => {
    handlePlay(position * 1000)
  })
}

export const setLyric = async() => {
  if (!playerState.musicInfo.id) return
  
  // 存储当前音乐信息
  currentMusicInfo = playerState.musicInfo
  
  if (playerState.musicInfo.lrc) {
    let tlrc = ''
    let rlrc = ''
    if (playerState.musicInfo.tlrc) tlrc = playerState.musicInfo.tlrc
    if (playerState.musicInfo.rlrc) rlrc = playerState.musicInfo.rlrc
    await handleSetLyric(playerState.musicInfo.lrc, tlrc, rlrc)
  }

  if (playerState.isPlay) play()
}
