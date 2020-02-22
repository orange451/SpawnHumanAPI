package spawnhuman.etc;

public class Ticks {
	
	/**
	 * Zero ticks.
	 */
	public static final long ZERO = 0;
	
	/**
	 * One tick.
	 */
	public static final long ONE = 1l;
	
	/**
	 * Two ticks.
	 */
	public static final long TWO = 2l;
	
	/**
	 * 20. One second in ticks.
	 */
	public static final long ONE_SECOND = fromSeconds(1);
	
	/**
	 * 10. Half of a second in ticks.
	 */
	public static final long HALF_SECOND = fromSeconds(0.5);
	
	/**
	 * 40. Two seconds in ticks.
	 */
	public static final long TWO_SECONDS = fromSeconds(2);

	/**
	 * Converts time measured in seconds to ticks.
	 * @param seconds
	 * @return
	 */
	public static long fromSeconds(double seconds) {
		return (long) (seconds * 20d);
	}
	
	/**
	 * Converts time measured in millisecond to ticks.
	 * @param millis
	 * @return
	 */
	public static long fromMilliseconds(long millis) {
		return (long) (millis / 50d);
	}
	
	/**
	 * Converts time measured in ticks to seconds.
	 * @param ticks
	 * @return
	 */
	public static double toSeconds(long ticks) {
		return ticks / 20d;
	}
	
	/**
	 * Converts time measured in ticks to milliseconds.
	 * @param ticks
	 * @return
	 */
	public static long toMilliseconds(long ticks) {
		return ticks * 50;
	}
}
