package info.vividcode.app.twitvc;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JWindow;

public interface ComponentUtils {
	
	public void showMessageDialog( String message );
	
	public JWindow createJWindow();
	public <T extends JWindow> T createJWindow( Class<T> c ) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException;
	
	public void raiseEvent( AppEvent evt );
	
	public void makeMainFrameActive();
	
}
