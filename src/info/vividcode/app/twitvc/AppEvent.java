package info.vividcode.app.twitvc;

public class AppEvent {
	
	private String mEventName;
	private Object[] mEventData;
	
	public AppEvent( String eventName, Object[] eventData ) {
		mEventName = eventName;
		mEventData = eventData;
	}
	
	public String getEventName() {
		return mEventName;
	}
	public Object[] getEventData() {
		return mEventData;
	}
	
}
