package info.vividcode.app.twitvc;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import info.vividcode.twitter.CredentialManager;
import info.vividcode.twitter.CredentialManager.Credential;

public class AuthComponent implements AppComponent, AppComponentPanelManager {
	
	private ComponentUtils mUtils;
	private Credential mTempCredential;
	private CredentialManager mCredentialManager;
	private JPanel mAuthPanel;
	
	private class UrlPanelMouseEventListener extends MouseAdapter {
		private URI mUri;
		public UrlPanelMouseEventListener( String urlStr ) throws URISyntaxException {
			mUri = new URI( urlStr );
		}
		@Override
		public void mouseClicked( java.awt.event.MouseEvent evt ) {
			try {
				Desktop.getDesktop().browse( mUri );
			} catch( IOException ex ) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			}
		};
	}
	
	private class VerifierInputFieldEventListener extends KeyAdapter {
		private Credential mCred;
		public VerifierInputFieldEventListener( Credential cred ) {
			mCred = cred;
		}
		@Override
		public void keyPressed( KeyEvent evt ) {
			if( evt.getKeyCode() == KeyEvent.VK_ENTER ) {
				JTextField tf = (JTextField) evt.getSource();
				showWaiterEDT();
				mAuthPanel.validate();
				mAuthPanel.repaint();
				final String tfText = tf.getText();
				new Thread() {
					@Override
					public void run() {
						AuthComponent.this.obtainAccCred( mCred, tfText );
					}
				}.start();
			}
		}
	}
	
	public JPanel getAuthPanel() {
		return mAuthPanel;
	}
	
	private void obtainTempCred() {
		mTempCredential = mCredentialManager.requestTemporaryCredential();
		String authUrlStr = mCredentialManager.getAuthorizationUrl( mTempCredential );
		showVerifierRequest( mTempCredential, authUrlStr );
	}
	
	private void obtainAccCred( Credential tempCred, String verifier ) {
		// イベントディスパッチスレッドではない
		Credential c = mCredentialManager.requestAccessCredential( tempCred, verifier );
		System.out.println( "Access Credentials obtained!! : " + c );
		if( c != null ) {
			// 正常に取得
			showMessage( "OAuth access credentials を取得しました!!" );
			Config.putAccessCredentials( c );
			mUtils.raiseEvent( new AppEvent( "info.vividcode.twitvc.CHANGE_CONFIG", null ) );
		} else {
			// エラー？
			showMessage( "OAuth access credentials の取得に失敗しました..." );
		}
	}
	
	/**
	 * "開始" ボタンがある状態を表示する. 
	 * イベントディスパッチスレッド以外から呼び出すこと. 
	 */
	private void showStarter() {
		SwingUtilities.invokeLater( new Runnable() {
			@Override public void run() {
				showStarterEDT();
				mAuthPanel.validate();
				mAuthPanel.repaint();
			}
		} );
	}
	
	private void showStarterEDT() {
		mAuthPanel.removeAll();
		JButton button = new JButton( "OAuth credentials 取得開始" );
		button.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent evt ) {
				new Thread() {
					@Override public void run() {
						obtainTempCred();
					}
				}.start();
				showWaiterEDT();
				mAuthPanel.validate();
				mAuthPanel.repaint();
			}
		} );
		mAuthPanel.add( button );
	}
	
	/**
	 * "開始" ボタンがある状態を表示する. 
	 * イベントディスパッチスレッド以外から呼び出すこと. 
	 */
	private void showVerifierRequest( final Credential cred, final String url ) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override public void run() {
				showVerifierRequestEDT( cred, url );
				mAuthPanel.validate();
				mAuthPanel.repaint();
			}
		} );
	}
	
	private void showVerifierRequestEDT( Credential cred, String url ) {
		mAuthPanel.removeAll();
		JPanel tp = new TextPanel( url, 200 );
		try {
			tp.addMouseListener( new UrlPanelMouseEventListener( url ) );
		} catch( URISyntaxException ex ) {
			// TODO ここにはこないはずだが... きたらどうする？
			ex.printStackTrace();
		}
		mAuthPanel.add( tp );
		JTextField tf = new JTextField( 15 );
		tf.addKeyListener( new VerifierInputFieldEventListener( cred ) );
		mAuthPanel.add( tf );
	}
	
	/**
	 * "開始" ボタンがある状態を表示する. 
	 * イベントディスパッチスレッド以外から呼び出すこと. 
	 */
	private void showMessage( final String message ) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override public void run() {
				showMessageEDT( message );
				mAuthPanel.validate();
				mAuthPanel.repaint();
			}
		} );
	}
	
	private void showMessageEDT( String message ) {
		mAuthPanel.removeAll();
		mAuthPanel.add( new TextPanel( message, 200 ) );
	}
	
	private void showWaiterEDT() {
		mAuthPanel.removeAll();
		mAuthPanel.add( new TextPanel( "お待ちください...", 200 ) );
	}
	
	@Override
	public void initialize( ComponentUtils utils, ComponentRegister register ) {
		mUtils = utils;
		mCredentialManager = new CredentialManager();
		SwingUtilities.invokeLater( new Runnable() {
			@Override public void run() {
				mAuthPanel = new JPanel();
				mAuthPanel.add( new TextPanel( "test", 300 ) );
				showStarterEDT();
			}
		} );
		register.registPanelManager( this, "OAuth 認証パネル" );
	}
	
	@Override
	public void dispose() {
		// TODO 処理中のスレッドを止める必要がある
	}
	
	@Override
	public JPanel getPanel() {
		return mAuthPanel;
	}
	
	@Override
	public boolean show() {
		return true;
	}
	
	@Override
	public boolean hidden() {
		return true;
	}
	
}
