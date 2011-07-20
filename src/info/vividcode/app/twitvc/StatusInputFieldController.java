package info.vividcode.app.twitvc;

import javax.swing.SwingUtilities;

import info.vividcode.twitter.CredentialManager.Credential;
import info.vividcode.twitter.StatusUpdater;

public class StatusInputFieldController {
	
	//private Credential mCredential;
	private StatusInputFieldPanel mPanel;
	
	StatusInputFieldController() {
		//mCredential = c;
		mPanel = new StatusInputFieldPanel( this );
	}
	
	public StatusInputFieldPanel getPanel() {
		// TODO
		return mPanel;
	}
	
	public void tweet( String text, String idInReplyTo ) throws Exception {
		assert ! SwingUtilities.isEventDispatchThread();
		System.out.println( "tweet! : " + text );
		Credential credential = Config.getAccessCredentials();
		StatusUpdater.update( text, credential, idInReplyTo );
	}
	
	public void setIdStrReplyTo( final String statusIdStr, final String screenName ) {
		assert ! SwingUtilities.isEventDispatchThread();
		SwingUtilities.invokeLater( new Runnable() {
			@Override public void run() {
				mPanel.setIdInReplyTo( statusIdStr, screenName );
			}
		} );
	}
	
}
