package tech.fastj.systems.audio;

import tech.fastj.math.Maths;

import tech.fastj.logging.Log;

import tech.fastj.systems.audio.state.PlaybackState;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;

/**
 * Class for playing {@link MemoryAudio memory audio}.
 *
 * @author Andrew Dey
 * @since 1.5.0
 */
public class MemoryAudioPlayer {

    /** See {@link MemoryAudio#play()}. */
    static void playAudio(MemoryAudio audio) {
        Clip clip = audio.getAudioSource();

        if (clip.isOpen()) {
            Log.warn(MemoryAudioPlayer.class, "Tried to play audio file \"{}\", but it was already open (and likely being used elsewhere.)", audio.getAudioPath().toString());
            return;
        }

        try {
            AudioInputStream audioInputStream = audio.getAudioInputStream();
            clip.open(audioInputStream);

            AudioManager.fireAudioEvent(
                    audio,
                    new LineEvent(
                            audio.getAudioSource(),
                            LineEvent.Type.OPEN,
                            audio.getAudioSource().getLongFramePosition()
                    )
            );

            playOrLoopAudio(audio);
        } catch (LineUnavailableException exception) {
            throw new IllegalStateException(
                    "No audio lines were available to load the file \"" + audio.getAudioPath().toAbsolutePath() + "\" as a MemoryAudio.",
                    exception
            );
        } catch (IOException exception) {
            throw new IllegalStateException("IO read error while trying to open \"" + audio.getAudioPath().toAbsolutePath() + "\".", exception);
        }
    }

    /** See {@link MemoryAudio#pause()}. */
    static void pauseAudio(MemoryAudio audio) {
        Clip clip = audio.getAudioSource();

        if (!clip.isOpen()) {
            Log.warn(MemoryAudioPlayer.class, "Tried to pause audio file \"{}\", but it wasn't being played.", audio.getAudioPath().toString());
            return;
        }

        clip.stop();

        audio.previousPlaybackState = audio.currentPlaybackState;
        audio.currentPlaybackState = PlaybackState.Paused;

        AudioManager.fireAudioEvent(
                audio,
                new LineEvent(
                        audio.getAudioSource(),
                        LineEvent.Type.STOP,
                        audio.getAudioSource().getLongFramePosition()
                )
        );
    }

    /** See {@link MemoryAudio#resume()}. */
    static void resumeAudio(MemoryAudio audio) {
        Clip clip = audio.getAudioSource();

        if (!clip.isOpen()) {
            Log.warn(MemoryAudioPlayer.class, "Tried to resume audio file \"{}\", but it wasn't being played.", audio.getAudioPath().toString());
            return;
        }

        playOrLoopAudio(audio);
    }

    /** See {@link MemoryAudio#stop()}. */
    static void stopAudio(MemoryAudio audio) {
        Clip clip = audio.getAudioSource();

        if (!clip.isOpen()) {
            Log.warn(MemoryAudioPlayer.class, "Tried to stop audio file \"{}\", but it wasn't being played.", audio.getAudioPath().toString());
            return;
        }

        clip.stop();

        clip.flush();
        clip.close();

        audio.previousPlaybackState = audio.currentPlaybackState;
        audio.currentPlaybackState = PlaybackState.Stopped;

        AudioManager.fireAudioEvent(
                audio,
                new LineEvent(
                        audio.getAudioSource(),
                        LineEvent.Type.STOP,
                        audio.getAudioSource().getLongFramePosition()
                )
        );

        AudioManager.fireAudioEvent(
                audio,
                new LineEvent(
                        audio.getAudioSource(),
                        LineEvent.Type.CLOSE,
                        audio.getAudioSource().getLongFramePosition()
                )
        );
    }

    /** See {@link MemoryAudio#seek(long)}. */
    static void seekInAudio(MemoryAudio audio, long timeChange) {
        Clip clip = audio.getAudioSource();

        if (clip.isActive()) {
            Log.warn(MemoryAudioPlayer.class, "Tried to change the playback position of audio file \"{}\", but it was still running.", audio.getAudioPath().toString());
            return;
        }

        long timeChangeInMilliseconds = TimeUnit.MICROSECONDS.convert(timeChange, TimeUnit.MILLISECONDS);
        clip.setMicrosecondPosition(clip.getMicrosecondPosition() + timeChangeInMilliseconds);
    }

    /** See {@link MemoryAudio#setPlaybackPosition(long)}. */
    static void setAudioPlaybackPosition(MemoryAudio audio, long playbackPosition) {
        Clip clip = audio.getAudioSource();

        if (clip.isActive()) {
            Log.warn(MemoryAudioPlayer.class, "Tried to set the playback position of audio file \"{}\", but it was still running.", audio.getAudioPath().toString());
            return;
        }

        long playbackPositionInMilliseconds = TimeUnit.MICROSECONDS.convert(playbackPosition, TimeUnit.MILLISECONDS);
        clip.setMicrosecondPosition(playbackPositionInMilliseconds);
    }

    /** See {@link MemoryAudio#rewindToBeginning()}. */
    static void rewindAudioToBeginning(MemoryAudio audio) {
        Clip clip = audio.getAudioSource();

        if (clip.isActive()) {
            Log.warn(MemoryAudioPlayer.class, "Tried to rewind audio file \"{}\", but it was still running.", audio.getAudioPath().toString());
            return;
        }

        clip.setMicrosecondPosition(0L);
    }

    /**
     * Plays -- or loops -- an {@link MemoryAudio} object.
     *
     * @param audio The audio to play/loop.
     */
    private static void playOrLoopAudio(MemoryAudio audio) {
        Clip clip = audio.getAudioSource();

        if (audio.shouldLoop()) {
            int clipFrameCount = clip.getFrameLength();
            int denormalizedLoopStart = denormalizeLoopStart(audio.getLoopStart(), clipFrameCount);
            int denormalizedLoopEnd = denormalizeLoopEnd(audio.getLoopEnd(), clipFrameCount);

            clip.setLoopPoints(denormalizedLoopStart, denormalizedLoopEnd);
            clip.loop(audio.getLoopCount());
        } else {
            clip.start();
        }

        audio.previousPlaybackState = audio.currentPlaybackState;
        audio.currentPlaybackState = PlaybackState.Playing;

        AudioManager.fireAudioEvent(
                audio,
                new LineEvent(
                        audio.getAudioSource(),
                        LineEvent.Type.START,
                        audio.getAudioSource().getLongFramePosition()
                )
        );
    }

    private static int denormalizeLoopStart(float normalizedLoopStart, int clipFrameCount) {
        if (normalizedLoopStart == MemoryAudio.LoopFromStart) {
            return MemoryAudio.LoopFromStart;
        }

        return (int) Maths.denormalize(normalizedLoopStart, 0f, clipFrameCount);
    }

    private static int denormalizeLoopEnd(float normalizedLoopEnd, int clipFrameCount) {
        if (normalizedLoopEnd == MemoryAudio.LoopAtEnd) {
            return MemoryAudio.LoopAtEnd;
        }

        return (int) Maths.denormalize(normalizedLoopEnd, 0f, clipFrameCount);
    }
}