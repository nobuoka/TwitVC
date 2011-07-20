package info.vividcode.app.twitvc;

import info.vividcode.app.twitvc.TextPanelIncludingLinks.LinkPosition;
import info.vividcode.twitter.CredentialManager.Credential;
import info.vividcode.util.json.JsonArray;
import info.vividcode.util.json.JsonObject;
import info.vividcode.util.json.JsonSerializer;
import info.vividcode.util.json.JsonValue;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import com.sun.awt.AWTUtilities;

public class StatusDisplayingComponent implements AppComponent {
	
	private ComponentUtils mUtils;
	private Random mRand;
	private ProfileImageManager mProfileImageManager;
	private Credential mAccessCredentials;
	
	@Override
	public void initialize( ComponentUtils utils, ComponentRegister register ) {
		mUtils = utils;
		mRand  = new SecureRandom();
		mProfileImageManager = new ProfileImageManager();
		register.registEventHandler( new RecieveStatusEventListener(), 
				"info.vividcode.twitvc.RECIEVE_USER_STREAM_JSON" );
		AppComponentEventHandler ccel = new ChangeConfigEventListener();
		register.registEventHandler( ccel, 
				"info.vividcode.twitvc.CHANGE_CONFIG" );
		ccel.listenEvent( null );
	}
	
	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
	
	private String decodeXmlEntityReference( String src ) {
		StringBuilder sb = new StringBuilder();
		char[] cs = src.toCharArray();
		for( int i = 0, len = cs.length; i < len; ++i ) {
			if( cs[i] != '&' ) {
				sb.append( cs[i] );
			} else {
				if( i + 3 < cs.length && cs[i+2] == 't' && cs[i+3] == ';' ) {
					if( cs[i+1] == 'l' ) {
						sb.append( '<' );
						i += 3;
					} else if( cs[i+1] == 'g' ) {
						sb.append( '>' );
						i += 3;
					} else {
						// TODO エラーとか出す？
						sb.append( '&' );
					}
				} else if( i + 4 < cs.length && cs[i+1] == 'a' && cs[i+2] == 'm' &&
						cs[i+3] == 'p' && cs[i+4] == ';' ) {
					sb.append( '&' );
					i += 4;
				} else {
					sb.append( '&' );
				}
			}
		}
		return sb.toString();
	}
	
	private boolean mMouseDeleterNeedsShift = true;
	private class ChangeConfigEventListener implements AppComponentEventHandler {
		@Override
		public void listenEvent( AppEvent evt ) {
			mAccessCredentials = Config.getAccessCredentials();
			Boolean b = Config.getPreferenceBoolean( "info.vividcode.twitvc.StatusWindowDeleterNeedsShift" );
			if( b == null ) b = true;
			mMouseDeleterNeedsShift = b;
		}
	}
	
	private class RecieveStatusEventListener implements AppComponentEventHandler {
		private int[][] mRecentStatusWindowPoss;
		private int mRecentStatusWindowPossIndex = 0;
		private int[] mProbabilities;
		public RecieveStatusEventListener() {
			 mRecentStatusWindowPoss = new int[5][2];
			 for( int[] pos : mRecentStatusWindowPoss ) {
				 pos[0] = -5000;
				 pos[1] = -5000;
			 }
			 mProbabilities = new int[1000];
		}
		@Override
		public synchronized void listenEvent( AppEvent evt ) {
			Object[] data = evt.getEventData();
			if( data.length < 1 ) {
				throw new IllegalArgumentException( "イベントオブジェクトが保持するデータが変" );
			} else if( ! ( data[0] instanceof JsonValue ) ) {
				throw new IllegalArgumentException( "イベントオブジェクトが保持するデータが変" + data[0].getClass()  );
			}
			JsonValue json = (JsonValue) data[0];
			final JsonValue jsonv = json;
			SwingUtilities.invokeLater( new Runnable() {
				@Override public void run() {
					JsonObject json = jsonv.objectValue();
					// JSON 解析
					boolean isStatus = ( json.get( "text" ) != null );
					if( isStatus ) {
						boolean isRetweet = false;
						boolean isMentionToMe = false;
						String statusIdStr = null;
						String text = null;
						String screenName = null;
						String retweeterScreenName = null;
						String profileImageUrl = null;
						if( json.get( "retweeted_status" ) != null ) {
							isRetweet = true;
							JsonObject j = json.get( "retweeted_status" ).objectValue();
							statusIdStr = j.get( "id_str" ).stringValue();
							text = j.get( "text" ).stringValue();
							screenName = j.get( "user" ).objectValue().get( "screen_name" ).stringValue();
							profileImageUrl = j.get( "user" ).objectValue().get( "profile_image_url" ).stringValue();
							retweeterScreenName = json.get( "user" ).objectValue().get( "screen_name" ).stringValue();
							System.out.println( "[INFO] (RT) screen_name : " + screenName + ", profile_image_url : " + profileImageUrl );
						} else {
							statusIdStr = json.get( "id_str" ).stringValue();
							text = json.get( "text" ).stringValue();
							screenName = json.get( "user" ).objectValue().get( "screen_name" ).stringValue();
							profileImageUrl = json.get( "user" ).objectValue().get( "profile_image_url" ).stringValue();
							System.out.println( "[INFO] screen_name : " + screenName + ", profile_image_url : " + profileImageUrl );
						}
						text = decodeXmlEntityReference( text );
						// Mention かどうか
						// entities/user_mentions[]/id or id_str or screen_name
						JsonArray userMentions = json.get("entities").objectValue().get("user_mentions").arrayValue();
						for( JsonValue v : userMentions ) {
							// TODO
							if( mAccessCredentials != null && mAccessCredentials.getUserIdStr().equals( 
									v.objectValue().get("id_str").stringValue() ) ) {
								isMentionToMe = true;
								System.out.println( "IS MENTION TO ME!!" );
							}
						}
						// URL 一覧
						// entities/urls[]/url
						// entities/urls[]/indices[0]
						// entities/urls[]/indices[1]
						// RT の場合 : retweeted_status/entities/...
						List<LinkPosition> linkPositionList = null;
						JsonArray urls = null;
						if( isRetweet ) {
							urls = json.get( "retweeted_status" ).objectValue().get( "entities" ).objectValue().get( "urls" ).arrayValue();
						} else {
							urls = json.get( "entities" ).objectValue().get( "urls" ).arrayValue();
						}
						if( urls.size() != 0 ) {
							linkPositionList = new ArrayList<LinkPosition>();
							for( JsonValue v : urls ) {
								JsonObject vobj = v.objectValue();
								String url = vobj.get( "url" ).stringValue();
								int s = vobj.get( "indices" ).arrayValue().get( 0 ).numberValue().intValue();
								int e = vobj.get( "indices" ).arrayValue().get( 1 ).numberValue().intValue();
								linkPositionList.add( new LinkPosition( s, e, url ) );
							}
						}
						// GUI 追加
						StatusWindow win = null;
						try {
							win = mUtils.createJWindow( StatusWindow.class );
						} catch( Exception ex ) {
							// TODO 例外処理どうしよ
							ex.printStackTrace();
						}
						win.setStatusIdStr( statusIdStr );
						win.setJsonStr( JsonSerializer.serialize( json, true ) );
						//JPanel statusPanel = new JPanel();
						StatusWindow.MainPanel mainPanel = win.getMainPanel();
						//ProfileImagePanel imagePanel = new ProfileImagePanel();
						TextPanelIncludingLinks c1 = new TextPanelIncludingLinks( 
								text, mRand.nextInt( 30 ) + 275, linkPositionList );
						// JWindow の透明化
				        // Determine if the GraphicsDevice supports translucency.
				        //If translucent windows aren't supported, exit.
				        if( AWTUtilities.isTranslucencySupported( AWTUtilities.Translucency.TRANSLUCENT ) ) {
				        	AWTUtilities.setWindowOpacity( win, 0.85f );
				        	/*
				        	//AWTUtilities.setWindowShape( win, new Ellipse2D.Double( 0,0,win.getWidth(),win.getHeight() ) );        // It is best practice to set the window's shape in
				            // the componentResized method.  Then, if the window
				            // changes size, the shape will be correctly recalculated.
				            win.addComponentListener( new ComponentAdapter() {
				                // Give the window an elliptical shape.
				                // If the window is resized, the shape is recalculated here.
				                @Override
				                public void componentResized( ComponentEvent evt ) {
				                	//setShape(new Ellipse2D.Double(0,0,getWidth(),getHeight()));
				                	JWindow win = (JWindow) evt.getComponent();
						        	//AWTUtilities.setWindowShape( win, 
						        	//		new Ellipse2D.Double( 0,0,win.getWidth(),win.getHeight() ) );        // It is best practice to set the window's shape in
				                }
				            } );
				            */
				        }
						//System.out.println( "0..." + hrange + ": " + hpos + ", 0..." + vrange + ": " + vpos );
						
						//statusPanel.add( imagePanel );
						mainPanel.setProfileImage( mProfileImageManager.getImageByUrl( profileImageUrl ) );
						//statusPanel.add( c1 );
						TextPanel namePanel = new TextPanel( 
								screenName + ( isRetweet ? " (RT by " + retweeterScreenName + ")" : "" ), 200 );
						mainPanel.setNamePanel( namePanel );
						mainPanel.setTextPanel( c1 );
						//mProfileImageManager.getImageByUrl( profileImageUrl, imagePanel );
						
						//win.getContentPane().add( statusPanel );
						win.setMaxWidth( mRand.nextInt( 30 ) + 275 );
						win.pack();
						win.setAlwaysOnTop( true );
						win.addMouseListener( new StatusWindowDClickListener( 
										"http://twitter.com/" + screenName + 
										"/status/" + statusIdStr, statusIdStr, screenName ) );
						win.addMouseListener( c1.getMouseInputAdapter() );
						win.addMouseMotionListener( c1.getMouseInputAdapter() );
						// 表示位置決定
						{
							int winHeight = win.getHeight();
							Dimension size = c1.getPreferredSize();
							int hsize = size.width;
							int vsize = size.height;
							int vrange = 600 - vsize;
							vrange = ( vrange < 0 ? 0 : vrange ) + 1;
							if( mProbabilities.length <= vrange ) {
								// TODO
								mProbabilities = new int[vrange + 1];
							}
							Arrays.fill( mProbabilities, 1 );
							for( int[] pos : mRecentStatusWindowPoss ) {
								if( pos[0] >= 0 ) {
									int th = Math.min( pos[1] - pos[0], winHeight ) / 2;
									int s = pos[0] - winHeight;
									if( s < 0 ) s = 0;
									int s2 = pos[0] - th;
									if( s2 < 0 ) s2 = 0;
									int e = pos[1];
									if( vrange < e ) e = vrange;
									int e2 = pos[1] - th;
									if( vrange < e2 ) e2 = vrange;
									for( int i = s; i <= e; ++ i ) {
										mProbabilities[i] += 2;
									}
									for( int i = s2; i <= e2; ++ i ) {
										mProbabilities[i] += 2;
									}
								}
							}
							int sum = 0;
							for( int i = 0; i <= vrange; ++ i ) {
								mProbabilities[i] = 100 / mProbabilities[i];
								sum += mProbabilities[i];
							}
							int rand = mRand.nextInt( sum );
							System.out.println( "rand : " + rand );
							int vpos = 0;
							while( rand > 0 ) {
								rand -= mProbabilities[vpos];
								vpos += 1;
							}
							mRecentStatusWindowPoss[mRecentStatusWindowPossIndex][0] = vpos;
							mRecentStatusWindowPoss[mRecentStatusWindowPossIndex][1] = vpos + winHeight;
							mRecentStatusWindowPossIndex += 1;
							if( mRecentStatusWindowPoss.length <= mRecentStatusWindowPossIndex ) {
								mRecentStatusWindowPossIndex = 0;
							}
							win.setLocation( 0, vpos + 300 );
						}
						// 表示
						win.setVisible( true );
						if( isMentionToMe ) {
							win.setBoundable( true );
							win.setDisposedTime( new Date().getTime() + 40000 + text.codePointCount( 0, text.length() ) * 200 );
						} else {
							win.setDisposedTime( new Date().getTime() + 10000 + text.codePointCount( 0, text.length() ) * 200 );
						}
						// マウスが触れると消去
						win.addMouseListener( new MouseAdapter() {
							private boolean mIsCompact;
							@Override
							public void mouseClicked( MouseEvent evt ) {
								StatusWindow win = ( (StatusWindow) evt.getSource() );
								if( mIsCompact ) {
									win.returnFromCompact();
									mIsCompact = false;
								}
								System.out.println( "debug here" );
							}
							@Override
							public void mouseEntered( MouseEvent evt ) {
								StatusWindow win = ( (StatusWindow) evt.getSource() );
								boolean reqDel;
								if( mMouseDeleterNeedsShift ) {
									reqDel = evt.isShiftDown();
								} else {
									reqDel = ! evt.isShiftDown();
								}
								System.out.println( reqDel );
								if( ! reqDel ) {
									if( mIsCompact ) {
										win.returnFromCompact();
										mIsCompact = false;
									}
									return;
								}
								//} else 
								if( ! mIsCompact ) {
									//Long oldDisposedTime = win.getDisposedTime();
									//win.setDisposedTime( -1 );
									win.compact();
									mIsCompact = true;
									/*
									new DataHoldableThread( new Object[] { win } ) {
										@Override public void run() {
											try {
												Thread.sleep( 5000 );
											} catch( InterruptedException ex ) {
												ex.printStackTrace();
											}
											SwingUtilities.invokeLater( new DataHoldableRunner( new Object[] { mDataArray[0] } ) {
												@Override public void run() {
													StatusWindow win = (StatusWindow) mDataArray[0];
													win.returnFromCompact();
													mIsCompact = false;
												}
											} );
										}
									}.start();
									*/
								}
							}
						} );
					}
				}
			} );
		}
	}
	
	/**
	 * Status 表示用ウィンドウをダブルクリックした際に, 予め指定されていた URL をブラウザで
	 * 開くためのクラス.
	 * @author nobuoka
	 */
	private class StatusWindowDClickListener extends MouseAdapter {
		private String mUrlStr;
		private String mStatusIdStr;
		private String mScreenName;
		public StatusWindowDClickListener( String url, String statusIdStr, String screenName ) {
			mUrlStr = url;
			mStatusIdStr = statusIdStr;
			mScreenName = screenName;
		}
		@Override
		public void mouseClicked( MouseEvent evt ) {
			if( evt.getClickCount() == 2 ) {
				//System.out.println( "[TODO] open " + mUrlStr + " by a web browser" );
				try {
					Desktop.getDesktop().browse( new URI( mUrlStr ) );
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if( evt.getClickCount() == 1 ) {
				if( ( evt.getModifiersEx() & InputEvent.CTRL_DOWN_MASK ) != 0 ) {
					// TODO
					//mStatusInputFieldController.setIdStrReplyTo( mStatusIdStr, mScreenName );
					mUtils.raiseEvent( new AppEvent( "info.vividcode.twitvc.REQUEST_TO_REPLY", 
							new Object[]{ mStatusIdStr, mScreenName } ) );
				}
			}
		}
	}
	
	private static class ProfileImagePanel extends JPanel 
	implements ProfileImageManager.ImageGettingListener {
		private static final long serialVersionUID = -7925243159358127561L;
		Image mImage;
		int mImageSize;
		public ProfileImagePanel() {
			mImage = null;
			mImageSize = 36;
		}
		@Override
		public void onGetImage( Image image ) {
			mImage = image;
			this.repaint(); // イベントディスパッチスレッド
		}
		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );
			if( mImage != null ) {
				g.drawImage( mImage, 0, 0, mImageSize, mImageSize, this );
			}
		}
		@Override
		public Dimension getPreferredSize() {
			return new Dimension( mImageSize, mImageSize );
		}
		
	}
	
}
