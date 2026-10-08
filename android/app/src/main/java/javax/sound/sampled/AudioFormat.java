package javax.sound.sampled;

/**
 * Minimal {@code javax.sound.sampled.AudioFormat} shim.
 *
 * <p>Android's {@code android.jar} does not ship the desktop {@code javax.sound}
 * hierarchy. Minecraft only uses this class as a plain data holder describing decoded
 * PCM layout, so a small stub is enough to keep {@code JOrbisAudioStream} and the
 * OpenAL bridge compiling. Audio playback goes through Android's own {@code AudioTrack}.
 */
public class AudioFormat {

   public static class Encoding {
      public static final Encoding PCM_SIGNED = new Encoding("PCM_SIGNED");
      public static final Encoding PCM_UNSIGNED = new Encoding("PCM_UNSIGNED");
      private final String name;

      public Encoding(final String name) {
         this.name = name;
      }

      public String toString() {
         return this.name;
      }
   }

   private final Encoding encoding;
   private final float sampleRate;
   private final int sampleSizeInBits;
   private final int channels;
   private final int frameSize;
   private final float frameRate;
   private final boolean bigEndian;

   public AudioFormat(final float sampleRate, final int sampleSizeInBits, final int channels, final boolean signed, final boolean bigEndian) {
      this(PCM_SIGNEDU(signed), sampleRate, sampleSizeInBits, channels, channels * (sampleSizeInBits / 8), sampleRate, bigEndian);
   }

   public AudioFormat(final Encoding encoding, final float sampleRate, final int sampleSizeInBits, final int channels, final int frameSize, final float frameRate, final boolean bigEndian) {
      this.encoding = encoding;
      this.sampleRate = sampleRate;
      this.sampleSizeInBits = sampleSizeInBits;
      this.channels = channels;
      this.frameSize = frameSize;
      this.frameRate = frameRate;
      this.bigEndian = bigEndian;
   }

   private static Encoding PCM_SIGNEDU(final boolean signed) {
      return signed ? Encoding.PCM_SIGNED : Encoding.PCM_UNSIGNED;
   }

   public Encoding getEncoding() {
      return this.encoding;
   }

   public float getSampleRate() {
      return this.sampleRate;
   }

   public int getSampleSizeInBits() {
      return this.sampleSizeInBits;
   }

   public int getChannels() {
      return this.channels;
   }

   public int getFrameSize() {
      return this.frameSize;
   }

   public float getFrameRate() {
      return this.frameRate;
   }

   public boolean isBigEndian() {
      return this.bigEndian;
   }

   public String toString() {
      return "AudioFormat[" + this.encoding + ", " + this.sampleRate + " Hz, " + this.sampleSizeInBits + " bits, " + this.channels + " ch]";
   }
}