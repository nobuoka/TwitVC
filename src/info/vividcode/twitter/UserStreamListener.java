package info.vividcode.twitter;

public interface UserStreamListener {

	public void onStart();
	public void onStop( Exception exception );
	public void onProcess( String jsonStr );

}
