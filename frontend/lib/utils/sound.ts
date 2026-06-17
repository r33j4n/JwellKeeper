/**
 * Play a sound notification
 * @param type - Type of sound: 'error' (beep), 'success' (chime), or 'warning' (alert)
 */
export function playSound(type: 'error' | 'success' | 'warning' = 'error') {
  try {
    const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    const oscillator = audioContext.createOscillator();
    const gainNode = audioContext.createGain();

    oscillator.connect(gainNode);
    gainNode.connect(audioContext.destination);

    const now = audioContext.currentTime;

    switch (type) {
      case 'error': {
        // Double beep - low frequency
        oscillator.frequency.setValueAtTime(400, now);
        oscillator.frequency.setValueAtTime(300, now + 0.1);
        gainNode.gain.setValueAtTime(0.3, now);
        gainNode.gain.setValueAtTime(0, now + 0.2);
        oscillator.start(now);
        oscillator.stop(now + 0.2);
        break;
      }
      case 'success': {
        // Two ascending tones
        oscillator.frequency.setValueAtTime(523.25, now); // C5
        oscillator.frequency.setValueAtTime(659.25, now + 0.15); // E5
        gainNode.gain.setValueAtTime(0.2, now);
        gainNode.gain.setValueAtTime(0, now + 0.3);
        oscillator.start(now);
        oscillator.stop(now + 0.3);
        break;
      }
      case 'warning': {
        // Triple alert tone
        oscillator.frequency.setValueAtTime(600, now);
        gainNode.gain.setValueAtTime(0.25, now);
        gainNode.gain.setValueAtTime(0, now + 0.15);
        oscillator.start(now);
        oscillator.stop(now + 0.15);

        // Second beep
        const osc2 = audioContext.createOscillator();
        osc2.connect(gainNode);
        osc2.frequency.setValueAtTime(600, now + 0.2);
        gainNode.gain.setValueAtTime(0.25, now + 0.2);
        gainNode.gain.setValueAtTime(0, now + 0.35);
        osc2.start(now + 0.2);
        osc2.stop(now + 0.35);
        break;
      }
    }
  } catch {
    // Fallback: use HTML5 Audio API if Web Audio API fails
    try {
      const beep = new Audio('data:audio/wav;base64,UklGRiYAAABXQVZFZm10IBAAAAABAAEAQB8AAAB9AAACABAAZGF0YQIAAAAAAAA=');
      beep.play().catch(() => {
        // Audio playback not supported
      });
    } catch {
      // Silently fail if audio is not supported
    }
  }
}
