package info.vividcode.app.twitvc;

public interface ComponentRegister {
	public void registPanelManager( AppComponentPanelManager manager, String panelName );
	public void registEventHandler( AppComponentEventHandler handler, String eventName );
}
