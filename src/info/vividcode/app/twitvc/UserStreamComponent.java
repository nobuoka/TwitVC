package info.vividcode.app.twitvc;

import info.vividcode.twitter.CredentialManager.Credential;
import info.vividcode.twitter.StreamProcessingThread;
import info.vividcode.twitter.UserStreamListener;
import info.vividcode.util.json.JsonObject;
import info.vividcode.util.json.JsonParser;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class UserStreamComponent 
implements UserStreamListener, AppComponent, AppComponentPanelManager {
	
	private JPanel mUserStreamPanel;
	private Credential mCred;
	//private Credential mCredential;
	private StatusInputFieldController mStatusInputFieldController;
	private boolean mIsDisposed = false;
	
	private ComponentUtils mUtils;
	
	/**
	 *UserStream を扱うスレッドが終了した際に呼び出される.
	 *例外発生により停止した場合も, 通常通り停止した場合もこのメソッド.
	 *例外発生かどうかは, 引数により判断.
	 */
	@Override
	public void onStop( final Exception ex ) {
		// ここはイベントディスパッチスレッドではない
		assert ! SwingUtilities.isEventDispatchThread();
		System.out.println( "onStop" );
		mUserStreamProcessingThread = null;
		// Disposed されているなら見た目を変更する必要はない
		if( mIsDisposed ) {
			System.out.println( "disposed..." );
			return;
		}
		SwingUtilities.invokeLater( new Thread() {
			@Override
			public void run() {
				mUserStreamPanel.remove( mButtonToStopUS );
				mUserStreamPanel.add( mButtonToStartUS, BorderLayout.PAGE_END );
				mButtonToStartUS.setEnabled( true );
				mUserStreamPanel.validate();
				mUserStreamPanel.repaint();
				if( ex != null ) {
					mUtils.showMessageDialog(  
							"例外が発生したため UserStream が停止しました\n" +
							"[例外メッセージ] " + ex.getMessage() );
					System.err.println( "[DEBUG] stop with error" );
					ex.printStackTrace();
				}
				System.out.println( "...debug!! 1" );
			}
		} );
		System.out.println( "...debug!! 2" );
	}
	
	/**
	 *UserStream を扱うスレッドが開始された際に呼び出される.
	 */
	@Override
	public void onStart() {
		assert ! SwingUtilities.isEventDispatchThread();
		System.out.println( "onStart (UserStream)" );
		SwingUtilities.invokeLater( new Thread() {
			@Override
			public void run() {
				mUserStreamPanel.remove( mButtonToStartUS );
				mUserStreamPanel.add( mButtonToStopUS, BorderLayout.PAGE_END );
				mButtonToStopUS.setEnabled( true );
				mUserStreamPanel.validate();
				mUserStreamPanel.repaint();
			}
		} );
	}
	
	/**
	 *UserStream を扱うスレッドが, UserStream からデータを受け取った際に呼び出される.
	 */
	@Override
	public void onProcess( String jsonStr ) {
		// TODO ここはイベントディスパッチスレッドなのか？
		System.out.println( "onProcess : " + jsonStr );
		putStatus( jsonStr );
	}
	
	private JButton mButtonToStartUS;
	private JButton mButtonToStopUS;
	
	private void initGui() {
		JPanel usp = new JPanel( new BorderLayout() );
		mButtonToStartUS = new JButton( "UserStream 開始" );
		mButtonToStartUS.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent evt ) {
				if( startUserStream() ) {
					JButton b = (JButton) evt.getSource();
					b.setEnabled( false );
				}
			}
		} );
		mButtonToStopUS  = new JButton( "UserStream 停止" );
		mButtonToStopUS.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent evt ) {
				stopUserStream();
				JButton b = (JButton) evt.getSource();
				b.setEnabled( false );
			}
		} );
		mStatusInputFieldController = new StatusInputFieldController();
		JPanel cpanel = mStatusInputFieldController.getPanel();
		usp.add( cpanel, BorderLayout.CENTER );
		usp.add( mButtonToStartUS, BorderLayout.PAGE_END );
		mUserStreamPanel = usp;
	}
	
	private void putStatus( String jsonStr ) {
		JsonObject json = JsonParser.parse( jsonStr ).objectValue();
		mUtils.raiseEvent( new AppEvent( "info.vividcode.twitvc.RECIEVE_USER_STREAM_JSON", new Object[]{ json } ) );
	}
	
	private StreamProcessingThread mUserStreamProcessingThread;
	private boolean startUserStream() {
		if( mUserStreamProcessingThread != null ) {
			// TODO
			throw new RuntimeException();
		}
		mCred = Config.getAccessCredentials();
		if( mCred == null ) {
			mUtils.showMessageDialog( "OAuth の access credentials がありませんので, UserStream を使用できません. " +
					"まずは OAuth の access credentials を取得してください." );
			return false;
		} else {
			mUserStreamProcessingThread = new StreamProcessingThread( this, mCred );
			mUserStreamProcessingThread.start();
			return true;
		}
	}
	
	private void stopUserStream() {
		mUserStreamProcessingThread.requestStop();
	}
	
	@Override
	public void initialize( ComponentUtils utils, ComponentRegister register ) {
		assert utils != null;
		mUtils = utils;
		initGui();
		register.registPanelManager( this, "TL 管理パネル" );
		// 返信要請イベントを受ける
		register.registEventHandler( new AppComponentEventHandler() {
			@Override
			public void listenEvent( AppEvent evt ) {
				Object[] data = evt.getEventData();
				if( data.length < 2 ) {
					throw new IllegalArgumentException();
				} else if( ! ( data[0] instanceof String && data[1] instanceof String ) ) {
					throw new IllegalArgumentException();
				}
				mStatusInputFieldController.setIdStrReplyTo( (String) data[0], (String) data[1] );
				SwingUtilities.invokeLater( new Runnable() {
					@Override public void run() {
						mUtils.makeMainFrameActive();
					}
				} );
			}
		}, "info.vividcode.twitvc.REQUEST_TO_REPLY" );
	}
	
	@Override
	public void dispose() {
		mIsDisposed = true;
		// TODO : マルチスレッドのこと考えないと
		if( mUserStreamProcessingThread != null ) {
			stopUserStream();
		}
	}
	
	@Override
	public JPanel getPanel() {
		return mUserStreamPanel;
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
